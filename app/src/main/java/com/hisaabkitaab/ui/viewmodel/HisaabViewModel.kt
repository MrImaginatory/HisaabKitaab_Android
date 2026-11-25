package com.hisaabkitaab.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.AnalyticsSlice
import com.hisaabkitaab.data.AppFontFamily
import com.hisaabkitaab.data.Category
import com.hisaabkitaab.data.CategoryCsvPreview
import com.hisaabkitaab.data.CategoryType
import com.hisaabkitaab.data.ChartPalette
import com.hisaabkitaab.data.CustomFontManager
import com.hisaabkitaab.data.DailyFlowPoint
import com.hisaabkitaab.data.DateRange
import com.hisaabkitaab.data.DbLinkStatus
import com.hisaabkitaab.data.ExternalDbSyncManager
import com.hisaabkitaab.data.FontScaleOption
import com.hisaabkitaab.data.HisaabDatabaseHelper
import com.hisaabkitaab.data.HisaabRepository
import com.hisaabkitaab.data.ImportFontResult
import com.hisaabkitaab.data.PaymentGroup
import com.hisaabkitaab.data.PaymentMedium
import com.hisaabkitaab.data.PeriodPreset
import com.hisaabkitaab.data.StatementImportPreview
import com.hisaabkitaab.data.StatementReport
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.SurfaceStyle
import com.hisaabkitaab.data.ThemePreferences
import com.hisaabkitaab.data.ThemePreferencesRepository
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.TransactionFormDefaults
import com.hisaabkitaab.data.TransactionType
import com.hisaabkitaab.data.UiResult
import com.hisaabkitaab.data.UpiReceiptDraft
import com.hisaabkitaab.data.UserProfile
import com.hisaabkitaab.export.CategoryCsvEngine
import com.hisaabkitaab.export.StatementCalculator
import com.hisaabkitaab.export.StatementPdfEngine
import com.hisaabkitaab.export.StatementSpreadsheetEngine
import com.hisaabkitaab.ocr.UpiReceiptParser
import com.hisaabkitaab.ocr.UpiReceiptScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HisaabViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = HisaabDatabaseHelper(application)
    private val repository = HisaabRepository(dbHelper)
    private val prefsRepo = ThemePreferencesRepository(application)
    private val externalSync = ExternalDbSyncManager(application)

    val categories: StateFlow<List<Category>> = repository.categories
    val accounts: StateFlow<List<Account>> = repository.accounts
    val transactions: StateFlow<List<Transaction>> = repository.transactions
    val paymentMediums: StateFlow<List<PaymentMedium>> = repository.paymentMediums
    val profile: StateFlow<UserProfile> = repository.profile
    val dbSizeBytes: StateFlow<Long> = repository.dbSizeBytes

    val themePreferences: StateFlow<ThemePreferences> = prefsRepo.themePreferences.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ThemePreferences()
    )

    val txnFormDefaults: StateFlow<TransactionFormDefaults> = prefsRepo.txnFormDefaults.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TransactionFormDefaults()
    )

    private val _isDbReady = MutableStateFlow(false)
    val isDbReady: StateFlow<Boolean> = _isDbReady.asStateFlow()

    private val _hasSavedLocalDb = MutableStateFlow(repository.hasExistingDatabase())
    val hasSavedLocalDb: StateFlow<Boolean> = _hasSavedLocalDb.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _categoryCsvPreview = MutableStateFlow<CategoryCsvPreview?>(null)
    val categoryCsvPreview: StateFlow<CategoryCsvPreview?> = _categoryCsvPreview.asStateFlow()

    private val _statementImportPreview = MutableStateFlow<StatementImportPreview?>(null)
    val statementImportPreview: StateFlow<StatementImportPreview?> = _statementImportPreview.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isStartupChecking = MutableStateFlow(true)
    val isStartupChecking: StateFlow<Boolean> = _isStartupChecking.asStateFlow()

    private val _pendingReceiptDraft = MutableStateFlow<UpiReceiptDraft?>(null)
    val pendingReceiptDraft: StateFlow<UpiReceiptDraft?> = _pendingReceiptDraft.asStateFlow()

    private val _isScanningReceipt = MutableStateFlow(false)
    val isScanningReceipt: StateFlow<Boolean> = _isScanningReceipt.asStateFlow()

    private val _dbLinkStatus = MutableStateFlow<DbLinkStatus>(DbLinkStatus.LocalOnly("hisaab_kitaab.db"))
    val dbLinkStatus: StateFlow<DbLinkStatus> = _dbLinkStatus.asStateFlow()

    private val _isSyncingDb = MutableStateFlow(false)
    val isSyncingDb: StateFlow<Boolean> = _isSyncingDb.asStateFlow()

    private var queuedReceiptFile: File? = null
    private var queuedReceiptText: String? = null

    init {
        repository.onDatabaseMutated = {
            syncActiveDbToLinkedUri(silentOnSuccess = true)
        }
        autoConnectSavedDatabase()
    }

    private fun autoConnectSavedDatabase() {
        viewModelScope.launch {
            try {
                val prefs = prefsRepo.themePreferences.first()
                val exists = repository.hasExistingDatabase()
                val hasLinked = prefs.linkedDbUri.isNotBlank()
                _hasSavedLocalDb.value = exists || hasLinked
                if (prefs.hasActiveDb && (exists || hasLinked)) {
                    connectFromPreferences(prefs, isManualReconnect = false)
                }
            } finally {
                _isStartupChecking.value = false
            }
        }
    }

    private suspend fun connectFromPreferences(prefs: ThemePreferences, isManualReconnect: Boolean) {
        val context = getApplication<Application>()
        val linkedUriStr = prefs.linkedDbUri.trim()

        if (linkedUriStr.isNotBlank()) {
            val uri = Uri.parse(linkedUriStr)
            val reloadResult = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        repository.importDatabase(stream)
                    } ?: UiResult.Error("Linked .db file could not be opened.")
                }.getOrElse { err ->
                    UiResult.Error(friendlySyncError(err))
                }
            }

            when (reloadResult) {
                is UiResult.Success -> {
                    val displayName = externalSync.resolveDisplayName(uri) ?: prefs.lastDbName
                    prefsRepo.setLinkedDbInfo(displayName, linkedUriStr, active = true)
                    _hasSavedLocalDb.value = true
                    _isDbReady.value = true
                    _dbLinkStatus.value = DbLinkStatus.LiveLinked(displayName, System.currentTimeMillis())
                    if (isManualReconnect) {
                        emitMessage("Live-synced with $displayName")
                    }
                    flushQueuedReceiptIfReady()
                    return
                }
                is UiResult.Error -> {
                    val reason = reloadResult.message
                    if (repository.hasExistingDatabase()) {
                        when (val localRes = repository.openExistingDatabase()) {
                            is UiResult.Success -> {
                                prefsRepo.setActiveDbInfo(prefs.lastDbName, true)
                                _hasSavedLocalDb.value = true
                                _isDbReady.value = true
                                _dbLinkStatus.value = DbLinkStatus.NeedsRelink(prefs.lastDbName, reason)
                                emitMessage("Opened local copy of ${prefs.lastDbName} — tap Relink to reconnect your .db file.")
                                flushQueuedReceiptIfReady()
                            }
                            is UiResult.Error -> emitMessage(localRes.message)
                        }
                    } else {
                        _dbLinkStatus.value = DbLinkStatus.NeedsRelink(prefs.lastDbName, reason)
                        emitMessage(reason)
                    }
                    return
                }
            }
        }

        // Legacy or unlinked local-only database
        when (val res = repository.openExistingDatabase()) {
            is UiResult.Success -> {
                if (isManualReconnect) {
                    prefsRepo.setActiveDbInfo(prefs.lastDbName, true)
                }
                _hasSavedLocalDb.value = true
                _isDbReady.value = true
                _dbLinkStatus.value = DbLinkStatus.LocalOnly(prefs.lastDbName)
                if (isManualReconnect) {
                    emitMessage("Connected to ${prefs.lastDbName} (local only)")
                }
                flushQueuedReceiptIfReady()
            }
            is UiResult.Error -> emitMessage(res.message)
        }
    }

    // ==================== DATABASE LIFECYCLE ====================

    fun reconnectLastDatabase() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val prefs = prefsRepo.themePreferences.first()
                connectFromPreferences(prefs, isManualReconnect = true)
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun createBlankDatabase(dbName: String = "hisaab_kitaab.db") {
        viewModelScope.launch {
            _isBusy.value = true
            when (val res = repository.createBlankDatabase()) {
                is UiResult.Success -> {
                    prefsRepo.setLinkedDbInfo(dbName, linkedUri = null, active = true)
                    _hasSavedLocalDb.value = true
                    _isDbReady.value = true
                    _dbLinkStatus.value = DbLinkStatus.LocalOnly(dbName)
                    emitMessage("New blank database initialized (local only)")
                    flushQueuedReceiptIfReady()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
            _isBusy.value = false
        }
    }

    fun createLinkedBlankDatabaseAtUri(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _isSyncingDb.value = true
            try {
                externalSync.takePersistableReadWritePermission(uri)
                val displayName = externalSync.resolveDisplayName(uri) ?: "hisaab_kitaab.db"
                when (val res = repository.createBlankDatabase()) {
                    is UiResult.Success -> {
                        val bytes = repository.exportDatabaseBytes()
                        val writeRes = bytes.fold(
                            onSuccess = { snapshot -> externalSync.writeBytesToUri(uri, snapshot) },
                            onFailure = { err -> Result.failure(err) }
                        )
                        prefsRepo.setLinkedDbInfo(displayName, uri.toString(), active = true)
                        _hasSavedLocalDb.value = true
                        _isDbReady.value = true
                        if (writeRes.isSuccess) {
                            _dbLinkStatus.value = DbLinkStatus.LiveLinked(displayName, System.currentTimeMillis())
                            emitMessage("Created & live-linked $displayName")
                        } else {
                            val reason = friendlySyncError(writeRes.exceptionOrNull())
                            _dbLinkStatus.value = DbLinkStatus.NeedsRelink(displayName, reason)
                            emitMessage("Created locally — couldn't write to $displayName. Tap Relink to choose another location.")
                        }
                        flushQueuedReceiptIfReady()
                    }
                    is UiResult.Error -> emitMessage(res.message)
                }
            } finally {
                _isSyncingDb.value = false
                _isBusy.value = false
            }
        }
    }

    fun importDatabaseFromUri(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                externalSync.takePersistableReadWritePermission(uri)
                val context = getApplication<Application>()
                val displayName = externalSync.resolveDisplayName(uri) ?: "imported_hisaab.db"
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            repository.importDatabase(stream)
                        } ?: UiResult.Error("Unable to open selected file.")
                    }.getOrElse { UiResult.Error(it.message ?: "Import failed") }
                }
                when (result) {
                    is UiResult.Success -> {
                        prefsRepo.setLinkedDbInfo(displayName, uri.toString(), active = true)
                        _hasSavedLocalDb.value = true
                        _isDbReady.value = true
                        _dbLinkStatus.value = DbLinkStatus.LiveLinked(displayName, System.currentTimeMillis())
                        emitMessage("Live-linked to $displayName")
                        flushQueuedReceiptIfReady()
                    }
                    is UiResult.Error -> emitMessage(result.message)
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun linkCurrentDatabaseToUri(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _isSyncingDb.value = true
            try {
                externalSync.takePersistableReadWritePermission(uri)
                val displayName = externalSync.resolveDisplayName(uri) ?: themePreferences.value.lastDbName
                val bytesRes = repository.exportDatabaseBytes()
                val writeRes = bytesRes.fold(
                    onSuccess = { bytes -> externalSync.writeBytesToUri(uri, bytes) },
                    onFailure = { err -> Result.failure(err) }
                )
                if (writeRes.isSuccess) {
                    prefsRepo.setLinkedDbInfo(displayName, uri.toString(), active = true)
                    _dbLinkStatus.value = DbLinkStatus.LiveLinked(displayName, System.currentTimeMillis())
                    emitMessage("Linked & synced all entries to $displayName")
                } else {
                    val reason = friendlySyncError(writeRes.exceptionOrNull())
                    _dbLinkStatus.value = DbLinkStatus.NeedsRelink(displayName, reason)
                    emitMessage("Could not link to $displayName: $reason")
                }
            } finally {
                _isSyncingDb.value = false
                _isBusy.value = false
            }
        }
    }

    fun syncLinkedDatabaseNow() {
        viewModelScope.launch {
            syncActiveDbToLinkedUri(silentOnSuccess = false)
        }
    }

    private suspend fun syncActiveDbToLinkedUri(silentOnSuccess: Boolean) {
        val prefs = prefsRepo.themePreferences.first()
        val uriStr = prefs.linkedDbUri.trim()
        if (uriStr.isBlank()) {
            _dbLinkStatus.value = DbLinkStatus.LocalOnly(prefs.lastDbName)
            if (!silentOnSuccess) {
                emitMessage("No .db file linked yet. Tap Link to choose a save location.")
            }
            return
        }

        _isSyncingDb.value = true
        try {
            val uri = Uri.parse(uriStr)
            val bytesRes = repository.exportDatabaseBytes()
            val writeRes = bytesRes.fold(
                onSuccess = { bytes -> externalSync.writeBytesToUri(uri, bytes) },
                onFailure = { err -> Result.failure(err) }
            )
            if (writeRes.isSuccess) {
                val displayName = externalSync.resolveDisplayName(uri) ?: prefs.lastDbName
                _dbLinkStatus.value = DbLinkStatus.LiveLinked(displayName, System.currentTimeMillis())
                if (!silentOnSuccess) {
                    emitMessage("Synced changes to $displayName")
                }
            } else {
                val reason = friendlySyncError(writeRes.exceptionOrNull())
                _dbLinkStatus.value = DbLinkStatus.NeedsRelink(prefs.lastDbName, reason)
                emitMessage("Saved locally — couldn't write to ${prefs.lastDbName}. Tap Relink to reconnect.")
            }
        } finally {
            _isSyncingDb.value = false
        }
    }

    private fun friendlySyncError(error: Throwable?): String {
        val raw = error?.message.orEmpty()
        return when {
            error is SecurityException || raw.contains("permission", ignoreCase = true) ->
                "Write permission expired or was revoked for this .db file."
            raw.contains("No such file", ignoreCase = true) ||
                raw.contains("ENOENT", ignoreCase = true) ||
                raw.contains("not found", ignoreCase = true) ->
                "The linked .db file was moved or deleted."
            raw.isNotBlank() -> raw
            else -> "Unable to access the linked .db file."
        }
    }

    fun exportDatabaseToUri(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        repository.exportDatabase(out)
                    } ?: UiResult.Error("Cannot write to selected location.")
                }.getOrElse { UiResult.Error(it.message ?: "Export failed") }
            }
            when (result) {
                is UiResult.Success -> emitMessage("SQLite .db backup copy saved")
                is UiResult.Error -> emitMessage(result.message)
            }
            _isBusy.value = false
        }
    }

    fun switchDatabaseScreen() {
        viewModelScope.launch {
            val prefs = prefsRepo.themePreferences.first()
            prefsRepo.setActiveDbInfo(prefs.lastDbName, false)
            _hasSavedLocalDb.value = repository.hasExistingDatabase() || prefs.linkedDbUri.isNotBlank()
            _isDbReady.value = false
        }
    }

    // ==================== THEME & SETTINGS ====================

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDarkMode(enabled) }
    fun setSurfaceStyle(style: SurfaceStyle) = viewModelScope.launch { prefsRepo.setSurfaceStyle(style) }
    fun cycleSurfaceStyle() = viewModelScope.launch {
        val next = when (themePreferences.value.surfaceStyle) {
            SurfaceStyle.CLASSIC_M3 -> SurfaceStyle.FROSTED
            SurfaceStyle.FROSTED -> SurfaceStyle.LIQUID_GLASS
            SurfaceStyle.LIQUID_GLASS -> SurfaceStyle.CLASSIC_M3
        }
        prefsRepo.setSurfaceStyle(next)
    }
    fun setAccentHex(hex: String) = viewModelScope.launch { prefsRepo.setAccentHex(hex) }
    fun setChartPalette(palette: ChartPalette) = viewModelScope.launch { prefsRepo.setChartPalette(palette) }
    fun setFontFamily(font: AppFontFamily) = viewModelScope.launch { prefsRepo.setFontFamily(font) }

    fun importCustomFontFromUri(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                CustomFontManager.importFontFromUri(context, uri)
            }
            when (result) {
                is ImportFontResult.Success -> {
                    prefsRepo.addAndActivateCustomFont(result.entry)
                    emitMessage("Applied custom font: ${result.entry.displayName}")
                }
                is ImportFontResult.Error -> {
                    emitMessage(result.message)
                }
            }
        }
    }

    fun selectCustomFont(fontId: String) = viewModelScope.launch {
        prefsRepo.selectCustomFont(fontId)
    }

    fun deleteCustomFont(fontId: String) = viewModelScope.launch {
        val removed = prefsRepo.removeCustomFont(fontId)
        if (removed != null) {
            withContext(Dispatchers.IO) {
                CustomFontManager.deleteFontFile(getApplication(), removed.fileName)
            }
            emitMessage("Removed custom font: ${removed.displayName}")
        }
    }

    fun setFontScale(scale: FontScaleOption) = viewModelScope.launch { prefsRepo.setFontScale(scale) }
    fun resetAppearanceDefaults() = viewModelScope.launch {
        prefsRepo.resetAppearanceDefaults()
        emitMessage("Appearance reset to defaults")
    }

    // ==================== ACCOUNTS ====================

    fun addAccount(
        name: String,
        openingBalance: Double,
        description: String,
        accountNumber: String,
        allowNegative: Boolean,
        date: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val res = repository.addAccount(name, openingBalance, description, accountNumber, allowNegative, date)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun updateAccount(
        id: String,
        name: String,
        openingBalance: Double,
        description: String,
        accountNumber: String,
        allowNegative: Boolean,
        date: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val res = repository.updateAccount(id, name, openingBalance, description, accountNumber, allowNegative, date)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun closeOrDeleteAccount(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.closeOrDeleteAccount(id)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    // ==================== CATEGORIES ====================

    fun addCategory(name: String, type: CategoryType, colorHex: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.addCategory(name, type, colorHex)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun updateCategory(id: String, name: String, type: CategoryType, colorHex: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.updateCategory(id, name, type, colorHex)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun deleteCategories(ids: Collection<String>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.deleteCategories(ids)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun previewCategoryCsvFromUri(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val fileName = resolveFileName(uri) ?: "categories.csv"
            val preview = withContext(Dispatchers.IO) {
                runCatching {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Could not read CSV file.")
                    CategoryCsvEngine.parseAndPreview(content, fileName, categories.value)
                }
            }
            preview.onSuccess {
                _categoryCsvPreview.value = it
            }.onFailure {
                emitMessage(it.message ?: "Failed to parse CSV")
            }
        }
    }

    fun dismissCategoryCsvPreview() {
        _categoryCsvPreview.value = null
    }

    fun confirmCategoryCsvImport(onCompleted: (CategoryCsvPreview) -> Unit = {}) {
        val current = _categoryCsvPreview.value ?: return
        viewModelScope.launch {
            when (val res = repository.bulkImportCategories(current.rows)) {
                is UiResult.Success -> {
                    _categoryCsvPreview.value = null
                    emitMessage(res.message)
                    onCompleted(current)
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun exportSampleCategoryCsvToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(CategoryCsvEngine.generateSampleCsv().toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            }.onSuccess {
                emitMessage("Sample category CSV saved")
            }.onFailure {
                emitMessage(it.message ?: "Could not save sample CSV")
            }
        }
    }

    // ==================== PAYMENT MEDIUMS ====================

    fun addPaymentMedium(name: String, group: PaymentGroup, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.addPaymentMedium(name, group)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun updatePaymentMedium(id: String, name: String, group: PaymentGroup, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.updatePaymentMedium(id, name, group)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun deletePaymentMediums(ids: Collection<String>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.deletePaymentMediums(ids)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    // ==================== TRANSACTIONS ====================

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String,
        categoryId: String,
        paymentGroup: PaymentGroup,
        paymentMediumId: String,
        reason: String,
        notes: String,
        date: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (
                val res = repository.addTransaction(
                    type, amount, accountId, toAccountId, categoryId, paymentMediumId, reason, notes, date
                )
            ) {
                is UiResult.Success -> {
                    prefsRepo.saveTxnFormDefaults(
                        TransactionFormDefaults(
                            type = type,
                            accountId = accountId,
                            toAccountId = toAccountId,
                            categoryId = categoryId,
                            paymentGroup = paymentGroup,
                            paymentMediumId = paymentMediumId
                        )
                    )
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun updateTransaction(
        id: String,
        type: TransactionType,
        amount: Double,
        accountId: String,
        toAccountId: String,
        categoryId: String,
        paymentMediumId: String,
        reason: String,
        notes: String,
        date: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (
                val res = repository.updateTransaction(
                    id, type, amount, accountId, toAccountId, categoryId, paymentMediumId, reason, notes, date
                )
            ) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun deleteTransaction(id: String, settlementAccountId: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.deleteTransaction(id, settlementAccountId)) {
                is UiResult.Success -> {
                    emitMessage(res.message)
                    onSuccess()
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    // ==================== UPI RECEIPT OCR & SHARE IMPORT ====================

    fun handleSharedReceiptUri(uri: Uri) {
        viewModelScope.launch {
            if (_isDbReady.value) {
                scanReceiptUriInternal(uri)
            } else {
                // Persist into cache while startup auto-connect or DB onboarding completes
                queuedReceiptFile?.delete()
                queuedReceiptText = null
                queuedReceiptFile = UpiReceiptScanner.copyUriToTempCache(getApplication(), uri)
                if (!_isStartupChecking.value && !_isDbReady.value) {
                    emitMessage("Connect or create a ledger to finish scanning your UPI receipt.")
                }
                if (_isDbReady.value) {
                    flushQueuedReceiptIfReady()
                }
            }
        }
    }

    fun handleSharedReceiptText(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            if (_isDbReady.value) {
                scanReceiptTextInternal(cleaned)
            } else {
                queuedReceiptFile?.delete()
                queuedReceiptFile = null
                queuedReceiptText = cleaned
                if (!_isStartupChecking.value && !_isDbReady.value) {
                    emitMessage("Connect or create a ledger to finish scanning your UPI receipt.")
                }
                if (_isDbReady.value) {
                    flushQueuedReceiptIfReady()
                }
            }
        }
    }

    fun scanReceiptUri(uri: Uri) {
        viewModelScope.launch {
            scanReceiptUriInternal(uri)
        }
    }

    fun clearPendingReceiptDraft() {
        _pendingReceiptDraft.value = null
    }

    private fun flushQueuedReceiptIfReady() {
        if (!_isDbReady.value) return
        val file = queuedReceiptFile
        val text = queuedReceiptText
        queuedReceiptFile = null
        queuedReceiptText = null

        if (file != null) {
            viewModelScope.launch {
                try {
                    scanReceiptFileInternal(file)
                } finally {
                    runCatching { file.delete() }
                }
            }
        } else if (!text.isNullOrBlank()) {
            viewModelScope.launch {
                scanReceiptTextInternal(text)
            }
        }
    }

    private suspend fun scanReceiptUriInternal(uri: Uri) {
        _isScanningReceipt.value = true
        try {
            val defaults = prefsRepo.txnFormDefaults.first()
            val result = UpiReceiptScanner.scanFromUri(
                context = getApplication(),
                uri = uri,
                accounts = accounts.value,
                transactions = transactions.value,
                fallbackAccountId = defaults.accountId.ifBlank { null },
                currencySymbol = profile.value.effectiveSymbol
            )
            applyScanResult(result)
        } finally {
            _isScanningReceipt.value = false
        }
    }

    private suspend fun scanReceiptFileInternal(file: File) {
        _isScanningReceipt.value = true
        try {
            val defaults = prefsRepo.txnFormDefaults.first()
            val result = UpiReceiptScanner.scanFromFile(
                context = getApplication(),
                file = file,
                accounts = accounts.value,
                transactions = transactions.value,
                fallbackAccountId = defaults.accountId.ifBlank { null },
                currencySymbol = profile.value.effectiveSymbol
            )
            applyScanResult(result)
        } finally {
            _isScanningReceipt.value = false
        }
    }

    private suspend fun scanReceiptTextInternal(rawText: String) {
        _isScanningReceipt.value = true
        try {
            val defaults = prefsRepo.txnFormDefaults.first()
            val draft = withContext(Dispatchers.Default) {
                UpiReceiptParser.parseReceiptText(
                    rawText = rawText,
                    accounts = accounts.value,
                    transactions = transactions.value,
                    fallbackAccountId = defaults.accountId.ifBlank { null },
                    currencySymbol = profile.value.effectiveSymbol
                )
            }
            applyScanResult(Result.success(draft))
        } finally {
            _isScanningReceipt.value = false
        }
    }

    private fun applyScanResult(result: Result<UpiReceiptDraft>) {
        result.onSuccess { draft ->
            _pendingReceiptDraft.value = draft
            if (draft.amount == null) {
                emitMessage("Receipt scanned (${draft.detectedApp}) — please verify the amount.")
            }
        }.onFailure { err ->
            emitMessage(err.message ?: "Could not read UPI receipt. Try a clearer screenshot.")
        }
    }

    // ==================== PROFILE ====================

    fun saveProfile(updated: UserProfile) {
        viewModelScope.launch {
            when (val res = repository.saveProfile(updated)) {
                is UiResult.Success -> emitMessage(res.message)
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    // ==================== STATEMENT & EXPORTS ====================

    fun buildStatementReport(range: DateRange, accountFilterId: String?): StatementReport {
        return StatementCalculator.buildReport(
            accounts = accounts.value,
            transactions = transactions.value,
            categories = categories.value,
            paymentMediums = paymentMediums.value,
            range = range,
            accountFilterId = accountFilterId
        )
    }

    fun exportStatementPdfToUri(uri: Uri, report: StatementReport, includeWatermark: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    StatementPdfEngine.generatePdf(report, profile.value, includeWatermark, out, context)
                } ?: throw IllegalStateException("Could not write PDF file")
            }.onSuccess {
                emitMessage(if (includeWatermark) "Watermarked PDF saved" else "PDF statement saved")
            }.onFailure {
                emitMessage(it.message ?: "PDF export failed")
            }
        }
    }

    fun prepareSharePdfFile(
        report: StatementReport,
        includeWatermark: Boolean,
        onFileReady: (File) -> Unit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    StatementPdfEngine.writeTempPdfForShare(context, report, profile.value, includeWatermark)
                }
            }
            result.onSuccess(onFileReady).onFailure {
                emitMessage(it.message ?: "Could not prepare PDF for sharing")
            }
        }
    }

    fun exportStatementXlsxToUri(uri: Uri, report: StatementReport) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    StatementSpreadsheetEngine.exportStatementXlsx(report, profile.value, out)
                } ?: throw IllegalStateException("Could not write Excel file")
            }.onSuccess {
                emitMessage("Excel (.xlsx) statement exported")
            }.onFailure {
                emitMessage(it.message ?: "Excel export failed")
            }
        }
    }

    fun exportStatementCsvToUri(uri: Uri, report: StatementReport) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    StatementSpreadsheetEngine.exportStatementCsv(report, out)
                } ?: throw IllegalStateException("Could not write CSV file")
            }.onSuccess {
                emitMessage("CSV statement exported")
            }.onFailure {
                emitMessage(it.message ?: "CSV export failed")
            }
        }
    }

    fun previewStatementImportFromUri(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val fileName = resolveFileName(uri) ?: "statement_import"
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        StatementSpreadsheetEngine.parseAndPreviewImport(
                            inputStream = input,
                            fileName = fileName,
                            accounts = accounts.value,
                            categories = categories.value,
                            paymentMediums = paymentMediums.value,
                            existingTransactions = transactions.value
                        )
                    } ?: throw IllegalStateException("Unable to read selected spreadsheet.")
                }
            }
            result.onSuccess {
                _statementImportPreview.value = it
            }.onFailure {
                emitMessage(it.message ?: "Could not parse spreadsheet")
            }
        }
    }

    fun dismissStatementImportPreview() {
        _statementImportPreview.value = null
    }

    fun confirmStatementImport() {
        val current = _statementImportPreview.value ?: return
        viewModelScope.launch {
            when (val res = repository.bulkImportStatementRows(current.rows)) {
                is UiResult.Success -> {
                    _statementImportPreview.value = null
                    emitMessage(res.message)
                }
                is UiResult.Error -> emitMessage(res.message)
            }
        }
    }

    // ==================== ANALYTICS COMPUTATIONS ====================

    fun computeExpenseByCategory(
        range: DateRange,
        accountFilterId: String?,
        palette: ChartPalette = ChartPalette.VIBRANT_JEWEL
    ): List<AnalyticsSlice> {
        val colors = palette.categoryColors
        val catsById = categories.value.associateBy { it.id }
        val filtered = transactions.value.filter { txn ->
            txn.type == TransactionType.EXPENSE &&
                txn.date >= range.start &&
                txn.date <= range.end &&
                (accountFilterId.isNullOrBlank() || txn.accountId == accountFilterId)
        }
        val total = filtered.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return filtered
            .groupBy { it.categoryId }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
            .mapIndexed { idx, (catId, list) ->
                val sum = list.sumOf { it.amount }
                val cat = catsById[catId]
                AnalyticsSlice(
                    id = catId.ifBlank { "uncat" },
                    label = cat?.let { StringUtils.displayName(it.name) } ?: "Uncategorized",
                    amount = sum,
                    colorHex = colors[idx % colors.size],
                    percentage = ((sum / total) * 100.0).toFloat()
                )
            }
    }

    fun computeExpenseByMedium(
        range: DateRange,
        accountFilterId: String?,
        palette: ChartPalette = ChartPalette.VIBRANT_JEWEL
    ): List<AnalyticsSlice> {
        val colors = palette.mediumColors
        val mediumsById = paymentMediums.value.associateBy { it.id }
        val filtered = transactions.value.filter { txn ->
            txn.type == TransactionType.EXPENSE &&
                txn.date >= range.start &&
                txn.date <= range.end &&
                (accountFilterId.isNullOrBlank() || txn.accountId == accountFilterId)
        }
        val total = filtered.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return filtered
            .groupBy { it.paymentMediumId }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.amount } }
            .mapIndexed { idx, (medId, list) ->
                val sum = list.sumOf { it.amount }
                val med = mediumsById[medId]
                AnalyticsSlice(
                    id = medId.ifBlank { "none" },
                    label = med?.let { StringUtils.displayName(it.name) } ?: "Unspecified",
                    amount = sum,
                    colorHex = colors[idx % colors.size],
                    percentage = ((sum / total) * 100.0).toFloat()
                )
            }
    }

    fun computeDailyFlow(
        range: DateRange,
        accountFilterId: String?
    ): List<DailyFlowPoint> {
        val days = StringUtils.eachDay(range.start, range.end)
        if (days.isEmpty()) return emptyList()

        val filtered = transactions.value.filter { txn ->
            txn.date >= range.start &&
                txn.date <= range.end &&
                (txn.type == TransactionType.INCOME || txn.type == TransactionType.EXPENSE) &&
                (accountFilterId.isNullOrBlank() || txn.accountId == accountFilterId)
        }
        val incomeByDay = filtered.filter { it.type == TransactionType.INCOME }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val expenseByDay = filtered.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        return days.map { day ->
            DailyFlowPoint(
                date = day,
                shortLabel = StringUtils.formatShortDate(day),
                income = incomeByDay[day] ?: 0.0,
                expense = expenseByDay[day] ?: 0.0
            )
        }
    }

    private fun emitMessage(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }

    private fun resolveFileName(uri: Uri): String? {
        val context = getApplication<Application>()
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }
}
