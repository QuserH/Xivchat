package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 图片上传：石之家用的是**腾讯云 COS 直传**，不是发到它自己的服务器。
 *
 * 整条链路是从移动站的 `Uploadtxcos` 组件读出来的（不是猜的）：
 *
 * 1. `GET /api/common/getCOSTokenI?channel=…` 拿临时凭证
 *    （注意 base 是 `/api/`，**不是**我们平时用的 `/api/home/`）
 *    返回 `{credentials:{tmpSecretId,tmpSecretKey,sessionToken}, startTime, expiredTime, keyDir}`
 * 2. 拿凭证签名，PUT 到 `https://ff14risingstones.gcloud.com.cn/<Key>`
 *    Bucket `ff14sq-1252004726`、Region `ap-shanghai`
 * 3. 成功后把返回的位置补上 `https://` 就是最终图片 URL，填进
 *    [ShizhijiaApi.commentPost] 的 `pics`
 *
 * **没有引腾讯云 SDK**，签名是按 COS 签名 v5 规范自己算的（就几十行，
 * 而这个项目所有网络请求本来都是手写 HttpURLConnection，加个 SDK 反而是唯一的例外）。
 *
 * 代价是**签名错了服务端只回 403、不说哪里错**。所以下面每一步都留了
 * 中间量的注释，调试时照着规范逐字段比对。
 */
object ShizhijiaCosUpload {

    private const val TOKEN_BASE = "https://apiff14risingstones.web.sdo.com/api/"
    private const val BUCKET = "ff14sq-1252004726"
    private const val REGION = "ap-shanghai"

    /** 站点自己用的自定义域名。签名里的 host 必须和这个一致。 */
    private const val HOST = "ff14risingstones.gcloud.com.cn"

    /** 单张上限，和站点一致（22020096 = 21MB）。 */
    const val MAX_BYTES = 22_020_096

    data class Cred(
        val tmpSecretId: String,
        val tmpSecretKey: String,
        val sessionToken: String,
        val startTime: Long,
        val expiredTime: Long,
        /** 服务端指定的目录前缀，最终 Key = `keyDir/<随机><时间>.<扩展名>`。 */
        val keyDir: String,
    )

    /**
     * 取临时凭证。
     *
     * [channel] 组件默认值是 `"default"`；站点各处会传自己的频道名
     * （帖子/动态/头像那些），传错的后果是文件落在别的目录下、
     * 有可能过不了后端对图片 URL 的正则校验，所以调用方按用途传。
     */
    suspend fun token(context: Context, channel: String = "default"): ShizhijiaApi.Res<Cred> {
        val json = ShizhijiaApi.rawGetOn(context, TOKEN_BASE, "common/getCOSTokenI", mapOf("channel" to channel))
            ?: return ShizhijiaApi.Res.Failed(null, "网络没通")
        return ShizhijiaApi.resOf(json) { root ->
            val d = root.optJSONObject("data") ?: return@resOf null
            val c = d.optJSONObject("credentials") ?: return@resOf null
            Cred(
                tmpSecretId = c.optString("tmpSecretId"),
                tmpSecretKey = c.optString("tmpSecretKey"),
                sessionToken = c.optString("sessionToken"),
                startTime = d.optLong("startTime"),
                expiredTime = d.optLong("expiredTime"),
                keyDir = d.optString("keyDir").trim('/'),
            )
        }
    }

    /**
     * 传一张本地图片，返回最终 URL。
     *
     * 上传前会压一遍（[compress]），和站点的做法一致——它也是先压再传，
     * 不压的话手机拍的原图动辄十几 MB，白占用户流量。
     */
    suspend fun upload(
        context: Context,
        uri: Uri,
        channel: String = "default",
    ): ShizhijiaApi.Res<String> = withContext(Dispatchers.IO) {
        val bytes = compress(context, uri)
            ?: return@withContext ShizhijiaApi.Res.Failed(null, "读不出这张图片")
        if (bytes.size > MAX_BYTES) {
            return@withContext ShizhijiaApi.Res.Failed(null, "图片太大了（压缩后仍超过 21MB）")
        }
        when (val t = token(context, channel)) {
            is ShizhijiaApi.Res.Ok -> put(t.value, bytes)
            is ShizhijiaApi.Res.NeedLogin -> ShizhijiaApi.Res.NeedLogin
            is ShizhijiaApi.Res.NeedCharacter -> ShizhijiaApi.Res.NeedCharacter
            is ShizhijiaApi.Res.Failed -> ShizhijiaApi.Res.Failed(t.code, t.msg)
        }
    }

