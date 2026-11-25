package com.hisaabkitaab.data

import kotlinx.serialization.Serializable

enum class CategoryType(val dbValue: String, val label: String) {
    INCOME("income", "Income"),
    EXPENSE("expense", "Expense");

    companion object {
        fun fromDb(value: String): CategoryType =
            entries.firstOrNull { it.dbValue.equals(value.trim(), ignoreCase = true) } ?: EXPENSE
    }
}

enum class TransactionType(val dbValue: String, val label: String) {
    EXPENSE("expense", "Expense"),
    INCOME("income", "Income"),
    TRANSFER("transfer", "Transfer");

    companion object {
        fun fromDb(value: String): TransactionType =
            entries.firstOrNull { it.dbValue.equals(value.trim(), ignoreCase = true) } ?: EXPENSE
    }
}

enum class PaymentGroup(val dbValue: String, val label: String) {
    ONLINE("online", "Online"),
    OFFLINE("offline", "Offline");

    companion object {
        fun fromDb(value: String): PaymentGroup =
            entries.firstOrNull { it.dbValue.equals(value.trim(), ignoreCase = true) } ?: ONLINE
    }
}

data class Category(
    val id: String,
    val name: String,
    val nameKey: String,
    val type: CategoryType,
    val color: String,
    val isDeleted: Boolean = false,
    val createdAt: Long
)

data class Account(
    val id: String,
    val name: String,
    val nameKey: String,
    val openingBalance: Double,
    val currentBalance: Double,
    val description: String = "",
    val accountNumber: String = "",
    val allowNegativeBalance: Boolean = false,
    val isClosed: Boolean = false,
    val date: String,
    val createdAt: Long
)

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val toAccountId: String = "",
    val categoryId: String = "",
    val paymentMediumId: String = "",
    val reason: String,
    val notes: String = "",
    val date: String,
    val createdAt: Long
)

data class PaymentMedium(
    val id: String,
    val name: String,
    val group: PaymentGroup,
    val isDeleted: Boolean = false,
    val createdAt: Long
)

data class UserProfile(
    val name: String = "",
    val address: String = "",
    val email: String = "",
    val contact: String = "",
    val watermark: String = "",
    val currencyName: String = "INR",
    val currencySymbol: String = "₹"
) {
    val effectiveSymbol: String
        get() = currencySymbol.ifBlank { "₹" }

    val effectiveCurrencyName: String
        get() = currencyName.ifBlank { "INR" }
}

data class TransactionFormDefaults(
    val type: TransactionType = TransactionType.EXPENSE,
    val accountId: String = "",
    val toAccountId: String = "",
    val categoryId: String = "",
    val paymentGroup: PaymentGroup = PaymentGroup.ONLINE,
    val paymentMediumId: String = ""
)

data class UpiReceiptDraft(
    val id: Long = System.currentTimeMillis(),
    val detectedApp: String,
    val type: TransactionType,
    val amount: Double?,
    val dateIso: String,
    val reason: String,
    val notes: String,
    val matchedAccountId: String?,
    val accountMatchedFromReceipt: Boolean,
    val matchedAccountHint: String?,
    val utrNumber: String?,
    val upiId: String?,
    val possibleDuplicateMessage: String?,
    val rawExtractedSnippet: String = ""
)

enum class AppFontFamily(val key: String, val displayName: String) {
    PLUS_JAKARTA_SANS("plus_jakarta_sans", "Plus Jakarta Sans"),
    INTER("inter", "Inter"),
    POPPINS("poppins", "Poppins"),
    OUTFIT("outfit", "Outfit");

    companion object {
        fun fromKey(key: String?): AppFontFamily =
            entries.firstOrNull { it.key == key } ?: PLUS_JAKARTA_SANS
    }
}

enum class FontScaleOption(val key: String, val label: String, val multiplier: Float) {
    XS("xs", "XS", 0.85f),
    S("s", "S", 0.92f),
    M("m", "M", 1.00f),
    L("l", "L", 1.08f),
    XL("xl", "XL", 1.16f),
    XXL("xxl", "XXL", 1.24f);

    companion object {
        fun fromKey(key: String?): FontScaleOption =
            entries.firstOrNull { it.key == key } ?: M
    }
}

enum class SurfaceStyle(val key: String, val label: String, val subtitle: String) {
    CLASSIC_M3("classic_m3", "Classic M3", "Solid Material 3 tonal surfaces"),
    FROSTED("frosted", "Frosted", "Soft blurred glass with crisp contrast"),
    LIQUID_GLASS("liquid_glass", "Liquid Glass", "Refractive glass & luminous edges");

