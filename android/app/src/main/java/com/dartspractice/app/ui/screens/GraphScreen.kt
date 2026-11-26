package com.dartspractice.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartspractice.app.appViewModel
import com.dartspractice.app.domain.stats.Trend
import com.dartspractice.app.ui.Format
import com.dartspractice.app.ui.graph.GraphUiState
import com.dartspractice.app.ui.graph.GraphViewModel
import com.dartspractice.app.ui.graph.Metric
import com.dartspractice.app.ui.graph.NumberPeriod
import com.dartspractice.app.ui.graph.Period
import com.dartspractice.app.ui.theme.DartsColors
import kotlin.math.abs

/** グラフ(メイン)。日ごとの平均と7日移動平均、ナンバー別の平均本数。 */
@Composable
fun GraphScreen(onOpenDay: (String) -> Unit) {
    ScreenOrientation(allowLandscape = false)
    val vm = appViewModel { GraphViewModel(it.menuSelection, it.stats) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val rate = ui.rule?.rate

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("グラフ", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(ui.menuName, fontSize = 13.sp, color = DartsColors.SubText)
        }

        Segmented(Period.entries.map { it.label }, ui.period.ordinal, Modifier.fillMaxWidth(), fill = true) {
            vm.setPeriod(Period.entries[it])
        }

        DartsCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (ui.metric == Metric.Rate) "日ごとの${rate?.label.orEmpty()}" else "日ごとの平均本数",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Segmented(listOf("本数", rate?.label.orEmpty()), ui.metric.ordinal) { vm.setMetric(Metric.entries[it]) }
            }

            if (ui.points.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    PlaceholderNote(if (ui.loading) "" else "この期間に完了した練習はありません")
                }
            } else {
                DailyChart(
                    points = ui.points,
                    movingAverage = ui.movingAverage,
                    metric = ui.metric,
                    rate = rate ?: return@DartsCard,
                    startDay = ui.startDay,
                    endDay = ui.today,
                    onSelect = vm::select,
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 8.dp),
                )
                Legend()
                ChangeLine(ui)
                SelectedDay(ui, onOpenDay)
            }
        }

        // ナンバーが1つのメニューでは出さない
        if (ui.targets.size > 1) DartsCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ナンバー別の平均本数", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Segmented(NumberPeriod.entries.map { it.label }, ui.numberPeriod.ordinal) {
                    vm.setNumberPeriod(NumberPeriod.entries[it])
                }
            }
            NumberAverages(ui)
        }
    }
}

@Composable
private fun Legend() {
    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(DartsColors.Accent, "日ごとの平均", dot = true)
        LegendItem(DartsColors.Yellow, "7日移動平均", dot = false)
    }
}

@Composable
private fun LegendItem(color: Color, label: String, dot: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            if (dot) Modifier.size(8.dp).clip(CircleShape).background(color)
            else Modifier.width(16.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color),
        )
        Text(label, fontSize = 11.sp, color = DartsColors.SubText)
    }
}

/** 期間の最初と最後の日の比較。「64本 → 49本(−15本) ・ MPR 3.28 → 4.29」 */
@Composable
private fun ChangeLine(ui: GraphUiState) {
    val rate = ui.rule?.rate ?: return
    val change = Trend.change(ui.points) ?: return
    if (change.first.day == change.last.day) return
    val diff = change.diff
    val diffText = when {
        diff < 0 -> "−${Format.darts(abs(diff))}本"
        diff > 0 -> "+${Format.darts(diff)}本"
        else -> "±0本"
    }
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DartsColors.Text)) {
                append("${Format.darts(change.first.avgDarts)}本 → ${Format.darts(change.last.avgDarts)}本")
            }
            append("(")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (diff < 0) DartsColors.Accent else DartsColors.SubText)) {
                append(diffText)
            }
            append(") ・ ${rate.label} ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DartsColors.Text)) {
                append(
                    "${rate.format(rate.of(change.first.avgDarts))} → " +
                        rate.format(rate.of(change.last.avgDarts)),
                )
            }
        },
        fontSize = 13.sp,
        color = DartsColors.SubText,
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(
        "${Format.monthDay(change.first.day.toString())} と ${Format.monthDay(change.last.day.toString())} の比較。点をタップするとその日の値が出ます",
        fontSize = 11.sp,
        color = DartsColors.SubText,
        modifier = Modifier.padding(top = 2.dp),
    )
}

/** 点をタップして選んだ日。押すと日別詳細へ。 */
@Composable
private fun SelectedDay(ui: GraphUiState, onOpenDay: (String) -> Unit) {
    val day = ui.selected ?: return
    val point = ui.points.firstOrNull { it.day == day } ?: return
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp).clip(shape)
            .border(1.dp, DartsColors.Accent, shape)
            .clickable { onOpenDay(day.toString()) }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                append(Format.dayWithWeek(day))
                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = DartsColors.SubText)) {
                    append("  平均${Format.darts(point.avgDarts)}本")
                }
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text("詳細を見る ›", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DartsColors.Accent)
    }
}

/** ナンバー別の平均本数。いちばん多いナンバーを「苦手」にする。 */
@Composable
private fun NumberAverages(ui: GraphUiState) {
    val byTarget = ui.targetStats.associateBy { it.target }
    if (byTarget.isEmpty()) {
        PlaceholderNote("この期間に完了した練習はありません")
        return
    }
    val max = byTarget.values.maxOf { it.avgDarts }
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ui.targets.forEach { t ->
            val avg = byTarget[t]?.avgDarts ?: 0.0
            val worst = avg == max && avg > 0
            NumberBarRow(
                label = targetLabel(t),
                ratio = if (max == 0.0) 0f else (avg / max).toFloat(),
                value = "%.1f本".format(avg),
                barColor = if (worst) DartsColors.Orange else DartsColors.Accent,
                tag = if (worst) "苦手" else null,
            )
        }
    }
}
