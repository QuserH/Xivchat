package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.data.market.MarketAlertReceiver
import com.quserh.eorzeaphone.data.market.MarketRepository
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneWarn

/**
 * Alert config sheet. Absolute OR ratio, never both -- picking one clears the
 * other in [MarketRepository.setAlert] so a stale threshold can't fire later.
 *
 * Ratio presets are 0.5/0.6/0.7/0.8 rather than 0.1/0.2 because measured against
 * live data 0.1x fires on ~2 of 5 items while 0.8x fires on 4 of 5. Users who
 * pick 0.1 blind would conclude alerts are broken; the current ratio is shown
 * next to the input so the choice is informed. Custom values are still allowed.
 */
@Composable
internal fun MarketAlertSheet(
    itemId: Int,
    scope: String,
    currentPrice: Int?,
    average: Double?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var mode by remember { mutableStateOf(MarketRepository.AlertMode.None) }
    var minText by remember { mutableStateOf("") }
    var maxText by remember { mutableStateOf("") }
    var ratio by remember { mutableStateOf(0.7) }
    var hqOnly by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    // Game-side monitor: the plugin polls the board itself every 60s and can buy
    // automatically. Independent of the phone-side alert above it.
    var monitorOn by remember { mutableStateOf(false) }
    var monitorText by remember { mutableStateOf("") }
    var monitorAutoBuy by remember { mutableStateOf(false) }
    var monitorCapText by remember { mutableStateOf("") }

    LaunchedEffect(itemId) {
        MarketRepository.watch(context, itemId)?.let { w ->
            mode = w.mode
            minText = w.minPrice?.toString() ?: ""
            maxText = w.maxPrice?.toString() ?: ""
            ratio = w.ratio ?: 0.7
            hqOnly = w.hqOnly
            monitorOn = w.monitorOn
            monitorText = w.monitorThreshold.takeIf { it > 0 }?.toString() ?: ""
            monitorAutoBuy = w.autoBuy
            monitorCapText = w.buyCap.takeIf { it > 0 }?.toString() ?: ""
        }
    }

    // Scrim + bottom card. Tapping the scrim dismisses, matching iOS sheets.
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(PhoneSurface)
                .clickable(enabled = false) {}
                .padding(20.dp),
        ) {
            Text("降价提醒", color = PhoneText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                buildString {
                    append("当前最低 ")
                    append(currentPrice?.let { gil(it) } ?: "—")
                    if (average != null && average > 0) {
                        append("   平均 ")
                        append(gil(average))
                        if (currentPrice != null) {
                            append("   现价是均价的 ")
                            append("%.2f".format(currentPrice / average))
                            append(" 倍")
                        }
                    }
                },
                color = PhoneMuted, fontSize = 11.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            // mode switch
            Row(
                Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MarketChip("关闭", mode == MarketRepository.AlertMode.None,
                    onClick = { mode = MarketRepository.AlertMode.None })
                MarketChip("按价格", mode == MarketRepository.AlertMode.Absolute,
                    onClick = { mode = MarketRepository.AlertMode.Absolute })
                MarketChip("按倍率", mode == MarketRepository.AlertMode.Ratio,
                    onClick = { mode = MarketRepository.AlertMode.Ratio })
            }

            when (mode) {
                MarketRepository.AlertMode.Absolute -> Column(Modifier.padding(top = 16.dp)) {
                    Text(
                        "低于上限就提醒；填了下限就是区间内提醒。",
                        color = PhoneMuted, fontSize = 11.sp,
                    )
                    Row(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NumField(minText, { minText = it }, "下限（可空）", Modifier.weight(1f))
                        NumField(maxText, { maxText = it }, "上限", Modifier.weight(1f))
                    }
                    currentPrice?.let {
                        Text(
                            "以现价填：上限 ${gil((it * 0.9).toInt())} 约为降价 10%",
                            color = PhoneMuted, fontSize = 10.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                MarketRepository.AlertMode.Ratio -> Column(Modifier.padding(top = 16.dp)) {
                    Text(
                        "低于均价的这个倍数就提醒。",
                        color = PhoneMuted, fontSize = 11.sp,
                    )
                    Row(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0.5, 0.6, 0.7, 0.8).forEach { r ->
                            MarketChip("%.1f".format(r), kotlin.math.abs(ratio - r) < 0.001,
                                onClick = { ratio = r })
                        }
                    }
                    Row(
                        Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0.1, 0.2, 0.3, 0.4).forEach { r ->
                            MarketChip("%.1f".format(r), kotlin.math.abs(ratio - r) < 0.001,
                                onClick = { ratio = r })
                        }
                    }
                    if (average != null && average > 0) {
                        Text(
                            "= 低于 ${gil(average * ratio)} 时提醒" +
                                if (ratio <= 0.2) "（很少触发，多为清仓价）" else "",
                            color = if (ratio <= 0.2) PhoneWarn else PhoneAccent,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }

                MarketRepository.AlertMode.None -> Text(
                    "只收藏，不提醒。",
                    color = PhoneMuted, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarketChip(if (hqOnly) "仅 HQ" else "含 NQ", hqOnly,
                    onClick = { hqOnly = !hqOnly })
                Text(
                    "  比较用的价格范围：$scope",
                    color = PhoneMuted, fontSize = 11.sp,
                )
            }

            // ---- game-side monitor (DR-style) ----
            PhoneHairlineRow(0.dp)
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "游戏内监控", color = PhoneText, fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "电脑插件每分钟替你查一次本服板子，低于阈值就通知手机",
                        color = PhoneMuted, fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                MarketChip(if (monitorOn) "已开启" else "关闭", monitorOn,
                    onClick = {
                        monitorOn = !monitorOn
                        if (monitorOn && monitorText.isBlank()) {
                            // Seed with the phone-side absolute cap, then 90% of the
                            // current price, then blank -- the first one that exists.
                            maxText.toIntOrNull()?.let { monitorText = it.toString() }
                                ?: currentPrice?.let { monitorText = (it * 0.9).toInt().toString() }
                        }
                    })
            }
            if (monitorOn) {
                Column(Modifier.padding(top = 10.dp)) {
                    Text(
                        "低于这个价就通知：",
                        color = PhoneMuted, fontSize = 11.sp,
                    )
                    NumField(
                        monitorText, { monitorText = it }, "价格阈值（gil）",
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "自动购买", color = PhoneText, fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        MarketChip(if (monitorAutoBuy) "开" else "关", monitorAutoBuy,
                            onClick = { monitorAutoBuy = !monitorAutoBuy })
                    }
                    if (monitorAutoBuy) {
                        Text(
                            "满足条件就自动买下（走和手动购买一样的校验，价格变了不会买）。",
                            color = PhoneWarn, fontSize = 10.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "累计买入上限（件，0 不限）", color = PhoneMuted,
                                fontSize = 11.sp, modifier = Modifier.weight(1f),
                            )
                            NumField(monitorCapText, { monitorCapText = it }, "0",
                                Modifier.width(96.dp))
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PhoneButton(
                    "取消", onClick = onDismiss,
                    kind = PhoneButtonKind.Ghost, modifier = Modifier.weight(1f),
                )
                PhoneButton(
                    "保存",
                    onClick = { saving = true },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Commit outside the click handler so it stays synchronous.
    LaunchedEffect(saving) {
        if (!saving) return@LaunchedEffect
        MarketRepository.setAlert(
            context, itemId, scope, mode,
            minPrice = minText.toIntOrNull(),
            maxPrice = maxText.toIntOrNull(),
            ratio = ratio,
            hqOnly = hqOnly,
        )
        // Monitor rule saved alongside: the plugin-side poller is driven by this.
        MarketRepository.setMonitor(
            context, itemId,
            on = monitorOn,
            threshold = monitorText.toIntOrNull() ?: 0,
            hqOnly = hqOnly,
            autoBuy = monitorAutoBuy,
            buyCap = monitorCapText.toIntOrNull() ?: 0,
        )
        // Start the poll as soon as a rule exists; stop it when the last one goes.
        val anyAlert = MarketRepository.watchList(context).any { it.hasAlert }
        MarketAlertReceiver.configure(context, anyAlert)
        // Evaluate immediately too: if the item already qualifies, waiting for the
        // first alarm tick just looks like the feature does nothing.
        if (anyAlert) runCatching { MarketAlertReceiver.checkNow(context) }
        saving = false
        onDismiss()
    }
}

@Composable
private fun NumField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp))
            .background(PhoneSurfaceRaised).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value,
            { s -> onChange(s.filter { it.isDigit() }.take(9)) },
            singleLine = true,
            textStyle = TextStyle(color = PhoneText, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { f ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) Text(hint, color = PhoneMuted, fontSize = 12.sp)
                    f()
                }
            },
        )
    }
}
