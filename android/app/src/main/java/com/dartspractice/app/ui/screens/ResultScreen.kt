package com.dartspractice.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartspractice.app.appViewModel
import com.dartspractice.app.domain.rule.Mpr
import com.dartspractice.app.ui.Format
import com.dartspractice.app.ui.result.ResultViewModel
import com.dartspractice.app.ui.theme.DartsColors

/** 結果。合計本数・MPR・前回との比較・ナンバー別本数・メモ。 */
@Composable
fun ResultScreen(sessionId: Long, onDone: () -> Unit) {
    ScreenOrientation(allowLandscape = false)
    val vm = appViewModel(key = "result-$sessionId") { ResultViewModel(sessionId, it.repository, it.stats) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    var memo by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    val loaded = ui.loaded ?: return // 読み込み中
    val p = loaded.state
    LaunchedEffect(loaded.session.id) { if (memo == null) memo = loaded.session.memo }

    val darts = p.dartsByTarget
    val maxDarts = darts.values.maxOrNull() ?: 0

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = DartsColors.Surface,
            title = { Text("この練習を破棄しますか?", fontWeight = FontWeight.Bold) },
            text = { Text("${p.totalDarts}本の記録が消えます。この操作は取り消せません。", color = DartsColors.SubText) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; vm.discard(onDone) }) {
                    Text("破棄する", color = DartsColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("キャンセル", color = DartsColors.SubText) }
            },
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("結果", fontSize = 22.sp, fontWeight = FontWeight.Black)
            val time = loaded.session.finishedAt?.let { " ・ ${Format.dateTime(it)}" }.orEmpty()
            Text(loaded.menu.name + time, fontSize = 13.sp, color = DartsColors.SubText)
        }

        DartsCard(radius = 20.dp) {
            Text("合計本数", fontSize = 13.sp, color = DartsColors.SubText)
            Text(
                buildAnnotatedString {
                    append("${p.totalDarts}")
                    withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) { append("本") }
                },
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
            )
            ui.previous?.let { prev ->
                val diff = p.totalDarts - prev.darts
                Text(
                    "前回(${prev.darts}本)より " + when {
                        diff < 0 -> "${-diff}本少ない"
                        diff > 0 -> "${diff}本多い"
                        else -> "同じ本数"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (diff < 0) DartsColors.Accent else DartsColors.SubText,
                )
            }
            Text(
                buildAnnotatedString {
                    append("MPR(1ターンの平均マーク数) ")
                    withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DartsColors.Text)) {
                        append(Mpr.format(Mpr.calc(p.rule.totalMarks, p.totalDarts.toDouble())))
                    }
                },
                fontSize = 13.sp,
                color = DartsColors.SubText,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        DartsCard {
            SectionTitle("ナンバー別の本数")
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                p.rule.targets.forEach { t ->
                    val n = darts[t] ?: 0
                    NumberBarRow(
                        label = targetLabel(t),
                        ratio = if (maxDarts == 0) 0f else n.toFloat() / maxDarts,
                        value = "${n}本",
                        barColor = if (n == maxDarts && n > 0) DartsColors.Orange else DartsColors.Accent,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ひとことメモ(任意)", fontSize = 13.sp, color = DartsColors.SubText)
            OutlinedTextField(
                value = memo.orEmpty(),
                onValueChange = { memo = it },
                modifier = Modifier.fillMaxWidth().height(96.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DartsColors.Surface,
                    unfocusedContainerColor = DartsColors.Surface,
                    focusedBorderColor = DartsColors.Accent,
                    unfocusedBorderColor = DartsColors.Border,
                    cursorColor = DartsColors.Accent,
                ),
            )
        }

        AccentButton("保存してホームへ") { vm.save(memo.orEmpty(), onDone) }
        OutlineButton("この練習を破棄する", color = DartsColors.Accent) { confirmDiscard = true }
    }
}
