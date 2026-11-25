package com.hisaabkitaab.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.abs

class HisaabRepository(private val dbHelper: HisaabDatabaseHelper) {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _paymentMediums = MutableStateFlow<List<PaymentMedium>>(emptyList())
    val paymentMediums: StateFlow<List<PaymentMedium>> = _paymentMediums.asStateFlow()

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _dbSizeBytes = MutableStateFlow(0L)
    val dbSizeBytes: StateFlow<Long> = _dbSizeBytes.asStateFlow()

    /**
     * Invoked on IO after any mutating SQLite operation commits and in-memory state refreshes.
     * Used by the ViewModel to flush the self-contained `.db` snapshot to the linked SAF URI.
     */
    var onDatabaseMutated: (suspend () -> Unit)? = null

    fun hasExistingDatabase(): Boolean = dbHelper.hasExistingDatabase()

    suspend fun openExistingDatabase(): UiResult<Unit> = withContext(Dispatchers.IO) {
        val result = dbHelper.openOrCreateActive()
        if (result.isSuccess) {
            refreshAllInternal()
            UiResult.Success(Unit, "Database connected")
        } else {
            UiResult.Error(result.exceptionOrNull()?.message ?: "Failed to open database")
        }
    }

    suspend fun createBlankDatabase(): UiResult<Unit> = withContext(Dispatchers.IO) {
        val result = dbHelper.createBlankDatabase()
        if (result.isSuccess) {
            refreshAllInternal()
            UiResult.Success(Unit, "Blank database created")
        } else {
            UiResult.Error(result.exceptionOrNull()?.message ?: "Failed to create database")
        }
    }

    suspend fun importDatabase(inputStream: InputStream): UiResult<Unit> = withContext(Dispatchers.IO) {
        val result = dbHelper.importFromStream(inputStream)
        if (result.isSuccess) {
            refreshAllInternal()
            UiResult.Success(Unit, "Database imported successfully")
        } else {
            UiResult.Error(result.exceptionOrNull()?.message ?: "Invalid SQLite database file")
        }
    }

    suspend fun exportDatabase(outputStream: OutputStream): UiResult<Unit> = withContext(Dispatchers.IO) {
        val result = dbHelper.exportToStream(outputStream)
        if (result.isSuccess) {
            _dbSizeBytes.value = dbHelper.getFileSizeBytes()
            UiResult.Success(Unit, "Database backup exported")
        } else {
            UiResult.Error(result.exceptionOrNull()?.message ?: "Failed to export database")
        }
    }

