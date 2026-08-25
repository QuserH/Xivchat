package com.quserh.eorzeaphone.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaApi
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaComment
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDynamic
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaLoginUser
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPostCard
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPostDetail
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPostPart
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaImageLoader
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSession
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSignLog
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSignReward
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSearchUser
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSearchGlamour
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneAccentContainer
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneOnAccentContainer
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneText
import kotlinx.coroutines.launch

/**
 * 石之家 (FF14 Rising Stones official community) - the in-phone "app".
 *
 * Renders the forum through the public JSON API: a post feed with partitions,
 * post detail (HTML body), comments, search, and the login-gated dynamics feed.
 * All screens share the existing MD3 theme, ScreenFrame/ScreenHeader scaffolds
 * and the user-configurable content margin. Internal navigation uses a simple
 * back stack so the system back button walks out of the app level-by-level.
 */

private sealed interface SzjRoute {
    data object Home : SzjRoute
    data class PostDetail(val postId: String) : SzjRoute
    data class DynamicDetail(val id: String) : SzjRoute
    data object Search : SzjRoute
    data object Login : SzjRoute
    data object SignCalendar : SzjRoute
}

/** App-wide full-screen image viewer state; any thumbnail sets its URL here. */
object SzjViewer {
    var url by mutableStateOf<String?>(null)
}

/**
 * Player avatar with the official default-portrait chain: custom photo →
 * per-race portrait (fetched lazily by uuid for players without a photo) →
 * letter chip. Mirrors how the official site treats missing avatars.
 */
@Composable
private fun SzjAvatar(name: String, avatar: String, uuid: String, sizeDp: Int) {
    val context = LocalContext.current
    var url by remember(uuid) { mutableStateOf(avatar) }
    LaunchedEffect(uuid, avatar) {
        if (url.isBlank() && uuid.isNotBlank()) {
            url = ShizhijiaApi.resolveAvatar(context, uuid)
        }
    }
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(Color(0xFFF0EDE6)), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            ShizhijiaRemoteImage(
                url = url,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                showPlaceholder = false,
            )
        } else {
            Text(name.take(1).ifBlank { "?" }, color = PhoneMuted, fontSize = (sizeDp * 0.38f).sp)
        }
    }
}

/**
 * Post-feed state held above the internal back stack so that opening a post
 * detail and returning does NOT lose the scroll position or loaded pages.
 */
private class SzjPostsState {
    val parts = mutableStateOf(listOf<ShizhijiaPostPart>())
    val partId = mutableStateOf("")
    val posts = mutableStateOf(listOf<ShizhijiaPostCard>())
    val page = mutableStateOf(1)
    val pageTime = mutableStateOf("")
    val loading = mutableStateOf(true)
    val seenPartId = mutableStateOf<String?>(null)
    val listState = androidx.compose.foundation.lazy.LazyListState()
}

@Composable
fun ShizhijiaScreen(state: PhoneState) {
    var stack by remember { mutableStateOf(listOf<SzjRoute>(SzjRoute.Home)) }
    val postsState = remember { SzjPostsState() }
    // Only swallow back while inside the app; the outer handler then leaves the desktop.
    BackHandler(enabled = stack.size > 1) { stack = stack.dropLast(1) }
    val route = stack.last()
    // nav pushes a destination; pop returns to the previous one (login success uses pop).
    val nav: (SzjRoute) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }
    Box(Modifier.fillMaxSize()) {
        when (route) {
            SzjRoute.Home -> ShizhijiaHomeScreen(state, nav, postsState)
            is SzjRoute.PostDetail -> ShizhijiaPostDetailScreen(state, route.postId, pop)
            is SzjRoute.DynamicDetail -> ShizhijiaDynamicDetailScreen(state, route.id, pop)
            SzjRoute.Search -> ShizhijiaSearchScreen(state, pop, nav)
            SzjRoute.Login -> ShizhijiaLoginScreen(state, pop)
            SzjRoute.SignCalendar -> ShizhijiaSignCalendarScreen(state, pop)
        }
        SzjViewer.url?.let { url ->
            // Full-screen overlay for viewing a tapped image at size.
            SzjPhotoViewer(url = url, onClose = { SzjViewer.url = null })
        }
    }
}

/** Full-screen image viewer: dark scrim, fitted image, X (top-left) or back closes. */
@Composable
private fun SzjPhotoViewer(url: String, onClose: () -> Unit) {
    BackHandler { onClose() }
    val context = LocalContext.current
    var bmp by remember(url) { mutableStateOf(if (url.startsWith("data:image")) decodeDataUri(url) else ShizhijiaImageLoader.peek(url)) }
    LaunchedEffect(url) { if (!url.startsWith("data:image")) bmp = ShizhijiaImageLoader.load(context, url) }
    Box(Modifier.fillMaxSize().background(Color(0xE6000000))) {
        if (bmp != null) {
            Image(bmp!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(34.dp)) }
        }
        Text("✕", color = Color.White, fontSize = 26.sp, modifier = Modifier.align(Alignment.TopStart)
            .clip(RoundedCornerShape(10.dp)).clickable(onClick = onClose).padding(14.dp))
    }
}

