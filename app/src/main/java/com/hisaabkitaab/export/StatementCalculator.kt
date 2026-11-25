package com.hisaabkitaab.export

import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.AccountOpeningSummary
import com.hisaabkitaab.data.Category
import com.hisaabkitaab.data.DateRange
import com.hisaabkitaab.data.PaymentMedium
import com.hisaabkitaab.data.StatementReport
import com.hisaabkitaab.data.StatementRow
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.TransactionType

object StatementCalculator {

    private data class ExpandedLeg(
        val txn: Transaction,
        val accountId: String,
        val credit: Double,
        val debit: Double,
        val isTransferLeg: Boolean,
        val sortPriority: Int // 0 = debit leg first, 1 = credit leg second
    )

    fun buildReport(
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        paymentMediums: List<PaymentMedium>,
        range: DateRange,
        accountFilterId: String? = null
    ): StatementReport {
        val accountsById = accounts.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }
        val mediumsById = paymentMediums.associateBy { it.id }

        val targetAccounts = if (accountFilterId.isNullOrBlank()) {
            accounts
        } else {
            accounts.filter { it.id == accountFilterId }
        }
        val targetIds = targetAccounts.map { it.id }.toSet()

        // 1. Compute pre-period opening balance for each target account (date < range.start)
        val prePeriodBalance = mutableMapOf<String, Double>()
        targetAccounts.forEach { acc ->
            prePeriodBalance[acc.id] = acc.openingBalance
        }

        transactions
            .filter { it.date < range.start }
            .forEach { txn ->
                when (txn.type) {
                    TransactionType.INCOME -> {
                        if (txn.accountId in targetIds) {
                            prePeriodBalance[txn.accountId] =
                                (prePeriodBalance[txn.accountId] ?: 0.0) + txn.amount
                        }
                    }
                    TransactionType.EXPENSE -> {
                        if (txn.accountId in targetIds) {
                            prePeriodBalance[txn.accountId] =
                                (prePeriodBalance[txn.accountId] ?: 0.0) - txn.amount
                        }
                    }
                    TransactionType.TRANSFER -> {
                        if (txn.accountId in targetIds) {
                            prePeriodBalance[txn.accountId] =
                                (prePeriodBalance[txn.accountId] ?: 0.0) - txn.amount
                        }
                        if (txn.toAccountId in targetIds) {
                            prePeriodBalance[txn.toAccountId] =
                                (prePeriodBalance[txn.toAccountId] ?: 0.0) + txn.amount
                        }
                    }
                }
            }

        // 2. Expand in-period transactions (transfer expands into debit leg + credit leg)
        val legs = mutableListOf<ExpandedLeg>()
        transactions
            .filter { it.date >= range.start && it.date <= range.end }
            .forEach { txn ->
                when (txn.type) {
                    TransactionType.INCOME -> {
                        if (txn.accountId in targetIds) {
                            legs.add(
                                ExpandedLeg(
                                    txn = txn,
                                    accountId = txn.accountId,
                                    credit = txn.amount,
                                    debit = 0.0,
                                    isTransferLeg = false,
                                    sortPriority = 1
                                )
                            )
                        }
                    }
                    TransactionType.EXPENSE -> {
                        if (txn.accountId in targetIds) {
                            legs.add(
                                ExpandedLeg(
                                    txn = txn,
                                    accountId = txn.accountId,
                                    credit = 0.0,
                                    debit = txn.amount,
                                    isTransferLeg = false,
                                    sortPriority = 0
                                )
                            )
                        }
                    }
                    TransactionType.TRANSFER -> {
                        if (txn.accountId in targetIds) {
                            legs.add(
                                ExpandedLeg(
                                    txn = txn,
                                    accountId = txn.accountId,
                                    credit = 0.0,
                                    debit = txn.amount,
                                    isTransferLeg = true,
                                    sortPriority = 0
                                )
                            )
                        }
                        if (txn.toAccountId in targetIds) {
                            legs.add(
                                ExpandedLeg(
                                    txn = txn,
                                    accountId = txn.toAccountId,
                                    credit = txn.amount,
                                    debit = 0.0,
                                    isTransferLeg = true,
                                    sortPriority = 1
                                )
                            )
                        }
                    }
                }
            }

