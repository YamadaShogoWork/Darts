package com.dartspractice.app.ui.settings

import android.content.ContentResolver
import android.database.SQLException
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dartspractice.app.data.MenuSelection
import com.dartspractice.app.data.PracticeRepository
import com.dartspractice.app.data.StatsRepository
import com.dartspractice.app.data.backup.BackupCodec
import com.dartspractice.app.data.backup.BackupData
import com.dartspractice.app.data.backup.BackupFormatException
import com.dartspractice.app.data.backup.BackupPrefs
import com.dartspractice.app.data.backup.BackupRepository
import com.dartspractice.app.data.debug.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 保存されている練習の数(完了したもの)。 */
data class DataCount(val sessions: Int = 0, val days: Int = 0)

/** 読み込む前の確認に出す内容。 */
data class ImportPreview(
    val data: BackupData,
    /** 完了した練習の数と日数。 */
    val sessions: Int,
    val days: Int,
)

/** 画面に出す一言。error なら赤字の失敗表示。 */
data class SettingsMessage(val text: String, val error: Boolean = false)

/** 設定。バックアップの書き出し・読み込みと、開発用の操作。 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val repo: PracticeRepository,
    stats: StatsRepository,
    private val backup: BackupRepository,
    private val prefs: BackupPrefs,
    private val resolver: ContentResolver,
    private val selection: MenuSelection,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** 全メニューの合計。 */
    val count: StateFlow<DataCount> = stats.totals()
        .map { DataCount(it.sessions, it.days) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataCount())

    private val _lastBackupAt = MutableStateFlow(prefs.lastBackupAt)
    val lastBackupAt: StateFlow<Long?> = _lastBackupAt.asStateFlow()

    private val _message = MutableStateFlow<SettingsMessage?>(null)
    val message: StateFlow<SettingsMessage?> = _message.asStateFlow()

    private val _preview = MutableStateFlow<ImportPreview?>(null)
    /** null でなければ読み込みの確認を出す。 */
    val preview: StateFlow<ImportPreview?> = _preview.asStateFlow()

    private var busy = false

    /** 選んだ場所にバックアップを書き出す。 */
    fun export(uri: Uri) = run {
        val time = now()
        val data = backup.exportData()
        val text = BackupCodec.encode(data, time)
        withContext(Dispatchers.IO) {
            val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("ファイルを開けませんでした")
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
        prefs.lastBackupAt = time
        _lastBackupAt.value = time
        SettingsMessage("書き出しました(練習${data.sessions.count { it.session.finished }}回分)")
    }

    /** 選んだファイルを読んで中身を確かめ、確認を出す(まだ置き換えない)。 */
    fun prepareImport(uri: Uri) = run {
        val text = withContext(Dispatchers.IO) {
            val input = resolver.openInputStream(uri) ?: throw IOException("ファイルを開けませんでした")
            input.use { it.readBytes().toString(Charsets.UTF_8) }
        }
        val data = withContext(Dispatchers.Default) { BackupCodec.decode(text) }
        val finished = data.sessions.filter { it.session.finished }
        val days = finished.mapNotNull { it.session.finishedAt }
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSet().size
        _preview.value = ImportPreview(data, finished.size, days)
        null
    }

    fun cancelImport() {
        _preview.value = null
    }

    /** 確認のあと、今のデータを読み込んだ内容に置き換える。 */
    fun confirmImport() {
        val p = _preview.value ?: return
        _preview.value = null
        run {
            backup.replaceAll(p.data)
            SettingsMessage("読み込みました(練習${p.sessions}回分)")
        }
    }

    /** 開発用: 選んでいるメニューに、14日分のサンプルの練習を追加する。 */
    fun insertSamples() = run {
        val (menu, rule) = selection.selected.first()
        val samples = SampleData.generate(rule, LocalDate.now())
        samples.forEach {
            repo.insertCompleted(menu.id, rule, it.startedAt, it.finishedAt, it.rawMarks, it.memo)
        }
        SettingsMessage("${menu.name}に、サンプルの練習を${samples.size}回分追加しました")
    }

    /** 開発用: 練習と投の記録をすべて消す。 */
    fun deleteAll() = run {
        repo.deleteAll()
        SettingsMessage("すべての練習を消しました")
    }

    /** 1つずつ実行し、失敗したら理由を出す。block が null を返したら一言は出さない。 */
    private fun run(block: suspend () -> SettingsMessage?) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()?.let { _message.value = it }
            } catch (e: BackupFormatException) {
                _message.value = SettingsMessage("読み込めませんでした: ${e.message}", error = true)
            } catch (e: IOException) {
                _message.value = SettingsMessage("ファイルの読み書きに失敗しました: ${e.message}", error = true)
            } catch (e: SecurityException) {
                _message.value = SettingsMessage("ファイルを開く許可がありません", error = true)
            } catch (e: SQLException) {
                // 置き換えの途中で失敗したときは、トランザクションで元のデータに戻る
                _message.value = SettingsMessage("保存に失敗しました。データは元のままです: ${e.message}", error = true)
            } finally {
                busy = false
            }
        }
    }
}
