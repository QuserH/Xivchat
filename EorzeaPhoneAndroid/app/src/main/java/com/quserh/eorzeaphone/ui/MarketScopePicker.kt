package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneText

/**
 * World / DC picker for the market tabs.
 *
 * Opened by tapping the already-selected tab, so the default (follow the
 * character) needs no extra control and the override is one tap away.
 *
 * [current] is the effective value, not the character's own -- the check mark has
 * to track what is on screen or it stops matching the tab label.
 */
@Composable
internal fun MarketScopePickerSheet(
    title: String,
    options: List<String>,
    current: String,
    /** The character's own world/DC. Labelled so "back to default" is findable. */
    homeOption: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(PhoneSurface)
                .clickable(enabled = false) {}
                .padding(vertical = 18.dp),
        ) {
            Text(
                title, color = PhoneText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 380.dp).padding(top = 6.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
            ) {
                items(options, key = { it }) { name ->
                    val on = name == current
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onPick(name) }
                            .padding(horizontal = 20.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (name == homeOption) "$name（当前角色）" else name,
                            color = if (on) PhoneAccent else PhoneText,
                            fontSize = 15.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (on) {
                            Icon(
                                painterResource(R.drawable.ic2_check), contentDescription = null,
                                tint = PhoneAccent, modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Text(
                "点已选中的标签可以再打开这里",
                color = PhoneMuted, fontSize = 11.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp),
            )
        }
    }
}
