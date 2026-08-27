package com.quserh.eorzeaphone.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Slider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaUserProfile
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGlamourDetail
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGlamourCard
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecentEvent
import kotlinx.coroutines.launch

// ---- 石之家专属极简设计 token（只作用于石之家，不污染全局主题） ----
// 跟随 App 的深/浅主题：浅色 = 暖米白底 + 深炭字 + 暗金；深色 = 近黑底 + 米白字 + 暖金。
private val SzjDarkBg = Color(0xFF0F1114)
private val SzjDarkCard = Color(0xFF16191E)
private val SzjDarkCardRaised = Color(0xFF1D2127)
private val SzjDarkGold = Color(0xFFC8A45E)
private val SzjDarkGoldSoft = Color(0xFF2A2417)
private val SzjDarkOnGoldSoft = Color(0xFFEBD5A2)
private val SzjDarkText = Color(0xFFECEAE4)
private val SzjDarkMuted = Color(0xFF9A968E)
private val SzjDarkLine = Color(0xFF2C3138)
private val SzjDarkHairline = Color(0xFF3B4048)
private val SzjLightBg = Color(0xFFF7F3EA)
private val SzjLightCard = Color(0xFFFFFFFF)
private val SzjLightCardRaised = Color(0xFFEFE9DC)
private val SzjLightGold = Color(0xFF8A6D2F)
private val SzjLightGoldSoft = Color(0xFFF1E6CB)
private val SzjLightOnGoldSoft = Color(0xFF4D3A12)
private val SzjLightText = Color(0xFF23201A)
private val SzjLightMuted = Color(0xFF6E6759)
private val SzjLightLine = Color(0xFFDCD3C0)
private val SzjLightHairline = Color(0xFFC9BFA9)

private val SzjBg: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightBg else SzjDarkBg
private val SzjCard: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightCard else SzjDarkCard
private val SzjCardRaised: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightCardRaised else SzjDarkCardRaised
private val SzjGold: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightGold else SzjDarkGold
private val SzjGoldSoft: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightGoldSoft else SzjDarkGoldSoft
private val SzjOnGoldSoft: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightOnGoldSoft else SzjDarkOnGoldSoft
private val SzjText: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightText else SzjDarkText
private val SzjMuted: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightMuted else SzjDarkMuted
private val SzjLine: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightLine else SzjDarkLine
private val SzjHairline: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) SzjLightHairline else SzjDarkHairline
private val SzjOnGold: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFFFFFFF) else Color(0xFF1A160D)
private val SzjCommentBg: Color @Composable get() = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFF1ECDF) else Color(0xFF14171B)

@Composable
private fun SzjHeader(title: String, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(SzjBg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                color = SzjGold,
                fontSize = 34.sp,
                lineHeight = 30.sp,
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = { onBack?.invoke() })
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Text(
                title,
                color = SzjText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            trailing?.invoke()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
    }
}

/**
 * 石之家 (FF14 Rising Stones official community) - the in-phone "app".
 *
 * Renders the forum through the public JSON API: a post feed with partitions,
 * post detail (HTML body), comments, search, and the login-gated dynamics feed.
 * The whole feature uses its own minimal "Rising Stones" design language:
 * near-black base, warm off-white text, antique-gold accents and thin hairline
 * dividers instead of the global purple Material3 theme. Internal navigation
 * uses a simple back stack so the system back button walks out level-by-level.
 */

private sealed interface SzjRoute {
    data object Home : SzjRoute
    data class PostDetail(val postId: String) : SzjRoute
    data class DynamicDetail(val id: String) : SzjRoute
    data object Search : SzjRoute
    data object Login : SzjRoute
    data object SignCalendar : SzjRoute
    data class UserProfile(val uuid: String) : SzjRoute
    data class GlamourDetail(val glamourId: String) : SzjRoute
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
/** 移动端风格定位图标（昵称与服务器之间的符号），颜色 #c4a86a（移动端 dwcolor 金色）。 */
@Composable
private fun SzjLocPin(sizeDp: Int = 16) {
    val ctx = LocalContext.current
    val pin = remember { runCatching { android.graphics.BitmapFactory.decodeStream(ctx.assets.open("loc_pin.png")) }.getOrNull() }
    if (pin != null) {
        Image(
            bitmap = pin.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFC4A86A)),
            modifier = Modifier.size(sizeDp.dp),
        )
    } else {
        Text("📍", color = Color(0xFFC4A86A), fontSize = (sizeDp * 0.85f).sp)
    }
}


@Composable
private fun SzjAvatar(name: String, avatar: String, uuid: String, sizeDp: Int) {
    val context = LocalContext.current
    var url by remember(uuid) { mutableStateOf(avatar) }
    LaunchedEffect(uuid, avatar) {
        if (url.isBlank() && uuid.isNotBlank()) {
            url = ShizhijiaApi.resolveAvatar(context, uuid)
        }
    }
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(SzjCardRaised)
        .border(1.dp, SzjLine, CircleShape), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            ShizhijiaRemoteImage(
                url = url,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                showPlaceholder = false,
            )
        } else {
            Text(name.take(1).ifBlank { "?" }, color = SzjMuted, fontSize = (sizeDp * 0.38f).sp)
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

/** Hoisted glamour feed state so it survives detail push/pop. */
private class SzjGlamourState {
    val tab = mutableStateOf(0)        // 0=全部 1=关注
    val sort = mutableStateOf(0)       // 0=推荐 1=最新
    val items = mutableStateOf(listOf<ShizhijiaGlamourCard>())
    val loading = mutableStateOf(false)
    val page = mutableStateOf(1)
    val ended = mutableStateOf(false)
    val loadedKey = mutableStateOf("")
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState()
    // 筛选
    val raceId = mutableStateOf(-1)
    val genderId = mutableStateOf(-1)
    val createTimeIdx = mutableStateOf(0)
    val filterOpen = mutableStateOf(false)
}

// 搜索状态提升到模块根部：进详情再返回时保留关键词/类型/结果与滚动位置
class SzjSearchState {
    val query = mutableStateOf("")
    val searchType = mutableStateOf(ShizhijiaApi.SEARCH_TYPE_POST)
    val hotWords = mutableStateOf(listOf<String>())
    val history = mutableStateOf(listOf<Pair<String, Int>>())
    val postResults = mutableStateOf<List<ShizhijiaPostCard>?>(null)
    val userResults = mutableStateOf<List<ShizhijiaSearchUser>?>(null)
    val glamourResults = mutableStateOf<List<ShizhijiaSearchGlamour>?>(null)
    val searching = mutableStateOf(false)
    val page = mutableStateOf(1)
    val ended = mutableStateOf(false)
    val loadingMore = mutableStateOf(false)
    val glamourGridState = androidx.compose.foundation.lazy.grid.LazyGridState()
    val postListState = androidx.compose.foundation.lazy.LazyListState()
    val userListState = androidx.compose.foundation.lazy.LazyListState()
}

@Composable
fun ShizhijiaScreen(state: PhoneState) {
    val context = LocalContext.current
    var stack by remember { mutableStateOf(listOf<SzjRoute>(SzjRoute.Home)) }
    val postsState = remember { SzjPostsState() }
    val glamourState = remember { SzjGlamourState() }
    val searchState = remember { SzjSearchState() }
    val homeMainTab = remember { mutableStateOf(MAIN_COMMUNITY) }
    val homeSubTab = remember { mutableStateOf(SUB_POSTS) }
    var barHeight by remember { mutableStateOf(56f) }
    var barBottom by remember { mutableStateOf(ShizhijiaSession.bottomBarBottom(context)) }
    LaunchedEffect(Unit) { barHeight = ShizhijiaSession.bottomBarHeight(context) }
    // Only swallow back while inside the app; the outer handler then leaves the desktop.
    BackHandler(enabled = stack.size > 1) { stack = stack.dropLast(1) }
    val route = stack.last()
    // nav pushes a destination; pop returns to the previous one (login success uses pop).
    val nav: (SzjRoute) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }
    Box(Modifier.fillMaxSize()) {
        when (route) {
SzjRoute.Home -> ShizhijiaHomeScreen(state, nav, postsState, glamourState, homeMainTab, homeSubTab, barHeightDp = barHeight, barBottomDp = barBottom, onBarHeightChange = { barHeight = it }, onBarBottomChange = { barBottom = it })
            is SzjRoute.PostDetail -> ShizhijiaPostDetailScreen(state, route.postId, pop, nav)
            is SzjRoute.DynamicDetail -> ShizhijiaDynamicDetailScreen(state, route.id, pop)
            SzjRoute.Search -> ShizhijiaSearchScreen(state, pop, nav, searchState)
            SzjRoute.Login -> ShizhijiaLoginScreen(state, pop)
            SzjRoute.SignCalendar -> ShizhijiaSignCalendarScreen(state, pop)
            is SzjRoute.UserProfile -> ShizhijiaUserProfileScreen(state, route.uuid, pop, nav)
            is SzjRoute.GlamourDetail -> ShizhijiaGlamourDetailScreen(state, route.glamourId, pop, nav)
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
    // Pinch-zoom + pan. All gestures are consumed here so the list underneath
    // never scrolls while the viewer is open.
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Box(Modifier.fillMaxSize().background(Color(0xE6000000))
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 5f)
                offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f },
            )
        }
    ) {
        val bmpV = bmp
        if (bmpV != null) {
            Image(bmpV.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y })
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(34.dp)) }
        }
        // Close button at the bottom-right corner.
        Text("✕", color = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFF4D3A12) else SzjGold, fontSize = 22.sp, modifier = Modifier.align(Alignment.BottomEnd)
            .padding(18.dp)
            .clip(CircleShape).background(SzjCardRaised.copy(alpha = 0.92f))
            .border(1.dp, SzjLine, CircleShape)
            .clickable(onClick = onClose).padding(horizontal = 14.dp, vertical = 8.dp))
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

