package com.dartspractice.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartspractice.app.appViewModel
import com.dartspractice.app.domain.rule.Mpr
import com.dartspractice.app.ui.Format
import com.dartspractice.app.ui.detail.DetailUiState
import com.dartspractice.app.ui.detail.DetailViewModel
import com.dartspractice.app.ui.detail.SessionDetail
import com.dartspractice.app.ui.theme.DartsColors
import com.dartspractice.app.ui.theme.DartsIcons

/** 日別詳細。その日の練習一覧・各練習のMPR・ナンバー別の投・メモ。‹ › で練習した日を移動する。 */
@Composable
fun DetailScreen(date: String?, onBack: () -> Unit) {
    ScreenOrientation(allowLandscape = false)
    val vm = appViewModel(key = "detail-${date ?: "latest"}") { DetailViewModel(date, it.repository, it.stats) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader(
            title = ui.day?.let { Format.dayWithWeek(it) } ?: "履歴",
            sub = ui.menuName,
            backLabel = "グラフへ戻る",
            onBack = onBack,
        ) {
            DayArrow(DartsIcons.Back, "前の練習日", ui.prevDay) { vm.showDay(it) }
            DayArrow(DartsIcons.Next, "次の練習日", ui.nextDay) { vm.showDay(it) }
        }

        when {
            ui.loading -> Unit
            ui.day == null -> DartsCard { PlaceholderNote("まだ練習の記録がありません。練習を完了すると、ここに日ごとの内訳が出ます。") }
            else -> DayContent(ui, vm::select)
        }
    }
}

@Composable
private fun DayArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, day: String?, onClick: (String) -> Unit) {
    IconButton(onClick = { day?.let(onClick) }, enabled = day != null, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = label, tint = if (day != null) DartsColors.Text else DartsColors.Border)
    }
}

@Composable
private fun DayContent(ui: DetailUiState, onSelect: (Int) -> Unit) {
    val stat = ui.dayStat
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard("練習回数", "${stat?.sessions ?: 0}", "回", null, Modifier.weight(1f))
        SummaryCard(
            "平均", stat?.let { Format.darts(it.avgDarts) } ?: "--", "本",
            stat?.let { Mpr.calc(ui.totalMarks, it.avgDarts) }, Modifier.weight(1f),
        )
        SummaryCard(
            "最少", stat?.minDarts?.toString() ?: "--", "本",
            stat?.let { Mpr.calc(ui.totalMarks, it.minDarts.toDouble()) }, Modifier.weight(1f),
        )
    }

    // 練習の選択
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ui.sessions.forEachIndexed { i, s ->
            val on = i == ui.selected
            val shape = RoundedCornerShape(18.dp)
            Box(
                Modifier.height(36.dp).clip(shape)
                    .background(if (on) DartsColors.Accent else DartsColors.Surface)
                    .border(1.dp, if (on) DartsColors.Accent else DartsColors.Border, shape)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${i + 1}回目 ${s.summary.darts}本",
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) DartsColors.OnAccent else DartsColors.SubText,
                )
            }
        }
    }

    val selected = ui.sessions.getOrNull(ui.selected) ?: return
    Breakdown(ui, ui.selected, selected)

    if (selected.summary.memo.isNotBlank()) {
        DartsCard {
            Text("メモ", fontSize = 12.sp, color = DartsColors.SubText)
            Text(selected.summary.memo, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** 選んだ練習の内訳。ナンバーごとに1投ずつのマーク数を投げた順に並べる。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Breakdown(ui: DetailUiState, index: Int, s: SessionDetail) {
    DartsCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}回目の内訳", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                buildAnnotatedString {
                    append("MPR ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DartsColors.Text)) {
                        append(Mpr.format(Mpr.calc(ui.totalMarks, s.summary.darts.toDouble())))
                    }
                    append(" ・ 開始 ${Format.time(s.summary.startedAt)}")
                },
                fontSize = 12.sp,
                color = DartsColors.SubText,
            )
        }
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val byTarget = s.throws.groupBy { it.target }
            ui.targets.forEach { t ->
                val list = byTarget[t].orEmpty()
                Row(Modifier.defaultMinSize(minHeight = 34.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(targetLabel(t), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                    FlowRow(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        list.forEach { th ->
                            val (bg, fg) = markChipColors(th.rawMarks)
                            Box(
                                Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(bg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${th.rawMarks}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
                            }
                        }
                    }
                    Text(
                        "${list.size}本",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(44.dp),
                    )
                }
            }
        }
        Text(
            "数字は1投ごとのマーク数です(0=ミス、3=トリプル)。左から投げた順に並びます。",
            fontSize = 11.sp,
            color = DartsColors.SubText,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SummaryCard(title: String, value: String, unit: String, mpr: Double?, modifier: Modifier = Modifier) {
    DartsCard(modifier = modifier) {
        Text(title, fontSize = 12.sp, color = DartsColors.SubText)
        Text(
            buildAnnotatedString {
                append(value)
                withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)) { append(unit) }
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
        )
        if (mpr != null) {
            Text(
                buildAnnotatedString {
                    append("MPR ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DartsColors.Text)) { append(Mpr.format(mpr)) }
                },
                fontSize = 12.sp,
                color = DartsColors.SubText,
            )
        }
    }
}
