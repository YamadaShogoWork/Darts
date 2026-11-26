package com.dartspractice.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.data.StatsRepository
import com.dartspractice.app.data.db.DailyStat
import com.dartspractice.app.data.db.SessionSummary
import com.dartspractice.app.data.db.TargetStat
import com.dartspractice.app.domain.rule.PracticeRules
import com.dartspractice.app.domain.stats.DayPoint
import com.dartspractice.app.domain.stats.Trend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 再開の確認に出す内容。 */
data class ResumeCandidate(val sessionId: Long, val menuId: Long, val darts: Int)

/** ホームに出す集計。 */
data class HomeStats(
    val menuName: String = "",
    val totalMarks: Int = 0,
    /** 今日の集計。今日まだ練習していなければ null。 */
    val today: DailyStat? = null,
    val best: SessionSummary? = null,
    /** 最後に練習した日。 */
    val latest: DailyStat? = null,
    /** 直近14日の日ごとの平均(古い順)。 */
    val recent: List<DayPoint> = emptyList(),
    /** 直近7日で平均本数が多いナンバー(多い順に2つ)。 */
    val weak: List<TargetStat> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repo: PracticeRepository,
    stats: StatsRepository,
) : ViewModel() {

    val stats: StateFlow<HomeStats> = flow { emit(repo.defaultMenu()) }
        .flatMapLatest { menu ->
            val totalMarks = PracticeRules.of(menu.ruleType).totalMarks
            val now = LocalDate.now()
            val weekFrom = now.minusDays(6).toString()
            combine(stats.daily(menu.id), stats.best(menu.id), stats.targetStats(menu.id, weekFrom)) { daily, best, targets ->
                val today = now.toString()
                val all = daily.map { DayPoint(LocalDate.parse(it.day), it.avgDarts) }
                HomeStats(
                    menuName = menu.name,
                    totalMarks = totalMarks,
                    today = daily.firstOrNull { it.day == today },
                    best = best,
                    latest = daily.lastOrNull(),
                    recent = Trend.inPeriod(all, now, 14),
                    weak = targets.sortedByDescending { it.avgDarts }.take(2),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStats())

    private val _resume = MutableStateFlow<ResumeCandidate?>(null)
    /** null でなければ「続きから再開 / 新しく始める」の確認を出す。 */
    val resume: StateFlow<ResumeCandidate?> = _resume.asStateFlow()

    private val _openRecord = Channel<Long>(Channel.BUFFERED)
    /** 記録入力画面を開く(練習の id)。 */
    val openRecord = _openRecord.receiveAsFlow()

    private var busy = false

    fun onStartPressed() = launchOnce {
        val menu = repo.defaultMenu()
        val unfinished = repo.resumable(menu.id)
        if (unfinished != null) {
            _resume.value = ResumeCandidate(unfinished.id, menu.id, repo.load(unfinished.id).state.totalDarts)
        } else {
            _openRecord.send(repo.startNew(menu.id))
        }
    }

    fun resume() {
        val c = _resume.value ?: return
        _resume.value = null
        viewModelScope.launch { _openRecord.send(c.sessionId) }
    }

    fun discardAndStartNew() {
        val c = _resume.value ?: return
        _resume.value = null
        launchOnce { _openRecord.send(repo.startNew(c.menuId)) }
    }

    fun dismissResume() {
        _resume.value = null
    }

    /** 連打で練習が2つできないようにする。 */
    private fun launchOnce(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { block() } finally { busy = false }
        }
    }
}
