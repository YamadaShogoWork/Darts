package com.dartspractice.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dartspractice.app.domain.rule.Cricket10Rule

@Database(
    entities = [MenuEntity::class, SessionEntity::class, DartThrowEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DartsDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun sessionDao(): SessionDao
    abstract fun dartThrowDao(): DartThrowDao
    abstract fun statsDao(): StatsDao

    companion object {
        const val DEFAULT_MENU_ID = 1L
        const val DEFAULT_MENU_NAME = "クリケット10マーク"

        /** DBを最初に作るときに、最初のメニューを1件入れる。 */
        val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO menu (id, name, ruleType) VALUES (?, ?, ?)",
                    arrayOf<Any>(DEFAULT_MENU_ID, DEFAULT_MENU_NAME, Cricket10Rule.ruleType),
                )
            }
        }

        fun create(context: Context): DartsDatabase =
            Room.databaseBuilder(context, DartsDatabase::class.java, "darts.db")
                .addCallback(seedCallback)
                .build()
    }
}
