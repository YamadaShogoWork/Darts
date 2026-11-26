package com.dartspractice.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dartspractice.app.ui.theme.DartsColors
import com.dartspractice.app.ui.theme.DartsIcons

/** 画面の向きを指定する。記録入力だけ横画面を許し、ほかは縦に固定する。 */
@Composable
fun ScreenOrientation(allowLandscape: Boolean) {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(allowLandscape) {
        activity.requestedOrientation = if (allowLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose { }
    }
}

/** モックアップのカード(背景 #171718・枠線 #2A2A2D・角丸)。 */
@Composable
fun DartsCard(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DartsColors.Surface)
            .border(1.dp, DartsColors.Border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

/** 戻るボタン付きの見出し。 */
@Composable
fun BackHeader(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    sub: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).offset(x = (-10).dp)) {
            Icon(DartsIcons.Back, contentDescription = backLabel, tint = DartsColors.Text)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = DartsColors.Text)
            if (sub != null) Text(sub, fontSize = 12.sp, color = DartsColors.SubText)
        }
        trailing()
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DartsColors.Text)
}

/** 大きな赤いボタン(練習開始・保存など)。 */
@Composable
fun AccentButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    fontSizeSp: Int = 18,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(DartsColors.Accent)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = fontSizeSp.sp, fontWeight = FontWeight.Black, color = DartsColors.OnAccent)
    }
}

/** 枠線だけのボタン(取り消し・破棄など)。 */
@Composable
fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DartsColors.Text,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .border(1.dp, DartsColors.Border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/** ナンバー別の本数バー(1行)。 */
@Composable
fun NumberBarRow(
    label: String,
    ratio: Float,
    value: String,
    barColor: Color = DartsColors.Accent,
    tag: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, modifier = Modifier.width(44.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(DartsColors.Track)) {
            Box(
                Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).height(10.dp)
                    .clip(RoundedCornerShape(5.dp)).background(barColor),
            )
        }
        if (tag != null) Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = barColor)
        Text(
            value,
            modifier = Modifier.width(52.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}

/** 中身はあとのステップで作る、という目印。 */
@Composable
fun PlaceholderNote(text: String) {
    Text(text, fontSize = 12.sp, color = DartsColors.SubText, modifier = Modifier.padding(top = 2.dp))
}

/** 1投のマーク数(0〜3)の色。背景と文字色。 */
fun markChipColors(rawMarks: Int): Pair<Color, Color> = when (rawMarks) {
    0 -> DartsColors.Track to DartsColors.SubText
    1 -> DartsColors.Red1 to DartsColors.OnRed1
    2 -> DartsColors.Red2 to DartsColors.OnRed2
    else -> DartsColors.Accent to DartsColors.OnAccent
}

/** 角丸の切り替えボタン(期間・指標)。fill = true なら横幅いっぱいに均等に並べる。 */
@Composable
fun Segmented(items: List<String>, selected: Int, modifier: Modifier = Modifier, fill: Boolean = false, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.clip(shape).background(DartsColors.Surface).border(1.dp, DartsColors.Border, shape).padding(3.dp),
    ) {
        items.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) DartsColors.Accent else DartsColors.Surface)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) DartsColors.OnAccent else DartsColors.SubText,
                )
            }
        }
    }
}
