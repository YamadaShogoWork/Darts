package com.dartspractice.app.data

import android.content.Context
import com.dartspractice.app.domain.rule.Award
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** アワードごとの ON/OFF(端末内の小さな設定ファイルに保存。最初は全部 ON)。 */
class AwardPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("awards", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(load())
    val enabled: StateFlow<Set<Award>> = _enabled.asStateFlow()

    fun setEnabled(award: Award, on: Boolean) {
        val next = if (on) _enabled.value + award else _enabled.value - award
        val disabled = (Award.entries.toSet() - next).map { it.name }.toSet()
        prefs.edit().putStringSet(KEY_DISABLED, disabled).apply()
        _enabled.value = next
    }

    private fun load(): Set<Award> {
        val disabled = prefs.getStringSet(KEY_DISABLED, emptySet()).orEmpty()
        return Award.entries.filterNot { it.name in disabled }.toSet()
    }

    private companion object {
        const val KEY_DISABLED = "disabled"
    }
}