    /**
     * 压缩。参数照站点的来：按原图大小选质量（≥1e7 用 0.4、≥5e6 用 0.6、
     * 其余 0.8），最长边压到 1920。站点输出 jpeg，这里也用 jpeg——
     * png 对照片没有压缩效果，几 MB 传上去毫无必要。
     */
    private fun compress(context: Context, uri: Uri): ByteArray? = runCatching {
        val src = context.contentResolver.openInputStream(uri)?.use { ins ->
            BitmapFactory.decodeStream(ins)
        } ?: return null
        val maxSide = 1920
        val scale = minOf(1f, maxSide.toFloat() / maxOf(src.width, src.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
        } else src
        // 质量按缩放后的像素量粗估，逻辑同站点的 quality 阶梯。
        val px = scaled.width.toLong() * scaled.height
        val quality = when {
            px >= 4_000_000 -> 40
            px >= 2_000_000 -> 60
            else -> 80
        }
        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }.getOrNull()

    /** Key 的构造照站点：`keyDir/<毫秒>_<8位hex><毫秒>.<扩展名>`。 */
    private fun buildKey(keyDir: String): String {
        fun hex4(): String = Integer.toHexString((0x10000 + (Math.random() * 0xFFFF).toInt())).substring(1)
        val now = System.currentTimeMillis()
        val prefix = "${now}_${hex4()}${hex4()}"
        val name = "${now + 1}.jpg"
        return "$keyDir/$prefix$name"
    }

    private fun put(cred: Cred, body: ByteArray): ShizhijiaApi.Res<String> {
        val key = buildKey(cred.keyDir)
        val path = "/$key"
        val auth = sign(cred, "put", path)
        return runCatching {
            val conn = (URL("https://$HOST$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("Host", HOST)
                setRequestProperty("Authorization", auth)
                // 临时凭证必须带这个头，否则一定 403。
                setRequestProperty("x-cos-security-token", cred.sessionToken)
                setRequestProperty("Content-Type", "image/jpeg")
                setFixedLengthStreamingMode(body.size)
            }
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            if (code in 200..299) {
                conn.disconnect()
                ShizhijiaApi.Res.Ok("https://$HOST$path")
            } else {
                // COS 的错误体是 XML，里面有 <Code> 和 <Message>，比 403 三个字有用。
                val err = runCatching {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }.getOrDefault("")
                conn.disconnect()
                val reason = Regex("<Code>(.*?)</Code>").find(err)?.groupValues?.get(1)
                ShizhijiaApi.Res.Failed(
                    code.toLong(),
                    when {
                        reason == "RequestTimeTooSkewed" -> "手机时间和服务器差太多，校对一下时间"
                        reason != null -> "上传失败：$reason"
                        else -> "上传失败（HTTP $code）"
                    },
                )
            }
        }.getOrElse { ShizhijiaApi.Res.Failed(null, it.message?.take(60) ?: "上传出错") }
    }

    /**
     * COS 签名 v5。规范里的四步，中间量都留了名字方便对照：
     *
     * ```
     * SignKey      = HMAC-SHA1(SecretKey, KeyTime)
     * HttpString   = method\n path\n query\n headers\n
     * StringToSign = "sha1\n" + KeyTime + "\n" + SHA1(HttpString) + "\n"
     * Signature    = HMAC-SHA1(SignKey, StringToSign)
     * ```
     *
     * 只签 `host` 一个头：签得越少越不容易错，未参与签名的头 COS 允许照常发送。
     * `q-header-list` 和 `headers` 必须严格对应，多一个少一个都是 403。
     */
    private fun sign(cred: Cred, method: String, path: String): String {
        val keyTime = "${cred.startTime};${cred.expiredTime}"
        val signKey = hmacSha1Hex(cred.tmpSecretKey, keyTime)
        val headerList = "host"
        val headers = "host=${cosEncode(HOST)}"
        val httpString = "${method.lowercase()}\n$path\n\n$headers\n"
        val stringToSign = "sha1\n$keyTime\n${sha1Hex(httpString)}\n"
        val signature = hmacSha1Hex(signKey, stringToSign)
        return "q-sign-algorithm=sha1" +
            "&q-ak=${cred.tmpSecretId}" +
            "&q-sign-time=$keyTime" +
            "&q-key-time=$keyTime" +
            "&q-header-list=$headerList" +
            "&q-url-param-list=" +
            "&q-signature=$signature"
    }

    /**
     * COS 要求的 URL 编码：标准 encodeURIComponent，但 `!'()*` 也要编码，
     * 而 `/` 在 header value 里不编码。这几个例外是 403 的常见来源。
     */
    private fun cosEncode(v: String): String =
        java.net.URLEncoder.encode(v, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")

    private fun hmacSha1Hex(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun sha1Hex(data: String): String =
        MessageDigest.getInstance("SHA-1").digest(data.toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
