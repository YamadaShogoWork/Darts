package com.dartspractice.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dartspractice.app.domain.stats.DayPoint
import com.dartspractice.app.ui.theme.DartsColors
import java.time.LocalDate

/**
 * ホーム用の小さな折れ線(操作なし)。横は startDay〜endDay の日付の位置、縦は本数。
 * 最後の点だけ大きく描く。
 */
@Composable
fun Sparkline(points: List<DayPoint>, startDay: LocalDate, endDay: LocalDate, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val pad = 6.dp.toPx()
        val baseY = size.height - 2.dp.toPx()
        drawLine(DartsColors.Border, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1.dp.toPx())
        if (points.isEmpty()) return@Canvas

        val days = (endDay.toEpochDay() - startDay.toEpochDay()).coerceAtLeast(1).toFloat()
        val min = points.minOf { it.avgDarts }
        val max = points.maxOf { it.avgDarts }
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        fun x(p: DayPoint) = pad + (size.width - pad * 2) * ((p.day.toEpochDay() - startDay.toEpochDay()) / days)
        // モックアップどおり、本数が多いほど上(上達すると右下がりになる)
        fun y(p: DayPoint) = pad + (baseY - pad * 2) * (1 - ((p.avgDarts - min) / range)).toFloat()

        val path = Path()
        points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(p), y(p)) else path.lineTo(x(p), y(p)) }
        drawPath(
            path,
            DartsColors.Accent,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val last = points.last()
        drawCircle(DartsColors.Accent, radius = 4.dp.toPx(), center = Offset(x(last), y(last)))
    }
}