        // 3. Sort chronologically: date ASC, createdAt ASC, debit before credit
        val sortedLegs = legs.sortedWith(
            compareBy<ExpandedLeg> { it.txn.date }
                .thenBy { it.txn.createdAt }
                .thenBy { it.sortPriority }
        )

        // 4. Step running balances per account
        val runningBalances = prePeriodBalance.toMutableMap()
        val periodCredits = mutableMapOf<String, Double>()
        val periodDebits = mutableMapOf<String, Double>()

        val rows = sortedLegs.mapIndexed { idx, leg ->
            val currentBal = (runningBalances[leg.accountId] ?: 0.0) + leg.credit - leg.debit
            runningBalances[leg.accountId] = currentBal
            periodCredits[leg.accountId] = (periodCredits[leg.accountId] ?: 0.0) + leg.credit
            periodDebits[leg.accountId] = (periodDebits[leg.accountId] ?: 0.0) + leg.debit

            val acc = accountsById[leg.accountId]
            val cat = categoriesById[leg.txn.categoryId]
            val med = mediumsById[leg.txn.paymentMediumId]

            val categoryName = if (leg.isTransferLeg) {
                "Self Transfer"
            } else {
                cat?.let { StringUtils.displayName(it.name) } ?: "Uncategorized"
            }
            val categoryColor = if (leg.isTransferLeg) "#3B82F6" else (cat?.color ?: "#707A8A")

            val reasonWithTransferHint = if (leg.isTransferLeg) {
                val counterpartId = if (leg.debit > 0) leg.txn.toAccountId else leg.txn.accountId
                val counterpartName = accountsById[counterpartId]?.let { StringUtils.displayName(it.name) } ?: ""
                val direction = if (leg.debit > 0) "To $counterpartName" else "From $counterpartName"
                if (leg.txn.reason.isNotBlank() && counterpartName.isNotBlank()) {
                    "${leg.txn.reason} ($direction)"
                } else {
                    leg.txn.reason.ifBlank { direction }
                }
            } else {
                leg.txn.reason
            }

            StatementRow(
                serialNo = idx + 1,
                transactionId = leg.txn.id,
                date = leg.txn.date,
                createdAt = leg.txn.createdAt,
                accountId = leg.accountId,
                accountName = acc?.let { StringUtils.displayName(it.name) } ?: "Unknown",
                categoryName = categoryName,
                categoryColor = categoryColor,
                paymentMediumName = med?.let { StringUtils.displayName(it.name) } ?: "—",
                reason = reasonWithTransferHint,
                notes = leg.txn.notes,
                credit = leg.credit,
                debit = leg.debit,
                remainingBalance = currentBal,
                isTransferLeg = leg.isTransferLeg
            )
        }

        val summaries = targetAccounts.map { acc ->
            val open = prePeriodBalance[acc.id] ?: acc.openingBalance
            val cr = periodCredits[acc.id] ?: 0.0
            val dr = periodDebits[acc.id] ?: 0.0
            AccountOpeningSummary(
                accountId = acc.id,
                accountName = StringUtils.displayName(acc.name),
                accountNumber = acc.accountNumber,
                openingBalanceBeforePeriod = open,
                periodCredit = cr,
                periodDebit = dr,
                closingBalance = open + cr - dr,
                isClosed = acc.isClosed
            )
        }

        val totalCredit = rows.sumOf { it.credit }
        val totalDebit = rows.sumOf { it.debit }
        val totalOpening = summaries.sumOf { it.openingBalanceBeforePeriod }
        val totalClosing = summaries.sumOf { it.closingBalance }

        return StatementReport(
            range = range,
            accountFilterId = accountFilterId,
            accountSummaries = summaries,
            rows = rows,
            totalCredit = totalCredit,
            totalDebit = totalDebit,
            netChange = totalCredit - totalDebit,
            totalOpeningBalance = totalOpening,
            totalClosingBalance = totalClosing
        )
    }
}
