package com.quserh.eorzeaphone.data

import android.content.Context
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class XivChatConnection(context: Context, private val scope: CoroutineScope, private val onEvent: (PhoneEvent) -> Unit, private val onSendFailure: () -> Unit = {}) {
    private val prefs = context.getSharedPreferences("eorzea_phone_connection", Context.MODE_PRIVATE)
    private val sodium = LazySodiumAndroid(SodiumAndroid())
    private var socket: Socket? = null
    @Volatile private var sessionOutput: DataOutputStream? = null
    @Volatile private var sessionTx: ByteArray? = null
    private var worker: Job? = null
    private val writing = Any()
    private val connected = AtomicBoolean(false)
    @Volatile private var reconnectEnabled = false

    fun isConnected(): Boolean = connected.get()

    fun connect(host: String, port: Int) {
        disconnect()
        reconnectEnabled = true
        worker = scope.launch(Dispatchers.IO) {
            var backoff = 2_000L
            while (reconnectEnabled && isActive) {
                runSession(host, port)
                if (!reconnectEnabled || !isActive) break
                kotlinx.coroutines.delay(backoff)
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }

    private suspend fun runSession(host: String, port: Int) {
        try {
            val keyPair = loadOrCreateKeyPair()
            val client = Socket()
            client.tcpNoDelay = true
            client.connect(InetSocketAddress(host.trim(), port), 8_000)
            socket = client
            val input = DataInputStream(BufferedInputStream(client.getInputStream()))
            val output = DataOutputStream(BufferedOutputStream(client.getOutputStream()))
            sessionOutput = output
            output.write(byteArrayOf(XivChatCodec.MAGIC_0.toByte(), XivChatCodec.MAGIC_1.toByte(), XivChatCodec.MAGIC_2.toByte()))
            output.write(keyPair.publicKey.asBytes)
            output.flush()
            val serverPublic = ByteArray(32)
            input.readFully(serverPublic)
            val remembered = prefs.getString("serverPublic", null)
            val serverHex = serverPublic.toHex()
            if (remembered != null && remembered != serverHex) {
                throw SecurityException("游戏插件公钥已变化，请在设置中清除信任后重试")
            }
            prefs.edit().putString("serverPublic", serverHex).apply()
            val (rx, tx) = XivChatCodec.deriveClientKeys(sodium, keyPair.publicKey.asBytes, keyPair.secretKey.asBytes, serverPublic)
            sessionTx = tx
            connected.set(true)
            onEvent(PhoneEvent.Connected)
            send(output, tx, 8, XivChatCodec.encodePreferences())
            // 角色资料到达后再按该角色游标补传；这里先拉最近 100 条（插件按当前角色过滤）
            send(output, tx, 4, XivChatCodec.encodeBacklog(100))
            send(output, tx, 6, XivChatCodec.encodePlayerList())
            send(output, tx, 6, XivChatCodec.encodePlayerList(1))

            while (!client.isClosed) {
                val frame = XivChatCodec.readSecret(input, sodium, rx)
                if (frame.isEmpty()) continue
                val code = frame[0].toInt() and 0xff
                val payload = frame.copyOfRange(1, frame.size)
                val unpacker = org.msgpack.core.MessagePack.newDefaultUnpacker(payload)
                try {
                    try {
                        when (code) {
                            1 -> Unit
                            2 -> { val m = XivChatCodec.readMessage(unpacker); onEvent(PhoneEvent.Chat(m)) }
                            3 -> return
                            4 -> if (payload.isEmpty()) {
                                onEvent(PhoneEvent.GameAvailability(false))
                            } else {
                                onEvent(PhoneEvent.Profile(XivChatCodec.readProfile(unpacker)))
                            }
                            5 -> onEvent(PhoneEvent.GameAvailability(XivChatCodec.readAvailability(unpacker)))
                            6 -> XivChatCodec.readChannel(unpacker).let { onEvent(PhoneEvent.Channel(it.first, it.second)) }
                            7 -> XivChatCodec.readBacklog(unpacker).forEach { m -> onEvent(PhoneEvent.Chat(m)) }
                            8 -> XivChatCodec.readPlayerList(unpacker).let { (type, players) ->
                                if (type == 1) onEvent(PhoneEvent.PartyList(players)) else onEvent(PhoneEvent.FriendList(players))
                            }
                            10 -> onEvent(PhoneEvent.Housing(XivChatCodec.readHousing(unpacker)))
                            11 -> onEvent(PhoneEvent.Inventory(XivChatCodec.readInventory(unpacker)))
                            12 -> onEvent(PhoneEvent.Wallet(XivChatCodec.readWallet(unpacker)))
                            13 -> onEvent(PhoneEvent.Weather(XivChatCodec.readWeather(unpacker)))
                            14 -> onEvent(PhoneEvent.Jobs(XivChatCodec.readJobs(unpacker)))
                            15 -> onEvent(PhoneEvent.Dailies(XivChatCodec.readDailies(unpacker)))
                            16 -> onEvent(PhoneEvent.Activity(XivChatCodec.readActivity(unpacker)))
                            17 -> onEvent(PhoneEvent.Collections(XivChatCodec.readCollections(unpacker)))
                            18 -> onEvent(PhoneEvent.Maps(XivChatCodec.readMaps(unpacker)))
                            19 -> onEvent(PhoneEvent.Fishing(XivChatCodec.readFishing(unpacker)))
                            20 -> onEvent(PhoneEvent.Submarine(XivChatCodec.readSubmarine(unpacker)))
                        }
                    } catch (error: Throwable) {
                        onEvent(PhoneEvent.Error("无法解析游戏数据 ($code): ${error.message ?: "未知错误"}"))
                    }
                } finally {
                    unpacker.close()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            onEvent(PhoneEvent.Error(error.message ?: "连接失败"))
        } finally {
            connected.set(false)
            sessionOutput = null
            sessionTx = null
            socket?.close()
            socket = null
            onSendFailure()
            onEvent(PhoneEvent.Disconnected("连接已断开"))
        }
    }

    fun sendChat(text: String) = sendCommand(XivChatCodec.encodeMessage(text), 2)

    fun changeChannel(channel: Int) = sendCommand(XivChatCodec.encodeChannel(channel), 9)

    // 按角色游标补传：在确认连接角色后调用，插件只返回该角色在该时间点之后的消息
    fun requestCatchUp(afterMillis: Long) {
        if (afterMillis <= 0L) return
        sendCommand(XivChatCodec.encodeCatchUp(afterMillis), 5)
    }

    fun sendChatOnChannel(channel: Int?, text: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val output = sessionOutput ?: return@launch
                val key = sessionTx ?: return@launch
                synchronized(writing) {
                    if (channel != null) send(output, key, 9, XivChatCodec.encodeChannel(channel))
                    send(output, key, 2, XivChatCodec.encodeMessage(text))
                }
            } catch (error: Throwable) {
                onEvent(PhoneEvent.Error(error.message ?: "发送失败"))
                onSendFailure()
            }
        }
    }

    fun friendAction(action: Int, contentId: Long, worldId: Int) = sendCommand(XivChatCodec.encodeFriendAction(action, contentId, worldId), 10)

    fun equipGearset(gearsetId: Int) = sendCommand(XivChatCodec.encodeJobsAction(gearsetId), 11)

    fun teleport(placeName: String) = sendCommand(XivChatCodec.encodeTeleport(placeName), 12)

    fun requestFriends() = sendCommand(XivChatCodec.encodePlayerList(), 6)
    fun requestParty() = sendCommand(XivChatCodec.encodePlayerList(1), 6)

    fun disconnect() {
        reconnectEnabled = false
        worker?.cancel()
        worker = null
        socket?.close()
        socket = null
        connected.set(false)
    }

    fun clearTrustedServer() {
        prefs.edit().remove("serverPublic").apply()
    }

    private fun sendCommand(payload: ByteArray, operation: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val output = sessionOutput ?: return@launch
                val key = sessionTx ?: return@launch
                send(output, key, operation, payload)
            } catch (error: Throwable) {
                onEvent(PhoneEvent.Error(error.message ?: "发送失败"))
                onSendFailure()
            }
        }
    }

    private fun send(output: DataOutputStream, key: ByteArray, operation: Int, payload: ByteArray) {
        synchronized(writing) {
            XivChatCodec.writeSecret(output, sodium, key, byteArrayOf(operation.toByte()) + payload)
        }
    }

    private fun loadOrCreateKeyPair(): KeyPair {
        val publicHex = prefs.getString("clientPublic", null)
        val secretHex = prefs.getString("clientSecret", null)
        if (publicHex != null && secretHex != null) return KeyPair(Key.fromBytes(publicHex.hexToBytes()), Key.fromBytes(secretHex.hexToBytes()))
        val pair = sodium.cryptoKxKeypair()
        prefs.edit().putString("clientPublic", pair.publicKey.asBytes.toHex()).putString("clientSecret", pair.secretKey.asBytes.toHex()).apply()
        return pair
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
