package com.dartspractice.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.domain.rule.PracticeRule
import com.dartspractice.app.domain.rule.PracticeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RecordUiState(
    val menuName: String = "",
    val rule: PracticeRule? = null,
    val practice: PracticeState? = null,
)

/** 記録入力。1投ごとにDBへ保存し、保存後の状態を画面に出す。 */
class RecordViewModel(
    private val sessionId: Long,
    private val repo: PracticeRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(RecordUiState())
    val ui: StateFlow<RecordUiState> = _ui.asStateFlow()

    /** 連打しても1投ずつ順番に保存する。 */
    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            val loaded = repo.load(sessionId)
            _ui.value = RecordUiState(
                menuName = loaded.menu.name,
                rule = loaded.state.rule,
                practice = loaded.state,
            )
        }
    }

    fun input(rawMarks: Int) {
        viewModelScope.launch {
            mutex.withLock {
                val ui = _ui.value
                val rule = ui.rule ?: return@withLock
                val target = ui.practice?.currentTarget ?: return@withLock // 終了後は受け付けない
                if (rawMarks !in 0..rule.maxMarks(target)) return@withLock
                val next = repo.addThrow(sessionId, rule, rawMarks)
                _ui.value = ui.copy(practice = next)
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            mutex.withLock {
                val ui = _ui.value
                val rule = ui.rule ?: return@withLock
                if (ui.practice?.throws.isNullOrEmpty()) return@withLock
                _ui.value = ui.copy(practice = repo.undo(sessionId, rule))
            }
        }
    }
}