    companion object {
        fun fromKey(key: String?, legacyLiquidGlass: Boolean? = null): SurfaceStyle {
            entries.firstOrNull { it.key == key }?.let { return it }
            return when (legacyLiquidGlass) {
                false -> CLASSIC_M3
                else -> LIQUID_GLASS
            }
        }
    }
}

enum class ChartPalette(
    val key: String,
    val label: String,
    val incomeHex: String,
    val expenseHex: String,
    val categoryColors: List<String>,
    val mediumColors: List<String>
) {
    VIBRANT_JEWEL(
        key = "vibrant_jewel",
        label = "Vibrant Jewel",
        incomeHex = "#0ECB81",
        expenseHex = "#F6465D",
        categoryColors = listOf("#6366F1", "#F59E0B", "#14B8A6", "#EC4899", "#8B5CF6", "#F97316", "#0EA5E9", "#84CC16"),
        mediumColors = listOf("#0EA5E9", "#10B981", "#A855F7", "#F97316", "#F43F5E", "#EAB308", "#06B6D4", "#6366F1")
    ),
    OCEAN_AURORA(
        key = "ocean_aurora",
        label = "Ocean Aurora",
        incomeHex = "#14B8A6",
        expenseHex = "#EC4899",
        categoryColors = listOf("#0EA5E9", "#14B8A6", "#6366F1", "#06B6D4", "#3B82F6", "#8B5CF6", "#2DD4BF", "#A855F7"),
        mediumColors = listOf("#2DD4BF", "#38BDF8", "#818CF8", "#0284C7", "#0D9488", "#C084FC", "#22D3EE", "#4F46E5")
    ),
    SUNSET_WARMTH(
        key = "sunset_warmth",
        label = "Sunset Warmth",
        incomeHex = "#F59E0B",
        expenseHex = "#E11D48",
        categoryColors = listOf("#F97316", "#F43F5E", "#EAB308", "#EC4899", "#FB7185", "#D97706", "#C026D3", "#EF4444"),
        mediumColors = listOf("#E11D48", "#FB923C", "#FACC15", "#DB2777", "#EA580C", "#A21CAF", "#F59E0B", "#DC2626")
    ),
    ROYAL_PRISM(
        key = "royal_prism",
        label = "Royal Prism",
        incomeHex = "#8B5CF6",
        expenseHex = "#06B6D4",
        categoryColors = listOf("#8B5CF6", "#06B6D4", "#F59E0B", "#EC4899", "#3B82F6", "#10B981", "#D946EF", "#F97316"),
        mediumColors = listOf("#06B6D4", "#A855F7", "#F43F5E", "#EAB308", "#22C55E", "#6366F1", "#FB923C", "#14B8A6")
    ),
    BOTANICAL_MINT(
        key = "botanical_mint",
        label = "Botanical Mint",
        incomeHex = "#10B981",
        expenseHex = "#EA580C",
        categoryColors = listOf("#10B981", "#84CC16", "#0D9488", "#F59E0B", "#22C55E", "#14B8A6", "#65A30D", "#0284C7"),
        mediumColors = listOf("#059669", "#CA8A04", "#0891B2", "#4ADE80", "#EA580C", "#2DD4BF", "#A3E635", "#0F766E")
    );

    companion object {
        fun fromKey(key: String?): ChartPalette =
            entries.firstOrNull { it.key == key } ?: VIBRANT_JEWEL
    }
}

@Serializable
data class CustomFontEntry(
    val id: String,
    val displayName: String,
    val fileName: String,
    val extension: String,
    val addedAtMillis: Long = 0L
)

data class ThemePreferences(
    val isDarkMode: Boolean = true,
    val surfaceStyle: SurfaceStyle = SurfaceStyle.LIQUID_GLASS,
    val accentHex: String = "#FCD535",
    val chartPalette: ChartPalette = ChartPalette.VIBRANT_JEWEL,
    val fontFamily: AppFontFamily = AppFontFamily.PLUS_JAKARTA_SANS,
    val activeCustomFontId: String = "",
    val customFonts: List<CustomFontEntry> = emptyList(),
    val fontScale: FontScaleOption = FontScaleOption.M,
    val lastDbName: String = "hisaab_kitaab.db",
    val hasActiveDb: Boolean = false,
    val linkedDbUri: String = ""
) {
    val isLiquidGlass: Boolean
        get() = surfaceStyle != SurfaceStyle.CLASSIC_M3

    val activeCustomFont: CustomFontEntry?
        get() = activeCustomFontId.takeIf { it.isNotBlank() }?.let { id ->
            customFonts.firstOrNull { it.id == id }
        }
}

