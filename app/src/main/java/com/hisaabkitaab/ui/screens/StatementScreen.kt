package com.hisaabkitaab.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.Category
import com.hisaabkitaab.data.PaymentMedium
import com.hisaabkitaab.data.PeriodPreset
import com.hisaabkitaab.data.StatementImportPreview
import com.hisaabkitaab.data.StatementImportStatus
import com.hisaabkitaab.data.StatementReport
import com.hisaabkitaab.data.StatementRow
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.UserProfile
import com.hisaabkitaab.ui.theme.AnimatedCurrencyText
import com.hisaabkitaab.ui.theme.DatePickerField
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassDropdownField
import com.hisaabkitaab.ui.theme.GlassFormBottomSheet
import com.hisaabkitaab.ui.theme.GlassIconButton
import com.hisaabkitaab.ui.theme.GlassPrimaryButton
import com.hisaabkitaab.ui.theme.GlassTonalButton
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.parseHexColorSafe
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel

@Composable
fun StatementScreen(
    viewModel: HisaabViewModel,
    accounts: List<Account>,
    transactions: List<Transaction>,
    categories: List<Category>,
    paymentMediums: List<PaymentMedium>,
    profile: UserProfile,
    importPreview: StatementImportPreview?,
    onExportPdf: (StatementReport, Boolean) -> Unit,
    onSharePdf: (StatementReport, Boolean) -> Unit,
    onExportXlsx: (StatementReport) -> Unit,
    onExportCsv: (StatementReport) -> Unit,
    onImportSpreadsheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val symbol = profile.effectiveSymbol

    var selectedPreset by remember { mutableStateOf(PeriodPreset.THIS_MONTH) }
    var customStart by remember { mutableStateOf(StringUtils.daysAgoIso(29)) }
    var customEnd by remember { mutableStateOf(StringUtils.todayIso()) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(1) }

    val range = remember(selectedPreset, customStart, customEnd) {
        StringUtils.computePeriodRange(selectedPreset, customStart, customEnd)
    }

    val report = remember(accounts, transactions, categories, paymentMediums, range, selectedAccountId) {
        viewModel.buildStatementReport(range, selectedAccountId)
    }

    val pageSize = 25
    val totalPages = maxOf(1, (report.rows.size + pageSize - 1) / pageSize)
    LaunchedEffect(report.rows.size, range, selectedAccountId) {
        page = 1
    }
    val pagedRows = remember(report.rows, page) {
        val start = ((page - 1) * pageSize).coerceAtLeast(0)
        report.rows.drop(start).take(pageSize)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("statement_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Text("Statement Filters", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PeriodPreset.entries) { preset ->
                        val active = preset == selectedPreset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) cs.primary else cs.surfaceContainerHigh.copy(alpha = 0.7f))
                                .border(1.dp, if (active) cs.primary else cs.outlineVariant, RoundedCornerShape(999.dp))
                                .clickable { selectedPreset = preset }
                                .testTag("statement_period_${preset.name.lowercase()}")
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatePickerField(
                            label = "From",
                            isoDate = customStart,
                            onDateSelected = { customStart = it },
                            modifier = Modifier.weight(1f)
                        )
                        DatePickerField(
                            label = "To",
                            isoDate = customEnd,
                            onDateSelected = { customEnd = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                val filterOptions: List<Account?> = remember(accounts) { listOf(null) + accounts }
                GlassDropdownField(
                    label = "Account Scope",
                    items = filterOptions,
                    selectedItem = filterOptions.firstOrNull { it?.id == selectedAccountId },
                    itemLabel = { acc -> acc?.let { StringUtils.displayName(it.name) } ?: "All Accounts" },
                    onSelect = { selectedAccountId = it?.id },
                    testTag = "statement_account_filter"
                )
            }
        }

        // Export & Import Suite Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevated = true,
                accentTint = cs.primary,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Export & Import Suite",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )
                        Text(
                            text = "Share, archive, or bulk import ledger statements",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }

                    // Primary hero share action
                    GlassPrimaryButton(
                        text = "Share Statement PDF",
                        icon = Icons.Rounded.Share,
                        onClick = { onSharePdf(report, profile.watermark.isNotBlank()) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "statement_share_pdf_button"
                    )

                    // Structured 2x2 Export Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatementActionTile(
                                title = "Save PDF",
                                subtitle = "Standard",
                                icon = Icons.Rounded.PictureAsPdf,
                                accent = ledger.expense,
                                onClick = { onExportPdf(report, false) },
                                modifier = Modifier.weight(1f),
                                testTag = "statement_export_pdf_button"
                            )
                            StatementActionTile(
                                title = "Watermarked",
                                subtitle = "PDF stamp",
                                icon = Icons.Rounded.Verified,
                                accent = cs.primary,
                                onClick = { onExportPdf(report, true) },
                                modifier = Modifier.weight(1f),
                                testTag = "statement_export_watermarked_pdf_button"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatementActionTile(
                                title = "Excel",
                                subtitle = ".xlsx sheet",
                                icon = Icons.Rounded.TableChart,
                                accent = ledger.income,
                                onClick = { onExportXlsx(report) },
                                modifier = Modifier.weight(1f),
                                testTag = "statement_export_xlsx_button"
                            )
                            StatementActionTile(
                                title = "CSV",
                                subtitle = "Raw table",
                                icon = Icons.Rounded.Description,
                                accent = ledger.transfer,
                                onClick = { onExportCsv(report) },
                                modifier = Modifier.weight(1f),
                                testTag = "statement_export_csv_button"
                            )
                        }
                    }

                    HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))

                    // Dedicated Import Strip
                    StatementImportBannerRow(
                        onClick = onImportSpreadsheet,
                        testTag = "statement_import_excel_button"
                    )
                }
            }
        }

        // Period Summary Totals
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetric("Opening", report.totalOpeningBalance, symbol, cs.onSurface)
                    SummaryMetric("Credit", report.totalCredit, symbol, ledger.income)
                    SummaryMetric("Debit", report.totalDebit, symbol, ledger.expense)
                    SummaryMetric("Closing", report.totalClosingBalance, symbol, cs.primary)
                }
            }
        }

        // Per-Account Breakdown
        if (report.accountSummaries.isNotEmpty()) {
            item {
                Text(
                    "Account Balances (${report.accountSummaries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(report.accountSummaries, key = { it.accountId }) { acc ->
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = acc.accountName + if (acc.isClosed) " (Closed)" else "",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Open ${StringUtils.formatCompactCurrency(acc.openingBalanceBeforePeriod, symbol)}  •  +${StringUtils.formatCompactCurrency(acc.periodCredit, symbol)}  •  -${StringUtils.formatCompactCurrency(acc.periodDebit, symbol)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                        Text(
                            text = StringUtils.formatCurrency(acc.closingBalance, symbol),
                            fontFamily = ledger.monoFont,
                            fontWeight = FontWeight.Bold,
                            color = if (acc.closingBalance < 0) ledger.expense else cs.onSurface
                        )
                    }
                }
            }
        }

        // Ledger Header + Pagination Info
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ledger Entries (${report.rows.size})", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Page $page of $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant
                )
            }
        }

        if (pagedRows.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
                    Text(
                        "No transactions recorded between ${StringUtils.formatDisplayDate(range.start)} and ${StringUtils.formatDisplayDate(range.end)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        } else {
            items(pagedRows, key = { "${it.transactionId}_${it.serialNo}" }) { row ->
                StatementEntryCard(row = row, symbol = symbol)
            }

            // Pagination Bar
            if (totalPages > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous page",
                            onClick = { if (page > 1) page-- },
                            testTag = "statement_prev_page_button"
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("Page $page / $totalPages", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(16.dp))
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next page",
                            onClick = { if (page < totalPages) page++ },
                            testTag = "statement_next_page_button"
                        )
                    }
                }
            }
        }
    }

    // Spreadsheet Import Preview Dialog
    importPreview?.let { preview ->
        StatementImportPreviewDialog(
            preview = preview,
            symbol = symbol,
            onDismiss = { viewModel.dismissStatementImportPreview() },
            onConfirm = { viewModel.confirmStatementImport() }
        )
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    amount: Double,
    symbol: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        AnimatedCurrencyText(
            amount = amount,
            symbol = symbol,
            compact = true,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = color
        )
    }
}

