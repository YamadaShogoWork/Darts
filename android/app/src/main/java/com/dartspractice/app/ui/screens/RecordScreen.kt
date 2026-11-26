package com.dartspractice.app.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dartspractice.app.appViewModel
import com.dartspractice.app.domain.rule.Cricket10Rule.BULL
import com.dartspractice.app.domain.rule.PracticeRule
import com.dartspractice.app.domain.rule.PracticeState
import com.dartspractice.app.ui.record.RecordViewModel
import com.dartspractice.app.ui.theme.DartsColors
import com.dartspractice.app.ui.theme.DartsIcons

/** ナンバーの表示名(BULL → ブル)。 */
fun targetLabel(target: String): String = if (target == BULL) "ブル" else target

/**
 * 記録入力。この画面だけ横画面に対応する(左に現在のナンバー・累計、右に入力ボタン)。
 * 開いている間は画面を消さない(スタンドに立てて使うため)。
 */
@Composable
fun RecordScreen(sessionId: Long, onShowResult: (Long) -> Unit, onBack: () -> Unit) {
    ScreenOrientation(allowLandscape = true)
    KeepScreenOn()

    val vm = appViewModel(key = "record-$sessionId") { RecordViewModel(sessionId, it.repository, it.awardPrefs) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val award by vm.award.collectAsStateWithLifecycle()
    val rule = ui.rule
    val p = ui.practice
    if (rule == null || p == null) return // 読み込み中

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val header = @Composable { RecordHeader(ui.menuName, p.totalDarts, onBack) }
    val controls = @Composable { modifier: Modifier ->
        if (p.finished) {
            DonePanel(p.totalDarts, modifier) { onShowResult(sessionId) }
        } else {
            InputButtons(rule, p.currentTarget!!, modifier, vm::input)
        }
    }

    // ターンごとのアワードの演出を、画面全体の上に重ねる(入力のじゃまはしない)
    AwardOverlay(award, Modifier.fillMaxSize()) {
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    header()
                    if (rule.targets.size > 1) NumberRail(rule, p, cellHeight = 50)
                    CurrentNumberCard(p, numberSize = 64.sp)
                }
                // ターンと何投目かは、入力ボタンのすぐ上に置いて常に見えるようにする
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TurnRow(p)
                    controls(Modifier.weight(1f))
                    FooterButtons(canUndo = p.throws.isNotEmpty(), onUndo = vm::undo, onBack = onBack)
                }
            }
        } else {
            BoxWithConstraints(
                Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            ) {
                // 入力ボタンは縮めない。画面に入りきらない機種では、大きな数字のほうを小さくする
                val numberSize = if (maxHeight < PORTRAIT_FULL_HEIGHT) 80.sp else 112.sp
                Column(Modifier.fillMaxSize()) {
                    header()
                    Spacer(Modifier.height(12.dp))
                    // ナンバーが1つのメニュー(ブル10本など)では、ナンバーの進み具合は出さない
                    if (rule.targets.size > 1) {
                        NumberRail(rule, p, cellHeight = 56)
                        Spacer(Modifier.height(16.dp))
                    }
                    CurrentNumberCard(p, numberSize = numberSize)
                    Spacer(Modifier.height(12.dp))
                    TurnRow(p)
                    Spacer(Modifier.weight(1f).height(12.dp))
                    controls(Modifier.height(204.dp))
                    Spacer(Modifier.height(12.dp))
                    FooterButtons(canUndo = p.throws.isNotEmpty(), onUndo = vm::undo, onBack = onBack)
                }
            }
        }
    }
}

/** 縦画面で大きな数字(112sp)のまま、入力ボタンを縮めずに全部入る高さ。これより低ければ数字を小さくする。 */
private val PORTRAIT_FULL_HEIGHT = 770.dp

/** 文字の上下の余分な余白をなくす。 */
private val TightText = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

/** 「このナンバーの投げ」の欄の高さ(チップ2行分)。 */
private val HISTORY_HEIGHT = 66.dp

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun RecordHeader(menuName: String, total: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).offset(x = (-10).dp)) {
            Icon(DartsIcons.Back, contentDescription = "中断してホームへ戻る", tint = DartsColors.Text)
        }
        Text(menuName, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(
            buildAnnotatedString {
                append("合計 ")
                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DartsColors.Text)) { append("$total") }
                append("本")
            },
            fontSize = 14.sp,
            color = DartsColors.SubText,
            modifier = Modifier.defaultMinSize(minWidth = 60.dp),
            textAlign = TextAlign.End,
        )
    }
}

