package ru.chtcholeg.aichat.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class DatabaseManager(context: Context) {
    private val driver: SqlDriver = AndroidSqliteDriver(
        schema = AppDatabase.Schema,
        context = context,
        name = "chats.db",
        callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                //db.execSQL("PRAGMA foreign_keys = ON")
            }

            override fun onConfigure(db: SupportSQLiteDatabase) {
                super.onConfigure(db)
                //db.setForeignKeyConstraintsEnabled(true)
            }
        }
    )

    val appDatabase = AppDatabase(driver)

    companion object {
        private var _instance: DatabaseManager? = null
        val instance get() = _instance!!

        fun init(context: Context) {
            _instance = DatabaseManager(context)
        }
    }
}
