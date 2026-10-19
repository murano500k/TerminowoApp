package com.stc.terminowo.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.stc.terminowo.data.local.db.DocumentDatabase
import com.stc.terminowo.platform.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.timeIntervalSince1970
import platform.posix.memcpy

private const val DB_NAME = "document_scanner.db"

@OptIn(ExperimentalForeignApi::class)
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return try {
            openDriver()
        } catch (e: Exception) {
            // Only a file that is not a database at all is moved aside; any other failure
            // (e.g. a full disk) is rethrown so existing data is never discarded.
            if (!isCorrupt(e)) throw e
            AppLogger.e("Database", "Corrupt database, moving it aside", e)
            moveAside()
            openDriver()
        }
    }

    private fun openDriver(): SqlDriver =
        NativeSqliteDriver(schema = DocumentDatabase.Schema, name = DB_NAME).also {
            it.execute(null, "PRAGMA foreign_keys = ON", 0)
        }

    private fun isCorrupt(e: Throwable): Boolean {
        val messages = generateSequence(e) { it.cause }.mapNotNull { it.message?.lowercase() }
        if (messages.any { "not a database" in it || "malformed" in it }) return true
        return !hasSqliteHeader()
    }

    private fun hasSqliteHeader(): Boolean {
        val path = dbPath() ?: return true
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return true
        val handle = NSFileHandle.fileHandleForReadingAtPath(path) ?: return true
        val data: NSData = handle.readDataOfLength(16u)
        handle.closeFile()
        if (data.length.toInt() < 16) return false
        val bytes = ByteArray(16)
        bytes.usePinned { memcpy(it.addressOf(0), data.bytes, 16u) }
        return bytes.decodeToString(0, 15) == "SQLite format 3"
    }

    private fun moveAside() {
        val path = dbPath() ?: return
        val suffix = ".corrupt-${NSDate().timeIntervalSince1970.toLong()}"
        val fm = NSFileManager.defaultManager
        listOf("", "-wal", "-shm").forEach { ext ->
            val file = path + ext
            if (fm.fileExistsAtPath(file)) fm.moveItemAtPath(file, file + suffix, null)
        }
    }

    private fun dbPath(): String? {
        val dir = NSFileManager.defaultManager
            .URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask)
            .firstOrNull() as? NSURL ?: return null
        return dir.path?.let { "$it/databases/$DB_NAME" }
    }
}
