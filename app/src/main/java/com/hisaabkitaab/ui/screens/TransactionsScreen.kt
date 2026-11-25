package com.hisaabkitaab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.Category
import com.hisaabkitaab.data.CategoryType
import com.hisaabkitaab.data.PaymentGroup
import com.hisaabkitaab.data.PaymentMedium
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.TransactionFormDefaults
import com.hisaabkitaab.data.TransactionType
import com.hisaabkitaab.data.UpiReceiptDraft
import com.hisaabkitaab.data.UserProfile
import com.hisaabkitaab.ui.theme.AnimatedCurrencyText
import com.hisaabkitaab.ui.theme.CaptchaDeleteDialog
import com.hisaabkitaab.ui.theme.DatePickerField
import com.hisaabkitaab.ui.theme.ExpressiveEmptyState
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassDangerButton
import com.hisaabkitaab.ui.theme.GlassDropdownField
import com.hisaabkitaab.ui.theme.GlassFormBottomSheet
import com.hisaabkitaab.ui.theme.GlassIconButton
import com.hisaabkitaab.ui.theme.GlassPrimaryButton
import com.hisaabkitaab.ui.theme.GlassSearchField
import com.hisaabkitaab.ui.theme.GlassTextField
import com.hisaabkitaab.ui.theme.GlassTonalButton
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.MaxLengthTransformation
import com.hisaabkitaab.ui.theme.RegexInputTransformation
import com.hisaabkitaab.ui.theme.SegmentedPillBar
import com.hisaabkitaab.ui.theme.glassTextFieldColors
import com.hisaabkitaab.ui.theme.parseHexColorSafe
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel
import androidx.compose.material.icons.rounded.ReceiptLong
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: HisaabViewModel,
    accounts: List<Account>,
    transactions: List<Transaction>,
    categories: List<Category>,
    paymentMediums: List<PaymentMedium>,
    profile: UserProfile,
    formDefaults: TransactionFormDefaults,
    onScanReceipt: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val symbol = profile.effectiveSymbol

    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var showFormSheet by remember { mutableStateOf(false) }
    var editingTxn by remember { mutableStateOf<Transaction?>(null) }
    var viewingTxn by remember { mutableStateOf<Transaction?>(null) }
    var deletingTxn by remember { mutableStateOf<Transaction?>(null) }

    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val mediumsById = remember(paymentMediums) { paymentMediums.associateBy { it.id } }
    val activeAccounts = remember(accounts) { accounts.filter { !it.isClosed } }

    val filteredTxns = remember(transactions, searchQuery, typeFilter, accountsById, categoriesById, mediumsById) {
        val q = searchQuery.trim().lowercase()
        transactions.filter { txn ->
            val typeOk = typeFilter == null || txn.type == typeFilter
            val queryOk = q.isEmpty() ||
                txn.reason.lowercase().contains(q) ||
                txn.notes.lowercase().contains(q) ||
                txn.date.contains(q) ||
                (accountsById[txn.accountId]?.name?.lowercase()?.contains(q) == true) ||
                (accountsById[txn.toAccountId]?.name?.lowercase()?.contains(q) == true) ||
                (categoriesById[txn.categoryId]?.name?.lowercase()?.contains(q) == true) ||
                (mediumsById[txn.paymentMediumId]?.name?.lowercase()?.contains(q) == true)
            typeOk && queryOk
        }
    }

    val totalIncome = remember(filteredTxns) {
        filteredTxns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredTxns) {
        filteredTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val netFlow = totalIncome - totalExpense

    // Pagination (25 entries per page — matching Statement ledger)
    val pageSize = 25
    var page by remember { mutableIntStateOf(1) }
    val totalPages = maxOf(1, (filteredTxns.size + pageSize - 1) / pageSize)
    LaunchedEffect(filteredTxns.size, searchQuery, typeFilter) {
        page = 1
    }
    val safePage = page.coerceIn(1, totalPages)
    val pagedTxns = remember(filteredTxns, safePage) {
        val start = ((safePage - 1) * pageSize).coerceAtLeast(0)
        filteredTxns.drop(start).take(pageSize)
    }
    val rangeStart = if (filteredTxns.isEmpty()) 0 else (safePage - 1) * pageSize + 1
    val rangeEnd = minOf(safePage * pageSize, filteredTxns.size)

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().testTag("transactions_screen")) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 126.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3 Animated KPI Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TxnKpiMini("Income", totalIncome, symbol, ledger.income, Modifier.weight(1f))
                    TxnKpiMini("Expense", totalExpense, symbol, ledger.expense, Modifier.weight(1f))
                    TxnKpiMini("Net Flow", netFlow, symbol, if (netFlow >= 0) ledger.income else ledger.expense, Modifier.weight(1f))
                }
            }

            // Search + Type Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search reason, account, category, date…",
                        testTag = "transactions_search_input"
                    )
                    val filterItems: List<TransactionType?> = listOf(null, TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
                    SegmentedPillBar(
                        options = filterItems,
                        selected = typeFilter,
                        onSelect = { typeFilter = it },
                        labelOf = { it?.label ?: "All" },
                        colorOf = {
                            when (it) {
                                TransactionType.INCOME -> ledger.income
                                TransactionType.EXPENSE -> ledger.expense
                                TransactionType.TRANSFER -> ledger.transfer
                                null -> cs.primary
                            }
                        },
                        testTagPrefix = "txn_filter"
                    )
                }
            }

            // Ledger Entries Header + Pagination Info
            if (filteredTxns.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ledger Entries ($rangeStart–$rangeEnd of ${filteredTxns.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onBackground
                        )
                        Text(
                            text = "Page $safePage of $totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }

            if (filteredTxns.isEmpty()) {
                item {
                    ExpressiveEmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "No transactions found",
                        subtitle = if (accounts.isEmpty())
                            "Create an account first, then record your expenses, income, or self-transfers."
                        else
                            "Tap the + button below to log your first ledger entry."
                    )
                }
            } else {
                items(pagedTxns, key = { it.id }) { txn ->
                    val srcAcc = accountsById[txn.accountId]
                    val dstAcc = accountsById[txn.toAccountId]
                    val cat = categoriesById[txn.categoryId]
                    val med = mediumsById[txn.paymentMediumId]

                    TransactionRowCard(
                        txn = txn,
                        srcAccountName = srcAcc?.let { StringUtils.displayName(it.name) } ?: "Unknown",
                        dstAccountName = dstAcc?.let { StringUtils.displayName(it.name) } ?: "",
                        category = cat,
                        mediumName = med?.let { StringUtils.displayName(it.name) } ?: "",
                        symbol = symbol,
                        onClick = { viewingTxn = txn }
                    )
                }

                // Pagination Bar (matching Statement screen)
                if (totalPages > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassIconButton(
                                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous page",
                                onClick = {
                                    if (safePage > 1) {
                                        page = safePage - 1
                                        scope.launch { listState.animateScrollToItem(2) }
                                    }
                                },
                                modifier = Modifier.alpha(if (safePage > 1) 1f else 0.42f),
                                testTag = "transactions_prev_page_button"
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = "Page $safePage / $totalPages",
                                style = MaterialTheme.typography.titleSmall,
                                color = cs.onBackground
                            )
                            Spacer(Modifier.width(16.dp))
                            GlassIconButton(
                                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next page",
                                onClick = {
                                    if (safePage < totalPages) {
                                        page = safePage + 1
                                        scope.launch { listState.animateScrollToItem(2) }
                                    }
                                },
                                modifier = Modifier.alpha(if (safePage < totalPages) 1f else 0.42f),
                                testTag = "transactions_next_page_button"
                            )
                        }
                    }
                }
            }
        }
    }

    // View Details Sheet
    viewingTxn?.let { txn ->
        val srcAcc = accountsById[txn.accountId]
        val dstAcc = accountsById[txn.toAccountId]
        val cat = categoriesById[txn.categoryId]
        val med = mediumsById[txn.paymentMediumId]
        val involvesClosed = (srcAcc?.isClosed == true) || (dstAcc?.isClosed == true)

        ModalBottomSheet(
            onDismissRequest = { viewingTxn = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cs.surfaceContainerLow,
            contentColor = cs.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(txn.type.label, style = MaterialTheme.typography.labelLarge, color = cs.primary)
                    Text(StringUtils.formatDisplayDate(txn.date), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                }
                Text(
                    text = StringUtils.formatCurrency(txn.amount, symbol),
                    fontFamily = ledger.monoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = when (txn.type) {
                        TransactionType.INCOME -> ledger.income
                        TransactionType.EXPENSE -> ledger.expense
                        TransactionType.TRANSFER -> ledger.transfer
                    }
                )
                Text(txn.reason, style = MaterialTheme.typography.titleLarge)
                HorizontalDivider(color = cs.outlineVariant)

                DetailLine("Account", srcAcc?.let { StringUtils.displayName(it.name) + if (it.isClosed) " (Closed)" else "" } ?: "Unknown")
                if (txn.type == TransactionType.TRANSFER) {
                    DetailLine("To Account", dstAcc?.let { StringUtils.displayName(it.name) + if (it.isClosed) " (Closed)" else "" } ?: "Unknown")
                } else {
                    DetailLine("Category", cat?.let { StringUtils.displayName(it.name) } ?: "Uncategorized")
                }
                if (med != null) {
                    DetailLine("Payment Medium", "${StringUtils.displayName(med.name)} (${med.group.label})")
                }
                if (txn.notes.isNotBlank()) {
                    DetailLine("Notes", txn.notes)
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassTonalButton(
                        text = "Edit",
                        icon = Icons.Rounded.Edit,
                        enabled = !involvesClosed,
                        onClick = {
                            viewingTxn = null
                            editingTxn = txn
                            showFormSheet = true
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "txn_detail_edit_button"
                    )
                    GlassDangerButton(
                        text = "Delete",
                        icon = Icons.Rounded.DeleteOutline,
                        enabled = !(txn.type == TransactionType.TRANSFER && involvesClosed),
                        onClick = {
                            viewingTxn = null
                            deletingTxn = txn
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "txn_detail_delete_button"
                    )
                }
                if (involvesClosed) {
                    Text(
                        text = if (txn.type == TransactionType.TRANSFER)
                            "Transfers involving a closed account cannot be edited or deleted directly."
                        else
                            "Account is closed. Editing is locked; deleting requires an active settlement account.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Add / Edit Transaction Sheet
    if (showFormSheet) {
        TransactionFormSheet(
            editingTxn = editingTxn,
            defaults = formDefaults,
            activeAccounts = activeAccounts,
            activeCategories = categories.filter { !it.isDeleted },
            activeMediums = paymentMediums.filter { !it.isDeleted },
            currencySymbol = symbol,
            onScanReceipt = if (editingTxn == null) onScanReceipt else null,
            onDismiss = { showFormSheet = false },
            onSubmit = { type, amt, accId, toAccId, catId, payGrp, medId, reason, notes, date ->
                val existing = editingTxn
                if (existing == null) {
                    viewModel.addTransaction(
                        type, amt, accId, toAccId, catId, payGrp, medId, reason, notes, date
                    ) { showFormSheet = false }
                } else {
                    viewModel.updateTransaction(
                        existing.id, type, amt, accId, toAccId, catId, medId, reason, notes, date
                    ) { showFormSheet = false }
                }
            }
        )
    }

    // CAPTCHA Delete Dialog (with settlement account support for closed accounts)
    deletingTxn?.let { txn ->
        val srcClosed = accountsById[txn.accountId]?.isClosed == true
        CaptchaDeleteDialog(
            title = "Delete Transaction?",
            description = if (srcClosed) {
                "This transaction belongs to a closed account. Deleting it will create an automatic reversing transfer using your selected Settlement Account so the closed account remains balanced at 0.00."
            } else {
                "Deleting \"${txn.reason}\" (${StringUtils.formatCurrency(txn.amount, symbol)}) will recalculate account balances immediately."
            },
            requireSettlementAccount = srcClosed,
            activeAccounts = activeAccounts,
            onDismiss = { deletingTxn = null },
            onConfirm = { settleId ->
                viewModel.deleteTransaction(txn.id, settleId) {
                    deletingTxn = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    editingTxn: Transaction?,
    defaults: TransactionFormDefaults,
    activeAccounts: List<Account>,
    activeCategories: List<Category>,
    activeMediums: List<PaymentMedium>,
    currencySymbol: String,
    receiptDraft: UpiReceiptDraft? = null,
    onScanReceipt: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSubmit: (
        TransactionType, Double, String, String, String, PaymentGroup, String, String, String, String
    ) -> Unit
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    val draftKey = receiptDraft?.id

    var type by remember(editingTxn, draftKey) {
        mutableStateOf(editingTxn?.type ?: receiptDraft?.type ?: defaults.type)
    }
    var amountText by remember(editingTxn, draftKey) {
        val initialAmount = editingTxn?.amount ?: receiptDraft?.amount
        mutableStateOf(
            initialAmount?.let { amt ->
                if (amt % 1.0 == 0.0 && editingTxn == null) {
                    amt.toLong().toString()
                } else {
                    String.format(java.util.Locale.US, "%.2f", amt)
                }
            } ?: ""
        )
    }
    var date by remember(editingTxn, draftKey) {
        mutableStateOf(editingTxn?.date ?: receiptDraft?.dateIso ?: StringUtils.todayIso())
    }
    var accountId by remember(editingTxn, draftKey, activeAccounts) {
        val candidate = editingTxn?.accountId ?: receiptDraft?.matchedAccountId ?: defaults.accountId
        mutableStateOf(activeAccounts.firstOrNull { it.id == candidate }?.id ?: activeAccounts.firstOrNull()?.id.orEmpty())
    }
    var toAccountId by remember(editingTxn, draftKey, activeAccounts) {
        val candidate = editingTxn?.toAccountId ?: defaults.toAccountId
        mutableStateOf(
            activeAccounts.firstOrNull { it.id == candidate && it.id != accountId }?.id
                ?: activeAccounts.firstOrNull { it.id != accountId }?.id.orEmpty()
        )
    }

    val filteredCategories = remember(type, activeCategories) {
        val targetCatType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        activeCategories.filter { it.type == targetCatType }
    }
    var categoryId by remember(editingTxn, draftKey, type, filteredCategories) {
        val candidate = editingTxn?.categoryId ?: defaults.categoryId
        mutableStateOf(
            filteredCategories.firstOrNull { it.id == candidate }?.id
                ?: filteredCategories.firstOrNull()?.id.orEmpty()
        )
    }

    val initialMedium = remember(editingTxn, draftKey, activeMediums) {
        activeMediums.firstOrNull { it.id == (editingTxn?.paymentMediumId ?: defaults.paymentMediumId) }
    }
    var payGroup by remember(editingTxn, draftKey) {
        mutableStateOf(initialMedium?.group ?: defaults.paymentGroup)
    }
    val filteredMediums = remember(payGroup, activeMediums) {
        activeMediums.filter { it.group == payGroup }
    }
    var mediumId by remember(payGroup, filteredMediums, editingTxn, draftKey) {
        val candidate = editingTxn?.paymentMediumId ?: defaults.paymentMediumId
        mutableStateOf(
            filteredMediums.firstOrNull { it.id == candidate }?.id
                ?: filteredMediums.firstOrNull()?.id.orEmpty()
        )
    }

    var reason by remember(editingTxn, draftKey) {
        mutableStateOf(editingTxn?.reason ?: receiptDraft?.reason ?: "")
    }
    var notes by remember(editingTxn, draftKey) {
        mutableStateOf(editingTxn?.notes ?: receiptDraft?.notes ?: "")
    }
    var errorMsg by remember(draftKey) { mutableStateOf<String?>(null) }

    val headerTint = when (type) {
        TransactionType.EXPENSE -> ledger.expense
        TransactionType.INCOME -> ledger.income
        TransactionType.TRANSFER -> ledger.transfer
    }

    GlassFormBottomSheet(
        title = if (editingTxn == null) "Record Transaction" else "Edit Transaction",
        subtitle = if (editingTxn == null && receiptDraft != null) {
            "Prefilled offline from ${receiptDraft.detectedApp} receipt — review & save"
        } else {
            "Log income, expenses, or account transfers offline"
        },
        icon = Icons.Rounded.SwapHoriz,
        iconTint = headerTint,
        onDismiss = onDismiss
    ) {
        // UPI Receipt OCR Scan Trigger / Status Banner
        if (editingTxn == null && receiptDraft != null) {
            UpiReceiptStatusBanner(
                draft = receiptDraft,
                onRescan = onScanReceipt
            )
            if (!receiptDraft.possibleDuplicateMessage.isNullOrBlank()) {
                UpiDuplicateWarningBanner(message = receiptDraft.possibleDuplicateMessage)
            }
        } else if (editingTxn == null && onScanReceipt != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.primary.copy(alpha = 0.10f))
                    .border(1.dp, cs.primary.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onScanReceipt)
                    .testTag("txn_scan_upi_receipt_button")
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cs.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DocumentScanner,
                        contentDescription = "Scan UPI screenshot",
                        tint = cs.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan UPI Screenshot",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "Auto-fill from GPay, PhonePe, Paytm, BHIM • Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        SegmentedPillBar(
            options = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER),
            selected = type,
            onSelect = { type = it },
            labelOf = { it.label },
            colorOf = {
                when (it) {
                    TransactionType.EXPENSE -> ledger.expense
                    TransactionType.INCOME -> ledger.income
                    TransactionType.TRANSFER -> ledger.transfer
                }
            },
            testTagPrefix = "txn_form_type"
        )

        val amountTransform = remember { RegexInputTransformation(Regex("^\\d*\\.?\\d{0,2}$")) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                inputTransformation = amountTransform,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = glassTextFieldColors(),
                modifier = Modifier.weight(1f).testTag("txn_amount_input")
            )
                DatePickerField(
                    label = "Date",
                    isoDate = date,
                    onDateSelected = { date = it },
                    modifier = Modifier.weight(1f),
                    testTag = "txn_date_picker"
                )
            }

            GlassDropdownField(
                label = if (type == TransactionType.TRANSFER) "From Account" else "Account",
                items = activeAccounts,
                selectedItem = activeAccounts.firstOrNull { it.id == accountId },
                itemLabel = { acc ->
                    "${StringUtils.displayName(acc.name)} (${StringUtils.formatCompactCurrency(acc.currentBalance, currencySymbol)})"
                },
                onSelect = { accountId = it.id },
                placeholder = "Select active account",
                testTag = "txn_account_dropdown"
            )

            if (type == TransactionType.TRANSFER) {
                val destOptions = activeAccounts.filter { it.id != accountId }
                GlassDropdownField(
                    label = "To Account",
                    items = destOptions,
                    selectedItem = destOptions.firstOrNull { it.id == toAccountId },
                    itemLabel = { acc ->
                        "${StringUtils.displayName(acc.name)} (${StringUtils.formatCompactCurrency(acc.currentBalance, currencySymbol)})"
                    },
                    onSelect = { toAccountId = it.id },
                    placeholder = "Select destination account",
                    testTag = "txn_to_account_dropdown"
                )
            } else {
                GlassDropdownField(
                    label = "Category (${if (type == TransactionType.INCOME) "Income" else "Expense"})",
                    items = filteredCategories,
                    selectedItem = filteredCategories.firstOrNull { it.id == categoryId },
                    itemLabel = { StringUtils.displayName(it.name) },
                    itemColorHex = { it.color },
                    onSelect = { categoryId = it.id },
                    placeholder = "Select category",
                    testTag = "txn_category_dropdown"
                )
            }

            SegmentedPillBar(
                options = listOf(PaymentGroup.ONLINE, PaymentGroup.OFFLINE),
                selected = payGroup,
                onSelect = { payGroup = it },
                labelOf = { it.label },
                testTagPrefix = "txn_pay_group"
            )

            GlassDropdownField(
                label = "Payment Medium",
                items = filteredMediums,
                selectedItem = filteredMediums.firstOrNull { it.id == mediumId },
                itemLabel = { StringUtils.displayName(it.name) },
                onSelect = { mediumId = it.id },
                placeholder = "Select medium",
                testTag = "txn_medium_dropdown"
            )

            val reasonLimit = remember { MaxLengthTransformation(50) }
            GlassTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason (${reason.length}/50)") },
                singleLine = true,
                inputTransformation = reasonLimit,
                shape = RoundedCornerShape(16.dp),
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("txn_reason_input")
            )

            val notesLimit = remember { MaxLengthTransformation(1000) }
            GlassTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional, ${notes.length}/1000)") },
                minLines = 2,
                maxLines = 4,
                inputTransformation = notesLimit,
                shape = RoundedCornerShape(16.dp),
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("txn_notes_input")
            )

            if (errorMsg != null) {
                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = ledger.expense)
            }

            GlassPrimaryButton(
                text = if (editingTxn == null) "Save Transaction" else "Update Transaction",
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    when {
                        amt == null || amt <= 0.0 -> errorMsg = "Enter a valid amount greater than 0."
                        accountId.isBlank() -> errorMsg = "Create and select an active account first."
                        type == TransactionType.TRANSFER && toAccountId.isBlank() -> errorMsg = "Select a destination account."
                        type != TransactionType.TRANSFER && categoryId.isBlank() -> errorMsg = "Create and select a category first."
                        reason.isBlank() -> errorMsg = "Please enter a brief reason."
                        else -> {
                            errorMsg = null
                            onSubmit(type, amt, accountId, toAccountId, categoryId, payGroup, mediumId, reason, notes, date)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "txn_save_button"
            )
    }
}

@Composable
private fun TxnKpiMini(
    title: String,
    amount: Double,
    symbol: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        accentTint = color,
        contentPadding = PaddingValues(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        AnimatedCurrencyText(
            amount = amount,
            symbol = symbol,
            compact = true,
            color = color,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TransactionRowCard(
    txn: Transaction,
    srcAccountName: String,
    dstAccountName: String,
    category: Category?,
    mediumName: String,
    symbol: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val badgeColor = when (txn.type) {
        TransactionType.TRANSFER -> ledger.transfer
        else -> category?.let { parseHexColorSafe(it.color, cs.primary) } ?: cs.primary
    }
    val amountColor = when (txn.type) {
        TransactionType.INCOME -> ledger.income
        TransactionType.EXPENSE -> ledger.expense
        TransactionType.TRANSFER -> ledger.transfer
    }
    val sign = when (txn.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.18f))
                    .border(1.dp, badgeColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (txn.type == TransactionType.TRANSFER) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = "Transfer", tint = badgeColor, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = (category?.name?.firstOrNull() ?: txn.reason.firstOrNull() ?: 'T').uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.reason,
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val meta = if (txn.type == TransactionType.TRANSFER) {
                    "$srcAccountName → $dstAccountName"
                } else {
                    val catName = category?.let { StringUtils.displayName(it.name) } ?: "Uncategorized"
                    "$catName • $srcAccountName"
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(StringUtils.formatDisplayDate(txn.date))
                        if (mediumName.isNotBlank()) append(" • $mediumName")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$sign${StringUtils.formatCurrency(txn.amount, symbol)}",
                fontFamily = ledger.monoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = amountColor
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun UpiReceiptStatusBanner(
    draft: UpiReceiptDraft,
    onRescan: (() -> Unit)?
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val matchTint = if (draft.accountMatchedFromReceipt) ledger.income else cs.onSurfaceVariant
    val matchBg = if (draft.accountMatchedFromReceipt) ledger.incomeContainer else cs.surfaceContainerHigh

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceContainerHigh.copy(alpha = 0.72f))
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("upi_receipt_status_banner"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(cs.primary.copy(alpha = 0.18f))
                        .border(1.dp, cs.primary.copy(alpha = 0.40f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = draft.detectedApp,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                }
                Text(
                    text = "Offline OCR",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
            if (onRescan != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(cs.surfaceContainerHighest.copy(alpha = 0.85f))
                        .clickable(onClick = onRescan)
                        .testTag("txn_rescan_upi_receipt_button")
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Re-scan UPI screenshot",
                        tint = cs.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Re-scan",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(matchBg)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (draft.accountMatchedFromReceipt) Icons.Rounded.CheckCircle else Icons.Rounded.History,
                contentDescription = null,
                tint = matchTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (draft.accountMatchedFromReceipt && !draft.matchedAccountHint.isNullOrBlank()) {
                    "Matched account: ${draft.matchedAccountHint}"
                } else {
                    "Account not matched — using last transaction's account & defaults"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (draft.accountMatchedFromReceipt) matchTint else cs.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UpiDuplicateWarningBanner(message: String) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ledger.warningContainer)
            .border(1.dp, ledger.warning.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("upi_duplicate_warning_banner"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = "Duplicate transaction warning",
            tint = ledger.warning,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Possible Duplicate Receipt",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ledger.warning
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurface
            )
        }
    }
}