    suspend fun exportDatabaseBytes(): Result<ByteArray> = withContext(Dispatchers.IO) {
        val result = dbHelper.exportBytesSnapshot()
        if (result.isSuccess) {
            _dbSizeBytes.value = dbHelper.getFileSizeBytes()
        }
        result
    }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        refreshAllInternal()
    }

    private suspend fun finishMutation() {
        refreshAllInternal()
        onDatabaseMutated?.invoke()
    }

    private fun refreshAllInternal() {
        dbHelper.withDb { db ->
            _categories.value = queryCategories(db)
            _accounts.value = queryAccounts(db)
            _transactions.value = queryTransactions(db)
            _paymentMediums.value = queryPaymentMediums(db)
            _profile.value = queryProfile(db)
        }
        _dbSizeBytes.value = dbHelper.getFileSizeBytes()
    }

    // ==================== CATEGORIES ====================

    private fun queryCategories(db: SQLiteDatabase): List<Category> {
        val list = mutableListOf<Category>()
        db.rawQuery(
            "SELECT id, name, nameKey, type, color, isDeleted, createdAt FROM categories ORDER BY isDeleted ASC, createdAt DESC;",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Category(
                        id = c.getString(0),
                        name = c.getString(1),
                        nameKey = c.getString(2),
                        type = CategoryType.fromDb(c.getString(3)),
                        color = c.getString(4),
                        isDeleted = c.getInt(5) != 0,
                        createdAt = c.getLong(6)
                    )
                )
            }
        }
        return list
    }

    suspend fun addCategory(
        name: String,
        type: CategoryType,
        colorHex: String
    ): UiResult<Category> = withContext(Dispatchers.IO) {
        val cleanName = StringUtils.toLowerTrim(name)
        if (cleanName.isEmpty()) return@withContext UiResult.Error("Category name is required.")
        val safeColor = StringUtils.normalizeHexColor(colorHex)

        try {
            val created = dbHelper.withDb { db ->
                if (categoryExistsByNameKey(db, cleanName, excludeId = null)) {
                    throw IllegalArgumentException("A category with this name already exists.")
                }
                val cat = Category(
                    id = StringUtils.generateId("cat"),
                    name = cleanName,
                    nameKey = cleanName,
                    type = type,
                    color = safeColor,
                    isDeleted = false,
                    createdAt = System.currentTimeMillis()
                )
                val cv = ContentValues().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("nameKey", cat.nameKey)
                    put("type", cat.type.dbValue)
                    put("color", cat.color)
                    put("isDeleted", 0)
                    put("createdAt", cat.createdAt)
                }
                db.insertOrThrow("categories", null, cv)
                cat
            }
            finishMutation()
            UiResult.Success(created, "Category added")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Could not add category")
        }
    }

    suspend fun updateCategory(
        id: String,
        name: String,
        type: CategoryType,
        colorHex: String
    ): UiResult<Unit> = withContext(Dispatchers.IO) {
        val cleanName = StringUtils.toLowerTrim(name)
        if (cleanName.isEmpty()) return@withContext UiResult.Error("Category name is required.")
        val safeColor = StringUtils.normalizeHexColor(colorHex)

        try {
            dbHelper.withDb { db ->
                if (categoryExistsByNameKey(db, cleanName, excludeId = id)) {
                    throw IllegalArgumentException("Another category already uses this name.")
                }
                val cv = ContentValues().apply {
                    put("name", cleanName)
                    put("nameKey", cleanName)
                    put("type", type.dbValue)
                    put("color", safeColor)
                }
                db.update("categories", cv, "id = ?", arrayOf(id))
            }
            finishMutation()
            UiResult.Success(Unit, "Category updated")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Could not update category")
        }
    }

    suspend fun deleteCategories(ids: Collection<String>): UiResult<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext UiResult.Error("No categories selected.")
        try {
            var softDeleted = 0
            var hardDeleted = 0
            dbHelper.withDb { db ->
                db.beginTransaction()
                try {
                    ids.forEach { id ->
                        val refCount = countScalar(
                            db,
                            "SELECT COUNT(*) FROM transactions WHERE categoryId = ?;",
                            arrayOf(id)
                        )
                        if (refCount > 0) {
                            db.execSQL("UPDATE categories SET isDeleted = 1 WHERE id = ?;", arrayOf(id))
                            softDeleted++
                        } else {
                            db.delete("categories", "id = ?", arrayOf(id))
                            hardDeleted++
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            finishMutation()
            val msg = buildString {
                if (hardDeleted > 0) append("$hardDeleted removed")
                if (softDeleted > 0) {
                    if (isNotEmpty()) append(", ")
                    append("$softDeleted archived (used in transactions)")
                }
            }
            UiResult.Success(hardDeleted to softDeleted, msg.ifEmpty { "Categories updated" })
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Failed to delete categories")
        }
    }

    suspend fun bulkImportCategories(rows: List<CategoryCsvRow>): UiResult<Int> = withContext(Dispatchers.IO) {
        val valid = rows.filter { it.status == CategoryCsvStatus.VALID && it.type != null }
        if (valid.isEmpty()) return@withContext UiResult.Error("No valid rows to import.")
        try {
            var inserted = 0
            dbHelper.withDb { db ->
                db.beginTransaction()
                try {
                    val baseTime = System.currentTimeMillis()
                    valid.forEachIndexed { idx, row ->
                        val key = row.normalizedName
                        if (!categoryExistsByNameKey(db, key, excludeId = null)) {
                            val cv = ContentValues().apply {
                                put("id", StringUtils.generateId("cat"))
                                put("name", key)
                                put("nameKey", key)
                                put("type", row.type!!.dbValue)
                                put("color", StringUtils.normalizeHexColor(row.colorHex))
                                put("isDeleted", 0)
                                put("createdAt", baseTime + idx)
                            }
                            db.insertOrThrow("categories", null, cv)
                            inserted++
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            finishMutation()
            UiResult.Success(inserted, "Imported $inserted categories")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Bulk category import failed")
        }
    }

    private fun categoryExistsByNameKey(db: SQLiteDatabase, nameKey: String, excludeId: String?): Boolean {
        return if (excludeId == null) {
            countScalar(
                db,
                "SELECT COUNT(*) FROM categories WHERE LOWER(nameKey) = LOWER(?);",
                arrayOf(nameKey)
            ) > 0
        } else {
            countScalar(
                db,
                "SELECT COUNT(*) FROM categories WHERE LOWER(nameKey) = LOWER(?) AND id != ?;",
                arrayOf(nameKey, excludeId)
            ) > 0
        }
    }

    // ==================== ACCOUNTS ====================

    private fun queryAccounts(db: SQLiteDatabase): List<Account> {
        val list = mutableListOf<Account>()
        val sql = """
            SELECT
                a.id,
                a.name,
                a.nameKey,
                a.openingBalance,
                a.description,
                a.accountNumber,
                a.allowNegativeBalance,
                a.isClosed,
                a.date,
                a.createdAt,
                a.openingBalance
                  + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'income'), 0)
                  - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'expense'), 0)
                  - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.accountId = a.id AND t.type = 'transfer'), 0)
                  + COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.toAccountId = a.id AND t.type = 'transfer'), 0)
                  AS currentBalance
            FROM accounts a
            ORDER BY a.isClosed ASC, a.createdAt DESC;
        """.trimIndent()
        db.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Account(
                        id = c.getString(0),
                        name = c.getString(1),
                        nameKey = c.getString(2),
                        openingBalance = c.getDouble(3),
                        description = c.getString(4) ?: "",
                        accountNumber = c.getString(5) ?: "",
                        allowNegativeBalance = c.getInt(6) != 0,
                        isClosed = c.getInt(7) != 0,
                        date = c.getString(8),
                        createdAt = c.getLong(9),
                        currentBalance = c.getDouble(10)
                    )
                )
            }
        }
        return list
    }

    suspend fun addAccount(
        name: String,
        openingBalance: Double,
        description: String,
        accountNumber: String,
        allowNegativeBalance: Boolean,
        date: String
    ): UiResult<Account> = withContext(Dispatchers.IO) {
        val cleanName = StringUtils.toLowerTrim(name)
        if (cleanName.isEmpty()) return@withContext UiResult.Error("Account name is required.")
        if (!allowNegativeBalance && openingBalance < 0.0) {
            return@withContext UiResult.Error("Opening balance cannot be negative unless negative balance is allowed.")
        }
        val cleanNum = accountNumber.filter { it.isDigit() }.takeLast(6)
        val cleanDesc = StringUtils.toLowerTrim(description)
        val cleanDate = date.ifBlank { StringUtils.todayIso() }

        try {
            val acc = dbHelper.withDb { db ->
                if (accountExistsByNameKey(db, cleanName, excludeId = null)) {
                    throw IllegalArgumentException("An account with this name already exists.")
                }
                val id = StringUtils.generateId("acc")
                val now = System.currentTimeMillis()
                val cv = ContentValues().apply {
                    put("id", id)
                    put("name", cleanName)
                    put("nameKey", cleanName)
                    put("openingBalance", openingBalance)
                    put("description", cleanDesc)
                    put("accountNumber", cleanNum)
                    put("allowNegativeBalance", if (allowNegativeBalance) 1 else 0)
                    put("isClosed", 0)
                    put("date", cleanDate)
                    put("createdAt", now)
                }
                db.insertOrThrow("accounts", null, cv)
                Account(
                    id = id,
                    name = cleanName,
                    nameKey = cleanName,
                    openingBalance = openingBalance,
                    currentBalance = openingBalance,
                    description = cleanDesc,
                    accountNumber = cleanNum,
                    allowNegativeBalance = allowNegativeBalance,
                    isClosed = false,
                    date = cleanDate,
                    createdAt = now
                )
            }
            finishMutation()
            UiResult.Success(acc, "Account created")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Failed to create account")
        }
    }

    suspend fun updateAccount(
        id: String,
        name: String,
        openingBalance: Double,
        description: String,
        accountNumber: String,
        allowNegativeBalance: Boolean,
        date: String
    ): UiResult<Unit> = withContext(Dispatchers.IO) {
        val cleanName = StringUtils.toLowerTrim(name)
        if (cleanName.isEmpty()) return@withContext UiResult.Error("Account name is required.")
        val cleanNum = accountNumber.filter { it.isDigit() }.takeLast(6)
        val cleanDesc = StringUtils.toLowerTrim(description)

        try {
            dbHelper.withDb { db ->
                val existing = queryAccounts(db).firstOrNull { it.id == id }
                    ?: throw IllegalArgumentException("Account not found.")
                if (existing.isClosed) {
                    throw IllegalArgumentException("Closed accounts cannot be edited.")
                }
                if (accountExistsByNameKey(db, cleanName, excludeId = id)) {
                    throw IllegalArgumentException("Another account already uses this name.")
                }
                val projectedBalance = (existing.currentBalance - existing.openingBalance) + openingBalance
                if (!allowNegativeBalance && projectedBalance < -0.001) {
                    throw IllegalArgumentException(
                        "Cannot update: projected balance (${StringUtils.formatIndianNumber(projectedBalance)}) would be negative."
                    )
                }
                val cv = ContentValues().apply {
                    put("name", cleanName)
                    put("nameKey", cleanName)
                    put("openingBalance", openingBalance)
                    put("description", cleanDesc)
                    put("accountNumber", cleanNum)
                    put("allowNegativeBalance", if (allowNegativeBalance) 1 else 0)
                    put("date", date.ifBlank { existing.date })
                }
                db.update("accounts", cv, "id = ?", arrayOf(id))
            }
            finishMutation()
            UiResult.Success(Unit, "Account updated")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Failed to update account")
        }
    }

    suspend fun closeOrDeleteAccount(id: String): UiResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val wasSoftClosed = dbHelper.withDb { db ->
                val acc = queryAccounts(db).firstOrNull { it.id == id }
                    ?: throw IllegalArgumentException("Account not found.")
                if (abs(acc.currentBalance) > 0.01) {
                    throw IllegalArgumentException(
                        "Account balance must be 0.00 before closing. Current balance: ${StringUtils.formatIndianNumber(acc.currentBalance)}."
                    )
                }
                val linkedCount = countScalar(
                    db,
                    "SELECT COUNT(*) FROM transactions WHERE accountId = ? OR toAccountId = ?;",
                    arrayOf(id, id)
                )
                if (linkedCount > 0) {
                    db.execSQL("UPDATE accounts SET isClosed = 1 WHERE id = ?;", arrayOf(id))
                    true
                } else {
                    db.delete("accounts", "id = ?", arrayOf(id))
                    false
                }
            }
            finishMutation()
            val msg = if (wasSoftClosed) {
                "Account closed and preserved for ledger history"
            } else {
                "Account permanently deleted"
            }
            UiResult.Success(wasSoftClosed, msg)
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Could not close account")
        }
    }

    private fun accountExistsByNameKey(db: SQLiteDatabase, nameKey: String, excludeId: String?): Boolean {
        return if (excludeId == null) {
            countScalar(
                db,
                "SELECT COUNT(*) FROM accounts WHERE LOWER(nameKey) = LOWER(?);",
                arrayOf(nameKey)
            ) > 0
        } else {
            countScalar(
                db,
                "SELECT COUNT(*) FROM accounts WHERE LOWER(nameKey) = LOWER(?) AND id != ?;",
                arrayOf(nameKey, excludeId)
            ) > 0
        }
    }

    // ==================== PAYMENT MEDIUMS ====================

    private fun queryPaymentMediums(db: SQLiteDatabase): List<PaymentMedium> {
        val list = mutableListOf<PaymentMedium>()
        db.rawQuery(
            "SELECT id, name, grp, isDeleted, createdAt FROM payment_mediums ORDER BY isDeleted ASC, grp DESC, createdAt ASC;",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    PaymentMedium(
                        id = c.getString(0),
                        name = c.getString(1),
                        group = PaymentGroup.fromDb(c.getString(2)),
                        isDeleted = c.getInt(3) != 0,
                        createdAt = c.getLong(4)
                    )
                )
            }
        }
        return list
    }

    suspend fun addPaymentMedium(name: String, group: PaymentGroup): UiResult<PaymentMedium> =
        withContext(Dispatchers.IO) {
            val cleanName = StringUtils.toLowerTrim(name)
            if (cleanName.isEmpty()) return@withContext UiResult.Error("Payment medium name is required.")
            try {
                val created = dbHelper.withDb { db ->
                    val dup = countScalar(
                        db,
                        "SELECT COUNT(*) FROM payment_mediums WHERE LOWER(name) = LOWER(?) AND isDeleted = 0;",
                        arrayOf(cleanName)
                    )
                    if (dup > 0) throw IllegalArgumentException("Payment medium already exists.")
                    val pm = PaymentMedium(
                        id = StringUtils.generateId("pm"),
                        name = cleanName,
                        group = group,
                        isDeleted = false,
                        createdAt = System.currentTimeMillis()
                    )
                    val cv = ContentValues().apply {
                        put("id", pm.id)
                        put("name", pm.name)
                        put("grp", pm.group.dbValue)
                        put("isDeleted", 0)
                        put("createdAt", pm.createdAt)
                    }
                    db.insertOrThrow("payment_mediums", null, cv)
                    pm
                }
                finishMutation()
                UiResult.Success(created, "Payment medium added")
            } catch (e: Exception) {
                UiResult.Error(e.message ?: "Failed to add payment medium")
            }
        }

    suspend fun updatePaymentMedium(id: String, name: String, group: PaymentGroup): UiResult<Unit> =
        withContext(Dispatchers.IO) {
            val cleanName = StringUtils.toLowerTrim(name)
            if (cleanName.isEmpty()) return@withContext UiResult.Error("Payment medium name is required.")
            try {
                dbHelper.withDb { db ->
                    val dup = countScalar(
                        db,
                        "SELECT COUNT(*) FROM payment_mediums WHERE LOWER(name) = LOWER(?) AND id != ? AND isDeleted = 0;",
                        arrayOf(cleanName, id)
                    )
                    if (dup > 0) throw IllegalArgumentException("Another payment medium uses this name.")
                    val cv = ContentValues().apply {
                        put("name", cleanName)
                        put("grp", group.dbValue)
                    }
                    db.update("payment_mediums", cv, "id = ?", arrayOf(id))
                }
                finishMutation()
                UiResult.Success(Unit, "Payment medium updated")
            } catch (e: Exception) {
                UiResult.Error(e.message ?: "Failed to update payment medium")
            }
        }

    suspend fun deletePaymentMediums(ids: Collection<String>): UiResult<Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            if (ids.isEmpty()) return@withContext UiResult.Error("No payment mediums selected.")
            try {
                var softDeleted = 0
                var hardDeleted = 0
                dbHelper.withDb { db ->
                    db.beginTransaction()
                    try {
                        ids.forEach { id ->
                            val refCount = countScalar(
                                db,
                                "SELECT COUNT(*) FROM transactions WHERE paymentMediumId = ?;",
                                arrayOf(id)
                            )
                            if (refCount > 0) {
                                db.execSQL(
                                    "UPDATE payment_mediums SET isDeleted = 1 WHERE id = ?;",
                                    arrayOf(id)
                                )
                                softDeleted++
                            } else {
                                db.delete("payment_mediums", "id = ?", arrayOf(id))
                                hardDeleted++
                            }
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
                finishMutation()
                val msg = buildString {
                    if (hardDeleted > 0) append("$hardDeleted removed")
                    if (softDeleted > 0) {
                        if (isNotEmpty()) append(", ")
                        append("$softDeleted archived (used in transactions)")
                    }
                }
                UiResult.Success(hardDeleted to softDeleted, msg.ifEmpty { "Updated" })
            } catch (e: Exception) {
                UiResult.Error(e.message ?: "Failed to delete payment mediums")
            }
        }

    // ==================== TRANSACTIONS ====================

    private fun queryTransactions(db: SQLiteDatabase): List<Transaction> {
        val list = mutableListOf<Transaction>()
        db.rawQuery(
            """
            SELECT id, type, amount, accountId, toAccountId, categoryId, paymentMediumId, reason, notes, date, createdAt
            FROM transactions
            ORDER BY date DESC, createdAt DESC;
            """.trimIndent(),
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Transaction(
                        id = c.getString(0),
                        type = TransactionType.fromDb(c.getString(1)),
                        amount = c.getDouble(2),
                        accountId = c.getString(3),
                        toAccountId = c.getString(4) ?: "",
                        categoryId = c.getString(5) ?: "",
                        paymentMediumId = c.getString(6) ?: "",
                        reason = c.getString(7) ?: "",
                        notes = c.getString(8) ?: "",
                        date = c.getString(9),
                        createdAt = c.getLong(10)
                    )
                )
            }
        }
        return list
    }

    suspend fun addTransaction(
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String,
        categoryId: String,
        paymentMediumId: String,
        reason: String,
        notes: String,
        date: String
    ): UiResult<Transaction> = withContext(Dispatchers.IO) {
        if (amount <= 0.0) return@withContext UiResult.Error("Amount must be greater than 0.")
        val cleanReason = reason.trim().take(50)
        val cleanNotes = notes.trim().take(1000)
        val cleanDate = date.ifBlank { StringUtils.todayIso() }
        if (cleanReason.isEmpty()) return@withContext UiResult.Error("Reason is required.")

        try {
            val created = dbHelper.withDb { db ->
                val allAccounts = queryAccounts(db).associateBy { it.id }
                val srcAcc = allAccounts[accountId]
                    ?: throw IllegalArgumentException("Please select a valid source account.")
                if (srcAcc.isClosed) throw IllegalArgumentException("Cannot post transactions to a closed account.")

                val finalToAccountId = if (type == TransactionType.TRANSFER) {
                    val dstAcc = allAccounts[toAccountId]
                        ?: throw IllegalArgumentException("Please select a valid destination account.")
                    if (dstAcc.isClosed) throw IllegalArgumentException("Destination account is closed.")
                    if (dstAcc.id == srcAcc.id) throw IllegalArgumentException("Source and destination accounts must differ.")
                    dstAcc.id
                } else ""

                val finalCategoryId = if (type == TransactionType.TRANSFER) {
                    ""
                } else {
                    if (categoryId.isBlank()) throw IllegalArgumentException("Please select a category.")
                    categoryId
                }

                if ((type == TransactionType.EXPENSE || type == TransactionType.TRANSFER) &&
                    !srcAcc.allowNegativeBalance &&
                    (srcAcc.currentBalance - amount) < -0.001
                ) {
                    throw IllegalArgumentException(
                        "Insufficient balance in ${StringUtils.displayName(srcAcc.name)}. Available: ${
                            StringUtils.formatIndianNumber(srcAcc.currentBalance)
                        }."
                    )
                }

                val txn = Transaction(
                    id = StringUtils.generateId("txn"),
                    type = type,
                    amount = amount,
                    accountId = srcAcc.id,
                    toAccountId = finalToAccountId,
                    categoryId = finalCategoryId,
                    paymentMediumId = paymentMediumId,
                    reason = cleanReason,
                    notes = cleanNotes,
                    date = cleanDate,
                    createdAt = System.currentTimeMillis()
                )
                val cv = ContentValues().apply {
                    put("id", txn.id)
                    put("type", txn.type.dbValue)
                    put("amount", txn.amount)
                    put("accountId", txn.accountId)
                    put("toAccountId", txn.toAccountId)
                    put("categoryId", txn.categoryId)
                    put("paymentMediumId", txn.paymentMediumId)
                    put("reason", txn.reason)
                    put("notes", txn.notes)
                    put("date", txn.date)
                    put("createdAt", txn.createdAt)
                }
                db.insertOrThrow("transactions", null, cv)
                txn
            }
            finishMutation()
            UiResult.Success(created, "Transaction recorded")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Failed to add transaction")
        }
    }

    suspend fun updateTransaction(
        id: String,
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String,
        categoryId: String,
        paymentMediumId: String,
        reason: String,
        notes: String,
        date: String
    ): UiResult<Unit> = withContext(Dispatchers.IO) {
        if (amount <= 0.0) return@withContext UiResult.Error("Amount must be greater than 0.")
        val cleanReason = reason.trim().take(50)
        val cleanNotes = notes.trim().take(1000)
        val cleanDate = date.ifBlank { StringUtils.todayIso() }
        if (cleanReason.isEmpty()) return@withContext UiResult.Error("Reason is required.")

        try {
            dbHelper.withDb { db ->
                val oldTxn = queryTransactions(db).firstOrNull { it.id == id }
                    ?: throw IllegalArgumentException("Transaction not found.")
                val accountsMap = queryAccounts(db).associateBy { it.id }

                val oldSrc = accountsMap[oldTxn.accountId]
                val oldDst = if (oldTxn.toAccountId.isNotBlank()) accountsMap[oldTxn.toAccountId] else null
                if (oldSrc?.isClosed == true || oldDst?.isClosed == true) {
                    throw IllegalArgumentException("Transactions involving a closed account cannot be edited.")
                }

                val newSrc = accountsMap[accountId]
                    ?: throw IllegalArgumentException("Select a valid source account.")
                if (newSrc.isClosed) throw IllegalArgumentException("Selected account is closed.")

                val finalToAccountId = if (type == TransactionType.TRANSFER) {
                    val newDst = accountsMap[toAccountId]
                        ?: throw IllegalArgumentException("Select a valid destination account.")
                    if (newDst.isClosed) throw IllegalArgumentException("Destination account is closed.")
                    if (newDst.id == newSrc.id) throw IllegalArgumentException("Source and destination accounts must differ.")
                    newDst.id
                } else ""

                val finalCategoryId = if (type == TransactionType.TRANSFER) {
                    ""
                } else {
                    if (categoryId.isBlank()) throw IllegalArgumentException("Please select a category.")
                    categoryId
                }

                // Compute virtual balance after reversing the old transaction's effect
                val virtualBalances = accountsMap.mapValues { it.value.currentBalance }.toMutableMap()
                when (oldTxn.type) {
                    TransactionType.INCOME -> {
                        virtualBalances[oldTxn.accountId] =
                            (virtualBalances[oldTxn.accountId] ?: 0.0) - oldTxn.amount
                    }
                    TransactionType.EXPENSE -> {
                        virtualBalances[oldTxn.accountId] =
                            (virtualBalances[oldTxn.accountId] ?: 0.0) + oldTxn.amount
                    }
                    TransactionType.TRANSFER -> {
                        virtualBalances[oldTxn.accountId] =
                            (virtualBalances[oldTxn.accountId] ?: 0.0) + oldTxn.amount
                        if (oldTxn.toAccountId.isNotBlank()) {
                            virtualBalances[oldTxn.toAccountId] =
                                (virtualBalances[oldTxn.toAccountId] ?: 0.0) - oldTxn.amount
                        }
                    }
                }

                // Apply new effect to virtual balances
                when (type) {
                    TransactionType.INCOME -> {
                        virtualBalances[newSrc.id] = (virtualBalances[newSrc.id] ?: 0.0) + amount
                    }
                    TransactionType.EXPENSE -> {
                        virtualBalances[newSrc.id] = (virtualBalances[newSrc.id] ?: 0.0) - amount
                    }
                    TransactionType.TRANSFER -> {
                        virtualBalances[newSrc.id] = (virtualBalances[newSrc.id] ?: 0.0) - amount
                        virtualBalances[finalToAccountId] =
                            (virtualBalances[finalToAccountId] ?: 0.0) + amount
                    }
                }

                // Guard negative balance on all affected accounts
                val affectedIds = setOf(oldTxn.accountId, oldTxn.toAccountId, newSrc.id, finalToAccountId)
                    .filter { it.isNotBlank() }
                for (accId in affectedIds) {
                    val acc = accountsMap[accId] ?: continue
                    val projected = virtualBalances[accId] ?: acc.currentBalance
                    if (!acc.allowNegativeBalance && projected < -0.001) {
                        throw IllegalArgumentException(
                            "Insufficient balance in ${StringUtils.displayName(acc.name)} after edit."
                        )
                    }
                }

                val cv = ContentValues().apply {
                    put("type", type.dbValue)
                    put("amount", amount)
                    put("accountId", newSrc.id)
                    put("toAccountId", finalToAccountId)
                    put("categoryId", finalCategoryId)
                    put("paymentMediumId", paymentMediumId)
                    put("reason", cleanReason)
                    put("notes", cleanNotes)
                    put("date", cleanDate)
                }
                db.update("transactions", cv, "id = ?", arrayOf(id))
            }
            finishMutation()
            UiResult.Success(Unit, "Transaction updated")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Failed to update transaction")
        }
    }

    suspend fun deleteTransaction(
        id: String,
        settlementAccountId: String? = null
    ): UiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            dbHelper.withDb { db ->
                val txn = queryTransactions(db).firstOrNull { it.id == id }
                    ?: throw IllegalArgumentException("Transaction not found.")
                val accountsMap = queryAccounts(db).associateBy { it.id }
                val srcAcc = accountsMap[txn.accountId]
                val dstAcc = if (txn.toAccountId.isNotBlank()) accountsMap[txn.toAccountId] else null

                if (txn.type == TransactionType.TRANSFER &&
                    (srcAcc?.isClosed == true || dstAcc?.isClosed == true)
                ) {
                    throw IllegalArgumentException(
                        "Cannot directly delete a transfer involving a closed account."
                    )
                }

                db.beginTransaction()
                try {
                    if (srcAcc?.isClosed == true) {
                        val settleId = settlementAccountId?.takeIf { it.isNotBlank() }
                            ?: throw IllegalArgumentException(
                                "Account is closed. Select an active Settlement Account to balance this deletion."
                            )
                        val settleAcc = accountsMap[settleId]
                            ?: throw IllegalArgumentException("Invalid settlement account.")
                        if (settleAcc.isClosed) {
                            throw IllegalArgumentException("Settlement account must be active.")
                        }

                        db.delete("transactions", "id = ?", arrayOf(id))

                        // Insert reversing settlement transfer so the closed account remains at 0.00
                        val fromId = if (txn.type == TransactionType.EXPENSE) srcAcc.id else settleAcc.id
                        val toId = if (txn.type == TransactionType.EXPENSE) settleAcc.id else srcAcc.id
                        val cv = ContentValues().apply {
                            put("id", StringUtils.generateId("txn"))
                            put("type", TransactionType.TRANSFER.dbValue)
                            put("amount", txn.amount)
                            put("accountId", fromId)
                            put("toAccountId", toId)
                            put("categoryId", "")
                            put("paymentMediumId", txn.paymentMediumId)
                            put("reason", "Settlement for deleted transaction")
                            put("notes", "Auto-settlement for deleted ${txn.type.label}: ${txn.reason}")
                            put("date", StringUtils.todayIso())
                            put("createdAt", System.currentTimeMillis())
                        }
                        db.insertOrThrow("transactions", null, cv)
                    } else {
                        db.delete("transactions", "id = ?", arrayOf(id))
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            finishMutation()
            UiResult.Success(Unit, "Transaction deleted")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Failed to delete transaction")
        }
    }

    suspend fun bulkImportStatementRows(rows: List<StatementImportRow>): UiResult<Int> =
        withContext(Dispatchers.IO) {
            val valid = rows.filter { it.status == StatementImportStatus.VALID }
            if (valid.isEmpty()) return@withContext UiResult.Error("No valid rows to import.")
            try {
                var inserted = 0
                dbHelper.withDb { db ->
                    db.beginTransaction()
                    try {
                        val baseNow = System.currentTimeMillis()
                        valid.forEachIndexed { idx, row ->
                            val cv = ContentValues().apply {
                                put("id", StringUtils.generateId("txn"))
                                put("type", row.type.dbValue)
                                put("amount", row.amount)
                                put("accountId", row.resolvedAccountId)
                                put("toAccountId", row.resolvedToAccountId)
                                put("categoryId", row.resolvedCategoryId)
                                put("paymentMediumId", row.resolvedPaymentMediumId)
                                put("reason", row.reason.trim().take(50).ifEmpty { "Imported entry" })
                                put("notes", row.notes.trim().take(1000))
                                put("date", row.date)
                                put("createdAt", baseNow + idx)
                            }
                            db.insertOrThrow("transactions", null, cv)
                            inserted++
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
                finishMutation()
                UiResult.Success(inserted, "Imported $inserted transactions")
            } catch (e: Exception) {
                UiResult.Error(e.message ?: "Statement import failed")
            }
        }

    // ==================== PROFILE ====================

    private fun queryProfile(db: SQLiteDatabase): UserProfile {
        db.rawQuery(
            "SELECT name, address, email, contact, watermark, currencyName, currencySymbol FROM profile WHERE id = 1;",
            null
        ).use { c ->
            if (c.moveToFirst()) {
                return UserProfile(
                    name = c.getString(0) ?: "",
                    address = c.getString(1) ?: "",
                    email = c.getString(2) ?: "",
                    contact = c.getString(3) ?: "",
                    watermark = c.getString(4) ?: "",
                    currencyName = (c.getString(5) ?: "").ifBlank { "INR" },
                    currencySymbol = (c.getString(6) ?: "").ifBlank { "₹" }
                )
            }
        }
        return UserProfile()
    }

    suspend fun saveProfile(profile: UserProfile): UiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            dbHelper.withDb { db ->
                val cv = ContentValues().apply {
                    put("id", 1)
                    put("name", profile.name.trim())
                    put("address", profile.address.trim())
                    put("email", profile.email.trim())
                    put("contact", profile.contact.trim())
                    put("watermark", profile.watermark.trim())
                    put("currencyName", profile.effectiveCurrencyName.trim())
                    put("currencySymbol", profile.effectiveSymbol.trim())
                }
                db.insertWithOnConflict("profile", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            finishMutation()
            UiResult.Success(Unit, "Profile saved")
        } catch (e: Exception) {
            UiResult.Error(e.message ?: "Could not save profile")
        }
    }

    private fun countScalar(db: SQLiteDatabase, sql: String, args: Array<String>): Int {
        return db.rawQuery(sql, args).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
