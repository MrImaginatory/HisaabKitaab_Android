package com.hisaabkitaab.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Direct SQLite database manager ensuring 100% byte-level compatibility with HisaabKitaab web `.db` files
 * (sql.js). Uses JOURNAL_MODE=DELETE so the active `.db` file is always a single self-contained file
 * ready for backup export or web import without WAL sidecar loss.
 */
class HisaabDatabaseHelper(private val context: Context) {

    private val lock = Any()
    private var database: SQLiteDatabase? = null

    val dbFile: File
        get() = File(context.filesDir, ACTIVE_DB_FILE_NAME)

    fun hasExistingDatabase(): Boolean = dbFile.exists() && dbFile.length() >= 100

    fun getFileSizeBytes(): Long = if (dbFile.exists()) dbFile.length() else 0L

    fun openOrCreateActive(): Result<SQLiteDatabase> = synchronized(lock) {
        runCatching {
            database?.takeIf { it.isOpen }?.let { return@runCatching it }
            dbFile.parentFile?.mkdirs()
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
            )
            configureJournalMode(db)
            ensureSchema(db)
            database = db
            db
        }
    }

    fun createBlankDatabase(): Result<Unit> = synchronized(lock) {
        runCatching {
            closeInternal()
            deleteDatabaseFiles(dbFile)
            dbFile.parentFile?.mkdirs()
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
            )
            configureJournalMode(db)
            ensureSchema(db)
            database = db
        }
    }

    fun importFromStream(inputStream: InputStream): Result<Unit> = synchronized(lock) {
        val tempFile = File(context.cacheDir, "import_validate_${System.currentTimeMillis()}.db")
        runCatching {
            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
            }
            if (!isValidSqliteFile(tempFile)) {
                throw IllegalArgumentException("Selected file is not a valid SQLite 3 database (.db / .sqlite).")
            }
            // Validate and migrate schema on the temp copy first before replacing active db
            var testDb: SQLiteDatabase? = null
            try {
                testDb = SQLiteDatabase.openDatabase(
                    tempFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )
                configureJournalMode(testDb)
                ensureSchema(testDb)
            } finally {
                testDb?.close()
            }

            closeInternal()
            deleteDatabaseFiles(dbFile)
            tempFile.copyTo(dbFile, overwrite = true)

            val activeDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            configureJournalMode(activeDb)
            ensureSchema(activeDb)
            database = activeDb
        }.also {
            deleteDatabaseFiles(tempFile)
        }
    }

    fun exportToStream(outputStream: OutputStream): Result<Unit> = synchronized(lock) {
        runCatching {
            val bytes = readFlushedDatabaseBytesInternal()
            outputStream.write(bytes)
            outputStream.flush()
        }
    }

    /**
     * Flushes any pending SQLite buffers into the single `.db` file and returns an immutable
     * byte snapshot under lock so external SAF writes never block or corrupt the active connection.
     */
    fun exportBytesSnapshot(): Result<ByteArray> = synchronized(lock) {
        runCatching {
            readFlushedDatabaseBytesInternal()
        }
    }

    private fun readFlushedDatabaseBytesInternal(): ByteArray {
        val wasOpen = database?.isOpen == true
        if (wasOpen) {
            runCatching {
                database?.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)?.use { }
            }
            closeInternal()
        }
        return try {
            if (!dbFile.exists()) {
                throw IllegalStateException("No active database found to sync or export.")
            }
            dbFile.readBytes()
        } finally {
            if (wasOpen) {
                openOrCreateActive().getOrThrow()
            }
        }
    }

    fun closeDatabase() = synchronized(lock) {
        closeInternal()
    }

    fun <T> withDb(block: (SQLiteDatabase) -> T): T = synchronized(lock) {
        val db = openOrCreateActive().getOrThrow()
        block(db)
    }

    private fun closeInternal() {
        runCatching { database?.close() }
        database = null
    }

    private fun deleteDatabaseFiles(file: File) {
        if (file.exists()) file.delete()
        File("${file.absolutePath}-wal").takeIf { it.exists() }?.delete()
        File("${file.absolutePath}-shm").takeIf { it.exists() }?.delete()
        File("${file.absolutePath}-journal").takeIf { it.exists() }?.delete()
    }

    private fun configureJournalMode(db: SQLiteDatabase) {
        runCatching {
            db.rawQuery("PRAGMA journal_mode=DELETE;", null).use { }
            db.rawQuery("PRAGMA foreign_keys=OFF;", null).use { }
        }
    }

    private fun isValidSqliteFile(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        val header = ByteArray(16)
        FileInputStream(file).use { input ->
            if (input.read(header) < 16) return false
        }
        val expected = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        return header.contentEquals(expected)
    }

    /**
     * Exact DDL & column migrations matching web `lib/db.ts`.
     */
    private fun ensureSchema(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                nameKey TEXT NOT NULL UNIQUE COLLATE NOCASE,
                type TEXT NOT NULL CHECK(type IN ('income', 'expense')),
                color TEXT NOT NULL,
                isDeleted BOOLEAN NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            );
            """.trimIndent()
        )
        addColumnIfMissing(db, "categories", "isDeleted", "BOOLEAN NOT NULL DEFAULT 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                nameKey TEXT NOT NULL UNIQUE COLLATE NOCASE,
                openingBalance REAL NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                accountNumber TEXT NOT NULL DEFAULT '',
                allowNegativeBalance INTEGER NOT NULL DEFAULT 0,
                isClosed INTEGER NOT NULL DEFAULT 0,
                date TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            );
            """.trimIndent()
        )
        addColumnIfMissing(db, "accounts", "description", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "accounts", "accountNumber", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "accounts", "allowNegativeBalance", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "accounts", "isClosed", "INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY,
                type TEXT NOT NULL CHECK(type IN ('income', 'expense', 'transfer')),
                amount REAL NOT NULL,
                accountId TEXT NOT NULL,
                toAccountId TEXT NOT NULL DEFAULT '',
                categoryId TEXT NOT NULL,
                paymentMediumId TEXT NOT NULL DEFAULT '',
                reason TEXT NOT NULL,
                notes TEXT NOT NULL,
                date TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            );
            """.trimIndent()
        )
        addColumnIfMissing(db, "transactions", "toAccountId", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "transactions", "paymentMediumId", "TEXT NOT NULL DEFAULT ''")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS payment_mediums (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                grp TEXT NOT NULL CHECK(grp IN ('online', 'offline')),
                isDeleted BOOLEAN NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            );
            """.trimIndent()
        )
        addColumnIfMissing(db, "payment_mediums", "isDeleted", "BOOLEAN NOT NULL DEFAULT 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                name TEXT NOT NULL DEFAULT '',
                address TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                contact TEXT NOT NULL DEFAULT '',
                watermark TEXT NOT NULL DEFAULT '',
                currencyName TEXT NOT NULL DEFAULT '',
                currencySymbol TEXT NOT NULL DEFAULT ''
            );
            """.trimIndent()
        )
        addColumnIfMissing(db, "profile", "watermark", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "profile", "currencyName", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "profile", "currencySymbol", "TEXT NOT NULL DEFAULT ''")

        seedDefaultPaymentMediumsIfEmpty(db)
    }

    private fun addColumnIfMissing(
        db: SQLiteDatabase,
        table: String,
        column: String,
        definition: String
    ) {
        val existingColumns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($table);", null).use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIdx >= 0) existingColumns.add(cursor.getString(nameIdx))
            }
        }
        if (column !in existingColumns) {
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition;")
        }
    }

    private fun seedDefaultPaymentMediumsIfEmpty(db: SQLiteDatabase) {
        val count = db.rawQuery("SELECT COUNT(*) FROM payment_mediums;", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        if (count > 0) return

        val now = System.currentTimeMillis()
        val defaults = listOf(
            Triple("pm_upi", "upi", "online"),
            Triple("pm_netbanking", "net banking", "online"),
            Triple("pm_cc", "credit card", "online"),
            Triple("pm_dc", "debit card", "online"),
            Triple("pm_wallet", "wallet", "online"),
            Triple("pm_cash", "cash", "offline"),
            Triple("pm_cheque", "cheque", "offline"),
            Triple("pm_dd", "demand draft", "offline")
        )
        db.beginTransaction()
        try {
            defaults.forEachIndexed { idx, (id, name, grp) ->
                db.execSQL(
                    "INSERT OR IGNORE INTO payment_mediums (id, name, grp, isDeleted, createdAt) VALUES (?, ?, ?, 0, ?);",
                    arrayOf(id, name, grp, now + idx)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val ACTIVE_DB_FILE_NAME = "hisaab_kitaab_active.db"
    }
}
