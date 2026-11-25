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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.UserProfile
import com.hisaabkitaab.ui.theme.AnimatedCurrencyText
import com.hisaabkitaab.ui.theme.CaptchaDeleteDialog
import com.hisaabkitaab.ui.theme.DatePickerField
import com.hisaabkitaab.ui.theme.DigitsMaxLengthTransformation
import com.hisaabkitaab.ui.theme.ExpressiveEmptyState
import com.hisaabkitaab.ui.theme.GlassCard
import com.hisaabkitaab.ui.theme.GlassDangerButton
import com.hisaabkitaab.ui.theme.GlassFormBottomSheet
import com.hisaabkitaab.ui.theme.GlassIconButton
import com.hisaabkitaab.ui.theme.GlassPrimaryButton
import com.hisaabkitaab.ui.theme.GlassSearchField
import com.hisaabkitaab.ui.theme.GlassTextField
import com.hisaabkitaab.ui.theme.GlassTonalButton
import com.hisaabkitaab.ui.theme.LocalLedgerColors
import com.hisaabkitaab.ui.theme.RegexInputTransformation
import com.hisaabkitaab.ui.theme.glassTextFieldColors
import com.hisaabkitaab.ui.viewmodel.HisaabViewModel

@Composable
fun AccountsScreen(
    viewModel: HisaabViewModel,
    accounts: List<Account>,
    profile: UserProfile,
    addTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    val symbol = profile.effectiveSymbol

    var searchQuery by remember { mutableStateOf("") }
    var isCompactList by remember { mutableStateOf(false) }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var closingAccount by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(addTrigger) {
        if (addTrigger > 0) {
            editingAccount = null
            showFormDialog = true
        }
    }

    val activeTotal = remember(accounts) {
        accounts.filter { !it.isClosed }.sumOf { it.currentBalance }
    }

    val filtered = remember(accounts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) accounts
        else accounts.filter {
            it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.accountNumber.contains(q)
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("accounts_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 126.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Active Balance Header Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    elevated = true,
                    accentTint = cs.primary,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Combined Active Balance", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            AnimatedCurrencyText(
                                amount = activeTotal,
                                symbol = symbol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 27.sp,
                                color = if (activeTotal < 0) ledger.expense else cs.onSurface
                            )
                        }
                        GlassIconButton(
                            icon = if (isCompactList) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                            contentDescription = "Toggle List or Card View",
                            onClick = { isCompactList = !isCompactList },
                            testTag = "accounts_toggle_view_button"
                        )
                    }
                }
            }

            item {
                GlassSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search accounts or last 6 digits…",
                    testTag = "accounts_search_input"
                )
            }

            if (filtered.isEmpty()) {
                item {
                    ExpressiveEmptyState(
                        icon = Icons.Rounded.AccountBalance,
                        title = "No accounts yet",
                        subtitle = "Tap the + button below to add bank accounts, wallets, credit cards, or cash ledgers."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { acc ->
                    AccountItemCard(
                        account = acc,
                        symbol = symbol,
                        compact = isCompactList,
                        onEdit = {
                            editingAccount = acc
                            showFormDialog = true
                        },
                        onCloseDelete = { closingAccount = acc }
                    )
                }
            }
        }
    }

    if (showFormDialog) {
        AccountFormDialog(
            editing = editingAccount,
            currencySymbol = symbol,
            onDismiss = { showFormDialog = false },
            onSave = { name, openBal, desc, accNum, allowNeg, date ->
                val ex = editingAccount
                if (ex == null) {
                    viewModel.addAccount(name, openBal, desc, accNum, allowNeg, date) {
                        showFormDialog = false
                    }
                } else {
                    viewModel.updateAccount(ex.id, name, openBal, desc, accNum, allowNeg, date) {
                        showFormDialog = false
                    }
                }
            }
        )
    }

    closingAccount?.let { acc ->
        CaptchaDeleteDialog(
            title = "Close / Delete Account?",
            description = "Account \"${StringUtils.displayName(acc.name)}\" must have a 0.00 balance. If it has linked transactions, it will be soft-closed to preserve your statement history; otherwise it will be permanently deleted.",
            confirmButtonLabel = "Close Account",
            onDismiss = { closingAccount = null },
            onConfirm = {
                viewModel.closeOrDeleteAccount(acc.id) {
                    closingAccount = null
                }
            }
        )
    }
}