sealed class DbLinkStatus {
    data class LiveLinked(val fileName: String, val lastSyncedAtMillis: Long) : DbLinkStatus()
    data class LocalOnly(val fileName: String) : DbLinkStatus()
    data class NeedsRelink(val fileName: String, val reason: String) : DbLinkStatus()
}

enum class PeriodPreset(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_30("Last 30 Days"),
    LAST_7("Last 7 Days"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

data class DateRange(
    val start: String,
    val end: String
)

data class AccountOpeningSummary(
    val accountId: String,
    val accountName: String,
    val accountNumber: String,
    val openingBalanceBeforePeriod: Double,
    val periodCredit: Double,
    val periodDebit: Double,
    val closingBalance: Double,
    val isClosed: Boolean
)

data class StatementRow(
    val serialNo: Int,
    val transactionId: String,
    val date: String,
    val createdAt: Long,
    val accountId: String,
    val accountName: String,
    val categoryName: String,
    val categoryColor: String,
    val paymentMediumName: String,
    val reason: String,
    val notes: String,
    val credit: Double,
    val debit: Double,
    val remainingBalance: Double,
    val isTransferLeg: Boolean
)

data class StatementReport(
    val range: DateRange,
    val accountFilterId: String?,
    val accountSummaries: List<AccountOpeningSummary>,
    val rows: List<StatementRow>,
    val totalCredit: Double,
    val totalDebit: Double,
    val netChange: Double,
    val totalOpeningBalance: Double,
    val totalClosingBalance: Double
)

data class CategoryCsvRow(
    val rowNumber: Int,
    val rawName: String,
    val normalizedName: String,
    val type: CategoryType?,
    val colorHex: String,
    val status: CategoryCsvStatus,
    val statusReason: String = ""
)

enum class CategoryCsvStatus {
    VALID,
    DUPLICATE_DB,
    DUPLICATE_CSV,
    INVALID
}

data class CategoryCsvPreview(
    val fileName: String,
    val rows: List<CategoryCsvRow>
) {
    val validRows: List<CategoryCsvRow> get() = rows.filter { it.status == CategoryCsvStatus.VALID }
    val skippedRows: List<CategoryCsvRow> get() = rows.filter { it.status != CategoryCsvStatus.VALID }
}

data class StatementImportRow(
    val rowNumber: Int,
    val date: String,
    val type: TransactionType,
    val amount: Double,
    val accountName: String,
    val toAccountName: String,
    val categoryName: String,
    val paymentMediumName: String,
    val reason: String,
    val notes: String,
    val status: StatementImportStatus,
    val statusReason: String = "",
    val resolvedAccountId: String = "",
    val resolvedToAccountId: String = "",
    val resolvedCategoryId: String = "",
    val resolvedPaymentMediumId: String = ""
)

enum class StatementImportStatus {
    VALID,
    DUPLICATE,
    INVALID
}

data class StatementImportPreview(
    val fileName: String,
    val rows: List<StatementImportRow>
) {
    val validRows: List<StatementImportRow> get() = rows.filter { it.status == StatementImportStatus.VALID }
    val duplicateRows: List<StatementImportRow> get() = rows.filter { it.status == StatementImportStatus.DUPLICATE }
    val invalidRows: List<StatementImportRow> get() = rows.filter { it.status == StatementImportStatus.INVALID }
}

data class AnalyticsSlice(
    val id: String,
    val label: String,
    val amount: Double,
    val colorHex: String,
    val percentage: Float
)

data class DailyFlowPoint(
    val date: String,
    val shortLabel: String,
    val income: Double,
    val expense: Double
)

enum class AppDestination(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    TRANSACTIONS("transactions", "Transactions"),
    ACCOUNTS("accounts", "Accounts"),
    STATEMENT("statement", "Statement"),
    CATEGORIES("categories", "Categories"),
    PAYMENT_MEDIUMS("payment_mediums", "Mediums"),
    PROFILE("profile", "Profile")
}

sealed class UiResult<out T> {
    data class Success<T>(val data: T, val message: String = "") : UiResult<T>()
    data class Error(val message: String) : UiResult<Nothing>()
}
