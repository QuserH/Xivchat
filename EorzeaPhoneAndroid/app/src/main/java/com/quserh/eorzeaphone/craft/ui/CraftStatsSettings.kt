package com.quserh.eorzeaphone.craft.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quserh.eorzeaphone.craft.CraftAppState
import com.quserh.eorzeaphone.craft.data.CraftJobs
import com.quserh.eorzeaphone.craft.data.CraftSimulationStats

@Composable
internal fun SimulationStatsEntry(job: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("制作属性", style = CraftType.Callout, color = CraftText, modifier = Modifier.weight(1f))
        Text("${CraftJobs.name(job)} ›", style = CraftType.Callout, color = CraftAccent)
    }
}

private data class StatsDraft(val cp: String, val craftsmanship: String, val control: String) {
    constructor(stats: CraftSimulationStats) : this(stats.cpMax.toString(), stats.craftsmanship.toString(), stats.control.toString())
    val valid: Boolean get() = listOf(cp, craftsmanship, control).all { (it.toIntOrNull() ?: 0) > 0 }
}

/** Editor-only drafts are local; saved values and game sync share PhoneState.craftStats. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CraftStatsSettingsSheet(state: CraftAppState, initialJob: Int, onDismiss: () -> Unit) {
    var job by remember(state.craftCharacterKey) { mutableStateOf(initialJob.coerceIn(0, 7)) }
    val drafts = remember(state.craftCharacterKey) { mutableStateMapOf<Int, StatsDraft>() }
    val stats = state.simulationStats(job)
    val draft = drafts[job] ?: StatsDraft(stats)
    val canSave = drafts.values.all { it.valid }
    LaunchedEffect(Unit) { if (state.craftConnected) state.refreshCraftSkills() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CraftSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("制作属性", style = CraftType.Headline, color = CraftText, modifier = Modifier.weight(1f))
                Text("保存", style = CraftType.Row, color = if (canSave) CraftAccent else CraftMuted,
                    modifier = Modifier.clickable(enabled = canSave) {
                        drafts.forEach { (jobIndex, values) ->
                            state.setSimulationStats(jobIndex, values.cp.toInt(), values.craftsmanship.toInt(), values.control.toInt())
                        }
                        onDismiss()
                    }.padding(10.dp))
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..7).chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { index ->
                            Text(CraftJobs.abbr(index), style = CraftType.Callout,
                                color = if (job == index) Color.White else CraftText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (job == index) CraftFill else CraftBackground)
                                    .clickable { job = index }.padding(vertical = 10.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(CraftJobs.name(job), style = CraftType.Row, color = CraftText, modifier = Modifier.weight(1f))
                if (drafts[job] == null && stats.syncedAtUnix > 0) {
                    Text("已从游戏同步", style = CraftType.Caption, color = CraftOk)
                }
            }
            GroupCard {
                StatField("作业精度", draft.craftsmanship) { drafts[job] = draft.copy(craftsmanship = it) }
                Hairline()
                StatField("加工精度", draft.control) { drafts[job] = draft.copy(control = it) }
                Hairline()
                StatField("制作力", draft.cp) { drafts[job] = draft.copy(cp = it) }
            }
            val liveJob = state.currentCraftJob
            if (state.craftConnected && liveJob != null) {
                Text("同步当前职业 · ${CraftJobs.name(liveJob)}", style = CraftType.Callout, color = CraftAccent,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).clickable {
                        drafts.remove(liveJob)
                        job = liveJob
                        state.refreshCraftSkills()
                    }.padding(vertical = 8.dp))
            } else {
                Text("连接游戏后，切换制作职业可自动同步", style = CraftType.Caption, color = CraftMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun StatField(label: String, value: String, onValue: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = CraftType.Callout, color = CraftText, modifier = Modifier.weight(1f))
        TextField(
            value = value,
            onValueChange = { onValue(it.filter { char -> char in '0'..'9' }.take(5)) },
            singleLine = true,
            textStyle = CraftType.Row.copy(textAlign = androidx.compose.ui.text.style.TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = CraftText,
                unfocusedTextColor = CraftText,
            ),
            modifier = Modifier.width(124.dp),
        )
    }
}