@Composable
private fun AccountItemCard(
    account: Account,
    symbol: String,
    compact: Boolean,
    onEdit: () -> Unit,
    onCloseDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (account.isClosed) 0.65f else 1f),
        accentTint = if (account.isClosed) null else if (account.currentBalance < 0) ledger.expense else ledger.income,
        contentPadding = PaddingValues(if (compact) 12.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = StringUtils.displayName(account.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (account.isClosed) {
                        StatusChip("Closed", ledger.expense)
                    } else if (account.allowNegativeBalance) {
                        StatusChip("Credit/OD", ledger.transfer)
                    }
                }
                if (account.accountNumber.isNotBlank()) {
                    Text(
                        text = "A/C ••••${account.accountNumber}",
                        fontFamily = ledger.monoFont,
                        fontSize = 11.5.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                if (!compact && account.description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = StringUtils.displayName(account.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = StringUtils.formatCurrency(account.currentBalance, symbol),
                    fontFamily = ledger.monoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 15.sp else 18.sp,
                    color = if (account.currentBalance < 0) ledger.expense else cs.onSurface
                )
                Text(
                    text = "Open: ${StringUtils.formatCompactCurrency(account.openingBalance, symbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
        }

        if (!compact && !account.isClosed) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Since ${StringUtils.formatDisplayDate(account.date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                GlassTonalButton(
                    text = "Edit",
                    icon = Icons.Rounded.Edit,
                    onClick = onEdit,
                    testTag = "edit_account_${account.id}"
                )
                GlassDangerButton(
                    text = "Close",
                    icon = Icons.Rounded.Close,
                    onClick = onCloseDelete,
                    testTag = "close_account_${account.id}"
                )
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountFormDialog(
    editing: Account?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, Boolean, String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ledger = LocalLedgerColors.current
    var name by remember(editing) { mutableStateOf(editing?.let { StringUtils.displayName(it.name) } ?: "") }
    var accNumber by remember(editing) { mutableStateOf(editing?.accountNumber ?: "") }
    var openingBal by remember(editing) {
        mutableStateOf(editing?.openingBalance?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "0")
    }
    var description by remember(editing) { mutableStateOf(editing?.let { StringUtils.displayName(it.description) } ?: "") }
    var allowNegative by remember(editing) { mutableStateOf(editing?.allowNegativeBalance ?: false) }
    var date by remember(editing) { mutableStateOf(editing?.date ?: StringUtils.todayIso()) }
    var error by remember { mutableStateOf<String?>(null) }

    GlassFormBottomSheet(
        title = if (editing == null) "Add Account" else "Edit Account",
        subtitle = if (editing == null) "Track a bank, wallet, cash, or credit card balance" else "Update account metadata and opening balance",
        icon = Icons.Rounded.AccountBalance,
        iconTint = cs.primary,
        onDismiss = onDismiss
    ) {
        GlassTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Account Name") },
            placeholder = { Text("e.g. HDFC Savings, Cash Wallet") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("account_name_input")
        )

        val openingBalTransform = remember { RegexInputTransformation(Regex("^-?\\d*\\.?\\d{0,2}$")) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassTextField(
                value = openingBal,
                onValueChange = { openingBal = it },
                label = { Text("Opening ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                inputTransformation = openingBalTransform,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = glassTextFieldColors(),
                modifier = Modifier.weight(1f).testTag("account_opening_bal_input")
            )
            DatePickerField(
                label = "Opening Date",
                isoDate = date,
                onDateSelected = { date = it },
                modifier = Modifier.weight(1f),
                testTag = "account_opening_date_picker"
            )
        }

        val accNumTransform = remember { DigitsMaxLengthTransformation(6) }
        GlassTextField(
            value = accNumber,
            onValueChange = { accNumber = it },
            label = { Text("Account Number (Last 6 digits, optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            inputTransformation = accNumTransform,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("account_number_input")
        )

        GlassTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (ledger.isLiquidGlass) ledger.glassSurface else cs.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = if (ledger.isLiquidGlass) ledger.glassBorderTop else cs.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Allow Negative Balance", style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                Text(
                    "Enable for credit cards or overdraft accounts",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
            Switch(
                checked = allowNegative,
                onCheckedChange = { allowNegative = it },
                modifier = Modifier.testTag("account_allow_negative_switch")
            )
        }

        if (error != null) {
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = ledger.expense)
        }

        Spacer(Modifier.height(2.dp))

        GlassPrimaryButton(
            text = if (editing == null) "Create Account" else "Update Account",
            onClick = {
                val bal = openingBal.toDoubleOrNull()
                when {
                    name.isBlank() -> error = "Enter an account name."
                    bal == null -> error = "Enter a valid opening balance."
                    !allowNegative && bal < 0 -> error = "Enable 'Allow Negative Balance' for negative opening balances."
                    else -> onSave(name, bal, description, accNumber, allowNegative, date)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "account_save_button"
        )
    }
}
