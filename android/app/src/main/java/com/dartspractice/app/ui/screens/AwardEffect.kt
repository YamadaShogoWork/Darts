package com.dartspractice.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dartspractice.app.domain.rule.Award
import com.dartspractice.app.ui.theme.DartsColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

/** 演出を出すきっかけ。id が変わるたびに最初から再生する(同じアワードが続いても出る)。 */
data class AwardEvent(val award: Award, val id: Long)

private const val TEXT_MS = 1600     // 1文字ぶんの点灯〜消灯
private const val CHAR_DELAY_MS = 35 // 1文字ずつずらす
private const val RING_MS = 1100
private const val RING_DELAY_MS = 140
private const val SHAKE_MS = 450
private const val EDGE_MS = 900

/**
 * アワードの演出(パターン3: リング衝撃波+ネオン、4段階)を content の上に重ねる。
 * 演出は触っても反応しないので、下の入力ボタンはそのまま押せる。段階4では content ごと揺らす。
 */
@Composable
fun AwardOverlay(event: AwardEvent?, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shake = remember { Animatable(1f) }
    LaunchedEffect(event?.id) {
        if (event?.award?.tier == 4) {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(SHAKE_MS, easing = LinearEasing))
        } else {
            shake.snapTo(1f)
        }
    }
    Box(modifier) {
        Box(
            Modifier.fillMaxSize().offset {
                // 0→1 の間に、だんだん小さくなる左右の揺れ
                val p = shake.value
                val amp = (1 - p) * 5.dp.toPx()
                IntOffset((sin(p * 4 * Math.PI) * amp).roundToInt(), (sin(p * 4 * Math.PI + 1) * amp * 0.4f).roundToInt())
            },
            content = content,
        )
        if (event != null) key(event.id) { AwardEffect(event.award) }
    }
}

@Composable
private fun AwardEffect(award: Award) {
    val tier = award.tier
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val total = maxOf(TEXT_MS + award.label.length * CHAR_DELAY_MS, RING_MS + 4 * RING_DELAY_MS)
        launch { clock.animateTo(total.toFloat(), tween(total, easing = LinearEasing)) }
    }
    val ms = clock.value

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 広がる輪(段階1:1本 / 2:2本 / 3:3本 / 4:5本。赤と黄を交互に)
        Canvas(Modifier.fillMaxSize()) {
            val rings = intArrayOf(1, 2, 3, 5)[tier - 1]
            repeat(rings) { i ->
                val p = ((ms - i * RING_DELAY_MS) / RING_MS).coerceIn(0f, 1f)
                if (p <= 0f || p >= 1f) return@repeat
                val eased = 1 - (1 - p) * (1 - p) // だんだん遅くなる
                val radius = 20.dp.toPx() * (0.3f + 8.7f * eased)
                drawCircle(
                    color = (if (i % 2 == 0) DartsColors.Accent else DartsColors.Yellow).copy(alpha = 1 - p),
                    radius = radius,
                    style = Stroke(width = 4.dp.toPx()),
                )
            }
            // 段階4: 画面の縁が一瞬赤く光る
            if (tier == 4 && ms < EDGE_MS) {
                val p = ms / EDGE_MS
                val a = if (p < 0.15f) p / 0.15f else 1 - (p - 0.15f) / 0.85f
                val layers = 8
                repeat(layers) { j ->
                    val w = 6.dp.toPx()
                    val inset = j * w
                    drawRect(
                        color = DartsColors.Accent.copy(alpha = 0.9f * a * (1 - j.toFloat() / layers)),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        style = Stroke(width = w),
                    )
                }
            }
        }
        NeonText(award, ms)
    }
}

/** 1文字ずつネオンのように点灯して消える文字。長い名前は2行にする。 */
@Composable
private fun NeonText(award: Award, ms: Float) {
    val tier = award.tier
    val long = award.label.length > 10
    val size = when {
        tier == 4 && long -> 34.sp
        tier == 4 -> 52.sp
        long -> 30.sp
        else -> 44.sp
    }
    val lines = if (long) award.label.split(" ").let { w -> listOf(w.take(2).joinToString(" "), w.drop(2).joinToString(" ")) }
    else listOf(award.label)

    // 段階ごとの光り方(色・にじみ)。いちばん外側の光から順に重ねる
    val glows: List<Pair<Color, Float>> = when (tier) {
        1 -> listOf(DartsColors.Accent to 16f)
        2 -> listOf(DartsColors.Accent to 36f, DartsColors.Accent to 16f)
        3 -> listOf(DartsColors.Accent to 60f, DartsColors.Yellow to 32f, Color.White to 12f)
        else -> listOf(DartsColors.Yellow to 120f, DartsColors.Accent to 80f, DartsColors.Accent to 36f, Color.White to 14f)
    }
    val color = if (tier == 1) Color(0xFFFFD0D1) else Color.White

    var k = 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        lines.forEach { line ->
            Row {
                line.forEach { ch ->
                    val local = ms - (k++) * CHAR_DELAY_MS
                    val (alpha, dy) = neonFrame(local)
                    Box(Modifier.graphicsLayer { this.alpha = alpha; translationY = dy * 10.dp.toPx() }) {
                        val base = TextStyle(fontSize = size, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                        glows.forEach { (c, blur) ->
                            Text(ch.toString(), style = base.copy(color = color, shadow = Shadow(c, Offset.Zero, blur)))
                        }
                    }
                }
            }
        }
    }
}

/** 1文字の明るさと上下のずれ(sample の CSS と同じ動き: 点灯 → 一瞬ちらつく → 保つ → 消える)。 */
private fun neonFrame(localMs: Float): Pair<Float, Float> {
    if (localMs <= 0f) return 0f to 1f
    val p = localMs / TEXT_MS
    return when {
        p < 0.10f -> (p / 0.10f) to (1 - p / 0.10f)
        p < 0.14f -> (1 - 0.7f * (p - 0.10f) / 0.04f) to 0f
        p < 0.18f -> (0.3f + 0.7f * (p - 0.14f) / 0.04f) to 0f
        p < 0.80f -> 1f to 0f
        p < 1f -> (1 - (p - 0.80f) / 0.20f) to 0f
        else -> 0f to 0f
    }
}