/** Slightly raised backdrop for the comment area, distinct from the article
 *  body so the two regions are obvious while scrolling. */
private val CommentAreaBg: Color @Composable get() = SzjCommentBg

@Composable
private fun ShizhijiaHomeScreen(
    state: PhoneState,
    nav: (SzjRoute) -> Unit,
    postsState: SzjPostsState,
    glamourState: SzjGlamourState,
    mainTabState: MutableState<Int>,
    subTabState: MutableState<Int>,
    barHeightDp: Float,
    onBarHeightChange: (Float) -> Unit,
    barBottomDp: Float,
    onBarBottomChange: (Float) -> Unit,
) {
    val context = LocalContext.current
    var mainTab by mainTabState
    var subTab by subTabState
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

    ScreenFrame(background = SzjBg) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // 品牌标题行：金色标记 + 石之家 + 细线，主界面专属的极简识别元素。
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(4.dp).background(SzjGold))
                    Spacer(Modifier.width(8.dp))
                    Text("石之家", color = SzjText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.height(1.dp).weight(1f).background(SzjLine))
                }
                // Top bar (avatar / sign-in / search) belongs to the community
                // tab only; other tabs have their own headers.
                if (mainTab == MAIN_COMMUNITY) {
                    ShizhijiaTopBar(state, nav, loggedIn, loginUser, onSignIn, signedToday)
                }
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
                    MAIN_GLAMOUR -> ShizhijiaGlamourTab(nav, loggedIn, glamourState)
                    else -> ShizhijiaMeTab(state, nav, loggedIn, loginUser, barHeightDp, barBottomDp, onBarHeightChange, onBarBottomChange)
                }
            }
            SzjBottomBar(mainTab, onSelect = { mainTab = it }, barHeightDp = barHeightDp, barBottomDp = barBottomDp, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Top bar with the account entry (avatar + login label) and a check-in button. */
@Composable
private fun ShizhijiaTopBar(state: PhoneState, nav: (SzjRoute) -> Unit, loggedIn: Boolean, loginUser: ShizhijiaLoginUser?, onSignIn: () -> Unit, signedToday: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(SzjCardRaised).border(1.dp, SzjLine, CircleShape), contentAlignment = Alignment.Center) {
            val ava = loginUser?.avatar
            // Default portraits arrive as inline data:image URIs; decode them
            // here so we can fall back to the first character on any failure.
            val bmp = if (!ava.isNullOrBlank() && ava.startsWith("data:image")) remember(ava) { decodeDataUri(ava) } else null
            if (bmp != null) {
                Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else if (!ava.isNullOrBlank() && !ava.startsWith("data:image")) {
                ShizhijiaRemoteImage(url = ava, modifier = Modifier.fillMaxSize().clip(CircleShape), showPlaceholder = false)
            } else {
                Text(loginUser?.name?.take(1) ?: if (loggedIn) "我" else "?", color = SzjMuted, fontSize = 15.sp)
            }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(loginUser?.name ?: if (loggedIn) "已登录" else "未登录", color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                val server = listOfNotNull(loginUser?.area, loginUser?.group)
                if (server.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(server.joinToString(" "), color = SzjMuted, fontSize = 11.sp) }
                } else {
                    Text("石之家 · FF14 官方社区", color = SzjMuted, fontSize = 11.sp)
                }
            }
            // Check-in button flips to a greyed "已签到" once done today; clicking it
            // then opens the sign-in calendar (rewards + signed days) instead.
            Text(
                if (signedToday) "已签到" else "签到",
                color = if (signedToday) SzjMuted else SzjOnGoldSoft,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (signedToday) SzjCardRaised else SzjGoldSoft)
                    .clickable {
                        if (signedToday) nav(SzjRoute.SignCalendar) else onSignIn()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("⌕", color = SzjGold, fontSize = 22.sp, modifier = Modifier
                .clip(RoundedCornerShape(4.dp)).clickable { nav(SzjRoute.Search) }.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Text(label, color = if (selected) SzjGold else SzjMuted, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) SzjGold else Color.Transparent))
    }
}

@Composable
private fun SzjSectionPlaceholder(label: String) {
    Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.Center) { Text("「$label」开发中", color = SzjMuted) }
}