/** 20〜ブルの進み具合。完了したナンバーは本数を出す。 */
@Composable
private fun NumberRail(rule: PracticeRule, p: PracticeState, cellHeight: Int) {
    val darts = p.dartsByTarget
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        rule.targets.forEach { t ->
            val done = p.isCleared(t)
            val current = t == p.currentTarget
            val (bg, fg, border) = when {
                done -> Triple(DartsColors.DoneBg, DartsColors.Accent, DartsColors.DoneBorder)
                current -> Triple(DartsColors.Accent, DartsColors.OnAccent, DartsColors.Accent)
                else -> Triple(DartsColors.Surface, DartsColors.SubText, DartsColors.Border)
            }
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.weight(1f).height(cellHeight.dp).clip(shape).background(bg).border(1.dp, border, shape),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 2段の文字は上下の余白をなくして詰める(横画面の低いマスでも切れないように)
                Text(
                    if (t == BULL) "B" else t,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    style = TightText,
                )
                // 今投げているマスは色だけでわかるので、下の段は空にする(ナンバーの位置はそろえる)
                Text(
                    when {
                        done -> "${darts[t]}本"
                        current -> ""
                        else -> "—"
                    },
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = fg,
                    style = TightText,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentNumberCard(p: PracticeState, numberSize: TextUnit) {
    // 終了後は最後のナンバー(ブル)を出す
    val shown = p.currentTarget ?: p.rule.targets.last()
    val marks = p.marksByTarget[shown] ?: 0
    val clear = p.rule.marksToClear(shown)
    DartsCard(radius = 20.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("いまのナンバー", fontSize = 12.sp, color = DartsColors.SubText, modifier = Modifier.weight(1f))
            Text(
                buildAnnotatedString {
                    append("${p.rule.progressLabel} ")
                    withStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, color = DartsColors.Accent)) { append("$marks") }
                    append(" / $clear")
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            if (p.finished) "完了" else targetLabel(shown),
            fontSize = numberSize,
            lineHeight = numberSize,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        Row(
            Modifier.fillMaxWidth().semantics { contentDescription = "${clear}中${marks}" },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(clear) { i ->
                Box(
                    Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (i < marks) DartsColors.Accent else DartsColors.Border),
                )
            }
        }
        // このナンバーの投げ: 高さを2行分に固定し、増えたら自動で一番下(最新)までスクロールする
        Text("このナンバーの投げ", fontSize = 12.sp, color = DartsColors.SubText, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        val scroll = rememberScrollState()
        LaunchedEffect(shown) {
            snapshotFlow { scroll.maxValue }.collect { scroll.animateScrollTo(it) }
        }
        FlowRow(
            Modifier.fillMaxWidth().height(HISTORY_HEIGHT).verticalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            p.throws.filter { it.target == shown }.forEach { t ->
                val (bg, fg) = markChipColors(t.rawMarks)
                Box(
                    Modifier.defaultMinSize(minWidth = 30.dp).height(30.dp).clip(RoundedCornerShape(8.dp))
                        .background(bg).padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (t.rawMarks == t.marks) "${t.rawMarks}" else "${t.rawMarks}→${t.marks}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                    )
                }
            }
        }
    }
}

/** 「ターン3 ・ 2投目」と、ターン内の投げた数の丸。 */
@Composable
private fun TurnRow(p: PracticeState) {
    val inTurn = p.throws.size % 3
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (p.finished) "練習おつかれさま" else "ターン${p.currentTurn} ・ ${p.dartInTurn}投目",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { i ->
                val filled = !p.finished && i < inTurn
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .background(if (filled) DartsColors.Accent else Color.Transparent)
                        .border(2.dp, if (filled) DartsColors.Accent else DartsColors.Dashed, CircleShape),
                )
            }
        }
    }
}

/** 入力ボタン(2×2)。ブルのときはトリプルを押せなくする。 */
@Composable
private fun InputButtons(rule: PracticeRule, target: String, modifier: Modifier, onInput: (Int) -> Unit) {
    val isBull = target == BULL
    val max = rule.maxMarks(target)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MarkButton("ミス", 0, DartsColors.Track, DartsColors.Text, DartsColors.SubText, true, Modifier.weight(1f), onInput, border = true)
            MarkButton(if (isBull) "シングルブル" else "シングル", 1, DartsColors.Red2, Color.White, DartsColors.OnRed1, 1 <= max, Modifier.weight(1f), onInput)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MarkButton(if (isBull) "ダブルブル" else "ダブル", 2, DartsColors.Red3, Color.White, DartsColors.OnRed2, 2 <= max, Modifier.weight(1f), onInput)
            MarkButton("トリプル", 3, DartsColors.Accent, DartsColors.OnAccent, DartsColors.OnAccent, 3 <= max, Modifier.weight(1f), onInput)
        }
    }
}

@Composable
private fun MarkButton(
    label: String,
    marks: Int,
    bg: Color,
    fg: Color,
    subColor: Color,
    enabled: Boolean,
    modifier: Modifier,
    onInput: (Int) -> Unit,
    border: Boolean = false,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxHeight()
            .alpha(if (enabled) 1f else 0.3f)
            .clip(shape)
            .background(bg)
            .then(if (border) Modifier.border(1.dp, DartsColors.Border, shape) else Modifier)
            .clickable(enabled = enabled) { onInput(marks) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, fontSize = 20.sp, fontWeight = if (marks == 3) FontWeight.Black else FontWeight.Bold, color = fg)
        Text("${marks}マーク", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subColor)
    }
}

/** ブルまで完了したときのパネル。取り消しもできるように、結果画面へは自分で進む。 */
@Composable
private fun DonePanel(total: Int, modifier: Modifier, onShowResult: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(DartsColors.DoneBg).border(1.dp, DartsColors.DoneBorder, shape).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("ブルまで完了!", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DartsColors.Accent)
        Text(
            buildAnnotatedString {
                append("合計 ")
                withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)) { append("$total") }
                append(" 本")
            },
            fontSize = 15.sp,
        )
        AccentButton("結果を見る", height = 56.dp, onClick = onShowResult)
    }
}

@Composable
private fun FooterButtons(canUndo: Boolean, onUndo: () -> Unit, onBack: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val shape = RoundedCornerShape(16.dp)
        Box(
            Modifier.weight(1f).height(52.dp).alpha(if (canUndo) 1f else 0.4f).clip(shape)
                .border(1.dp, DartsColors.Dashed, shape).clickable(enabled = canUndo, onClick = onUndo),
            contentAlignment = Alignment.Center,
        ) {
            Text("1投取り消す", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        OutlineButton("中断して戻る", Modifier.weight(1f), color = DartsColors.SubText, onClick = onBack)
    }
}