// ---------------------------------------------------------------------------
// Home: partition chips + post feed / dynamics feed tabs
// ---------------------------------------------------------------------------

private const val MAIN_COMMUNITY = 0
private const val MAIN_RECRUIT = 1
private const val MAIN_GLAMOUR = 2
private const val MAIN_ME = 3
private const val SUB_POSTS = 0
private const val SUB_DYNAMICS = 1
private const val SUB_GUIDE = 2

/** Tinted backdrop for the comment area, distinct from the article body so the
 *  two regions are obvious while scrolling. */
private val CommentAreaBg = Color(0xFFEDEBF3)

@Composable
private fun ShizhijiaHomeScreen(state: PhoneState, nav: (SzjRoute) -> Unit, postsState: SzjPostsState) {
    val context = LocalContext.current
    var mainTab by remember { mutableStateOf(MAIN_COMMUNITY) }
    var subTab by remember { mutableStateOf(SUB_POSTS) }
    // Login state drives the top bar and the dynamics tab.
    var loggedIn by remember { mutableStateOf(ShizhijiaSession.hasSession(context)) }
    // Hydrate from the persisted profile first so the top bar shows the real
    // character immediately (no 已登录→昵称 flash while the network call runs).
    var loginUser by remember { mutableStateOf(ShizhijiaSession.cachedLoginUser(context)) }
    val scope = rememberCoroutineScope()
    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    var signedToday by remember { mutableStateOf(ShizhijiaSession.signDate(context) == todayStr) }
    // Auto check-in once per day when logged in, and refresh the top bar state.
    LaunchedEffect(Unit) {
        val logged = ShizhijiaApi.isLoggedIn(context)
        loggedIn = logged
        if (logged) {
            loginUser = ShizhijiaApi.getLoginUser(context)
            loginUser?.let { ShizhijiaSession.cacheLoginUser(context, it) }
            android.util.Log.d("ShizhijiaLogin", "loginUser=${loginUser?.name} ava=${(loginUser?.avatar ?: "").take(50)}")
            if (!signedToday) {
                val ok = ShizhijiaApi.signIn(context)
                if (ok) { signedToday = true; ShizhijiaSession.setSignDate(context, todayStr); android.widget.Toast.makeText(context, "签到成功", android.widget.Toast.LENGTH_SHORT).show() }
            }
        } else {
            ShizhijiaSession.clearCachedUser(context)
            loginUser = null
        }
    }
    // Manual check-in from the top bar button. A duplicate check-in is rejected
    // by the server with a non-10000 code, so on failure we cross-check the
    // monthly sign log - when today shows up there the state still flips to 已签到.
    val onSignIn: () -> Unit = {
        scope.launch {
            val ok = ShizhijiaApi.signIn(context) || ShizhijiaApi.isSignedToday(context)
            if (ok) {
                signedToday = true
                ShizhijiaSession.setSignDate(context, todayStr)
                android.widget.Toast.makeText(context, "今日已签到", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "签到失败，请稍后再试", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    ScreenFrame {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ShizhijiaTopBar(state, nav, loggedIn, loginUser, onSignIn, signedToday)
                when (mainTab) {
                    MAIN_COMMUNITY -> {
                        SzjSubTabRow(subTab) { subTab = it }
                        Box(Modifier.weight(1f)) {
                            when (subTab) {
                                SUB_POSTS -> ShizhijiaPostsTab(state, nav, postsState)
                                SUB_DYNAMICS -> ShizhijiaDynamicsTab(nav, loggedIn)
                                else -> SzjSectionPlaceholder("攻略")
                            }
                        }
                    }
                    MAIN_RECRUIT -> SzjSectionPlaceholder("招募")
                    MAIN_GLAMOUR -> SzjSectionPlaceholder("幻化")
                    else -> ShizhijiaMeTab(state, nav, loggedIn)
                }
            }
            SzjBottomBar(mainTab, onSelect = { mainTab = it }, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Top bar with the account entry (avatar + login label) and a check-in button. */
@Composable
private fun ShizhijiaTopBar(state: PhoneState, nav: (SzjRoute) -> Unit, loggedIn: Boolean, loginUser: ShizhijiaLoginUser?, onSignIn: () -> Unit, signedToday: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF0EDE6)), contentAlignment = Alignment.Center) {
            val ava = loginUser?.avatar
            // Default portraits arrive as inline data:image URIs; decode them
            // here so we can fall back to the first character on any failure.
            val bmp = if (!ava.isNullOrBlank() && ava.startsWith("data:image")) remember(ava) { decodeDataUri(ava) } else null
            if (bmp != null) {
                Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else if (!ava.isNullOrBlank() && !ava.startsWith("data:image")) {
                ShizhijiaRemoteImage(url = ava, modifier = Modifier.fillMaxSize().clip(CircleShape), showPlaceholder = false)
            } else {
                Text(loginUser?.name?.take(1) ?: if (loggedIn) "我" else "?", color = PhoneMuted, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(loginUser?.name ?: if (loggedIn) "已登录" else "未登录", color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            val server = listOfNotNull(loginUser?.area, loginUser?.group)
            Text(if (server.isNotEmpty()) server.joinToString(" ") else "石之家 · FF14 官方社区", color = PhoneMuted, fontSize = 11.sp)
        }
        // Check-in button flips to a greyed "已签到" once done today; clicking it
        // then opens the sign-in calendar (rewards + signed days) instead.
        Text(
            if (signedToday) "已签到" else "签到",
            color = if (signedToday) PhoneMuted else PhoneOnAccentContainer,
            fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (signedToday) Color(0xFFE2E0E8) else PhoneAccentContainer)
                .clickable {
                    if (signedToday) nav(SzjRoute.SignCalendar) else onSignIn()
                }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("⌕", color = PhoneAccent, fontSize = 22.sp, modifier = Modifier
            .clip(RoundedCornerShape(10.dp)).clickable { nav(SzjRoute.Search) }.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

/** Second-level tab row inside the Community section: 帖子 / 动态 / 攻略. */
@Composable
private fun SzjSubTabRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SzjSubTab("帖子", selected == SUB_POSTS) { onSelect(SUB_POSTS) }
        SzjSubTab("动态", selected == SUB_DYNAMICS) { onSelect(SUB_DYNAMICS) }
        SzjSubTab("攻略", selected == SUB_GUIDE) { onSelect(SUB_GUIDE) }
    }
}

@Composable
private fun SzjSubTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, color = if (selected) PhoneAccent else PhoneMuted, fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp))
}

@Composable
private fun SzjSectionPlaceholder(label: String) {
    Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.Center) { Text("「$label」开发中", color = PhoneMuted) }
}

@Composable
private fun ShizhijiaMeTab(state: PhoneState, nav: (SzjRoute) -> Unit, loggedIn: Boolean) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(bottom = 90.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (loggedIn) {
            Text("已登录石之家", color = PhoneText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Button(onClick = { ShizhijiaSession.clear(context) }, colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent)) { Text("退出登录") }
        } else {
            Text("未登录", color = PhoneMuted)
            Spacer(Modifier.height(14.dp))
            Button(onClick = { nav(SzjRoute.Login) }, colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent)) { Text("登录") }
        }
    }
}

/** MD3 floating bottom bar: a rounded capsule that hovers over the content. */
@Composable
private fun SzjBottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(horizontal = 18.dp, vertical = 10.dp).fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(PhoneSurface)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SzjBottomTab("社区", selected == MAIN_COMMUNITY) { onSelect(MAIN_COMMUNITY) }
        SzjBottomTab("招募", selected == MAIN_RECRUIT) { onSelect(MAIN_RECRUIT) }
        SzjBottomTab("幻化", selected == MAIN_GLAMOUR) { onSelect(MAIN_GLAMOUR) }
        SzjBottomTab("我", selected == MAIN_ME) { onSelect(MAIN_ME) }
    }
}

