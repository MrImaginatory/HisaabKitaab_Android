package com.hisaabkitaab

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.core.content.IntentCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabkitaab.data.AppDestination
import com.hisaabkitaab.data.DbLinkStatus
import com.hisaabkitaab.data.StatementReport
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.export.StatementPdfEngine
import com.hisaabkitaab.ui.screens.AccountsScreen
import com.hisaabkitaab.ui.screens.CategoryScreen
import com.hisaabkitaab.ui.screens.DashboardScreen
import com.hisaabkitaab.ui.screens.DatabaseOnboardingScreen
import com.hisaabkitaab.ui.screens.DeveloperConnectSheet
import com.hisaabkitaab.ui.screens.ManageHubBottomSheet
import com.hisaabkitaab.ui.screens.PaymentMediumScreen
import com.hisaabkitaab.ui.screens.ProfileScreen
import com.hisaabkitaab.ui.screens.SettingsBottomSheet
import com.hisaabkitaab.ui.screens.SplashScreen
import com.hisaabkitaab.ui.screens.StatementScreen
import com.hisaabkitaab.ui.screens.TransactionFormSheet
import com.hisaabkitaab.ui.screens.TransactionsScreen
import com.hisaabkitaab.ui.theme.HisaabBackdrop
import com.hisaabkitaab.ui.theme.HisaabBrandLogo
import com.hisaabkitaab.ui.theme.HisaabKitaabTheme
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.QuickAddGlassFab
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val incomingShareIntent = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureShareIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            val viewModel: HisaabViewModel = viewModel()
            val themePrefs by viewModel.themePreferences.collectAsStateWithLifecycle()
            val pendingIntent by incomingShareIntent.collectAsStateWithLifecycle()

            LaunchedEffect(pendingIntent) {
                val shareIntent = pendingIntent ?: return@LaunchedEffect
                incomingShareIntent.value = null
                dispatchShareIntent(shareIntent, viewModel)
            }

            LaunchedEffect(themePrefs.isDarkMode) {
                if (themePrefs.isDarkMode) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    )
                }
            }

            HisaabKitaabTheme(preferences = themePrefs) {
                HisaabKitaabRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureShareIntent(intent)
    }

    private fun captureShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            incomingShareIntent.value = intent
        }
    }

    private fun dispatchShareIntent(intent: Intent, viewModel: HisaabViewModel) {
        val mime = intent.type.orEmpty()
        val streamUri: Uri? = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

        when {
            streamUri != null && (mime.startsWith("image/") || mime.isBlank()) -> {
                viewModel.handleSharedReceiptUri(streamUri)
            }
            mime.startsWith("text/") -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                if (sharedText.isNotBlank()) {
                    viewModel.handleSharedReceiptText(sharedText)
                } else if (streamUri != null) {
                    viewModel.handleSharedReceiptUri(streamUri)
                }
            }
            streamUri != null -> {
                viewModel.handleSharedReceiptUri(streamUri)
            }
        }
    }
}

