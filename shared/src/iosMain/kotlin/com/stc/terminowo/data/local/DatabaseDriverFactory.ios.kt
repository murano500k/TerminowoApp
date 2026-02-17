package com.stc.terminowo.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.stc.terminowo.data.local.db.DocumentDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = DocumentDatabase.Schema,
            name = "document_scanner.db"
        )
    }
}