@Composable
private fun SzjBottomTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = if (selected) PhoneAccent else PhoneMuted, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.width(20.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
            .background(if (selected) PhoneAccent else Color.Transparent))
    }
}

// ---- Post feed -------------------------------------------------------------

@Composable
private fun ShizhijiaPostsTab(state: PhoneState, nav: (SzjRoute) -> Unit, ps: SzjPostsState) {
    val context = LocalContext.current
    // All feed state lives on `ps`, which is held above the internal back stack,
    // so returning from a post detail keeps the scroll position and pages.
    val listState = ps.listState

    LaunchedEffect(Unit) { ps.parts.value = ShizhijiaApi.getPostParts(context) }
    // Reload the feed ONLY when the partition actually changes (first visit or a
    // real switch). Re-entering after a post detail keeps loaded pages + scroll.
    LaunchedEffect(ps.partId.value) {
        if (ps.seenPartId.value == ps.partId.value) return@LaunchedEffect
        ps.seenPartId.value = ps.partId.value
        ps.loading.value = true
        ps.posts.value = emptyList(); ps.page.value = 1; ps.pageTime.value = ""
        val result = ShizhijiaApi.getPostsList(context, partId = ps.partId.value)
        ps.posts.value = result.rows; ps.pageTime.value = result.pageTime
        ps.loading.value = false
    }

    // Infinite scroll: fetch the next page shortly before reaching the end.
    val nearEnd by remember { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= ps.posts.value.size - 3
    } }
    LaunchedEffect(nearEnd, ps.partId.value) {
        if (nearEnd && !ps.loading.value && ps.posts.value.isNotEmpty() && ps.pageTime.value.isNotBlank()) {
            ps.loading.value = true
            val next = ShizhijiaApi.getPostsList(context, partId = ps.partId.value, page = ps.page.value + 1, pageTime = ps.pageTime.value)
            // Stop paging when the server returns no more rows.
            if (next.rows.isEmpty()) ps.pageTime.value = "" else {
                ps.posts.value = ps.posts.value + next.rows
                ps.pageTime.value = next.pageTime
                ps.page.value += 1
            }
            ps.loading.value = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Partition chips: "推荐" (all) plus the returned partitions.
        LazyRow(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "all") { SzjPartChip("推荐", ps.partId.value == "") { ps.partId.value = "" } }
            items(ps.parts.value, key = { it.id }) { p -> SzjPartChip(p.name, ps.partId.value == p.id) { ps.partId.value = p.id } }
        }
        Spacer(Modifier.height(4.dp))
        when {
            ps.loading.value && ps.posts.value.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(30.dp))
            }
            ps.posts.value.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无帖子", color = PhoneMuted)
            }
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 96.dp)) {
                items(ps.posts.value, key = { it.postsId }) { post ->
                    SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) })
                }
                item(key = "loading-footer") {
                    if (ps.loading.value) Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SzjPartChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) PhoneOnAccentContainer else PhoneMuted,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) PhoneAccentContainer else PhoneSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun SzjPostRow(post: ShizhijiaPostCard, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp)).background(PhoneSurface)
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        // Line 1: title with the [partition] tag on its left.
        Row(verticalAlignment = Alignment.Top) {
            if (post.partName.isNotBlank()) {
                Text(
                    post.partName,
                    color = PhoneOnAccentContainer, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, end = 6.dp)
                        .clip(RoundedCornerShape(5.dp)).background(PhoneAccentContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(
                post.title,
                color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        // Line 2: main-image thumbnails, up to 3 (deduplicated). Every thumbnail keeps
        // the SAME fixed ~1/3 width whether 1, 2 or 3 are shown; failed images
        // collapse away (no blank frame before the healthy ones).
        if (post.coverPics.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                post.coverPics.distinct().take(3).forEach { url ->
                    ShizhijiaRemoteImage(url = url, modifier = Modifier.fillMaxWidth(0.32f).height(130.dp), showPlaceholder = false, collapseOnFail = true, onClick = { SzjViewer.url = it })
                }
            }
        }
        // Line 3: author on the left; comment / read counts on the right.
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(post.characterName.ifBlank { "匿名玩家" }, color = PhoneMuted, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (post.commentCount > 0) Text("${post.commentCount}评论 ", color = PhoneMuted, fontSize = 11.sp)
            if (post.readCount > 0) Text("${post.readCount}阅读", color = PhoneMuted, fontSize = 11.sp)
        }
    }
}

// ---- Dynamics feed ----------------------------------------------------------

@Composable
private fun ShizhijiaDynamicsTab(nav: (SzjRoute) -> Unit, loggedIn: Boolean) {
    val context = LocalContext.current
    var dynamics by remember { mutableStateOf(listOf<ShizhijiaDynamic>()) }
    var loading by remember { mutableStateOf(loggedIn) }
    LaunchedEffect(loggedIn) {
        if (loggedIn) { loading = true; dynamics = ShizhijiaApi.getFollowDynamicList(context).rows; loading = false }
    }
    Column(Modifier.fillMaxSize()) {
        if (!loggedIn) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("登录后查看关注动态", color = PhoneMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { nav(SzjRoute.Login) }, colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent)) { Text("登录") }
                }
            }
        } else if (loading && dynamics.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(30.dp)) }
        } else if (dynamics.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无动态", color = PhoneMuted) }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
                items(dynamics, key = { it.id }) { d -> SzjDynamicRow(d, onClick = { nav(SzjRoute.DynamicDetail(d.id)) }) }
            }
        }
    }
}