@Composable
private fun ShizhijiaMeTab(state: PhoneState, nav: (SzjRoute) -> Unit, loggedIn: Boolean, loginUser: ShizhijiaLoginUser?, bottomBarHeightDp: Float, barBottomDp: Float, onBarHeightChange: (Float) -> Unit, onBarBottomChange: (Float) -> Unit) {
    val context = LocalContext.current
    var bottomBarHeightDp by remember { mutableStateOf(bottomBarHeightDp) }
    var barBottomDp by remember { mutableStateOf(barBottomDp) }
    LaunchedEffect(barBottomDp) { onBarBottomChange(barBottomDp) }
    LaunchedEffect(bottomBarHeightDp) { onBarHeightChange(bottomBarHeightDp) }
    var showSettings by remember { mutableStateOf(false) }
    val p = loginUser
    Column(Modifier.fillMaxSize().padding(bottom = 90.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        if (showSettings) {
            // ---- 设置页 ----
            Text("设置", color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("外观设置", color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("悬浮底栏高度: ${bottomBarHeightDp.toInt()} dp", color = SzjMuted, fontSize = 12.sp)
            Slider(
                value = bottomBarHeightDp,
                onValueChange = {
                    bottomBarHeightDp = it
                    ShizhijiaSession.setBottomBarHeight(context, it)
                },
                valueRange = 48f..96f,
            )
            Spacer(Modifier.height(10.dp))
            Text("距底部距离: " + barBottomDp.toInt() + " dp", color = SzjMuted, fontSize = 12.sp)
            Slider(
                value = barBottomDp,
                onValueChange = {
                    barBottomDp = it
                    ShizhijiaSession.setBottomBarBottom(context, it)
                },
                valueRange = 0f..40f,
            )
            Spacer(Modifier.height(16.dp))
            if (loggedIn) {
                Button(onClick = {
                    ShizhijiaSession.clear(context)
                    showSettings = false
                    android.widget.Toast.makeText(context, "已退出登录", android.widget.Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = SzjGold, contentColor = SzjOnGold), modifier = Modifier.fillMaxWidth()) { Text("退出登录") }
            }
            Spacer(Modifier.height(10.dp))
            Text("返回", color = SzjGold, fontSize = 14.sp, modifier = Modifier.clickable { showSettings = false }.padding(8.dp))
        } else if (loggedIn) {
            // ---- 资料头卡: 头像 + 关注/粉丝/获赞 ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SzjAvatar(p?.name ?: "", p?.avatar ?: "", "", 64)
                Spacer(Modifier.width(16.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("关注" to 0, "粉丝" to 0, "获赞" to 0).forEach { (label, num) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$num", color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(label, color = SzjMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(p?.name ?: "已登录", color = SzjText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(listOfNotNull(p?.area, p?.group).joinToString(" "), color = SzjMuted, fontSize = 12.sp) }
            Spacer(Modifier.height(14.dp))
            // ---- 入口宫格 ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("收藏", "我的部队", "招募管理", "设置").forEach { label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                            if (label == "设置") showSettings = true
                            else android.widget.Toast.makeText(context, label + "开发中", android.widget.Toast.LENGTH_SHORT).show()
                        }.padding(8.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(4.dp)).background(SzjCardRaised)
                            .border(1.dp, SzjLine, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text(label.take(1), color = SzjGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(label, color = SzjMuted, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("切换角色", "专项数据").forEach { label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                            android.widget.Toast.makeText(context, label + "开发中", android.widget.Toast.LENGTH_SHORT).show()
                        }.padding(8.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(4.dp)).background(SzjCardRaised)
                            .border(1.dp, SzjLine, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Text(label.take(1), color = SzjGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(label, color = SzjMuted, fontSize = 11.sp)
                    }
                }
            }
        } else {
            Text("未登录", color = SzjMuted)
            Spacer(Modifier.height(14.dp))
            Button(onClick = { nav(SzjRoute.Login) }, colors = ButtonDefaults.buttonColors(containerColor = SzjGold, contentColor = SzjOnGold)) { Text("登录") }
        }
    }
}

/** MD3 floating bottom bar: a rounded capsule that hovers over the content. */
@Composable
private fun SzjBottomBar(selected: Int, onSelect: (Int) -> Unit, barHeightDp: Float, barBottomDp: Float, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp + barBottomDp.dp).fillMaxWidth().height(barHeightDp.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(SzjCard)
            .border(1.dp, SzjLine, RoundedCornerShape(18.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp),
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
    // Selected tab is wrapped in a filled circle, like a floating action chip.
    Box(
        Modifier.size(56.dp)
            .clip(CircleShape)
            .background(if (selected) SzjGoldSoft else Color.Transparent)
            .border(if (selected) 1.dp else 0.dp, SzjGold.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) SzjOnGoldSoft else SzjMuted, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
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
                CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(30.dp))
            }
            ps.posts.value.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无帖子", color = SzjMuted)
            }
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 96.dp)) {
                items(ps.posts.value, key = { it.postsId }) { post ->
                    SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) })
                }
                item(key = "loading-footer") {
                    if (ps.loading.value) Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(22.dp))
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
        color = if (selected) SzjOnGoldSoft else SzjMuted,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(if (selected) SzjGoldSoft else SzjCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun SzjPostRow(post: ShizhijiaPostCard, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp)).background(SzjCard)
            .border(1.dp, SzjLine, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        // Line 1: title with the [partition] tag on its left.
        Row(verticalAlignment = Alignment.Top) {
            if (post.partName.isNotBlank()) {
                Text(
                    post.partName,
                    color = SzjOnGoldSoft, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, end = 6.dp)
                        .clip(RoundedCornerShape(4.dp)).background(SzjGoldSoft)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(
                post.title,
                color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
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
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cell = (maxWidth - 12.dp) / 3
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.coverPics.distinct().take(3).forEach { url ->
                        ShizhijiaRemoteImage(url = url, modifier = Modifier.width(cell).height(cell).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop, showPlaceholder = false, collapseOnFail = true, onClick = { SzjViewer.url = it })
                    }
                }
            }
        }
        // Line 3: author on the left; comment / read counts on the right.
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(post.characterName.ifBlank { "匿名玩家" }, color = SzjMuted, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (post.groupName.isNotBlank()) {
                Spacer(Modifier.width(3.dp))
                SzjLocPin(14)
                Spacer(Modifier.width(2.dp))
                Text(post.groupName, color = SzjMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            if (post.commentCount > 0) Text(" ${post.commentCount}评论 ", color = SzjMuted, fontSize = 11.sp)
            if (post.readCount > 0) Text("${post.readCount}阅读", color = SzjMuted, fontSize = 11.sp)
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
                    Text("登录后查看关注动态", color = SzjMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { nav(SzjRoute.Login) }, colors = ButtonDefaults.buttonColors(containerColor = SzjGold, contentColor = SzjOnGold)) { Text("登录") }
                }
            }
        } else if (loading && dynamics.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(30.dp)) }
        } else if (dynamics.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无动态", color = SzjMuted) }
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
            .clip(RoundedCornerShape(4.dp)).background(SzjCard)
            .border(1.dp, SzjLine, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SzjAvatar(d.characterName, d.avatar, d.uuid, 38)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(d.characterName.ifBlank { "光之战士" }, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val dserver = listOf(d.areaName, d.groupName).filter { it.isNotBlank() }.joinToString(" ")
                if (dserver.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(dserver, color = SzjMuted, fontSize = 11.sp) }
                if (d.createdAt.isNotBlank()) Text(d.createdAt, color = SzjMuted, fontSize = 11.sp)
            }
        }
        if (d.contentText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(d.contentText, color = SzjText, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
        d.images.firstOrNull()?.let { first ->
            Spacer(Modifier.height(8.dp))
            ShizhijiaRemoteImage(url = first, modifier = Modifier.fillMaxWidth().height(160.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            if (d.likeCount > 0) Text("赞 ${d.likeCount}", color = SzjMuted, fontSize = 12.sp)
            if (d.commentCount > 0) { Spacer(Modifier.width(12.dp)); Text("评论 ${d.commentCount}", color = SzjMuted, fontSize = 12.sp) }
        }
    }
}

// ---------------------------------------------------------------------------
// Post detail
// ---------------------------------------------------------------------------

@Composable
private fun ShizhijiaPostDetailScreen(state: PhoneState, postId: String, pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<ShizhijiaPostDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Comments are rendered inline below the article body on the same screen.
    // Server order values: default (post time asc) / hot (likes) / time (newest).
    var commentOrder by remember { mutableStateOf("earliest") }
    var onlyAuthor by remember { mutableStateOf(false) } // 只看楼主
    var comments by remember { mutableStateOf(listOf<ShizhijiaComment>()) }
    var commentPage by remember { mutableStateOf(1) }
    var commentPageTime by remember { mutableStateOf("") }
    var commentLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(postId) {
        loading = true
        detail = ShizhijiaApi.getPostDetail(context, postId)
        loading = false
    }
    // (Re)load comments whenever the ordering changes. The old list stays
    // visible while fetching and the scroll position is not touched.
    LaunchedEffect(postId, commentOrder, onlyAuthor) {
        commentLoading = true
        val result = ShizhijiaApi.getPostComments(context, postId, commentOrder, onlyLandlord = onlyAuthor)
        comments = result.rows; commentPageTime = result.pageTime; commentPage = 1
        commentLoading = false
    }
    // Infinite scroll for comments.
    val nearEnd by remember { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= comments.size - 2
    } }
    LaunchedEffect(nearEnd, commentOrder, postId, onlyAuthor) {
        if (nearEnd && !commentLoading && comments.isNotEmpty() && commentPageTime.isNotBlank()) {
            commentLoading = true
            val next = ShizhijiaApi.getPostComments(context, postId, commentOrder, page = commentPage + 1, pageTime = commentPageTime, onlyLandlord = onlyAuthor)
            if (next.rows.isEmpty()) commentPageTime = "" else {
                comments = comments + next.rows
                commentPageTime = next.pageTime
                commentPage += 1
            }
            commentLoading = false
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("帖子详情", onBack = { pop() })
        if (loading && detail == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(30.dp)) }
            return@ScreenFrame
        }
        val d = detail
        if (d == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("加载失败", color = SzjMuted) }
            return@ScreenFrame
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(d.title, color = SzjText, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { nav(SzjRoute.UserProfile(d.uuid)) }) {
                        SzjAvatar(d.characterName, d.avatar, d.uuid, 36)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.characterName, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(listOf(d.areaName, d.groupName).filter { it.isNotBlank() }.joinToString(" "), color = SzjMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); if (d.createdAt.isNotBlank()) { Text(" " + d.createdAt, color = SzjMuted, fontSize = 11.sp, maxLines = 1) } }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (d.readCount > 0) Text("阅读 ${d.readCount}", color = SzjMuted, fontSize = 12.sp)
                        if (d.likeCount > 0) Text("赞 ${d.likeCount}", color = SzjMuted, fontSize = 12.sp)
                        if (d.commentCount > 0) Text("评论 ${d.commentCount}", color = SzjMuted, fontSize = 12.sp)
                        if (d.starCount > 0) Text("收藏 ${d.starCount}", color = SzjMuted, fontSize = 12.sp)
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
                    Text("全部评论", color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Row(Modifier.clip(RoundedCornerShape(4.dp)).background(SzjCard)) {
                        SzjSmallOption("默认", commentOrder == "earliest") { commentOrder = "earliest" }
                        SzjSmallOption("热门", commentOrder == "hottest") { commentOrder = "hottest" }
                        SzjSmallOption("最新", commentOrder == "latest") { commentOrder = "latest" }
                    }
                    Spacer(Modifier.width(8.dp))
                    // 只看楼主: client-side filter on the loaded comment list.
                    Text("只看楼主", color = if (onlyAuthor) SzjOnGoldSoft else SzjMuted, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(if (onlyAuthor) SzjGoldSoft else SzjCard)
                            .clickable { onlyAuthor = !onlyAuthor }
                            .padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            if (commentLoading && comments.isEmpty()) {
                item(key = "comments-loading") {
                    Box(Modifier.fillMaxWidth().background(CommentAreaBg).padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(24.dp)) }
                }
            } else if (comments.isEmpty()) {
                item(key = "comments-empty") { Text("暂无评论", color = SzjMuted, modifier = Modifier.fillMaxWidth().background(CommentAreaBg).padding(24.dp), textAlign = TextAlign.Center) }
            } else {
                items(comments, key = { it.id }) { c -> SzjCommentRow(c, nav) }
                item(key = "comments-footer") {
                    if (commentLoading) Box(Modifier.fillMaxWidth().background(CommentAreaBg).padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(20.dp))
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
    Text(label, color = if (selected) SzjOnGoldSoft else SzjMuted, fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(if (selected) SzjGoldSoft else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
private fun SzjCommentRow(c: ShizhijiaComment, nav: (SzjRoute) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(4.dp)).background(SzjCard)
        .border(1.dp, SzjLine, RoundedCornerShape(4.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { nav(SzjRoute.UserProfile(c.uuid)) }) {
            SzjAvatar(c.characterName, c.avatar, c.uuid, 30)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.characterName.ifBlank { "匿名玩家" }, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (c.isPostsAuthor) Text(" 作者", color = SzjGold, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(listOf(c.areaName, c.groupName).filter { it.isNotBlank() }.joinToString(" "), color = SzjMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); if (c.createdAt.isNotBlank()) { Text(" " + c.createdAt, color = SzjMuted, fontSize = 11.sp, maxLines = 1) } }
            }
            if (c.likeCount > 0) Text("赞 ${c.likeCount}", color = SzjMuted, fontSize = 11.sp)
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
private fun ShizhijiaSearchScreen(state: PhoneState, pop: () -> Unit, nav: (SzjRoute) -> Unit, s: SzjSearchState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var typeMenu by remember { mutableStateOf(false) }
    val query = s.query
    val searchType = s.searchType
    val hotWords = s.hotWords
    val history = s.history
    val postResults = s.postResults
    val userResults = s.userResults
    val glamourResults = s.glamourResults
    val searching = s.searching
    val page = s.page
    val ended = s.ended
    val loadingMore = s.loadingMore
    // Search channel: 帖子 / 攻略 / 用户 / 幻化 (common/search type ids).
    val typeLabel = when (searchType.value) {
        ShizhijiaApi.SEARCH_TYPE_STRAT -> "攻略"
        ShizhijiaApi.SEARCH_TYPE_USER -> "用户"
        ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> "幻化"
        else -> "帖子"
    }
    LaunchedEffect(Unit) { hotWords.value = ShizhijiaApi.getHotSearchList(context).map { it.text }.filter { it.isNotBlank() }.distinct() }

    fun doSearch() {
        val q = query.value.trim()
        if (q.isEmpty()) return
        ShizhijiaSession.addSearchHistory(context, q, searchType.value)
        history.value = ShizhijiaSession.searchHistory(context)
        scope.launch {
            searching.value = true
            loadingMore.value = false
            ended.value = false
            page.value = 1
            postResults.value = null; userResults.value = null; glamourResults.value = null
            when (searchType.value) {
                ShizhijiaApi.SEARCH_TYPE_USER -> userResults.value = ShizhijiaApi.searchUsers(context, q)
                ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> glamourResults.value = ShizhijiaApi.searchGlamours(context, q, page.value)
                else -> postResults.value = ShizhijiaApi.searchPosts(context, q, searchType.value, page.value)
            }
            searching.value = false
        }
    }

    fun loadMore() {
        val q = query.value.trim()
        if (q.isEmpty() || ended.value || loadingMore.value || searching.value) return
        loadingMore.value = true
        scope.launch {
            val nextPage = page.value + 1
            when (searchType.value) {
                ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> {
                    val next = ShizhijiaApi.searchGlamours(context, q, nextPage)
                    glamourResults.value = (glamourResults.value.orEmpty() + next)
                    if (next.isEmpty()) ended.value = true else page.value = nextPage
                }
                else -> {
                    val next = ShizhijiaApi.searchPosts(context, q, searchType.value, nextPage)
                    postResults.value = (postResults.value.orEmpty() + next)
                    if (next.isEmpty()) ended.value = true else page.value = nextPage
                }
            }
            loadingMore.value = false
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("搜索", onBack = { pop() })
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(SzjCard)
                .border(1.dp, SzjLine, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                // 类型切换与输入框融合在同一搜索栏内（左侧）
                Box {
                    Row(Modifier.clip(RoundedCornerShape(4.dp)).background(SzjCardRaised).border(1.dp, SzjLine, RoundedCornerShape(4.dp))
                        .clickable { typeMenu = true }.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(typeLabel, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("▾", color = SzjMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                    androidx.compose.material3.DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        listOf(
                            "帖子" to ShizhijiaApi.SEARCH_TYPE_POST,
                            "攻略" to ShizhijiaApi.SEARCH_TYPE_STRAT,
                            "用户" to ShizhijiaApi.SEARCH_TYPE_USER,
                            "幻化" to ShizhijiaApi.SEARCH_TYPE_GLAMOUR,
                        ).forEach { (label, id) ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(label, color = if (id == searchType.value) SzjGold else SzjText) },
                                onClick = { searchType.value = id; typeMenu = false },
                            )
                        }
                    }
                }
                BasicTextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    singleLine = true,
                    textStyle = TextStyle(color = SzjText, fontSize = 15.sp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { doSearch() }),
                    decorationBox = { inner ->
                        Box {
                            if (query.value.isEmpty()) Text("搜索$typeLabel", color = SzjMuted, fontSize = 15.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp),
                )
                // 放大镜图标替换原来的“搜索”文字按钮
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).background(SzjGoldSoft)
                    .border(1.dp, SzjGold.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                    .clickable { doSearch() }, contentAlignment = Alignment.Center) {
                    Text("⌕", color = SzjOnGoldSoft, fontSize = 18.sp)
                }
            }
            if (postResults.value == null && userResults.value == null && glamourResults.value == null && !searching.value) {
                if (history.value.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索记录", color = SzjMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("清空", color = SzjMuted, fontSize = 11.sp,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                                history.value.forEach { ShizhijiaSession.removeSearchHistory(context, it.first, it.second) }
                                history.value = emptyList()
                            }.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    // History chips: tap = quick search, long-press = remove entry.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        history.value.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (q, t) ->
                                    val typeLabel2 = when (t) {
                                        ShizhijiaApi.SEARCH_TYPE_STRAT -> "攻略"
                                        ShizhijiaApi.SEARCH_TYPE_USER -> "用户"
                                        ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> "幻化"
                                        else -> "帖子"
                                    }
                                    Text("$typeLabel2·$q", fontSize = 12.sp, color = SzjText,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SzjCardRaised)
                                            .pointerInput(q, t) {
                                                detectTapGestures(
                                                    onTap = { query.value = q; searchType.value = t; doSearch() },
                                                    onLongPress = {
                                                        ShizhijiaSession.removeSearchHistory(context, q, t)
                                                        history.value = ShizhijiaSession.searchHistory(context)
                                                    },
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Text("热门搜索", color = SzjMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    hotWords.value.take(6).forEach { word ->
                        SzjPartChip(word, selected = false) { query.value = word; doSearch() }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                when {
                    searching.value -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(28.dp)) }
                    searchType.value == ShizhijiaApi.SEARCH_TYPE_USER -> {
                        if (userResults.value.isNullOrEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("未找到相关用户", color = SzjMuted) }
                        else LazyColumn(state = s.userListState, modifier = Modifier.fillMaxSize()) {
                            items(userResults.value.orEmpty(), key = { it.uuid }) { u ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { nav(SzjRoute.UserProfile(u.uuid)) }, verticalAlignment = Alignment.CenterVertically) {
                                    SzjAvatar(u.name, u.avatar, u.uuid, 44)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(u.name, color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        val line = listOf(u.areaName, u.groupName).filter { it.isNotBlank() }.joinToString(" ")
                                        if (line.isNotBlank()) Text(line, color = SzjMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (u.profile.isNotBlank()) Text(u.profile, color = SzjMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("粉丝 ${u.fansNum}", color = SzjMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    searchType.value == ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> {
                        if (glamourResults.value.isNullOrEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("未找到相关幻化", color = SzjMuted) }
                        else androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            state = s.glamourGridState,
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val results = glamourResults.value.orEmpty()
                            items(results.size, key = { results[it].id }) { idx ->
                                val g = results[idx]
                                // 一行三列，只显示头图，点击进入幻化详情（不是预览）
                                Box(Modifier.clip(RoundedCornerShape(4.dp)).clickable { nav(SzjRoute.GlamourDetail(g.id)) }) {
                                    SzjGlamourImage(url = g.mainImage)
                                }
                            }
                        }
                    }
                    postResults.value.isNullOrEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("未找到相关帖子", color = SzjMuted) }
                    else -> LazyColumn(state = s.postListState, modifier = Modifier.fillMaxSize()) {
                        items(postResults.value.orEmpty(), key = { it.postsId }) { post ->
                            SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) })
                        }
                    }
                }
                // 滚动接近底部自动加载下一页（幻化/帖子）
                val gridNearEnd = remember { derivedStateOf {
                    val last = s.glamourGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = glamourResults.value?.size ?: 0
                    searchType.value == ShizhijiaApi.SEARCH_TYPE_GLAMOUR && total > 0 && last >= total - 3
                } }
                val listNearEnd = remember { derivedStateOf {
                    val last = s.postListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = postResults.value?.size ?: 0
                    searchType.value != ShizhijiaApi.SEARCH_TYPE_GLAMOUR && searchType.value != ShizhijiaApi.SEARCH_TYPE_USER && total > 0 && last >= total - 3
                } }
                LaunchedEffect(gridNearEnd.value, listNearEnd.value, loadingMore.value, ended.value) {
                    if ((gridNearEnd.value || listNearEnd.value) && !loadingMore.value && !ended.value) loadMore()
                }
                if (loadingMore.value) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(20.dp))
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
    ScreenFrame(background = SzjBg) {
        SzjHeader("动态详情", onBack = { pop() })
        val item = d
        if (item == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("加载中…", color = SzjMuted) }; return@ScreenFrame }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShizhijiaRemoteImage(url = item.avatar, modifier = Modifier.size(40.dp).clip(CircleShape)
                            .border(1.dp, SzjLine, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(item.characterName, color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (item.createdAt.isNotBlank()) Text(item.createdAt, color = SzjMuted, fontSize = 12.sp)
                        }
                    }
                    if (item.contentText.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(item.contentText, color = SzjText, fontSize = 15.sp, lineHeight = 22.sp) }
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

    ScreenFrame(background = SzjBg) {
        SzjHeader("登录", onBack = { pop() })
        when (verified) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(30.dp))
            }
            true -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("已登录石之家", color = SzjText, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    // Drop the stored cookie and clear the WebView jar so the
                    // next composition shows the SSO page again.
                    ShizhijiaSession.clear(context)
                    CookieManager.getInstance().removeAllCookies(null)
                    verified = false
                }, colors = ButtonDefaults.buttonColors(containerColor = SzjGold, contentColor = SzjOnGold)) { Text("重新登录") }
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
                Box(Modifier.fillMaxSize().background(SzjBg.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("正在加载登录页，请稍候…", color = SzjMuted, fontSize = 14.sp)
                    }
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

    ScreenFrame(background = SzjBg) {
        SzjHeader("签到日历", onBack = pop)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp).clip(RoundedCornerShape(4.dp))
                    .background(SzjCardRaised).border(1.dp, SzjLine, RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
                    Text("本月已签到 ", fontSize = 14.sp, color = SzjText)
                    Text("${log?.count ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SzjGold)
                    Text(" 天", fontSize = 13.sp, color = SzjMuted)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(6.dp).background(SzjGold))
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
                                    color = if (signed) SzjOnGoldSoft else if (today) SzjGold else SzjMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (signed) SzjGoldSoft else SzjCardRaised)
                                        .border(1.dp, if (today) SzjGold.copy(alpha = 0.8f) else SzjLine, RoundedCornerShape(4.dp))
                                        .wrapContentSize(Alignment.Center),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("累计奖励", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SzjText)
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
                        Text(r.itemName, fontSize = 14.sp, color = SzjText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("累计签到 ${r.rule} 天", fontSize = 12.sp, color = SzjMuted)
                    }
                    when {
                        r.isGet == 1 -> Text("已领取", fontSize = 12.sp, color = SzjMuted,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SzjCardRaised)
                                .border(1.dp, SzjLine, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 5.dp))
                        !claimable -> Text("未满足", fontSize = 12.sp, color = SzjMuted,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SzjCardRaised)
                                .border(1.dp, SzjLine, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 5.dp))
                        else -> Text(if (claimingId == r.id) "…" else "领取", fontSize = 12.sp,
                            color = SzjOnGoldSoft, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SzjGoldSoft)
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

/** 种族/部族中文名（id 与官方一致）。 */
private fun szjRaceName(id: Int) = when (id) {
    1 -> "人族"; 2 -> "精灵族"; 3 -> "拉拉菲尔族"; 4 -> "猫魅族"
    5 -> "鲁加族"; 6 -> "敖龙族"; 7 -> "硌狮族"; 8 -> "维埃拉族"; else -> ""
}

private fun szjTribeName(id: Int) = when (id) {
    1 -> "中原之民"; 2 -> "高地之民"; 3 -> "森林之民"; 4 -> "黑影之民"
    5 -> "平原之民"; 6 -> "丘陵之民"; 7 -> "逐日之民"; 8 -> "追月之民"
    9 -> "海洋之民"; 10 -> "红血之民"; 11 -> "晨曦之民"; 12 -> "月影之民"
    13 -> "日耀之民"; 14 -> "流浪之民"; 15 -> "拉维之民"; 16 -> "维娜之民"; else -> ""
}

/** 能工巧匠/大地使者没有官方图标资源，用标准英文缩写徽章代替。 */
private fun szjCrafterAbbr(name: String) = when (name) {
    "刻木匠" -> "CRP"; "锻铁匠" -> "BSM"; "铸甲匠" -> "ARM"; "雕金匠" -> "GLD"
    "制革匠" -> "LTH"; "裁衣匠" -> "WVR"; "炼金术士" -> "ALC"; "烹调师" -> "CUL"
    "采矿工" -> "MIN"; "园艺工" -> "BTN"; "捕鱼人" -> "FSH"; else -> name.take(1)
}

/** 玩家主页：资料卡(头像/UID/粉丝获赞) + 信息(职业/种族/部队/游戏数据) + TA的帖子。 */
@Composable
private fun ShizhijiaUserProfileScreen(state: PhoneState, uuid: String, pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ShizhijiaUserProfile?>(null) }
    var avatarUrl by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) } // 0=信息 1=帖子
    var posts by remember { mutableStateOf(listOf<ShizhijiaPostCard>()) }
    var postLoading by remember { mutableStateOf(false) }
    var postPage by remember { mutableStateOf(1) }
    var postEnded by remember { mutableStateOf(false) }

    var jobIcons by remember { mutableStateOf(mapOf<String, String>()) }
    var tipCareer by remember { mutableStateOf<String?>(null) }
    var recents by remember { mutableStateOf(listOf<ShizhijiaRecentEvent>()) }
    var recentsPrivate by remember { mutableStateOf(false) }
    LaunchedEffect(uuid) {
        jobIcons = ShizhijiaApi.jobIconByName(context)
        android.util.Log.d("ShizhijiaLogin", "profile jobIcons size=${jobIcons.size} crp=${jobIcons["裁衣匠"] ?: "MISS"}")
        profile = ShizhijiaApi.getUserProfile(context, uuid)
        scope.launch {
            val r = ShizhijiaApi.getRecentEvents(context, uuid)
            recents = r
            recentsPrivate = r.isEmpty()
        }
        val p = profile
        avatarUrl = when {
            p == null -> ""
            p.avatar.isNotBlank() -> p.avatar
            p.race > 0 && p.tribe > 0 && p.gender >= 0 ->
                "https://static.web.sdo.com/jijiamobile/pic/ff14/2023ffstone/${p.race}-${p.tribe}-${p.gender}.jpg"
            else -> ""
        }
    }
    LaunchedEffect(uuid, tab) {
        if (tab == 1 && posts.isEmpty() && !postEnded) {
            postLoading = true
            val next = ShizhijiaApi.getUserPosts(context, uuid, postPage)
            posts = posts + next.rows
            if (next.rows.isEmpty()) postEnded = true else postPage += 1
            postLoading = false
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("玩家主页", onBack = pop)
        val p = profile
        if (p == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(28.dp)) }
            return@ScreenFrame
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SzjAvatar(p.name, avatarUrl, p.uuid, 64)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = SzjText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(listOf(p.areaName, p.groupName).filter { it.isNotBlank() }.joinToString(" "), color = SzjMuted, fontSize = 12.sp) }
                            Text("UID $uuid", color = SzjMuted, fontSize = 11.sp)
                        }
                    }
                    if (p.profile.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(p.profile, color = SzjText, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(SzjCardRaised)
                        .border(1.dp, SzjLine, RoundedCornerShape(4.dp)).padding(vertical = 10.dp)) {
                        listOf("关注" to p.followNum, "粉丝" to p.fansNum, "获赞" to p.likedNum).forEach { (label, num) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$num", color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(label, color = SzjMuted, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            // Tabs: 信息 / 帖子
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    listOf("信息" to 0, "帖子" to 1).forEach { (label, id) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { tab = id }) {
                            Text(label, fontSize = 14.sp,
                                color = if (tab == id) SzjGold else SzjMuted,
                                fontWeight = if (tab == id) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 4.dp))
                            Box(Modifier.fillMaxWidth().height(2.dp).background(if (tab == id) SzjGold else Color.Transparent))
                        }
                    }
                }
            }
            if (tab == 0) {
                // ---- 信息 tab ----
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val battle = p.careers.filter { it.type !in listOf("能工巧匠", "大地使者") }.sortedByDescending { it.level }
                        val craft = p.careers.filter { it.type in listOf("能工巧匠", "大地使者") }.sortedByDescending { it.level }
                        if (battle.isNotEmpty()) {
                            Text("战斗精英 & 魔法导师", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                battle.chunked(5).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        row.forEach { c ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                val icon = jobIcons[c.name].orEmpty()
                                                val abbr = szjCrafterAbbr(c.name)
                                                var showTip = tipCareer == c.name
                                                Box {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.clickable { tipCareer = if (showTip) null else c.name }) {
                                                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).background(SzjCardRaised), contentAlignment = Alignment.Center) {
                                                            if (icon.isNotBlank()) ShizhijiaRemoteImage(url = icon, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, showPlaceholder = false)
                                                            else Text(abbr, color = SzjGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                        Text("${c.level}", fontSize = 10.sp, color = SzjText)
                                                    }
                                                    if (showTip) {
                                                        // Small bubble above the icon with the job name.
                                                        Box(Modifier.matchParentSize()) {
                                                            Text(c.name, color = SzjOnGoldSoft, fontSize = 10.sp,
                                                                modifier = Modifier.align(Alignment.TopCenter)
                                                                    .offset(y = (-22).dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(SzjGoldSoft)
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (craft.isNotEmpty()) {
                            Text("能工巧匠 & 大地使者", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                craft.chunked(5).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        row.forEach { c ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                val icon = jobIcons[c.name].orEmpty()
                                                val abbr = szjCrafterAbbr(c.name)
                                                var showTip = tipCareer == c.name
                                                Box {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.clickable { tipCareer = if (showTip) null else c.name }) {
                                                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).background(SzjCardRaised), contentAlignment = Alignment.Center) {
                                                            if (icon.isNotBlank()) ShizhijiaRemoteImage(url = icon, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, showPlaceholder = false)
                                                            else Text(abbr, color = SzjGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                        Text("${c.level}", fontSize = 10.sp, color = SzjText)
                                                    }
                                                    if (showTip) {
                                                        // Small bubble above the icon with the job name.
                                                        Box(Modifier.matchParentSize()) {
                                                            Text(c.name, color = SzjOnGoldSoft, fontSize = 10.sp,
                                                                modifier = Modifier.align(Alignment.TopCenter)
                                                                    .offset(y = (-22).dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(SzjGoldSoft)
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        val genderText = when (p.gender) { 0 -> "男"; 1 -> "女"; else -> "" }
                        listOf(
                            "种族性别" to listOfNotNull(szjRaceName(p.race).ifBlank { null }, szjTribeName(p.tribe).ifBlank { null }, genderText.takeIf { it.isNotBlank() }).joinToString(""),
                            "部队名称" to listOfNotNull(p.guildName.takeIf { it.isNotBlank() }, p.guildTag.takeIf { it.isNotBlank() }?.let { "<$it>" }).joinToString(" "),
                            "创作时间" to p.createTime,
                            "最近登录时间" to p.lastLoginTime,
                            "累计游戏时长" to p.playTime,
                            "房屋信息" to p.houseInfo,
                            "幻理模板使用次数" to if (p.washingNum > 0) p.washingNum.toString() else "",
                            "伪零挑战击败数" to if (p.killTimes > 0) p.killTimes.toString() else "",
                            "水晶沙竞技场段位" to p.crystalRank,
                            "钓鱼抛竿次数" to if (p.fishTimes > 0) p.fishTimes.toString() else "",
                            "称号宝物击败数" to if (p.treasureTimes > 0) p.treasureTimes.toString() else "",
                            "无人岛开拓等级" to if (p.newrank > 0) p.newrank.toString() else "",
                        ).forEach { (label, value) ->
                            if (value.isNotBlank()) {
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Text(label + "：", color = SzjMuted, fontSize = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(value, color = SzjText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // 特殊成就: medal icons + name/detail/time.
                        if (p.achievements.isNotEmpty()) {
                            Text("特殊成就 (${p.achievements.size})", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            p.achievements.take(20).forEach { a ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val medalUrl = if (a.medalId.isNotBlank())
                                        "https://static.web.sdo.com/jijiamobile/pic/ff14/ffstones/medal/medal${a.medalId}.png" else ""
ShizhijiaRemoteImage(url = medalUrl, modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit, showPlaceholder = false)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(a.name, color = SzjText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (a.detail.isNotBlank()) Text(a.detail, color = SzjMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (a.time.isNotBlank()) Text(a.time.take(10), color = SzjMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        // 游戏近况: recent/r{typeId}.png + event text.
                        Text("游戏近况", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        if (recents.isEmpty()) {
                            Text(if (recentsPrivate) "对方设置了隐私，无法查看" else "暂无动态", color = SzjMuted, fontSize = 12.sp)
                        } else {
                            recents.take(15).forEach { r ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val recentUrl = if (r.typeId.isNotBlank())
                                        "https://static.web.sdo.com/jijiamobile/pic/ff14/ffstones/recent/r${r.typeId}.png" else ""
                                    ShizhijiaRemoteImage(url = recentUrl, modifier = Modifier.size(30.dp), showPlaceholder = false)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(r.eventType, color = SzjText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        if (r.detail.isNotBlank()) Text(r.detail, color = SzjMuted, fontSize = 11.sp)
                                        if (r.logTime.isNotBlank()) Text(r.logTime, color = SzjMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ---- 帖子 tab ----
                if (posts.isEmpty() && !postLoading) {
                    item { Text("暂无帖子", color = SzjMuted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
                } else {
                    items(posts, key = { it.postsId }) { post ->
                        SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) })
                    }
                    if (!postEnded) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(20.dp))
                            }
                            LaunchedEffect(posts.size) {
                                if (posts.isNotEmpty()) {
                                    postLoading = true
                                    val next = ShizhijiaApi.getUserPosts(context, uuid, postPage)
                                    posts = posts + next.rows
                                    if (next.rows.isEmpty()) postEnded = true else postPage += 1
                                    postLoading = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 幻化详情：作者条 + 图片轮播 + 标题/种族性别/日期 + 双列装备表（仿官方布局）。 */
@Composable
private fun ShizhijiaGlamourDetailScreen(state: PhoneState, glamourId: String, pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    var g by remember { mutableStateOf<ShizhijiaGlamourDetail?>(null) }
    LaunchedEffect(glamourId) { g = ShizhijiaApi.getGlamourDetail(context, glamourId) }

    val slotLabels = mapOf(
        "MAIN_HAND" to "主手", "OFF_HAND" to "副手", "HEAD" to "头部", "EARS" to "耳坠",
        "BODY" to "上衣", "NECK" to "项链", "GLOVES" to "手部", "WRISTS" to "手镯",
        "LEGS" to "腿部", "FINGER_LEFT" to "戒指", "FEET" to "脚部", "FINGER_RIGHT" to "戒指",
        "GLASSES" to "面部配饰", "ORNAMENT" to "时尚配饰",
    )
    val leftSlots = listOf("MAIN_HAND", "HEAD", "BODY", "GLOVES", "LEGS", "FEET", "GLASSES")
    val rightSlots = listOf("OFF_HAND", "EARS", "NECK", "WRISTS", "FINGER_LEFT", "FINGER_RIGHT", "ORNAMENT")

    ScreenFrame(background = SzjBg) {
        SzjHeader("幻化详情", onBack = pop)
        val d = g
        if (d == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(28.dp)) }
            return@ScreenFrame
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { if (d.authorUuid.isNotBlank()) nav(SzjRoute.UserProfile(d.authorUuid)) }, verticalAlignment = Alignment.CenterVertically) {
                    SzjAvatar(d.authorName, d.authorAvatar, d.authorUuid, 32)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(d.authorName, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        val line = listOf(d.areaName, d.groupName).filter { it.isNotBlank() }.joinToString(" ")
                        if (line.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(line, color = SzjMuted, fontSize = 11.sp) }
                    }
                }
            }
            item {
                if (d.images.isNotEmpty()) {
                    // Swipeable full-aspect pager: each page shows the complete
                    // picture, no cropping and no need to tap for full view.
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState { d.images.size }
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        ShizhijiaRemoteImage(
                            url = d.images[page],
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth,
                            showPlaceholder = true,
                            onClick = { url -> SzjViewer.url = url },
                        )
                    }
                    if (d.images.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("${pagerState.currentPage + 1} / ${d.images.size}", color = SzjMuted, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(d.title, color = SzjText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val genderText = when (d.gender) { 1 -> "男性"; 2 -> "女性"; else -> "" }
                        val rg = (d.races + listOfNotNull(genderText.takeIf { it.isNotBlank() })).joinToString(" / ")
                        if (rg.isNotBlank()) Text(rg, color = SzjGold, fontSize = 13.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(d.createdAt.take(10), color = SzjMuted, fontSize = 12.sp)
                    }
                    if (d.desc.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(d.desc, color = SzjText, fontSize = 13.sp)
                    }
                    if (d.jobs.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("适用职业: ${d.jobs.joinToString("、")}", color = SzjMuted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            val rowsCount = maxOf(leftSlots.size, rightSlots.size)
            items(rowsCount) { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(leftSlots.getOrNull(row), rightSlots.getOrNull(row)).forEach { slot ->
                        Box(Modifier.weight(1f)) {
                            if (slot == null) {
                                Spacer(Modifier.height(1.dp))
                            } else {
                                val label = slotLabels[slot] ?: slot
                                val equip = d.equips.firstOrNull { it.slot == slot }
                                val extraName = when (slot) { "GLASSES" -> d.glassesName; "ORNAMENT" -> d.ornamentName; else -> "" }
                                val extraIcon = when (slot) { "GLASSES" -> d.glassesIconUrl; "ORNAMENT" -> d.ornamentIconUrl; else -> "" }
                                Column {
                                    Text(label, color = SzjMuted, fontSize = 12.sp)
                                    Spacer(Modifier.height(4.dp))
                                    if (equip == null && extraName.isBlank()) {
                                        Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(4.dp)).background(SzjCardRaised)
                                            .border(1.dp, SzjLine, RoundedCornerShape(4.dp)))
                                    } else {
                                        val eName = equip?.name ?: extraName
                                        val eIcon = equip?.iconUrl ?: extraIcon
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (eIcon.isNotBlank()) {
                                                Box {
                                                    ShizhijiaRemoteImage(url = eIcon, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                                                        if (equip?.isMallItem == true) {
                                                            // 商城角标：预渲染的整图（黄底圆+购物袋），直接 Image 显示，密度无关。
                                                            val ctx = LocalContext.current
                                                            val badge = remember {
                                                                runCatching {
                                                                    android.graphics.BitmapFactory.decodeStream(ctx.assets.open("mall_badge.png"))
                                                                }.getOrNull()
                                                            }
                                                            if (badge != null) {
                                                                Image(
                                                                    bitmap = badge.asImageBitmap(),
                                                                    contentDescription = null,
                                                                    contentScale = ContentScale.Fit,
                                                                    modifier = Modifier
                                                                        .align(Alignment.TopEnd)
                                                                        .offset(x = 5.dp, y = (-5).dp)
                                                                        .size(18.dp),
                                                                )
                                                            }
                                                        }
                                                }
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Column {
                                                Text(eName, color = SzjText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Spacer(Modifier.height(2.dp))
                                                val holeCount = maxOf(equip?.dyeHoleCount ?: 0, 0)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    for (hi in 0 until holeCount) {
                                                        val dy = equip?.dyes?.getOrNull(hi)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (dy != null) {
                                                                val dyeColor = dy.color.takeIf { it.startsWith("#") }?.let { runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: SzjCardRaised
                                                                Box(Modifier.size(10.dp).clip(CircleShape).background(dyeColor).border(0.5.dp, SzjMuted, CircleShape))
                                                                Spacer(Modifier.width(3.dp))
                                                                Text(dy.name.removeSuffix("染剂"), color = SzjMuted, fontSize = 10.sp, maxLines = 1)
                                                            } else {
                                                                Text("⊘", color = SzjMuted, fontSize = 11.sp)
                                                                Spacer(Modifier.width(2.dp))
                                                                Text("无染色", color = SzjMuted, fontSize = 10.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.padding(horizontal = 16.dp)) {
                    Text("♥ ${d.likes}", color = SzjMuted, fontSize = 13.sp)
                    Spacer(Modifier.width(14.dp))
                    Text("★ ${d.favorites}", color = SzjMuted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}


/** 底栏「幻化」：关注/全部 + 推荐/最新 + 双列卡片流（仿官方布局）。 */
@Composable
private fun ShizhijiaGlamourTab(nav: (SzjRoute) -> Unit, loggedIn: Boolean, gs: SzjGlamourState) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by gs.tab        // 0=全部 1=关注
    var sort by gs.sort      // 0=推荐 1=最新
    var items by gs.items
    var loading by gs.loading
    var page by gs.page
    var ended by gs.ended
    val gridState = gs.gridState
    // 筛选: 种族 / 性别 / 发布时间
    var raceId by gs.raceId
    var genderId by gs.genderId
    var createTimeIdx by gs.createTimeIdx
    var filterOpen by gs.filterOpen
    val createTimeValues = listOf("all", "last24H", "lastWeek", "lastMonth")

    fun load(reset: Boolean) {
        if (loading) return
        if (reset) { page = 1; ended = false; items = emptyList() }
        loading = true
        scope.launch {
            val next = if (tab == 1) ShizhijiaApi.getFollowGlamours(context, page)
            else ShizhijiaApi.getGlamours(context, page, order = if (sort == 1) "time" else "", raceId = raceId, genderId = genderId, createTime = createTimeValues[createTimeIdx])
            items = items + next
            if (next.isEmpty()) ended = true else page += 1
            loading = false
        }
    }
    // Reload only when the channel/filters changed; returning from a detail
    // page keeps the loaded feed, scroll position and active tab.
    LaunchedEffect(tab, sort, raceId, genderId, createTimeIdx) {
        val key = "$tab-$sort-$raceId-$genderId-$createTimeIdx"
        if (gs.loadedKey.value != key || items.isEmpty()) {
            gs.loadedKey.value = key
            load(reset = true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    Text("关注", fontSize = 15.sp,
                        color = if (tab == 1) SzjGold else SzjMuted,
                        fontWeight = if (tab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.clickable { tab = 1 }.padding(horizontal = 12.dp, vertical = 6.dp))
                    Spacer(Modifier.width(18.dp))
                    Text("全部", fontSize = 15.sp,
                        color = if (tab == 0) SzjGold else SzjMuted,
                        fontWeight = if (tab == 0) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.clickable { tab = 0 }.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                Text("筛选", fontSize = 13.sp, color = SzjOnGoldSoft,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SzjGoldSoft)
                        .border(1.dp, SzjGold.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                        .clickable { filterOpen = !filterOpen }
                        .padding(horizontal = 10.dp, vertical = 6.dp))
            }
            if (tab == 0) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                    Row(Modifier.clip(RoundedCornerShape(4.dp)).background(SzjCardRaised)
                        .border(1.dp, SzjLine, RoundedCornerShape(4.dp))) {
                        listOf("推荐" to 0, "最新" to 1).forEach { (label, id) ->
                            Text(label, fontSize = 12.sp,
                                color = if (sort == id) SzjOnGoldSoft else SzjMuted,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(if (sort == id) SzjGoldSoft else Color.Transparent)
                                    .clickable { if (sort != id) { sort = id } }
                                    .padding(horizontal = 14.dp, vertical = 5.dp))
                        }
                    }
                }
            }
            if (tab == 1 && !loggedIn) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("登录后可查看关注的幻化", color = SzjMuted) }
                return@Column
            }
            androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid(
                columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
            ) {
                if (tab == 0) {
                    item(key = "glamour-banner") {
                        val ctx = LocalContext.current
                        val banner = remember(ctx) {
                            runCatching {
                                android.graphics.BitmapFactory.decodeStream(ctx.assets.open("glamour_banner.png"))
                            }.onFailure { android.util.Log.w("ShizhijiaImg", "banner: ${it.message}") }.getOrNull()
                        }
                        if (banner != null) {
                            Image(
                                bitmap = banner.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                items(items.size, key = { items[it].id }) { idx ->
                    SzjGlamourCardItem(items[idx], nav)
                }
            }
            // 滚动到底自动加载下一页
            val nearEnd by remember { derivedStateOf {
                val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                items.isNotEmpty() && last >= items.size - 3
            } }
            LaunchedEffect(nearEnd, loading, ended) {
                if (nearEnd && !loading && !ended) load(reset = false)
            }
            if (loading && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(24.dp))
                }
            }
        }
        // 筛选面板: 从顶部展开, 点击面板外区域自动收起。
        if (filterOpen) {
            val noRipple = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Column(
                Modifier.fillMaxSize()
                    .background(Color(0x66000000))
                    .pointerInput(Unit) { detectTapGestures { filterOpen = false } }
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 8.dp))
                        .background(SzjBg)
                        .border(1.dp, SzjLine, RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 8.dp))
                        .clickable(interactionSource = noRipple, indication = null) { }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    SzjFilterSection("种族", listOf("全部种族" to -1, "人族" to 1, "精灵族" to 2, "拉拉菲尔族" to 3, "猫魅族" to 4, "鲁加族" to 5, "敖龙族" to 6, "硌狮族" to 7, "维埃拉族" to 8), raceId) { raceId = it }
                    Spacer(Modifier.height(8.dp))
                    SzjFilterSection("性别", listOf("全部" to -1, "男性" to 1, "女性" to 2), genderId) { genderId = it }
                    Spacer(Modifier.height(8.dp))
                    SzjFilterSection("发布时间", listOf("全部" to 0, "24小时内" to 1, "最近一周" to 2, "最近一个月" to 3), createTimeIdx) { createTimeIdx = it }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { raceId = -1; genderId = -1; createTimeIdx = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = SzjCardRaised),
                            modifier = Modifier.weight(1f),
                        ) { Text("重置", color = SzjText) }
                        Button(
                            onClick = {
                                filterOpen = false
                                gs.loadedKey.value = ""
                                load(reset = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SzjGoldSoft),
                            modifier = Modifier.weight(1f),
                        ) { Text("确认", color = SzjOnGoldSoft) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SzjFilterSection(label: String, options: List<Pair<String, Int>>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(label, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label2, id) ->
                        Text(label2, fontSize = 12.sp,
                            color = if (selected == id) SzjOnGoldSoft else SzjText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selected == id) SzjGoldSoft else SzjCardRaised)
                                .border(1.dp, if (selected == id) SzjGold.copy(alpha = 0.55f) else SzjLine, RoundedCornerShape(4.dp))
                                .clickable { onSelect(id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}
/**
 * 幻化封面：先按（缓存的）真实宽高比占位，图片再慢慢加载填充。
 * 这样瀑布流的高度一开始就是正确的，快速滑动也不会因为图片加载晚而重排跳动。
 */
@Composable
private fun SzjGlamourImage(url: String) {
    val context = LocalContext.current
    var bmp by remember(url) { mutableStateOf<android.graphics.Bitmap?>(ShizhijiaImageLoader.peek(url)) }
    var loaded by remember(url) { mutableStateOf(bmp != null) }
    LaunchedEffect(url) {
        if (!loaded) {
            bmp = ShizhijiaImageLoader.load(context, url)
            loaded = true
        }
    }
    // 所有卡片用统一的 9:16 比例（实测移动端封面统一 9:16），保证间距均匀。
    Box(
        Modifier.fillMaxWidth().aspectRatio(9f / 16f).background(SzjCardRaised),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(color = SzjGold, modifier = Modifier.size(22.dp))
        }
    }
}

/** 幻化瀑布流卡片：封面限高裁切 + 标题 + 作者/服务器 + 收藏/点赞。 */
@Composable
private fun SzjGlamourCardItem(card: ShizhijiaGlamourCard, nav: (SzjRoute) -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(4.dp)).background(SzjCard)
        .border(1.dp, SzjLine, RoundedCornerShape(4.dp))
        .clickable { nav(SzjRoute.GlamourDetail(card.id)) }) {
        SzjGlamourImage(url = card.mainImage)
        Column(Modifier.padding(8.dp)) {
            Text(card.title, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.characterName, color = SzjText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (card.groupName.isNotBlank()) {
                    Spacer(Modifier.width(3.dp))
                    SzjLocPin(14)
                    Spacer(Modifier.width(2.dp))
                    Text(card.groupName, color = SzjMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("★ ${card.favorites}", color = SzjMuted, fontSize = 11.sp)
                Spacer(Modifier.width(10.dp))
                Text("👍 ${card.likes}", color = SzjMuted, fontSize = 11.sp)
            }
        }
    }
}
