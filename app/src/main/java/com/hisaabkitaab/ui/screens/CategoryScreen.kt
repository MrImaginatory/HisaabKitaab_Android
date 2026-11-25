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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hisaabkitaab.data.Category
import com.hisaabkitaab.data.CategoryCsvPreview
import com.hisaabkitaab.data.CategoryCsvRow
import com.hisaabkitaab.data.CategoryCsvStatus
import com.hisaabkitaab.data.CategoryType
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.ui.theme.ColorSwatchSelector
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassDangerButton
import com.hisaabkitaab.ui.theme.GlassFormBottomSheet
import com.hisaabkitaab.ui.theme.GlassIconButton
import com.hisaabkitaab.ui.theme.GlassPrimaryButton
import com.hisaabkitaab.ui.theme.GlassSearchField
import com.hisaabkitaab.ui.theme.GlassTextField
import com.hisaabkitaab.ui.theme.GlassTonalButton
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.SegmentedPillBar
import com.hisaabkitaab.ui.theme.SimpleConfirmDeleteDialog
import com.hisaabkitaab.ui.theme.glassTextFieldColors
import com.hisaabkitaab.ui.theme.parseHexColorSafe
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel

@Composable
fun CategoryScreen(
    viewModel: HisaabViewModel,
    categories: List<Category>,
    csvPreview: CategoryCsvPreview?,
    addTrigger: Int = 0,
    onRequestImportCsv: () -> Unit,
    onRequestSampleCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<CategoryType?>(null) }
    var compactView by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var pendingDeleteIds by remember { mutableStateOf<List<String>?>(null) }
    var skippedSummaryRows by remember { mutableStateOf<List<CategoryCsvRow>?>(null) }

    LaunchedEffect(addTrigger) {
        if (addTrigger > 0) {
            editingCategory = null
            showFormDialog = true
        }
    }

    val filtered = remember(categories, searchQuery, typeFilter) {
        val q = searchQuery.trim().lowercase()
        categories.filter { cat ->
            (typeFilter == null || cat.type == typeFilter) &&
                (q.isEmpty() || cat.name.lowercase().contains(q) || cat.type.label.lowercase().contains(q))
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("category_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 136.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CSV Actions + View Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassTonalButton(
                        text = "Import CSV",
                        icon = Icons.Rounded.UploadFile,
                        onClick = onRequestImportCsv,
                        modifier = Modifier.weight(1f),
                        testTag = "category_import_csv_button"
                    )
                    GlassTonalButton(
                        text = "Sample",
                        icon = Icons.Rounded.Download,
                        onClick = onRequestSampleCsv,
                        modifier = Modifier.weight(1f),
                        testTag = "category_sample_csv_button"
                    )
                    GlassIconButton(
                        icon = if (compactView) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                        contentDescription = "Toggle list or card view",
                        onClick = { compactView = !compactView },
                        testTag = "category_toggle_view_button"
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search categories…",
                        testTag = "category_search_input"
                    )
                    val options: List<CategoryType?> = listOf(null, CategoryType.EXPENSE, CategoryType.INCOME)
                    SegmentedPillBar(
                        options = options,
                        selected = typeFilter,
                        onSelect = { typeFilter = it },
                        labelOf = { it?.label ?: "All (${categories.count { c -> !c.isDeleted }})" },
                        colorOf = {
                            when (it) {
                                CategoryType.INCOME -> ledger.income
                                CategoryType.EXPENSE -> ledger.expense
                                null -> cs.primary
                            }
                        },
                        testTagPrefix = "cat_filter"
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(28.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No categories yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Add Income & Expense categories manually or bulk-import from a CSV file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { cat ->
                    val isSelected = cat.id in selectedIds
                    val color = parseHexColorSafe(cat.color, cs.primary)

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (cat.isDeleted) 0.58f else 1f),
                        accentTint = if (isSelected) cs.primary else null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = if (compactView) 8.dp else 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!cat.isDeleted) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + cat.id else selectedIds - cat.id
                                    }
                                )
                            } else {
                                Spacer(Modifier.width(12.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, cs.outlineVariant, CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = StringUtils.displayName(cat.name),
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (cat.isDeleted) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cat.type.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cat.type == CategoryType.INCOME) ledger.income else ledger.expense
                                    )
                                    Text(
                                        text = cat.color.uppercase(),
                                        fontFamily = ledger.monoFont,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSurfaceVariant
                                    )
                                    if (cat.isDeleted) {
                                        Text(
                                            text = "Archived",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ledger.expense
                                        )
                                    }
                                }
                            }
                            if (!cat.isDeleted) {
                                IconButton(
                                    onClick = {
                                        editingCategory = cat
                                        showFormDialog = true
                                    }
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit category")
                                }
                                IconButton(onClick = { pendingDeleteIds = listOf(cat.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete category", tint = ledger.expense)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Multi-select bulk delete bar
        if (selectedIds.isNotEmpty()) {
            GlassCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 84.dp)
                    .fillMaxWidth(),
                elevated = true,
                accentTint = ledger.expense,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedIds.size} selected", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { selectedIds = emptySet() }) { Text("Clear") }
                        GlassDangerButton(
                            text = "Delete (${selectedIds.size})",
                            icon = Icons.Rounded.DeleteOutline,
                            onClick = { pendingDeleteIds = selectedIds.toList() },
                            testTag = "category_bulk_delete_button"
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        CategoryFormDialog(
            editing = editingCategory,
            onDismiss = { showFormDialog = false },
            onSave = { name, type, hex ->
                val ex = editingCategory
                if (ex == null) {
                    viewModel.addCategory(name, type, hex) { showFormDialog = false }
                } else {
                    viewModel.updateCategory(ex.id, name, type, hex) { showFormDialog = false }
                }
            }
        )
    }

    pendingDeleteIds?.let { ids ->
        SimpleConfirmDeleteDialog(
            title = if (ids.size == 1) "Delete Category?" else "Delete ${ids.size} Categories?",
            description = "Categories referenced by existing transactions will be archived (hidden from new pickers); unused categories will be permanently removed.",
            onDismiss = { pendingDeleteIds = null },
            onConfirm = {
                viewModel.deleteCategories(ids) {
                    pendingDeleteIds = null
                    selectedIds = selectedIds - ids.toSet()
                }
            }
        )
    }

    csvPreview?.let { preview ->
        CategoryCsvPreviewDialog(
            preview = preview,
            onDismiss = { viewModel.dismissCategoryCsvPreview() },
            onConfirmImport = {
                viewModel.confirmCategoryCsvImport { completed ->
                    if (completed.skippedRows.isNotEmpty()) {
                        skippedSummaryRows = completed.skippedRows
                    }
                }
            }
        )
    }

    skippedSummaryRows?.let { skipped ->
        GlassFormBottomSheet(
            title = "Skipped Rows (${skipped.size})",
            subtitle = "These rows were ignored during CSV import",
            icon = Icons.Rounded.WarningAmber,
            iconTint = ledger.expense,
            onDismiss = { skippedSummaryRows = null }
        ) {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skipped.forEach { row ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cs.surfaceContainerHigh.copy(alpha = 0.6f))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Row ${row.rowNumber}: ${row.rawName.ifBlank { "(blank)" }}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                        Text(
                            row.statusReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = ledger.expense
                        )
                    }
                }
            }
            GlassPrimaryButton(
                text = "Done",
                onClick = { skippedSummaryRows = null },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryFormDialog(
    editing: Category?,
    onDismiss: () -> Unit,
    onSave: (String, CategoryType, String) -> Unit
) {
    val ledger = LocalLedgerColors.current
    var name by remember(editing) { mutableStateOf(editing?.let { StringUtils.displayName(it.name) } ?: "") }
    var type by remember(editing) { mutableStateOf(editing?.type ?: CategoryType.EXPENSE) }
    var colorHex by remember(editing) { mutableStateOf(editing?.color ?: "#F97316") }
    var error by remember { mutableStateOf<String?>(null) }

    GlassFormBottomSheet(
        title = if (editing == null) "Add Category" else "Edit Category",
        subtitle = "Organize income & expense entries with custom color tags",
        icon = Icons.Rounded.Category,
        iconTint = parseHexColorSafe(colorHex),
        onDismiss = onDismiss
    ) {
        SegmentedPillBar(
            options = listOf(CategoryType.EXPENSE, CategoryType.INCOME),
            selected = type,
            onSelect = { type = it },
            labelOf = { it.label },
            colorOf = { if (it == CategoryType.INCOME) ledger.income else ledger.expense },
            testTagPrefix = "cat_form_type"
        )
        GlassTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category Name") },
            placeholder = { Text("e.g. Groceries, Salary, Travel") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("category_name_input")
        )
        Text(
            text = "Tag Color",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ColorSwatchSelector(
            selectedHex = colorHex,
            onHexSelected = { colorHex = it }
        )
        if (error != null) {
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = ledger.expense)
        }
        Spacer(Modifier.height(2.dp))
        GlassPrimaryButton(
            text = if (editing == null) "Add Category" else "Update Category",
            onClick = {
                if (name.isBlank()) error = "Category name is required."
                else onSave(name, type, colorHex)
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "category_save_button"
        )
    }
}

@Composable
private fun CategoryCsvPreviewDialog(
    preview: CategoryCsvPreview,
    onDismiss: () -> Unit,
    onConfirmImport: () -> Unit
) {
    val ledger = LocalLedgerColors.current
    val cs = MaterialTheme.colorScheme
    val validCount = preview.validRows.size
    val skippedCount = preview.skippedRows.size

    GlassFormBottomSheet(
        title = "CSV Import Preview",
        subtitle = "${preview.fileName} • $validCount ready, $skippedCount skipped",
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
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("$validCount Ready", style = MaterialTheme.typography.labelLarge, color = ledger.income)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ledger.expenseContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("$skippedCount Skipped", style = MaterialTheme.typography.labelLarge, color = ledger.expense)
            }
        }

        Column(
            modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            preview.rows.forEach { row ->
                val statusColor = when (row.status) {
                    CategoryCsvStatus.VALID -> ledger.income
                    CategoryCsvStatus.DUPLICATE_DB, CategoryCsvStatus.DUPLICATE_CSV -> cs.primary
                    CategoryCsvStatus.INVALID -> ledger.expense
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceContainerHigh.copy(alpha = 0.6f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseHexColorSafe(row.colorHex))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${row.rowNumber} ${StringUtils.displayName(row.rawName).ifBlank { "(blank)" }}",
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSurface
                        )
                        Text(
                            text = "${row.type?.label ?: "Invalid"} • ${row.statusReason}",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
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
                text = "Import ($validCount)",
                enabled = validCount > 0,
                onClick = onConfirmImport,
                modifier = Modifier.weight(1.4f),
                testTag = "confirm_csv_import_button"
            )
        }
    }
}