@Composable
private fun HisaabKitaabRoot(viewModel: HisaabViewModel) {
    val context = LocalContext.current
    val themePrefs by viewModel.themePreferences.collectAsStateWithLifecycle()
    val isDbReady by viewModel.isDbReady.collectAsStateWithLifecycle()
    val isStartupChecking by viewModel.isStartupChecking.collectAsStateWithLifecycle()
    val hasSavedLocalDb by viewModel.hasSavedLocalDb.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val paymentMediums by viewModel.paymentMediums.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val dbSizeBytes by viewModel.dbSizeBytes.collectAsStateWithLifecycle()
    val txnDefaults by viewModel.txnFormDefaults.collectAsStateWithLifecycle()
    val categoryCsvPreview by viewModel.categoryCsvPreview.collectAsStateWithLifecycle()
    val statementImportPreview by viewModel.statementImportPreview.collectAsStateWithLifecycle()
    val pendingReceiptDraft by viewModel.pendingReceiptDraft.collectAsStateWithLifecycle()
    val isScanningReceipt by viewModel.isScanningReceipt.collectAsStateWithLifecycle()
    val dbLinkStatus by viewModel.dbLinkStatus.collectAsStateWithLifecycle()
    val isSyncingDb by viewModel.isSyncingDb.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }
    var currentDest by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showManageSheet by remember { mutableStateOf(false) }
    var showConnectSheet by remember { mutableStateOf(false) }
    var showQuickAddTxnSheet by remember { mutableStateOf(false) }
    var relinkBannerDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(dbLinkStatus) {
        if (dbLinkStatus is DbLinkStatus.NeedsRelink) {
            relinkBannerDismissed = false
        }
    }

    var addAccountTrigger by remember { mutableIntStateOf(0) }
    var addCategoryTrigger by remember { mutableIntStateOf(0) }
    var addMediumTrigger by remember { mutableIntStateOf(0) }

    val onPrimaryAddAction: () -> Unit = {
        when (currentDest) {
            AppDestination.ACCOUNTS -> addAccountTrigger++
            AppDestination.CATEGORIES -> addCategoryTrigger++
            AppDestination.PAYMENT_MEDIUMS -> addMediumTrigger++
            else -> showQuickAddTxnSheet = true
        }
    }
    val primaryAddDescription = when (currentDest) {
        AppDestination.ACCOUNTS -> "Add Account"
        AppDestination.CATEGORIES -> "Add Category"
        AppDestination.PAYMENT_MEDIUMS -> "Add Payment Medium"
        else -> "Add Transaction"
    }

    // Pending SAF export payloads
    var pendingPdfExport by remember { mutableStateOf<Pair<StatementReport, Boolean>?>(null) }
    var pendingXlsxExport by remember { mutableStateOf<StatementReport?>(null) }
    var pendingCsvExport by remember { mutableStateOf<StatementReport?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // ---- Zero-Permission Android Storage Access Framework (SAF) Launchers ----
    val openDbLauncher = rememberLauncherForActivityResult(PersistableOpenDocument()) { uri ->
        uri?.let { viewModel.importDatabaseFromUri(it) }
    }
    val createLinkedDbLauncher = rememberLauncherForActivityResult(
        PersistableCreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.createLinkedBlankDatabaseAtUri(it) }
    }
    val linkCurrentDbLauncher = rememberLauncherForActivityResult(
        PersistableCreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.linkCurrentDatabaseToUri(it) }
    }
    val saveDbLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { viewModel.exportDatabaseToUri(it) }
    }
    val launchCreateLinkedDb: () -> Unit = {
        createLinkedDbLauncher.launch("HisaabKitaab_${StringUtils.todayIso()}.db")
    }
    val launchLinkCurrentDb: () -> Unit = {
        val suggestedName = themePrefs.lastDbName
            .takeIf { it.isNotBlank() && it.endsWith(".db", ignoreCase = true) }
            ?: "HisaabKitaab_${StringUtils.todayIso()}.db"
        linkCurrentDbLauncher.launch(suggestedName)
    }
    val importCategoryCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.previewCategoryCsvFromUri(it) }
    }
    val saveSampleCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.exportSampleCategoryCsvToUri(it) }
    }
    val savePdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val payload = pendingPdfExport
        if (uri != null && payload != null) {
            viewModel.exportStatementPdfToUri(uri, payload.first, payload.second)
        }
        pendingPdfExport = null
    }
    val saveXlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val report = pendingXlsxExport
        if (uri != null && report != null) {
            viewModel.exportStatementXlsxToUri(uri, report)
        }
        pendingXlsxExport = null
    }
    val saveStatementCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val report = pendingCsvExport
        if (uri != null && report != null) {
            viewModel.exportStatementCsvToUri(uri, report)
        }
        pendingCsvExport = null
    }
    val importStatementLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.previewStatementImportFromUri(it) }
    }
    val importCustomFontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importCustomFontFromUri(it) }
    }
    // Zero-permission Android Photo Picker for UPI receipt screenshots
    val pickReceiptImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.scanReceiptUri(it) }
    }
    val launchReceiptPicker: () -> Unit = {
        pickReceiptImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    if (showSplash || isStartupChecking) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    if (!isDbReady) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) { _ ->
            DatabaseOnboardingScreen(
                hasSavedLocalDb = hasSavedLocalDb,
                themePrefs = themePrefs,
                isBusy = isBusy,
                onReconnectLast = { viewModel.reconnectLastDatabase() },
                onOpenDbFile = { openDbLauncher.launch(arrayOf("*/*")) },
                onCreateBlankDb = launchCreateLinkedDb,
                onToggleDarkMode = { viewModel.setDarkMode(!themePrefs.isDarkMode) },
                onCycleSurfaceStyle = { viewModel.cycleSurfaceStyle() }
            )
        }
        return
    }

    // Handle Android Back button cleanly when on any secondary screen
    BackHandler(enabled = currentDest != AppDestination.DASHBOARD) {
        currentDest = AppDestination.DASHBOARD
    }

    HisaabBackdrop {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpanded = maxWidth >= 720.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (isExpanded) {
                    AdaptiveRail(
                        currentDest = currentDest,
                        onSelect = { currentDest = it },
                        onQuickAdd = onPrimaryAddAction,
                        onOpenSettings = { showSettingsSheet = true },
                        onOpenConnect = { showConnectSheet = true }
                    )
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = if (isExpanded) 16.dp else 94.dp)
                        )
                    },
                    topBar = {
                        HisaabTopBar(
                            destination = currentDest
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 980.dp)
                                .fillMaxSize()
                        ) {
                            AnimatedContent(
                                targetState = currentDest,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + slideInVertically(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        initialOffsetY = { it / 28 }
                                    )) togetherWith fadeOut(tween(150))
                                },
                                label = "screenTransition"
                            ) { dest ->
                                when (dest) {
                                    AppDestination.DASHBOARD -> DashboardScreen(
                                        viewModel = viewModel,
                                        accounts = accounts,
                                        transactions = transactions,
                                        categories = categories,
                                        paymentMediums = paymentMediums,
                                        profile = profile,
                                        chartPalette = themePrefs.chartPalette,
                                        onOpenTransactions = { currentDest = AppDestination.TRANSACTIONS },
                                        onOpenAccounts = { currentDest = AppDestination.ACCOUNTS }
                                    )

                                    AppDestination.TRANSACTIONS -> TransactionsScreen(
                                        viewModel = viewModel,
                                        accounts = accounts,
                                        transactions = transactions,
                                        categories = categories,
                                        paymentMediums = paymentMediums,
                                        profile = profile,
                                        formDefaults = txnDefaults,
                                        onScanReceipt = launchReceiptPicker
                                    )

                                    AppDestination.ACCOUNTS -> AccountsScreen(
                                        viewModel = viewModel,
                                        accounts = accounts,
                                        profile = profile,
                                        addTrigger = addAccountTrigger
                                    )

                                    AppDestination.STATEMENT -> StatementScreen(
                                        viewModel = viewModel,
                                        accounts = accounts,
                                        transactions = transactions,
                                        categories = categories,
                                        paymentMediums = paymentMediums,
                                        profile = profile,
                                        importPreview = statementImportPreview,
                                        onExportPdf = { report, watermark ->
                                            pendingPdfExport = report to watermark
                                            val suffix = if (watermark) "watermarked" else "statement"
                                            savePdfLauncher.launch("HisaabKitaab_${report.range.start}_to_${report.range.end}_$suffix.pdf")
                                        },
                                        onSharePdf = { report, watermark ->
                                            viewModel.prepareSharePdfFile(report, watermark) { file ->
                                                val shareIntent = StatementPdfEngine.buildShareIntent(
                                                    context = context,
                                                    file = file,
                                                    mimeType = "application/pdf",
                                                    chooserTitle = "Share Financial Statement PDF"
                                                )
                                                context.startActivity(shareIntent)
                                            }
                                        },
                                        onExportXlsx = { report ->
                                            pendingXlsxExport = report
                                            saveXlsxLauncher.launch("HisaabKitaab_${report.range.start}_to_${report.range.end}.xlsx")
                                        },
                                        onExportCsv = { report ->
                                            pendingCsvExport = report
                                            saveStatementCsvLauncher.launch("HisaabKitaab_${report.range.start}_to_${report.range.end}.csv")
                                        },
                                        onImportSpreadsheet = {
                                            importStatementLauncher.launch(arrayOf("*/*"))
                                        }
                                    )

                                    AppDestination.CATEGORIES -> CategoryScreen(
                                        viewModel = viewModel,
                                        categories = categories,
                                        csvPreview = categoryCsvPreview,
                                        addTrigger = addCategoryTrigger,
                                        onRequestImportCsv = { importCategoryCsvLauncher.launch(arrayOf("*/*")) },
                                        onRequestSampleCsv = { saveSampleCsvLauncher.launch("hisaab_categories_sample.csv") }
                                    )

                                    AppDestination.PAYMENT_MEDIUMS -> PaymentMediumScreen(
                                        viewModel = viewModel,
                                        paymentMediums = paymentMediums,
                                        addTrigger = addMediumTrigger
                                    )

                                    AppDestination.PROFILE -> ProfileScreen(
                                        viewModel = viewModel,
                                        profile = profile
                                    )
                                }
                            }

                            // Top floating status overlays (Relink warning + OCR scanning pill)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val needsRelink = dbLinkStatus as? DbLinkStatus.NeedsRelink
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = needsRelink != null && !relinkBannerDismissed,
                                    enter = fadeIn(tween(180)),
                                    exit = fadeOut(tween(180))
                                ) {
                                    if (needsRelink != null) {
                                        DbRelinkWarningBanner(
                                            fileName = needsRelink.fileName,
                                            reason = needsRelink.reason,
                                            onRelink = launchLinkCurrentDb,
                                            onDismiss = { relinkBannerDismissed = true }
                                        )
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isScanningReceipt,
                                    enter = fadeIn(tween(180)),
                                    exit = fadeOut(tween(180))
                                ) {
                                    UpiScanningOverlayPill()
                                }
                            }

                            // Floating Glass Pill Bar + Context-Aware Quick-Add (+) Button on phones
                            if (!isExpanded) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FloatingGlassBottomNav(
                                        currentDest = currentDest,
                                        onSelect = { currentDest = it },
                                        onOpenManage = { showManageSheet = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                    QuickAddGlassFab(
                                        onClick = onPrimaryAddAction,
                                        contentDescription = primaryAddDescription,
                                        testTag = "universal_quick_add_fab"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Universal Quick-Add / UPI Receipt Prefilled Transaction Bottom Sheet
    if (showQuickAddTxnSheet || pendingReceiptDraft != null) {
        TransactionFormSheet(
            editingTxn = null,
            defaults = txnDefaults,
            activeAccounts = accounts.filter { !it.isClosed },
            activeCategories = categories.filter { !it.isDeleted },
            activeMediums = paymentMediums.filter { !it.isDeleted },
            currencySymbol = profile.effectiveSymbol,
            receiptDraft = pendingReceiptDraft,
            onScanReceipt = launchReceiptPicker,
            onDismiss = {
                showQuickAddTxnSheet = false
                viewModel.clearPendingReceiptDraft()
            },
            onSubmit = { type, amt, accId, toAccId, catId, payGrp, medId, reason, notes, date ->
                viewModel.addTransaction(
                    type, amt, accId, toAccId, catId, payGrp, medId, reason, notes, date
                ) {
                    showQuickAddTxnSheet = false
                    viewModel.clearPendingReceiptDraft()
                }
            }
        )
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            viewModel = viewModel,
            themePrefs = themePrefs,
            dbLinkStatus = dbLinkStatus,
            isSyncingDb = isSyncingDb,
            dbSizeBytes = dbSizeBytes,
            accountCount = accounts.size,
            txnCount = transactions.size,
            categoryCount = categories.size,
            mediumCount = paymentMediums.size,
            onLinkCurrentDbToFile = launchLinkCurrentDb,
            onSyncLinkedDbNow = { viewModel.syncLinkedDatabaseNow() },
            onCreateLinkedBlankDb = launchCreateLinkedDb,
            onExportDbBackup = {
                saveDbLauncher.launch("HisaabKitaab_backup_${StringUtils.todayIso()}.db")
            },
            onImportDbFile = {
                openDbLauncher.launch(arrayOf("*/*"))
            },
            onSwitchDatabase = {
                viewModel.switchDatabaseScreen()
            },
            onImportCustomFont = {
                importCustomFontLauncher.launch(
                    arrayOf(
                        "font/ttf",
                        "font/otf",
                        "application/x-font-ttf",
                        "application/x-font-otf",
                        "application/font-sfnt",
                        "application/octet-stream",
                        "*/*"
                    )
                )
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showManageSheet) {
        ManageHubBottomSheet(
            onNavigate = { currentDest = it },
            onScanUpiReceipt = launchReceiptPicker,
            onOpenSettings = { showSettingsSheet = true },
            onExportDbBackup = {
                saveDbLauncher.launch("HisaabKitaab_backup_${StringUtils.todayIso()}.db")
            },
            onOpenConnect = { showConnectSheet = true },
            onDismiss = { showManageSheet = false }
        )
    }

    if (showConnectSheet) {
        DeveloperConnectSheet(onDismiss = { showConnectSheet = false })
    }
}

@Composable
private fun HisaabTopBar(
    destination: AppDestination
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HisaabBrandLogo(size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = cs.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FloatingGlassBottomNav(
    currentDest: AppDestination,
    onSelect: (AppDestination) -> Unit,
    onOpenManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    val isManageSection = currentDest in setOf(
        AppDestination.CATEGORIES,
        AppDestination.PAYMENT_MEDIUMS,
        AppDestination.PROFILE
    )

    val containerBrush = if (ledger.isLiquidGlass) {
        Brush.verticalGradient(
            listOf(ledger.dockSurface, ledger.glassSurfaceStrong)
        )
    } else {
        Brush.verticalGradient(listOf(cs.surfaceContainerHigh, cs.surfaceContainerHigh))
    }

    val borderBrush = if (ledger.isLiquidGlass) {
        Brush.verticalGradient(listOf(ledger.glassBorderTop, ledger.glassBorderBottom))
    } else {
        Brush.verticalGradient(listOf(cs.outlineVariant, cs.outlineVariant))
    }

    val pillShape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .shadow(
                elevation = if (ledger.isLiquidGlass) 14.dp else 4.dp,
                shape = pillShape,
                ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f),
                spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f)
            )
            .clip(pillShape)
            .background(containerBrush)
            .border(if (ledger.isFullLiquidGlass) 1.2.dp else 1.dp, borderBrush, pillShape)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavPillItem(
            selectedIcon = Icons.Rounded.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            label = "Home",
            selected = currentDest == AppDestination.DASHBOARD,
            testTag = "nav_dashboard",
            modifier = Modifier.weight(1f)
        ) { onSelect(AppDestination.DASHBOARD) }

        NavPillItem(
            selectedIcon = Icons.Rounded.ReceiptLong,
            unselectedIcon = Icons.Outlined.ReceiptLong,
            label = "Ledger",
            selected = currentDest == AppDestination.TRANSACTIONS,
            testTag = "nav_transactions",
            modifier = Modifier.weight(1f)
        ) { onSelect(AppDestination.TRANSACTIONS) }

        NavPillItem(
            selectedIcon = Icons.Rounded.AccountBalance,
            unselectedIcon = Icons.Outlined.AccountBalance,
            label = "Vault",
            selected = currentDest == AppDestination.ACCOUNTS,
            testTag = "nav_accounts",
            modifier = Modifier.weight(1f)
        ) { onSelect(AppDestination.ACCOUNTS) }

        NavPillItem(
            selectedIcon = Icons.Rounded.Description,
            unselectedIcon = Icons.Outlined.Description,
            label = "Report",
            selected = currentDest == AppDestination.STATEMENT,
            testTag = "nav_statement",
            modifier = Modifier.weight(1f)
        ) { onSelect(AppDestination.STATEMENT) }

        NavPillItem(
            selectedIcon = Icons.Rounded.Apps,
            unselectedIcon = Icons.Outlined.Apps,
            label = "More",
            selected = isManageSection,
            testTag = "nav_manage",
            modifier = Modifier.weight(1f)
        ) { onOpenManage() }
    }
}

@Composable
private fun NavPillItem(
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val pillBg by animateColorAsState(
        targetValue = if (selected) cs.primary.copy(alpha = 0.22f) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navPillBg"
    )
    val contentTint by animateColorAsState(
        targetValue = if (selected) cs.primary else cs.onSurfaceVariant,
        label = "navPillTint"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(pillBg)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(vertical = 7.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = contentTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentTint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdaptiveRail(
    currentDest: AppDestination,
    onSelect: (AppDestination) -> Unit,
    onQuickAdd: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenConnect: () -> Unit
) {
    val items = listOf(
        AppDestination.DASHBOARD to Icons.Rounded.Dashboard,
        AppDestination.TRANSACTIONS to Icons.Rounded.ReceiptLong,
        AppDestination.ACCOUNTS to Icons.Rounded.AccountBalance,
        AppDestination.STATEMENT to Icons.Rounded.Description,
        AppDestination.CATEGORIES to Icons.Rounded.Category,
        AppDestination.PAYMENT_MEDIUMS to Icons.Rounded.CreditCard,
        AppDestination.PROFILE to Icons.Rounded.Person
    )

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
        header = {
            Spacer(Modifier.height(8.dp))
            HisaabBrandLogo(size = 38.dp)
            Spacer(Modifier.height(10.dp))
            QuickAddGlassFab(
                onClick = onQuickAdd,
                testTag = "rail_quick_add_fab"
            )
            Spacer(Modifier.height(8.dp))
        }
    ) {
        items.forEach { (dest, icon) ->
            NavigationRailItem(
                selected = currentDest == dest,
                onClick = { onSelect(dest) },
                icon = { Icon(icon, contentDescription = dest.label) },
                label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            selected = false,
            onClick = onOpenSettings,
            icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
        NavigationRailItem(
            selected = false,
            onClick = onOpenConnect,
            icon = { Icon(Icons.Rounded.Code, contentDescription = "Connect") },
            label = { Text("Connect") }
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun UpiScanningOverlayPill() {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = Modifier
            .shadow(12.dp, shape)
            .clip(shape)
            .background(if (ledger.isLiquidGlass) ledger.dockSurface else cs.surfaceContainerHighest)
            .border(1.dp, if (ledger.isLiquidGlass) ledger.glassBorderTop else cs.outlineVariant, shape)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("upi_scanning_overlay_pill"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.4.dp,
            color = cs.primary
        )
        Text(
            text = "Scanning UPI receipt offline…",
            style = MaterialTheme.typography.labelLarge,
            color = cs.onSurface
        )
    }
}

@Composable
private fun DbRelinkWarningBanner(
    fileName: String,
    reason: String,
    onRelink: () -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape)
            .clip(shape)
            .background(if (ledger.isLiquidGlass) ledger.dockSurface else cs.surfaceContainerHighest)
            .border(1.2.dp, ledger.warning.copy(alpha = 0.65f), shape)
            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
            .testTag("db_relink_warning_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = "Database sync warning",
            tint = ledger.warning,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Saved locally — couldn't sync $fileName",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        FilledTonalButton(
            onClick = onRelink,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = ledger.warningContainer,
                contentColor = ledger.warning
            ),
            modifier = Modifier.testTag("db_relink_banner_action_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Relink", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("db_relink_banner_dismiss_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss sync warning",
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private class PersistableOpenDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }
}

private class PersistableCreateDocument(mimeType: String) : ActivityResultContracts.CreateDocument(mimeType) {
    override fun createIntent(context: Context, input: String): Intent {
        return super.createIntent(context, input).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }
}