@Composable
private fun SzjDynamicRow(d: ShizhijiaDynamic, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp)).background(PhoneSurface)
            .clickable(onClick = onClick).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SzjAvatar(d.characterName, d.avatar, d.uuid, 38)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(d.characterName.ifBlank { "光之战士" }, color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (d.createdAt.isNotBlank()) Text(d.createdAt, color = PhoneMuted, fontSize = 11.sp)
            }
        }
        if (d.contentText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(d.contentText, color = PhoneText, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
        d.images.firstOrNull()?.let { first ->
            Spacer(Modifier.height(8.dp))
            ShizhijiaRemoteImage(url = first, modifier = Modifier.fillMaxWidth().height(160.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            if (d.likeCount > 0) Text("赞 ${d.likeCount}", color = PhoneMuted, fontSize = 12.sp)
            if (d.commentCount > 0) { Spacer(Modifier.width(12.dp)); Text("评论 ${d.commentCount}", color = PhoneMuted, fontSize = 12.sp) }
        }
    }
}

// ---------------------------------------------------------------------------
// Post detail
// ---------------------------------------------------------------------------

@Composable
private fun ShizhijiaPostDetailScreen(state: PhoneState, postId: String, pop: () -> Unit) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<ShizhijiaPostDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Comments are rendered inline below the article body on the same screen.
    var commentOrder by remember { mutableStateOf("like") } // "like" hottest / "new" newest
    var comments by remember { mutableStateOf(listOf<ShizhijiaComment>()) }
    var commentPage by remember { mutableStateOf(1) }
    var commentPageTime by remember { mutableStateOf("") }
    var commentLoading by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        loading = true
        detail = ShizhijiaApi.getPostDetail(context, postId)
        loading = false
    }
    // (Re)load comments whenever the ordering changes.
    LaunchedEffect(postId, commentOrder) {
        commentLoading = true
        comments = emptyList(); commentPage = 1; commentPageTime = ""
        val result = ShizhijiaApi.getPostComments(context, postId, commentOrder)
        comments = result.rows; commentPageTime = result.pageTime
        commentLoading = false
    }
    val listState = rememberLazyListState()
    // Infinite scroll for comments.
    val nearEnd by remember { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= comments.size - 2
    } }
    LaunchedEffect(nearEnd, commentOrder, postId) {
        if (nearEnd && !commentLoading && comments.isNotEmpty() && commentPageTime.isNotBlank()) {
            commentLoading = true
            val next = ShizhijiaApi.getPostComments(context, postId, commentOrder, page = commentPage + 1, pageTime = commentPageTime)
            if (next.rows.isEmpty()) commentPageTime = "" else {
                comments = comments + next.rows
                commentPageTime = next.pageTime
                commentPage += 1
            }
            commentLoading = false
        }
    }

    ScreenFrame {
        ScreenHeader("帖子详情", state, onBack = { pop() })
        if (loading && detail == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(30.dp)) }
            return@ScreenFrame
        }
        val d = detail
        if (d == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("加载失败", color = PhoneMuted) }
            return@ScreenFrame
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(d.title, color = PhoneText, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SzjAvatar(d.characterName, d.avatar, d.uuid, 36)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.characterName, color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOf(d.areaName, d.groupName, d.createdAt).filter { it.isNotBlank() }.joinToString(" · "),
                                color = PhoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (d.readCount > 0) Text("阅读 ${d.readCount}", color = PhoneMuted, fontSize = 12.sp)
                        if (d.likeCount > 0) Text("赞 ${d.likeCount}", color = PhoneMuted, fontSize = 12.sp)
                        if (d.commentCount > 0) Text("评论 ${d.commentCount}", color = PhoneMuted, fontSize = 12.sp)
                        if (d.starCount > 0) Text("收藏 ${d.starCount}", color = PhoneMuted, fontSize = 12.sp)
                    }
                }
            }
            item {
                // Rich HTML body: paragraphs, bold, inline images, links.
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ShizhijiaRichContent(d.contentHtml)
                }
            }
            item {
                // Comments header with an inline ordering toggle. Its tinted
                // backdrop signals the switch from the article body into the
                // comment area, so the two never blur together while scrolling.
                Row(Modifier.fillMaxWidth().background(CommentAreaBg).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("全部评论", color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(Modifier.clip(RoundedCornerShape(10.dp)).background(PhoneSurface)) {
                        SzjSmallOption("最热", commentOrder == "like") { commentOrder = "like" }
                        SzjSmallOption("最新", commentOrder == "new") { commentOrder = "new" }
                    }
                }
            }
            if (commentLoading && comments.isEmpty()) {
                item(key = "comments-loading") {
                    Box(Modifier.fillMaxWidth().background(CommentAreaBg).padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(24.dp)) }
                }
            } else if (comments.isEmpty()) {
                item(key = "comments-empty") { Text("暂无评论", color = PhoneMuted, modifier = Modifier.fillMaxWidth().background(CommentAreaBg).padding(24.dp), textAlign = TextAlign.Center) }
            } else {
                items(comments, key = { it.id }) { c -> SzjCommentRow(c) }
                item(key = "comments-footer") {
                    if (commentLoading) Box(Modifier.fillMaxWidth().background(CommentAreaBg).padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Comments (rendered inline inside the post detail screen)
// ---------------------------------------------------------------------------

@Composable
private fun SzjSmallOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, color = if (selected) PhoneOnAccentContainer else PhoneMuted, fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) PhoneAccentContainer else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
private fun SzjCommentRow(c: ShizhijiaComment) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(14.dp)).background(PhoneSurface).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SzjAvatar(c.characterName, c.avatar, c.uuid, 30)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.characterName.ifBlank { "匿名玩家" }, color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (c.isPostsAuthor) Text(" 作者", color = PhoneAccent, fontSize = 11.sp)
                }
                Text(listOf(c.areaName, c.groupName, c.createdAt).filter { it.isNotBlank() }.joinToString(" · "),
                    color = PhoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (c.likeCount > 0) Text("赞 ${c.likeCount}", color = PhoneMuted, fontSize = 11.sp)
        }
        if (c.contentHtml.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            // Backend emoji ([emoN]) is expanded into a small image by the renderer.
            ShizhijiaRichContent(c.contentHtml)
        }
        if (c.commentPic.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            // Comment pictures render small (capped 200px) at their own ratio,
            // never stretched full-bleed; a failed picture collapses away.
            ShizhijiaRemoteImage(
                url = c.commentPic,
                modifier = Modifier.widthIn(max = 200.dp).heightIn(max = 200.dp),
                contentScale = ContentScale.Fit,
                fitByAspect = true,
                collapseOnFail = true,
                onClick = { SzjViewer.url = it },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

@Composable
private fun ShizhijiaSearchScreen(state: PhoneState, pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    // Search channel: 帖子 / 攻略 / 用户 / 幻化 (common/search type ids).
    var searchType by remember { mutableStateOf(ShizhijiaApi.SEARCH_TYPE_POST) }
    var typeMenu by remember { mutableStateOf(false) }
    val typeLabel = when (searchType) {
        ShizhijiaApi.SEARCH_TYPE_STRAT -> "攻略"
        ShizhijiaApi.SEARCH_TYPE_USER -> "用户"
        ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> "幻化"
        else -> "帖子"
    }
    var hotWords by remember { mutableStateOf(listOf<String>()) }
    var postResults by remember { mutableStateOf<List<ShizhijiaPostCard>?>(null) }
    var userResults by remember { mutableStateOf<List<ShizhijiaSearchUser>?>(null) }
    var glamourResults by remember { mutableStateOf<List<ShizhijiaSearchGlamour>?>(null) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hotWords = ShizhijiaApi.getHotSearchList(context).map { it.text }.filter { it.isNotBlank() }.distinct() }

    fun doSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        scope.launch {
            searching = true
            postResults = null; userResults = null; glamourResults = null
            when (searchType) {
                ShizhijiaApi.SEARCH_TYPE_USER -> userResults = ShizhijiaApi.searchUsers(context, q)
                ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> glamourResults = ShizhijiaApi.searchGlamours(context, q)
                else -> postResults = ShizhijiaApi.searchPosts(context, q, searchType)
            }
            searching = false
        }
    }

    ScreenFrame {
        ScreenHeader("搜索", state, onBack = { pop() }, trailing = {
            Text("搜索", color = PhoneAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { doSearch() }.padding(horizontal = 10.dp, vertical = 4.dp))
        })
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Channel switch button in front of the search bar.
                Box {
                    Text("$typeLabel ▾", color = PhoneText, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(PhoneSurface)
                            .clickable { typeMenu = true }.padding(horizontal = 10.dp, vertical = 12.dp))
                    androidx.compose.material3.DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        listOf(
                            "帖子" to ShizhijiaApi.SEARCH_TYPE_POST,
                            "攻略" to ShizhijiaApi.SEARCH_TYPE_STRAT,
                            "用户" to ShizhijiaApi.SEARCH_TYPE_USER,
                            "幻化" to ShizhijiaApi.SEARCH_TYPE_GLAMOUR,
                        ).forEach { (label, id) ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(label, color = if (id == searchType) PhoneAccent else PhoneText) },
                                onClick = { searchType = id; typeMenu = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Row(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(PhoneSurface).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕", color = PhoneMuted, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = PhoneText, fontSize = 15.sp),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) Text("搜索$typeLabel", color = PhoneMuted, fontSize = 15.sp)
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                    )
                }
            }
            if (postResults == null && userResults == null && glamourResults == null && !searching) {
                Spacer(Modifier.height(18.dp))
                Text("热门搜索", color = PhoneMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    hotWords.take(6).forEach { word ->
                        SzjPartChip(word, selected = false) { query = word; doSearch() }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                when {
                    searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(28.dp)) }
                    searchType == ShizhijiaApi.SEARCH_TYPE_USER -> {
                        if (userResults.isNullOrEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("未找到相关用户", color = PhoneMuted) }
                        else LazyColumn(Modifier.fillMaxSize()) {
                            items(userResults.orEmpty(), key = { it.uuid }) { u ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    SzjAvatar(u.name, u.avatar, u.uuid, 44)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(u.name, color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        val line = listOf(u.areaName, u.groupName).filter { it.isNotBlank() }.joinToString(" · ")
                                        if (line.isNotBlank()) Text(line, color = PhoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (u.profile.isNotBlank()) Text(u.profile, color = PhoneMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("粉丝 ${u.fansNum}", color = PhoneMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    searchType == ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> {
                        if (glamourResults.isNullOrEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("未找到相关幻化", color = PhoneMuted) }
                        else LazyColumn(Modifier.fillMaxSize()) {
                            items(glamourResults.orEmpty(), key = { it.id }) { g ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    ShizhijiaRemoteImage(url = g.mainImage, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(g.title, color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val line = listOf(g.areaName, g.groupName, g.characterName).filter { it.isNotBlank() }.joinToString(" · ")
                                        Text(line, color = PhoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("♥ ${g.likes}  ★ ${g.favorites}", color = PhoneMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    postResults.isNullOrEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("未找到相关帖子", color = PhoneMuted) }
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(postResults.orEmpty(), key = { it.postsId }) { post ->
                            SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) })
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dynamic detail (minimal)
// ---------------------------------------------------------------------------

@Composable
private fun ShizhijiaDynamicDetailScreen(state: PhoneState, id: String, pop: () -> Unit) {
    val context = LocalContext.current
    var d by remember { mutableStateOf<ShizhijiaDynamic?>(null) }
    LaunchedEffect(id) { d = ShizhijiaApi.getDynamicDetail(context, id) }
    ScreenFrame {
        ScreenHeader("动态详情", state, onBack = { pop() })
        val item = d
        if (item == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("加载中…", color = PhoneMuted) }; return@ScreenFrame }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShizhijiaRemoteImage(url = item.avatar, modifier = Modifier.size(40.dp).clip(CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(item.characterName, color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (item.createdAt.isNotBlank()) Text(item.createdAt, color = PhoneMuted, fontSize = 12.sp)
                        }
                    }
                    if (item.contentText.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(item.contentText, color = PhoneText, fontSize = 15.sp, lineHeight = 22.sp) }
                }
            }
            items(item.images) { img ->
                ShizhijiaRemoteImage(url = img, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Login via SDO pass WebView
// ---------------------------------------------------------------------------

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ShizhijiaLoginScreen(state: PhoneState, pop: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var done by remember { mutableStateOf(false) }
    // Show a friendly "loading" hint above the WebView while the QQ page loads,
    // which can take a while on a real device.
    var pageLoading by remember { mutableStateOf(false) }
    // null = probing, true = stored cookie works, false = no/invalid session.
    var verified by remember { mutableStateOf<Boolean?>(null) }

    // Validate whatever session cookie we already hold before trusting it.
    LaunchedEffect(Unit) {
        verified = if (ShizhijiaSession.hasSession(context)) ShizhijiaApi.isLoggedIn(context) else false
        android.util.Log.d("ShizhijiaLogin", "initial verified=$verified hasSession=${ShizhijiaSession.hasSession(context)}")
        if (verified == false) {
            // Discard a stale/invalid cookie so the app does not pretend to be logged in.
            ShizhijiaSession.clear(context)
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    // WeGame login: let the tgp authorize page build the OAuth state (it embeds a
    // QQ iframe that renders blank in WebView), then lift that iframe's URL to the
    // top-level page where the QQ QR code displays (0.7.164 confirmed top-level
    // QQ renders). The lifted URL keeps tgp's state, so the callback is complete.
    val loginUrl = ShizhijiaSession.loginUrl("https://ff14risingstones.web.sdo.com/pc/index.html#/me")
    val apiHost = "apiff14risingstones.web.sdo.com"

    // Only persists the session after a real isLogin probe succeeds using the
    // cookie just read from the WebView jar (it has not been persisted yet).
    fun tryFinalizeLogin() {
        if (done) return
        val cookie = ShizhijiaSession.cookieFromWebView()
        android.util.Log.d("ShizhijiaLogin", "cookie=(${cookie?.take(60) ?: "null"})")
        if (cookie.isNullOrBlank()) return
        scope.launch {
            val ok = ShizhijiaApi.isLoggedIn(context, cookie)
            android.util.Log.d("ShizhijiaLogin", "isLoggedIn(fullCookie)=$ok")
            if (ok) {
                ShizhijiaSession.save(context, cookie)
                done = true
                pop()
            }
        }
    }

    // Poll while the login form is visible: the WeGame/pass flow uses several
    // redirects and a final page finish may arrive before the session cookie
    // is usable, so a periodic probe is more reliable than one-shot detection.
    // Interval kept modest to avoid tripping server-side rate limits.
    LaunchedEffect(verified) {
        while (verified == false && !done) {
            tryFinalizeLogin()
            kotlinx.coroutines.delay(3_000)
        }
    }

    ScreenFrame {
        ScreenHeader("登录", state, onBack = { pop() })
        when (verified) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(30.dp))
            }
            true -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("已登录石之家", color = PhoneText, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    // Drop the stored cookie and clear the WebView jar so the
                    // next composition shows the SSO page again.
                    ShizhijiaSession.clear(context)
                    CookieManager.getInstance().removeAllCookies(null)
                    verified = false
                }, colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent)) { Text("重新登录") }
            }
            false -> Box(Modifier.fillMaxSize()) {
                AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        // The SSO dance jumps across domains (app -> pass.sdo.com
                        // -> app), so both first- and third-party cookies must be
                        // accepted for the session cookie to survive the redirects.
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        // A realistic phone Chrome UA avoids being mistaken for a bot.
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                        // The WeGame/QQ OAuth page mixes http sub-resources into
                        // an https page; WebView blocks mixed content by default
                        // (unlike a desktop browser), which blanks the page.
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageLoading = newProgress in 1..99
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                android.util.Log.d("ShizhijiaLogin", "onPageFinished url=${url?.take(90)}")
                                val host = url?.let { runCatching { android.net.Uri.parse(it).host }.getOrNull() }
                                // Lift the QQ login iframe out of the tgp authorize
                                // page so the QR code renders top-level in WebView.
                                // Poll a few times: the iframe can appear a moment
                                // after onPageFinished, especially on real devices.
                                if (host == "api.rail.tgp.qq.com" && url.orEmpty().contains("/login/authorize")) {
                                    fun pollLift(attempt: Int) {
                                        if (attempt <= 0) return
                                        view?.evaluateJavascript("(function(){var f=document.querySelector('iframe');return f?f.src:''})()") { r ->
                                            val src = (r ?: "").trim().removeSurrounding("\"")
                                            if (src.isNotBlank()) {
                                                android.util.Log.d("ShizhijiaLogin", "liftQQ iframe src=${src.take(90)}")
                                                view.post { if (view.url != src) view.loadUrl(src) }
                                            } else {
                                                view.postDelayed({ pollLift(attempt - 1) }, 700)
                                            }
                                        }
                                    }
                                    pollLift(12)
                                }
                                view?.evaluateJavascript(
                                    "setInterval(function(){var f=document.querySelector('iframe');var h=f?(f.offsetHeight+'x'+f.offsetWidth):'-';var ft='-';try{ft=f?f.contentDocument.body.innerText.length:0}catch(e){ft='x'}var cn=document.querySelectorAll('canvas').length;var im=document.querySelectorAll('img').length;var app=document.getElementById('app');console.log('DBG vis='+(app?getComputedStyle(app).visibility:'na')+' ifh='+h+' ifText='+ft+' canvas='+cn+' img='+im);},2000);", null)
                                if (host == apiHost || host == "ff14risingstones.web.sdo.com") tryFinalizeLogin()
                            }
                        }
                        loadUrl(loginUrl)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (pageLoading) {
                // Friendly hint over the WebView while the QQ page loads (slow on
                // real devices), so it never looks like the app is stuck.
                Box(Modifier.fillMaxSize().background(Color(0x66FFFFFF)), contentAlignment = Alignment.Center) {
                    Text("正在加载登录页,请稍候…", color = PhoneMuted, fontSize = 14.sp)
                }
            }
        }
    }
}
}

/** 签到日历页：本月签到记录 + 累计奖励表（满足天数可直接领取）。 */
@Composable
private fun ShizhijiaSignCalendarScreen(state: PhoneState, pop: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monthFmt = remember { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()) }
    var month by remember { mutableStateOf(monthFmt.format(java.util.Date())) }
    var log by remember { mutableStateOf<ShizhijiaSignLog?>(null) }
    var rewards by remember { mutableStateOf(listOf<ShizhijiaSignReward>()) }
    var claimingId by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            log = ShizhijiaApi.getSignLog(context, month)
            rewards = ShizhijiaApi.getSignRewards(context, month)
        }
    }
    LaunchedEffect(month) { reload() }

    // Signed days as day-of-month numbers for the chip strip.
    val daysInMonth = remember(month) {
        runCatching {
            val cal = java.util.Calendar.getInstance()
            cal.time = monthFmt.parse(month)!!
            cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        }.getOrDefault(31)
    }
    val signedDays = remember(log) {
        log?.days
            ?.mapNotNull { s -> s.split('-').lastOrNull()?.takeWhile { it.isDigit() }?.toIntOrNull() }
            ?.toSortedSet() ?: sortedSetOf<Int>()
    }
    val todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)

    ScreenFrame {
        ScreenHeader("签到日历", state, onBack = pop)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text("本月已签到 ", fontSize = 14.sp, color = PhoneText)
                    Text("${log?.count ?: 0}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PhoneAccent)
                    Text(" 天", fontSize = 13.sp, color = PhoneMuted)
                }
                // Day strip: 7 per row, signed days highlighted.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..daysInMonth).chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { day ->
                                val signed = day in signedDays
                                val today = day == todayDay && month == monthFmt.format(java.util.Date())
                                Text(
                                    day.toString(),
                                    fontSize = 12.sp,
                                    color = if (signed) PhoneOnAccentContainer else if (today) PhoneAccent else PhoneMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (signed) PhoneAccentContainer else Color(0xFFF0EDE6))
                                        .wrapContentSize(Alignment.Center),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("累计奖励", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PhoneText)
                Spacer(Modifier.height(4.dp))
            }
            items(rewards.size) { i ->
                val r = rewards[i]
                val claimable = r.isGet != 1 && log.let { it != null && it.count >= r.rule } && r.isGet != -1
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShizhijiaRemoteImage(url = r.itemPic, modifier = Modifier.size(46.dp), showPlaceholder = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.itemName, fontSize = 14.sp, color = PhoneText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("累计签到 ${r.rule} 天", fontSize = 12.sp, color = PhoneMuted)
                    }
                    when {
                        r.isGet == 1 -> Text("已领取", fontSize = 12.sp, color = PhoneMuted,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFE2E0E8)).padding(horizontal = 12.dp, vertical = 5.dp))
                        !claimable -> Text("未满足", fontSize = 12.sp, color = PhoneMuted,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFE2E0E8)).padding(horizontal = 12.dp, vertical = 5.dp))
                        else -> Text(if (claimingId == r.id) "…" else "领取", fontSize = 12.sp,
                            color = PhoneOnAccentContainer, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PhoneAccentContainer)
                                .clickable(enabled = claimingId == null) {
                                    scope.launch {
                                        claimingId = r.id
                                        val ok = ShizhijiaApi.claimSignReward(context, r.id, month)
                                        claimingId = null
                                        if (ok) android.widget.Toast.makeText(context, "奖励领取成功", android.widget.Toast.LENGTH_SHORT).show()
                                        else android.widget.Toast.makeText(context, "领取失败", android.widget.Toast.LENGTH_SHORT).show()
                                        reload()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}