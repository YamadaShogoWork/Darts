package com.dartspractice.app.ui.screens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dartspractice.app.domain.rule.RateSpec
import com.dartspractice.app.domain.stats.DayPoint
import com.dartspractice.app.ui.Format
import com.dartspractice.app.ui.graph.Metric
import com.dartspractice.app.ui.theme.DartsColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 日ごとの平均(赤い線と点)と7日移動平均(黄色の太い線)の折れ線。Vico で描く。
 * 横軸は日付(エポック日)で、練習していない日は点を打たずに前後を結ぶ。
 * 点をタップすると、その日を [onSelect] で知らせる(もう一度タップで解除 → null)。
 */
@Composable
fun DailyChart(
    points: List<DayPoint>,
    movingAverage: List<DayPoint>,
    metric: Metric,
    rate: RateSpec,
    startDay: LocalDate,
    endDay: LocalDate,
    onSelect: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun value(avgDarts: Double): Double =
        if (metric == Metric.Rate) rate.of(avgDarts) ?: 0.0 else avgDarts

    val xs = points.map { it.day.toEpochDay() }
    val ys = points.map { value(it.avgDarts) }
    val maYs = movingAverage.map { value(it.avgDarts) }

    val model = remember(points, movingAverage, metric, rate) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(x = xs, y = ys)
                series(x = movingAverage.map { it.day.toEpochDay() }, y = maYs)
            },
        )
    }

    // 縦軸の範囲と目盛り(本数は5本ごと、MPR・ブル率はそれぞれの間隔)
    val step = if (metric == Metric.Rate) rate.axisStep else 5.0
    val all = ys + maYs
    val minY = floor((all.min() - step * 0.4) / step) * step
    val maxY = ceil((all.max() + step * 0.4) / step) * step
    val startX = startDay.toEpochDay().toDouble()
    val endX = maxOf(endDay.toEpochDay().toDouble(), startX + 1)
    val labelSpacing = maxOf(1, ceil((endX - startX) / 4).toInt())

    val red = DartsColors.Accent
    val yellow = DartsColors.Yellow
    val lineProvider = LineCartesianLayer.LineProvider.series(
        LineCartesianLayer.rememberLine(
            fill = remember { LineCartesianLayer.LineFill.single(Fill(red)) },
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp, cap = StrokeCap.Round),
            pointProvider = LineCartesianLayer.PointProvider.single(
                LineCartesianLayer.Point(rememberShapeComponent(fill = Fill(red), shape = CircleShape), size = 7.dp),
            ),
        ),
        LineCartesianLayer.rememberLine(
            fill = remember { LineCartesianLayer.LineFill.single(Fill(yellow)) },
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 4.dp, cap = StrokeCap.Round),
        ),
    )

    val currentOnSelect = rememberUpdatedState(onSelect)
    val listener = remember {
        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) =
                notify(targets)

            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) =
                notify(targets)

            override fun onHidden(marker: CartesianMarker) {
                currentOnSelect.value(null)
            }

            private fun notify(targets: List<CartesianMarker.Target>) {
                targets.firstOrNull()?.let { currentOnSelect.value(LocalDate.ofEpochDay(it.x.toLong())) }
            }
        }
    }

    val unit = if (metric == Metric.Rate) "" else "本"
    fun fmt(v: Double) = if (metric == Metric.Rate) rate.format(v) else Format.darts(v)
    val maByDay = movingAverage.associate { it.day.toEpochDay() to value(it.avgDarts) }
    val byDay = points.associate { it.day.toEpochDay() to value(it.avgDarts) }

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = TextStyle(color = DartsColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            padding = Insets(horizontal = 8.dp, vertical = 4.dp),
            background = rememberShapeComponent(
                fill = Fill(DartsColors.Surface),
                shape = RoundedCornerShape(6.dp),
                strokeFill = Fill(DartsColors.Border),
                strokeThickness = 1.dp,
            ),
        ),
        valueFormatter = remember(points, movingAverage, metric, rate) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val x = targets.firstOrNull()?.x?.toLong() ?: return@ValueFormatter ""
                val day = Format.monthDay(LocalDate.ofEpochDay(x).toString())
                val v = byDay[x]?.let { "${fmt(it)}$unit" } ?: "—"
                val ma = maByDay[x]?.let { " (7日 ${fmt(it)}$unit)" }.orEmpty()
                "$day $v$ma"
            }
        },
        guideline = rememberAxisGuidelineComponent(fill = Fill(DartsColors.SubText)),
    )

    val axisLabel = rememberAxisLabelComponent(style = TextStyle(color = DartsColors.SubText, fontSize = 11.sp))
    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = lineProvider,
            rangeProvider = remember(minY, maxY, startX, endX) {
                CartesianLayerRangeProvider.fixed(minX = startX, maxX = endX, minY = minY, maxY = maxY)
            },
        ),
        startAxis = VerticalAxis.rememberStart(
            label = axisLabel,
            line = null,
            tick = null,
            guideline = rememberAxisGuidelineComponent(fill = Fill(DartsColors.Border)),
            valueFormatter = remember(metric, rate) {
                CartesianValueFormatter { _, v, _ -> if (metric == Metric.Rate) rate.axis(v) else v.toInt().toString() }
            },
            itemPlacer = remember(step) { VerticalAxis.ItemPlacer.step({ step }) },
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            label = axisLabel,
            line = rememberAxisLineComponent(fill = Fill(DartsColors.Border)),
            tick = null,
            guideline = null,
            valueFormatter = remember {
                CartesianValueFormatter { _, v, _ -> Format.monthDay(LocalDate.ofEpochDay(v.toLong()).toString()) }
            },
            itemPlacer = remember(labelSpacing) {
                HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing }, addExtremeLabelPadding = true)
            },
        ),
        marker = marker,
        markerVisibilityListener = listener,
        getXStep = { _, _, _ -> 1.0 },
        markerController = CartesianMarkerController.rememberToggleOnTap(),
    )

    CartesianChartHost(
        chart = chart,
        model = model,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(
            zoomEnabled = false,
            initialZoom = Zoom.Content,
            minZoom = Zoom.Content,
            maxZoom = Zoom.Content,
        ),
    )
}
