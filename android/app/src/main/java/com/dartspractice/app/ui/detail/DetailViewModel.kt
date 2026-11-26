package com.dartspractice.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartspractice.app.data.MenuSelection
import com.dartspractice.app.data.StatsRepository
import com.dartspractice.app.data.db.DailyStat
import com.dartspractice.app.data.db.DartThrowEntity
import com.dartspractice.app.data.db.SessionSummary
import com.dartspractice.app.domain.rule.PracticeRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** 1回分の練習と、その投(投げた順)。 */
data class SessionDetail(val summary: SessionSummary, val throws: List<DartThrowEntity>)

data class DetailUiState(
    val loading: Boolean = true,
    val menuName: String = "",
    val targets: List<String> = emptyList(),
    /** 選んでいるメニューのルール(読み込み中は null)。 */
    val rule: PracticeRule? = null,
    /** 練習した日(古い順)。 */
    val days: List<String> = emptyList(),
    /** 表示している日。練習がひとつもなければ null。 */
    val day: String? = null,
    val dayStat: DailyStat? = null,
    val sessions: List<SessionDetail> = emptyList(),
    val selected: Int = 0,
) {
    val prevDay: String? get() = day?.let { d -> days.lastOrNull { it < d } }
    val nextDay: String? get() = day?.let { d -> days.firstOrNull { it > d } }
}

/**
 * 日別詳細。date が null なら最後に練習した日を出す。
 * ‹ › で練習した日を前後に移動できる。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModel(
    date: String?,
    selection: MenuSelection,
    stats: StatsRepository,
) : ViewModel() {

    private val requestedDay = MutableStateFlow(date)
    private val selected = MutableStateFlow(0)

    val ui: StateFlow<DetailUiState> = selection.selected
        .flatMapLatest { (menu, rule) ->
            combine(stats.daily(menu.id), requestedDay) { daily, requested -> daily to (requested ?: daily.lastOrNull()?.day) }
                .flatMapLatest { (daily, day) ->
                    val base = DetailUiState(
                        loading = false,
                        menuName = menu.name,
                        targets = rule.targets,
                        rule = rule,
                        days = daily.map { it.day },
                        day = day,
                        dayStat = daily.firstOrNull { it.day == day },
                    )
                    if (day == null) {
                        flowOf(base)
                    } else {
                        combine(
                            stats.sessionsOfDay(menu.id, day),
                            stats.throwsOfDay(menu.id, day),
                            selected,
                        ) { sessions, throws, sel ->
                            val byId = throws.groupBy { it.sessionId }
                            val details = sessions.map { SessionDetail(it, byId[it.id].orEmpty()) }
                            base.copy(sessions = details, selected = sel.coerceIn(0, (details.size - 1).coerceAtLeast(0)))
                        }
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun showDay(day: String) {
        selected.value = 0
        requestedDay.value = day
    }

    fun select(index: Int) {
        selected.value = index
    }
}
