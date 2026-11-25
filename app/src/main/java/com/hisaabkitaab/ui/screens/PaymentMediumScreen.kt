package com.hisaabkitaab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import com.hisaabkitaab.data.PaymentGroup
import com.hisaabkitaab.data.PaymentMedium
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassDangerButton
import com.hisaabkitaab.ui.theme.GlassFormBottomSheet
import com.hisaabkitaab.ui.theme.GlassIconButton
import com.hisaabkitaab.ui.theme.GlassPrimaryButton
import com.hisaabkitaab.ui.theme.GlassSearchField
import com.hisaabkitaab.ui.theme.GlassTextField
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.SegmentedPillBar
import com.hisaabkitaab.ui.theme.SimpleConfirmDeleteDialog
import com.hisaabkitaab.ui.theme.glassTextFieldColors
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel

@Composable
fun PaymentMediumScreen(
    viewModel: HisaabViewModel,
    paymentMediums: List<PaymentMedium>,
    addTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    var searchQuery by remember { mutableStateOf("") }
    var groupFilter by remember { mutableStateOf<PaymentGroup?>(null) }
    var compactView by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingMedium by remember { mutableStateOf<PaymentMedium?>(null) }
    var pendingDeleteIds by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(addTrigger) {
        if (addTrigger > 0) {
            editingMedium = null
            showFormDialog = true
        }
    }

    val filtered = remember(paymentMediums, searchQuery, groupFilter) {
        val q = searchQuery.trim().lowercase()
        paymentMediums.filter { pm ->
            (groupFilter == null || pm.group == groupFilter) &&
                (q.isEmpty() || pm.name.lowercase().contains(q) || pm.group.label.lowercase().contains(q))
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("payment_medium_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 136.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search payment mediums…",
                        modifier = Modifier.weight(1f),
                        testTag = "medium_search_input"
                    )
                    GlassIconButton(
                        icon = if (compactView) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                        contentDescription = "Toggle list or card view",
                        onClick = { compactView = !compactView }
                    )
                }
            }

            item {
                val options: List<PaymentGroup?> = listOf(null, PaymentGroup.ONLINE, PaymentGroup.OFFLINE)
                SegmentedPillBar(
                    options = options,
                    selected = groupFilter,
                    onSelect = { groupFilter = it },
                    labelOf = { it?.label ?: "All (${paymentMediums.count { m -> !m.isDeleted }})" },
                    colorOf = {
                        when (it) {
                            PaymentGroup.ONLINE -> ledger.transfer
                            PaymentGroup.OFFLINE -> ledger.income
                            null -> cs.primary
                        }
                    },
                    testTagPrefix = "pm_filter"
                )
            }

            items(filtered, key = { it.id }) { pm ->
                val isSelected = pm.id in selectedIds
                val badgeColor = if (pm.group == PaymentGroup.ONLINE) ledger.transfer else ledger.income

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (pm.isDeleted) 0.58f else 1f),
                    accentTint = if (isSelected) cs.primary else null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = if (compactView) 8.dp else 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!pm.isDeleted) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + pm.id else selectedIds - pm.id
                                }
                            )
                        } else {
                            Spacer(Modifier.width(12.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (pm.group == PaymentGroup.ONLINE) Icons.Rounded.Wifi else Icons.Rounded.Payments,
                                contentDescription = pm.group.label,
                                tint = badgeColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = StringUtils.displayName(pm.name),
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (pm.isDeleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(pm.group.label, style = MaterialTheme.typography.labelSmall, color = badgeColor)
                                if (pm.isDeleted) {
                                    Text("Archived", style = MaterialTheme.typography.labelSmall, color = ledger.expense)
                                }
                            }
                        }

                        if (!pm.isDeleted) {
                            IconButton(
                                onClick = {
                                    editingMedium = pm
                                    showFormDialog = true
                                }
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit payment medium")
                            }
                            IconButton(onClick = { pendingDeleteIds = listOf(pm.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete payment medium", tint = ledger.expense)
                            }
                        }
                    }
                }
            }
        }

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
                            testTag = "medium_bulk_delete_button"
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        PaymentMediumFormDialog(
            editing = editingMedium,
            onDismiss = { showFormDialog = false },
            onSave = { name, group ->
                val ex = editingMedium
                if (ex == null) {
                    viewModel.addPaymentMedium(name, group) { showFormDialog = false }
                } else {
                    viewModel.updatePaymentMedium(ex.id, name, group) { showFormDialog = false }
                }
            }
        )
    }

    pendingDeleteIds?.let { ids ->
        SimpleConfirmDeleteDialog(
            title = if (ids.size == 1) "Delete Payment Medium?" else "Delete ${ids.size} Mediums?",
            description = "Mediums used in existing transactions will be archived; unused mediums will be permanently deleted.",
            onDismiss = { pendingDeleteIds = null },
            onConfirm = {
                viewModel.deletePaymentMediums(ids) {
                    pendingDeleteIds = null
                    selectedIds = selectedIds - ids.toSet()
                }
            }
        )
    }
}

@Composable
private fun PaymentMediumFormDialog(
    editing: PaymentMedium?,
    onDismiss: () -> Unit,
    onSave: (String, PaymentGroup) -> Unit
) {
    val ledger = LocalLedgerColors.current
    var name by remember(editing) { mutableStateOf(editing?.let { StringUtils.displayName(it.name) } ?: "") }
    var group by remember(editing) { mutableStateOf(editing?.group ?: PaymentGroup.ONLINE) }
    var error by remember { mutableStateOf<String?>(null) }

    GlassFormBottomSheet(
        title = if (editing == null) "Add Payment Medium" else "Edit Payment Medium",
        subtitle = "Classify digital transfers and offline cash methods",
        icon = if (group == PaymentGroup.ONLINE) Icons.Rounded.CreditCard else Icons.Rounded.Payments,
        iconTint = if (group == PaymentGroup.ONLINE) ledger.transfer else ledger.income,
        onDismiss = onDismiss
    ) {
        SegmentedPillBar(
            options = listOf(PaymentGroup.ONLINE, PaymentGroup.OFFLINE),
            selected = group,
            onSelect = { group = it },
            labelOf = { it.label },
            colorOf = { if (it == PaymentGroup.ONLINE) ledger.transfer else ledger.income },
            testTagPrefix = "pm_form_group"
        )
        GlassTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Medium Name") },
            placeholder = { Text("e.g. UPI, Cash, NEFT, Credit Card") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("medium_name_input")
        )
        if (error != null) {
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = ledger.expense)
        }
        Spacer(Modifier.height(2.dp))
        GlassPrimaryButton(
            text = if (editing == null) "Add Payment Medium" else "Update Payment Medium",
            onClick = {
                if (name.isBlank()) error = "Medium name is required."
                else onSave(name, group)
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "medium_save_button"
        )
    }
}
