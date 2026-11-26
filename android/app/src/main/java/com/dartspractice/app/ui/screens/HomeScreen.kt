package com.dartspractice.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartspractice.app.appViewModel

import com.dartspractice.app.ui.Format
import com.dartspractice.app.ui.home.HomeViewModel
import com.dartspractice.app.ui.theme.DartsColors
import com.dartspractice.app.ui.theme.DartsIcons
import java.time.LocalDate

/** ホーム(案F)。 */
@Composable
fun HomeScreen(
    onOpenRecord: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGraph: () -> Unit,
    onOpenDay: (String) -> Unit,
) {
    ScreenOrientation(allowLandscape = false)
    val today = LocalDate.now()

    val vm = appViewModel { HomeViewModel(it.repository, it.stats, it.menuSelection) }
    val stats by vm.stats.collectAsStateWithLifecycle()
    val resume by vm.resume.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.openRecord.collect(onOpenRecord) }

    resume?.let { c ->
        AlertDialog(
            onDismissRequest = vm::dismissResume,
            containerColor = DartsColors.Surface,
            title = { Text("途中の練習があります", fontWeight = FontWeight.Bold) },
            text = { Text("${c.darts}本まで記録した練習があります。続きから再開しますか?", color = DartsColors.SubText) },
            confirmButton = {
                TextButton(onClick = vm::resume) {
                    Text("続きから再開", color = DartsColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::discardAndStartNew) {
                    Text("破棄して新しく始める", color = DartsColors.SubText)
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 見出し
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ダーツ練習", fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(Format.dayWithWeek(today), fontSize = 13.sp, color = DartsColors.SubText)
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(44.dp).offset(x = 10.dp)) {
                Icon(DartsIcons.Settings, contentDescription = "設定", tint = DartsColors.Text)
            }
        }

        // 今日のメニュー
        // 今日のメニュー(チップで切り替える。選んだメニューは端末に保存)
        val sel = stats.selected
        DartsCard(radius = 20.dp) {
            Text("今日のメニュー", fontSize = 12.sp, color = DartsColors.SubText)
            Text(sel?.menu?.name.orEmpty(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            Text(sel?.rule?.description.orEmpty(), fontSize = 13.sp, color = DartsColors.SubText, modifier = Modifier.padding(top = 6.dp))
            Row(
                Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stats.menus.forEach { m ->
                    val on = m.id == sel?.menu?.id
                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        Modifier.height(36.dp).clip(shape)
                            .background(if (on) DartsColors.Accent else DartsColors.Surface)
                            .border(1.dp, if (on) DartsColors.Accent else DartsColors.Dashed, shape)
                            .clickable { vm.selectMenu(m.id) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            m.name,
                            fontSize = 13.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            color = if (on) DartsColors.OnAccent else DartsColors.SubText,
                        )
                    }
                }
            }
        }

        // 練習開始
        AccentButton(
            text = "練習開始",
            height = 72.dp,
            fontSizeSp = 22,
            icon = { Icon(DartsIcons.Target, contentDescription = null, tint = DartsColors.OnAccent, modifier = Modifier.size(28.dp)) },
            onClick = vm::onStartPressed,
        )

        // 今日の平均・自己ベスト
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val rate = sel?.rule?.rate
            val t = stats.today
            StatCard(
                title = "今日の平均",
                value = t?.let { Format.darts(it.avgDarts) },
                sub = t?.let { "${it.sessions}回の平均" } ?: "まだ練習していません",
                rateLabel = rate?.label.orEmpty(),
                rateText = rate?.format(t?.let { rate.of(it.avgDarts) }) ?: "—",
                modifier = Modifier.weight(1f),
            )
            val b = stats.best
            StatCard(
                title = "自己ベスト",
                value = b?.darts?.toString(),
                sub = b?.let { "${Format.monthDay(it.day)} の練習" } ?: "まだ記録がありません",
                rateLabel = rate?.label.orEmpty(),
                rateText = rate?.format(b?.let { rate.of(it.darts.toDouble()) }) ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        // 日ごとの平均本数(グラフへの入口)
        DartsCard(onClick = onOpenGraph) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("日ごとの平均本数", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("グラフへ →", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DartsColors.Accent)
            }
            val recent = stats.recent
            if (recent.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    PlaceholderNote("直近14日に完了した練習はありません")
                }
            } else {
                Sparkline(recent, today.minusDays(13), today, Modifier.fillMaxWidth().height(80.dp).padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                    val first = recent.first()
                    val last = recent.last()
                    Text(
                        "${Format.monthDay(first.day.toString())} ${Format.darts(first.avgDarts)}本",
                        fontSize = 11.sp,
                        color = DartsColors.SubText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${if (last.day == today) "今日" else Format.monthDay(last.day.toString())} ${Format.darts(last.avgDarts)}本",
                        fontSize = 11.sp,
                        color = DartsColors.SubText,
                    )
                }
            }
        }

        // 苦手ナンバー(直近7日で平均本数が多い順に2つ)。ナンバーが1つのメニューでは出さない
        if ((sel?.rule?.targets?.size ?: 0) > 1) DartsCard {
            SectionTitle("苦手ナンバー(直近7日の平均)")
            if (stats.weak.isEmpty()) {
                PlaceholderNote("直近7日に完了した練習はありません")
            } else {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.weak.forEachIndexed { i, w ->
                        NumberBarRow(
                            label = targetLabel(w.target),
                            ratio = (w.avgDarts / 10).toFloat(),
                            value = "%.1f本".format(w.avgDarts),
                            barColor = if (i == 0) DartsColors.Orange else DartsColors.Yellow,
                        )
                    }
                }
            }
        }

        // 最後に練習した日
        val latest = stats.latest
        DartsCard(onClick = latest?.let { { onOpenDay(it.day) } }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        if (latest == null) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = DartsColors.SubText)) {
                                append("まだ練習の記録がありません")
                            }
                        } else {
                            append(Format.dayWithWeek(latest.day))
                            withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = DartsColors.SubText)) {
                                append("  ${latest.sessions}回 平均${Format.darts(latest.avgDarts)}本")
                            }
                        }
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (latest != null) Text("›", fontSize = 18.sp, color = DartsColors.SubText)
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String?, sub: String, rateLabel: String, rateText: String, modifier: Modifier = Modifier) {
    DartsCard(modifier = modifier) {
        Text(title, fontSize = 12.sp, color = DartsColors.SubText)
        Text(
            buildAnnotatedString {
                if (value == null) {
                    withStyle(SpanStyle(color = DartsColors.SubText)) { append("--") }
                } else {
                    append(value)
                }
                withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)) { append("本") }
            },
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(sub, fontSize = 12.sp, color = DartsColors.SubText)
        Text(
            buildAnnotatedString {
                append("$rateLabel ")
                withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DartsColors.Text)) { append(rateText) }
            },
            fontSize = 12.sp,
            color = DartsColors.SubText,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
