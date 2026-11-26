package com.dartspractice.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** モックアップ「案F(最初の案×赤黒)」の配色。全画面でこれを使う。 */
object DartsColors {
    val Background = Color(0xFF0B0B0C)
    val Surface = Color(0xFF171718)      // カード
    val Border = Color(0xFF2A2A2D)       // カードの枠線・区切り線
    val Track = Color(0xFF242427)        // バーの下地
    val NavBar = Color(0xFF101011)       // 下部ナビの背景
    val Dashed = Color(0xFF52525B)       // 点線の枠
    val Text = Color(0xFFF4F4F5)
    val SubText = Color(0xFFA1A1AA)
    val Accent = Color(0xFFFF5A5F)       // 赤(メインの強調)
    val OnAccent = Color(0xFF1A0506)     // 赤の上の文字
    val Orange = Color(0xFFFF9F43)       // 苦手ナンバーの最多
    val Yellow = Color(0xFFFFD166)       // 移動平均・2番目の苦手

    // 記録入力の段階色(モックアップの緑の濃淡を赤に置き換えたもの)
    val Red1 = Color(0xFF4A2224)         // 1マークのチップ
    val Red2 = Color(0xFF6E2A2D)         // 2マークのチップ・シングルのボタン
    val Red3 = Color(0xFFA8383C)         // ダブルのボタン
    val OnRed1 = Color(0xFFFFC9CA)
    val OnRed2 = Color(0xFFFFE9EA)
    val DoneBg = Color(0xFF3A1A1C)       // 完了したナンバー・完了パネル
    val DoneBorder = Color(0xFF7A2E31)
}

private val ColorScheme = darkColorScheme(
    primary = DartsColors.Accent,
    onPrimary = DartsColors.OnAccent,
    secondary = DartsColors.Yellow,
    background = DartsColors.Background,
    onBackground = DartsColors.Text,
    surface = DartsColors.Surface,
    onSurface = DartsColors.Text,
    surfaceVariant = DartsColors.Track,
    onSurfaceVariant = DartsColors.SubText,
    outline = DartsColors.Border,
    outlineVariant = DartsColors.Dashed,
)

@Composable
fun DartsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography(),
        content = content,
    )
}
