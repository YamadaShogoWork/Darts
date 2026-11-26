package com.dartspractice.app.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartspractice.app.data.MenuSelection
import com.dartspractice.app.data.StatsRepository
import com.dartspractice.app.data.db.TargetStat
import com.dartspractice.app.domain.rule.PracticeRule
import com.dartspractice.app.domain.stats.DayPoint
import com.dartspractice.app.domain.stats.Trend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** 表示期間。days が null なら全期間。 */
enum class Period(val label: String, val days: Int?) {
    Week("7日", 7), Month("30日", 30), All("全期間", null)
}

/** 縦軸の指標。Rate はメニューごとの指標(クリケットは MPR、ブル10本はブル率)。 */
enum class Metric { Darts, Rate }

/** ナンバー別平均の期間。 */
enum class NumberPeriod(val label: String, val days: Int) { Week("直近7日", 7), Month("直近30日", 30) }

data class GraphUiState(
    val loading: Boolean = true,
    val menuName: String = "",
    val targets: List<String> = emptyList(),
    /** 選んでいるメニューのルール(読み込み中は null)。 */
    val rule: PracticeRule? = null,
    val today: LocalDate = LocalDate.now(),
    val period: Period = Period.Week,
    val metric: Metric = Metric.Darts,
    /** 期間内の日ごとの平均(古い順)。 */
    val points: List<DayPoint> = emptyList(),
    /** 期間内の7日移動平均(計算は全期間のデータで行う)。 */
    val movingAverage: List<DayPoint> = emptyList(),
    val numberPeriod: NumberPeriod = NumberPeriod.Week,
    val targetStats: List<TargetStat> = emptyList(),
    /** グラフで選んだ日。 */
    val selected: LocalDate? = null,
) {
    /** 横軸の左端(全期間なら最初に練習した日)。 */
    val startDay: LocalDate
        get() = period.days?.let { today.minusDays((it - 1).toLong()) } ?: (points.firstOrNull()?.day ?: today)
}

@OptIn(ExperimentalCoroutinesApi::class)
class GraphViewModel(
    selection: MenuSelection,
    stats: StatsRepository,
) : ViewModel() {

    private val period = MutableStateFlow(Period.Week)
    private val metric = MutableStateFlow(Metric.Darts)
    private val numberPeriod = MutableStateFlow(NumberPeriod.Week)
    private val selected = MutableStateFlow<LocalDate?>(null)

    val ui: StateFlow<GraphUiState> = selection.selected
        .flatMapLatest { (menu, rule) ->
            val today = LocalDate.now()
            val targetStats = numberPeriod.flatMapLatest { np ->
                stats.targetStats(menu.id, today.minusDays((np.days - 1).toLong()).toString())
            }
            combine(stats.daily(menu.id), period, metric, combine(numberPeriod, targetStats, ::Pair), selected) {
                    daily, p, m, (np, ts), sel ->
                val all = daily.map { DayPoint(LocalDate.parse(it.day), it.avgDarts) }
                val ma = Trend.movingAverage(all)
                GraphUiState(
                    loading = false,
                    menuName = menu.name,
                    targets = rule.targets,
                    rule = rule,
                    today = today,
                    period = p,
                    metric = m,
                    points = Trend.inPeriod(all, today, p.days),
                    movingAverage = Trend.inPeriod(ma, today, p.days),
                    numberPeriod = np,
                    targetStats = ts,
                    selected = sel,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GraphUiState())

    fun setPeriod(p: Period) {
        period.value = p
        selected.value = null
    }

    fun setMetric(m: Metric) {
        metric.value = m
    }

    fun setNumberPeriod(p: NumberPeriod) {
        numberPeriod.value = p
    }

    fun select(day: LocalDate?) {
        selected.value = day
    }
}
