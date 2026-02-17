package com.stc.terminowo.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.stc.terminowo.data.local.db.DocumentDatabase

actual class DatabaseDriverFactory(
    private val context: Context
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = DocumentDatabase.Schema,
            context = context,
            name = "document_scanner.db"
        )
    }
}
