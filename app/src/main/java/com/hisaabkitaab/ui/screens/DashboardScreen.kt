package com.hisaabkitaab.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.Category
import com.hisaabkitaab.data.PaymentMedium
import com.hisaabkitaab.data.PeriodPreset
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.TransactionType
import com.hisaabkitaab.data.UserProfile
import com.hisaabkitaab.ui.components.IncomeExpenseLineChart
import com.hisaabkitaab.ui.components.LedgerDonutChart
import com.hisaabkitaab.ui.theme.AnimatedCurrencyText
import com.hisaabkitaab.data.ChartPalette
import com.hisaabkitaab.ui.theme.DatePickerField
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassDropdownField
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.SegmentedPillBar
import com.hisaabkitaab.ui.theme.parseHexColorSafe
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel

@Composable
fun DashboardScreen(
    viewModel: HisaabViewModel,
    accounts: List<Account>,
    transactions: List<Transaction>,
    categories: List<Category>,
    paymentMediums: List<PaymentMedium>,
    profile: UserProfile,
    chartPalette: ChartPalette = ChartPalette.VIBRANT_JEWEL,
    onOpenTransactions: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val symbol = profile.effectiveSymbol

    var selectedPreset by remember { mutableStateOf(PeriodPreset.THIS_MONTH) }
    var customStart by remember { mutableStateOf(StringUtils.daysAgoIso(29)) }
    var customEnd by remember { mutableStateOf(StringUtils.todayIso()) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var donutMode by remember { mutableStateOf("Category") }

    val range = remember(selectedPreset, customStart, customEnd) {
        StringUtils.computePeriodRange(selectedPreset, customStart, customEnd)
    }

    val activeAccounts = remember(accounts) { accounts.filter { !it.isClosed } }
    val activeCategoryCount = remember(categories) { categories.count { !it.isDeleted } }
    val netBalance = remember(activeAccounts) { activeAccounts.sumOf { it.currentBalance } }
    val totalIncomeAll = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpenseAll = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val netFlowAll = totalIncomeAll - totalExpenseAll

    val categorySlices = remember(transactions, categories, range, selectedAccountId, chartPalette) {
        viewModel.computeExpenseByCategory(range, selectedAccountId, chartPalette)
    }
    val mediumSlices = remember(transactions, paymentMediums, range, selectedAccountId, chartPalette) {
        viewModel.computeExpenseByMedium(range, selectedAccountId, chartPalette)
    }
    val dailyPoints = remember(transactions, range, selectedAccountId) {
        viewModel.computeDailyFlow(range, selectedAccountId)
    }
    val chartIncomeColor = remember(chartPalette, ledger.income) {
        parseHexColorSafe(chartPalette.incomeHex, ledger.income)
    }
    val chartExpenseColor = remember(chartPalette, ledger.expense) {
        parseHexColorSafe(chartPalette.expenseHex, ledger.expense)
    }

    val cutoff15Days = remember { StringUtils.daysAgoIso(14) }
    val recentTransactions = remember(transactions, cutoff15Days) {
        transactions.filter { it.date >= cutoff15Days }.take(8)
    }
    val recentIncome = remember(transactions, cutoff15Days) {
        transactions.filter { it.type == TransactionType.INCOME && it.date >= cutoff15Days }.take(6)
    }

    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Expressive Asymmetric Hero Net Worth Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevated = true,
                accentTint = cs.primary,
                contentPadding = PaddingValues(22.dp),
                onClick = onOpenAccounts
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (profile.name.isNotBlank()) "Hello, ${profile.name}" else "Total Net Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        AnimatedCurrencyText(
                            amount = netBalance,
                            symbol = symbol,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netBalance < 0) ledger.expense else cs.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${activeAccounts.size} active ${if (activeAccounts.size == 1) "account" else "accounts"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cs.primary.copy(alpha = 0.20f))
                            .border(1.dp, cs.primary.copy(alpha = 0.40f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = "Accounts overview",
                            tint = cs.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                HeroCashflowRatioBar(
                    inflow = totalIncomeAll,
                    outflow = totalExpenseAll,
                    entriesCount = transactions.size,
                    categoriesCount = activeCategoryCount,
                    symbol = symbol
                )
            }
        }

        // Unified Single-Line 4-KPI Strip
        item {
            KpiSingleLineStrip(
                netFlow = netFlowAll,
                activeAccountsCount = activeAccounts.size,
                entriesCount = transactions.size,
                categoriesCount = activeCategoryCount,
                symbol = symbol,
                onOpenAccounts = onOpenAccounts,
                onOpenTransactions = onOpenTransactions
            )
        }

        // Spend Analytics Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    text = "Spend Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${StringUtils.formatDisplayDate(range.start)} — ${StringUtils.formatDisplayDate(range.end)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Period Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PeriodPreset.entries) { preset ->
                        val active = preset == selectedPreset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (active) cs.primary else cs.surfaceContainerHigh.copy(alpha = 0.7f)
                                )
                                .border(
                                    1.dp,
                                    if (active) cs.primary else cs.outlineVariant,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable { selectedPreset = preset }
                                .testTag("dashboard_period_${preset.name.lowercase()}")
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) cs.onPrimary else cs.onSurface
                            )
                        }
                    }
                }

                if (selectedPreset == PeriodPreset.CUSTOM) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DatePickerField(
                            label = "Start",
                            isoDate = customStart,
                            onDateSelected = { customStart = it },
                            modifier = Modifier.weight(1f)
                        )
                        DatePickerField(
                            label = "End",
                            isoDate = customEnd,
                            onDateSelected = { customEnd = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Account Filter
                val filterOptions: List<Account?> = remember(accounts) { listOf(null) + accounts }
                GlassDropdownField(
                    label = "Filter by Account",
                    items = filterOptions,
                    selectedItem = filterOptions.firstOrNull { it?.id == selectedAccountId },
                    itemLabel = { acc ->
                        acc?.let { StringUtils.displayName(it.name) } ?: "All Accounts"
                    },
                    onSelect = { selectedAccountId = it?.id },
                    testTag = "dashboard_account_filter"
                )

                Spacer(Modifier.height(16.dp))

                SegmentedPillBar(
                    options = listOf("Category", "Payment Medium"),
                    selected = donutMode,
                    onSelect = { donutMode = it },
                    labelOf = { "By $it" },
                    testTagPrefix = "donut_mode"
                )

                Spacer(Modifier.height(14.dp))

                LedgerDonutChart(
                    title = if (donutMode == "Category") "Expenses by Category" else "Expenses by Payment Medium",
                    slices = if (donutMode == "Category") categorySlices else mediumSlices,
                    currencySymbol = symbol,
                    itemSingular = if (donutMode == "Category") "category" else "method",
                    itemPlural = if (donutMode == "Category") "categories" else "methods"
                )

                Spacer(Modifier.height(20.dp))

                IncomeExpenseLineChart(
                    points = dailyPoints,
                    currencySymbol = symbol,
                    incomeColor = chartIncomeColor,
                    expenseColor = chartExpenseColor
                )
            }
        }

        // Recent Income (15 Days)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentTint = ledger.income,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Recent Income", style = MaterialTheme.typography.titleLarge)
                        Text("Last 15 days", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                    Text(
                        text = StringUtils.formatCurrency(recentIncome.sumOf { it.amount }, symbol, showPositiveSign = true),
                        fontFamily = ledger.monoFont,
                        fontWeight = FontWeight.Bold,
                        color = ledger.income
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (recentIncome.isEmpty()) {
                    Text(
                        "No income recorded in the last 15 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentIncome.forEach { txn ->
                            CompactTxnRow(
                                txn = txn,
                                accountName = accountsById[txn.accountId]?.let { StringUtils.displayName(it.name) } ?: "Account",
                                toAccountName = accountsById[txn.toAccountId]?.let { StringUtils.displayName(it.name) } ?: "",
                                category = categoriesById[txn.categoryId],
                                symbol = symbol
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions (15 Days)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Recent Transactions", style = MaterialTheme.typography.titleLarge)
                        Text("Last 15 days", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onOpenTransactions)
                            .testTag("dashboard_view_all_txns")
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("View All", style = MaterialTheme.typography.labelLarge, color = cs.primary)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "View all transactions",
                            tint = cs.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (recentTransactions.isEmpty()) {
                    Text(
                        "No transactions recorded in the last 15 days. Tap + on Transactions to log your first entry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentTransactions.forEach { txn ->
                            CompactTxnRow(
                                txn = txn,
                                accountName = accountsById[txn.accountId]?.let { StringUtils.displayName(it.name) } ?: "Account",
                                toAccountName = accountsById[txn.toAccountId]?.let { StringUtils.displayName(it.name) } ?: "",
                                category = categoriesById[txn.categoryId],
                                symbol = symbol
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCashflowRatioBar(
    inflow: Double,
    outflow: Double,
    entriesCount: Int,
    categoriesCount: Int,
    symbol: String,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val totalFlow = inflow + outflow
    val rawInflowFraction = if (totalFlow > 0.0) (inflow / totalFlow).toFloat().coerceIn(0f, 1f) else 0f
    val inflowPct = (rawInflowFraction * 100f).roundToInt()

    val animatedInflowFraction by animateFloatAsState(
        targetValue = rawInflowFraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "cashflowRatio"
    )

    val containerBg = if (ledger.isDark) {
        cs.surfaceContainerHigh.copy(alpha = 0.55f)
    } else {
        cs.surfaceContainerLowest.copy(alpha = 0.85f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerBg)
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .testTag("hero_cashflow_ratio_bar")
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Cashflow Ratio (entries • categories) + XX% IN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cashflow Ratio ($entriesCount entries • $categoriesCount categories)",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$inflowPct% IN",
                fontFamily = ledger.monoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = if (totalFlow > 0.0) ledger.income else cs.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Split horizontal pill progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(cs.outlineVariant.copy(alpha = 0.35f))
        ) {
            if (totalFlow > 0.0) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (animatedInflowFraction > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(animatedInflowFraction.coerceAtLeast(0.001f))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            lerp(ledger.income, Color.White, 0.14f),
                                            ledger.income
                                        )
                                    )
                                )
                        )
                    }
                    val outflowFraction = (1f - animatedInflowFraction).coerceAtLeast(0f)
                    if (outflowFraction > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(outflowFraction.coerceAtLeast(0.001f))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            ledger.expense,
                                            lerp(ledger.expense, Color.Black, 0.10f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Footer: Inflow (+amount) & Outflow (-amount)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Inflow",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(1.dp))
                AnimatedCurrencyText(
                    amount = inflow,
                    symbol = symbol,
                    compact = true,
                    prefix = if (inflow > 0.0) "+" else "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ledger.income
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Outflow",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(1.dp))
                AnimatedCurrencyText(
                    amount = outflow,
                    symbol = symbol,
                    compact = true,
                    prefix = if (outflow > 0.0) "-" else "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ledger.expense
                )
            }
        }
    }
}

@Composable
private fun KpiSingleLineStrip(
    netFlow: Double,
    activeAccountsCount: Int,
    entriesCount: Int,
    categoriesCount: Int,
    symbol: String,
    onOpenAccounts: () -> Unit,
    onOpenTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val netFlowAccent = if (netFlow >= 0) ledger.income else ledger.expense

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_kpi_strip"),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KpiStripCell(
                label = "Net Flow",
                icon = Icons.Rounded.TrendingUp,
                accent = netFlowAccent,
                modifier = Modifier.weight(1.18f),
                testTag = "kpi_net_flow"
            ) {
                AnimatedCurrencyText(
                    amount = netFlow,
                    symbol = symbol,
                    compact = true,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
            }

            KpiStripDivider()

            KpiStripCell(
                label = "Accounts",
                icon = Icons.Rounded.AccountBalanceWallet,
                accent = cs.primary,
                onClick = onOpenAccounts,
                modifier = Modifier.weight(0.94f),
                testTag = "kpi_accounts"
            ) {
                Text(
                    text = "$activeAccountsCount",
                    fontFamily = ledger.monoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            KpiStripDivider()

            KpiStripCell(
                label = "Entries",
                icon = Icons.Rounded.ReceiptLong,
                accent = ledger.transfer,
                onClick = onOpenTransactions,
                modifier = Modifier.weight(0.94f),
                testTag = "kpi_entries"
            ) {
                Text(
                    text = "$entriesCount",
                    fontFamily = ledger.monoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            KpiStripDivider()

            KpiStripCell(
                label = "Categories",
                icon = Icons.Rounded.Category,
                accent = cs.secondary,
                modifier = Modifier.weight(0.94f),
                testTag = "kpi_categories"
            ) {
                Text(
                    text = "$categoriesCount",
                    fontFamily = ledger.monoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun KpiStripDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(0.62f)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    )
}

@Composable
private fun KpiStripCell(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    valueContent: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val clickMod = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .then(clickMod)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        valueContent()
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompactTxnRow(
    txn: Transaction,
    accountName: String,
    toAccountName: String,
    category: Category?,
    symbol: String
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val dotColor = when (txn.type) {
        TransactionType.TRANSFER -> ledger.transfer
        else -> category?.let { parseHexColorSafe(it.color, cs.primary) } ?: cs.primary
    }
    val amountColor = when (txn.type) {
        TransactionType.INCOME -> ledger.income
        TransactionType.EXPENSE -> ledger.expense
        TransactionType.TRANSFER -> ledger.transfer
    }
    val prefix = when (txn.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> "⇄ "
    }
    val subtitle = if (txn.type == TransactionType.TRANSFER) {
        "$accountName → $toAccountName • ${StringUtils.formatShortDate(txn.date)}"
    } else {
        "$accountName • ${category?.let { StringUtils.displayName(it.name) } ?: "General"} • ${StringUtils.formatShortDate(txn.date)}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceContainerHigh.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.reason.ifBlank { "Transaction" },
                style = MaterialTheme.typography.titleSmall,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$prefix${StringUtils.formatCurrency(txn.amount, symbol)}",
            fontFamily = ledger.monoFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = amountColor
        )
    }
}
