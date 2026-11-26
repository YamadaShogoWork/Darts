package com.dartspractice.app.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.dartspractice.app.data.db.DartsDatabase

/** DBの中身をまるごと取り出す・置き換える。 */
class BackupRepository(private val db: DartsDatabase) {
    private val menus = db.menuDao()
    private val sessions = db.sessionDao()
    private val throwsDao = db.dartThrowDao()

    /** 全部のメニュー・練習(途中の練習も含む)・投。 */
    suspend fun exportData(): BackupData = db.withTransaction {
        val byId = throwsDao.all().groupBy { it.sessionId }
        BackupData(
            menus = menus.all(),
            sessions = sessions.all().map { BackupSession(it, byId[it.id].orEmpty()) },
        )
    }

    /** 今のデータを全部消して、data に置き換える。途中で失敗したら元のまま。 */
    suspend fun replaceAll(data: BackupData) = db.withTransaction {
        sessions.deleteAll() // 投も一緒に消える
        menus.deleteAll()
        data.menus.forEach { menus.insert(it) }
        data.sessions.forEach { (session, throws) ->
            sessions.insert(session)
            throws.forEach { throwsDao.insert(it.copy(id = 0)) }
        }
    }
}

/** 前回のバックアップ日時(端末内の小さな設定ファイルに保存)。 */
class BackupPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("backup", Context.MODE_PRIVATE)

    var lastBackupAt: Long?
        get() = prefs.getLong(KEY_LAST, -1L).takeIf { it >= 0 }
        set(value) {
            prefs.edit().apply { if (value == null) remove(KEY_LAST) else putLong(KEY_LAST, value) }.apply()
        }

    private companion object {
        const val KEY_LAST = "lastBackupAt"
    }
}