@Composable
private fun StatementEntryCard(row: StatementRow, symbol: String) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val isCredit = row.credit > 0.0
    val amountColor = if (isCredit) ledger.income else ledger.expense

    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(parseHexColorSafe(row.categoryColor, cs.primary))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${row.serialNo}  ${row.reason}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${row.accountName} • ${row.categoryName} • ${row.paymentMediumName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = StringUtils.formatDisplayDate(row.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isCredit) "+${StringUtils.formatCurrency(row.credit, symbol)}"
                    else "-${StringUtils.formatCurrency(row.debit, symbol)}",
                    fontFamily = ledger.monoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = amountColor
                )
                Text(
                    text = "Bal ${StringUtils.formatCurrency(row.remainingBalance, symbol)}",
                    fontFamily = ledger.monoFont,
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatementImportPreviewDialog(
    preview: StatementImportPreview,
    symbol: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme

    GlassFormBottomSheet(
        title = "Import Statement Preview",
        subtitle = preview.fileName,
        icon = Icons.Rounded.UploadFile,
        iconTint = cs.primary,
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ledger.incomeContainer)
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text("${preview.validRows.size} Ready", style = MaterialTheme.typography.labelMedium, color = ledger.income)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.primary.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text("${preview.duplicateRows.size} Dupes", style = MaterialTheme.typography.labelMedium, color = cs.primary)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ledger.expenseContainer)
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text("${preview.invalidRows.size} Invalid", style = MaterialTheme.typography.labelMedium, color = ledger.expense)
            }
        }

        Column(
            modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            preview.rows.forEach { row ->
                val color = when (row.status) {
                    StatementImportStatus.VALID -> ledger.income
                    StatementImportStatus.DUPLICATE -> cs.primary
                    StatementImportStatus.INVALID -> ledger.expense
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceContainerHigh.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "#${row.rowNumber} ${row.reason}",
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            StringUtils.formatCurrency(row.amount, symbol),
                            fontFamily = ledger.monoFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )
                    }
                    Text(
                        "${row.date} • ${row.accountName} • ${row.statusReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassTonalButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            GlassPrimaryButton(
                text = "Import (${preview.validRows.size})",
                enabled = preview.validRows.isNotEmpty(),
                onClick = onConfirm,
                modifier = Modifier.weight(1.4f),
                testTag = "confirm_statement_import_button"
            )
        }
    }
}

@Composable
private fun StatementActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "exportTileScale"
    )
    val tileBg = when {
        ledger.isLiquidGlass && ledger.isDark -> ledger.glassSurfaceStrong
        ledger.isLiquidGlass -> cs.surfaceContainerHigh.copy(alpha = 0.78f)
        else -> cs.surfaceContainerHigh
    }
    val tileBorder = if (ledger.isLiquidGlass && ledger.isDark) ledger.glassBorderTop else cs.outlineVariant

    Row(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tileBg)
            .border(1.dp, tileBorder, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
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
    }
}

@Composable
private fun StatementImportBannerRow(
    onClick: () -> Unit,
    testTag: String
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "importBannerScale"
    )
    val rowBg = when {
        ledger.isLiquidGlass && ledger.isDark -> ledger.glassSurfaceStrong
        ledger.isLiquidGlass -> cs.surfaceContainerHigh.copy(alpha = 0.78f)
        else -> cs.surfaceContainerHigh
    }
    val rowBorder = if (ledger.isLiquidGlass && ledger.isDark) ledger.glassBorderTop else cs.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(cs.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.UploadFile,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Import Excel / CSV",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Bulk add transactions with row-by-row preview",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Open import picker",
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

