package com.quserh.eorzeaphone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.data.normalizedPlayerName
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaAvatarStore
import com.quserh.eorzeaphone.data.stripPlayerDecorations
import com.quserh.eorzeaphone.data.tellNamePart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The person on the other end of a DM thread.
 *
 * [friend] is set when they are on the friend list, so the caller can reuse the
 * friend-scoped avatar maps instead of re-deriving an owner key.
 */
internal data class TellPeer(
    val name: String,
    val homeWorld: String,
    val currentWorld: String,
    val friend: PhoneFriend? = null,
)

/**
 * 会话行头像用的贴纸池。只有"头像"那一栏能被自动分配，
 * 和 [PhoneState.ensureFriendAvatars] 用的是同一批图。
 */
private val avatarStickerPool: List<String> by lazy {
    builtinConversationIcons.filter { it.category == "avatar" }.map { it.id }
}

/**
 * 按 key 稳定取一张贴纸。
 *
 * 非好友的私聊从来没被分配过兜底贴纸（那只覆盖好友名单），所以这里不落盘、
 * 直接用 hashCode 取模——String.hashCode 是 JDK 规范里写死的，重启后同一个人
 * 还是同一张图。
 */
internal fun stableStickerFor(seed: String): String =
    if (avatarStickerPool.isEmpty()) "" else avatarStickerPool[seed.hashCode().mod(avatarStickerPool.size)]

private fun isAutoSticker(icon: String): Boolean = icon in avatarStickerPool

/**
 * 认出私聊对面是谁。
 *
 * 会话 key 里问不出服务器：`conversationKey()` 存的是
 * `tell:${normalizedPlayerName()}`，而那个函数会把 `@` 删掉，
 * 所以得从 [ChatConversation.tellRecipient]（`名字@服务器`）和消息里的
 * senderWorld 反推。
 *
 * 命中好友名单时优先用好友记录：这样缓存 key 和联系人那一屏完全一致，
 * 同一个人不会被当成两个人各查一次。
 */
internal fun tellPeerFor(conversation: ChatConversation, state: PhoneState): TellPeer? {
    val raw = conversation.tellRecipient.takeIf { it.isNotBlank() }
        ?: conversation.messages.lastOrNull { !it.selfFlag && !it.senderName.isNullOrBlank() }?.senderName
        ?: return null
    val cleaned = raw.stripPlayerDecorations()
    val name = cleaned.substringBefore('@').trim()
    if (name.isBlank()) return null

    val nameKey = name.tellNamePart().normalizedPlayerName()
    state.friends.firstOrNull { it.name.tellNamePart().normalizedPlayerName() == nameKey }?.let { friend ->
        return TellPeer(friend.name, friend.homeWorld, friend.world, friend)
    }

    // 名字后面跟的服务器是对方的**原服**：游戏只在对方原服 ≠ 我当前所在服时才显示它。
    // 这一点要紧——石之家搜人必须拿原服比（见 ShizhijiaFriendLink 的注释），
    // 拿访问服比会把注册过的人误报成"没注册"，而那个结论会被缓存下来。
    val suffix = cleaned.substringAfter('@', "").trim().ifBlank {
        conversation.messages.lastOrNull { !it.selfFlag && !it.senderWorld.isNullOrBlank() }
            ?.senderWorld?.stripPlayerDecorations().orEmpty()
    }
    // 没后缀 → 对方就在我当前所在的服，那个服即是他的原服。
    // 留空的话 find() 会退到"同名唯一才算"，同名跨服存在时就查不出来了。
    val home = suffix.ifBlank { state.profile?.currentWorld.orEmpty() }
    // currentWorld 不填我自己的服：跨服私聊时对方在哪个服根本不知道，
    // 填了等于给 find() 一个错的兜底，可能匹配到同名的另一个人。
    return TellPeer(name, home, suffix)
}

/**
 * 会话行的头像。取用顺序和联系人那一屏（`friendAvatarFor`）保持一致：
 *
 *   1. 用户自己挑过的     → 永远最优先
 *   2. 石之家头像         → 上传的照片，或按 uuid 拼的官方立绘
 *   3. 随机贴纸           → 这人没注册石之家
 *
 * 群聊/系统频道不适用，原样走 [PhoneState.conversationIcon]。
 *
 * 为什么不直接用 `conversationIcon`：它对私聊返回的是
 * 「用户挑的 ?: 自动分配的贴纸」，两者混在一个返回值里，于是自动贴纸永远
 * 抢在石之家头像前面——和 `PhoneModels` 注释里记的 0.7.228 那个死代码同一类问题。
 * 这里把三段拆开，不改模型层语义。
 *
 * 缓存：结论落在 [ShizhijiaAvatarStore]（查过没有会记下来，不重复请求），
 * 图片本身走 ShizhijiaImageLoader 的内存 + 磁盘缓存。所以每次登录/点开都不会重新拉。
 */
@Composable
internal fun conversationAvatarFor(
    conversation: ChatConversation,
    state: PhoneState,
    /** 名册版本号。名册拉回来后 +1，让头像重新查表。 */
    rosterTick: Int = 0,
): String {
    val context = LocalContext.current
    val isTell = conversation.category == ChatCategory.Tell || conversation.key.startsWith("tell:")
    val raw = state.conversationIcon(conversation.key, conversation.category)
    if (!isTell) return raw

    val peer = tellPeerFor(conversation, state)
    if (peer == null || peer.name.isBlank()) return raw

    // 用户挑的那张：好友走好友表（和联系人屏同一份），非好友走会话覆盖表。
    // 非好友且值恰好是贴纸池里的图时，当成自动分配的——它只可能来自旧版本，
    // 让石之家头像盖过去正是这次要的顺序。
    val picked = peer.friend?.let { state.friendAvatar(it) } ?: raw.takeUnless { isAutoSticker(it) }.orEmpty()
    if (picked.isNotBlank()) return picked

    var resolved by remember(peer.name, peer.homeWorld, rosterTick) {
        mutableStateOf(ShizhijiaAvatarStore.peek(context, peer.name, peer.homeWorld).orEmpty())
    }
    LaunchedEffect(peer.name, peer.homeWorld, rosterTick) {
        if (resolved.isBlank()) {
            val url = withContext(Dispatchers.IO) {
                ShizhijiaAvatarStore.resolve(context, peer.name, peer.homeWorld, peer.currentWorld)
            }
            if (url.isNotBlank()) resolved = url
        }
    }
    if (resolved.isNotBlank()) return resolved

    val fallback = peer.friend?.let { state.friendFallbackAvatar(it) }.orEmpty()
    if (fallback.isNotBlank()) return fallback
    return raw.takeIf { isAutoSticker(it) } ?: stableStickerFor("${peer.name}@${peer.homeWorld}")
}
