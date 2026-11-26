package com.dartspractice.app.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartspractice.app.data.LoadedPractice
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.data.StatsRepository
import com.dartspractice.app.data.db.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResultUiState(
    val loaded: LoadedPractice? = null,
    /** 前回の練習(比較用)。なければ null。 */
    val previous: SessionSummary? = null,
)

/** 結果画面。メモの保存と練習の破棄を行う。 */
class ResultViewModel(
    private val sessionId: Long,
    private val repo: PracticeRepository,
    private val stats: StatsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ResultUiState())
    val ui: StateFlow<ResultUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repo.load(sessionId)
            val finishedAt = loaded.session.finishedAt
            val previous = finishedAt?.let { stats.previous(loaded.menu.id, it) }
            _ui.value = ResultUiState(loaded, previous)
        }
    }

    fun save(memo: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.saveMemo(sessionId, memo)
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(sessionId)
            onDone()
        }
    }
}
