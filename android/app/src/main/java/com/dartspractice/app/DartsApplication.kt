package com.dartspractice.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dartspractice.app.data.AwardPrefs
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.data.StatsRepository
import com.dartspractice.app.data.backup.BackupPrefs
import com.dartspractice.app.data.backup.BackupRepository
import com.dartspractice.app.data.db.DartsDatabase

/** DBと Repository をアプリ全体で1つだけ持つ。 */
class DartsApplication : Application() {
    val database by lazy { DartsDatabase.create(this) }
    val repository by lazy { PracticeRepository(database) }
    val stats by lazy { StatsRepository(database) }
    val backup by lazy { BackupRepository(database) }
    val backupPrefs by lazy { BackupPrefs(this) }
    val awardPrefs by lazy { AwardPrefs(this) }
}

/** DartsApplication を使って ViewModel を作る。 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (DartsApplication) -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory {
        initializer { create(this[APPLICATION_KEY] as DartsApplication) }
    },
)
