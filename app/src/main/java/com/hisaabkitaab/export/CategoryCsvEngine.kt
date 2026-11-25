package com.hisaabkitaab.export

import com.hisaabkitaab.data.Category
import com.hisaabkitaab.data.CategoryCsvPreview
import com.hisaabkitaab.data.CategoryCsvRow
import com.hisaabkitaab.data.CategoryCsvStatus
import com.hisaabkitaab.data.CategoryType
import com.hisaabkitaab.data.StringUtils

object CategoryCsvEngine {

    private val defaultColors = listOf(
        "#0ECB81", "#F6465D", "#FCD535", "#3B82F6",
        "#A855F7", "#2DBDB6", "#F97316", "#EC4899"
    )

    fun generateSampleCsv(): String = buildString {
        appendLine("name,type,color")
        appendLine("Salary,income,#0ECB81")
        appendLine("Freelance,income,#2DBDB6")
        appendLine("Dividends,income,#3B82F6")
        appendLine("Groceries,expense,#F97316")
        appendLine("Rent,expense,#F6465D")
        appendLine("Utilities,expense,#FCD535")
        appendLine("Transport,expense,#A855F7")
        appendLine("Dining Out,expense,#EC4899")
    }

    fun parseAndPreview(
        csvContent: String,
        fileName: String,
        existingCategories: List<Category>
    ): CategoryCsvPreview {
        val rawRows = parseCsvLines(csvContent)
        if (rawRows.isEmpty()) {
            return CategoryCsvPreview(fileName = fileName, rows = emptyList())
        }

        // Detect header row
        val firstRowLower = rawRows.first().map { it.trim().lowercase() }
        val hasHeader = firstRowLower.any { it in setOf("name", "category", "type", "color") }
        val header = if (hasHeader) firstRowLower else listOf("name", "type", "color")
        val dataRows = if (hasHeader) rawRows.drop(1) else rawRows

        val nameIdx = header.indexOfFirst { it in setOf("name", "category", "category name") }.takeIf { it >= 0 } ?: 0
        val typeIdx = header.indexOfFirst { it == "type" || it == "category type" }.takeIf { it >= 0 } ?: 1
        val colorIdx = header.indexOfFirst { it == "color" || it == "colour" || it == "hex" }.takeIf { it >= 0 } ?: 2

        val existingKeys = existingCategories.map { it.nameKey.lowercase() }.toSet()
        val seenInCsv = mutableSetOf<String>()

        val previewRows = dataRows.mapIndexedNotNull { idx, cols ->
            if (cols.all { it.isBlank() }) return@mapIndexedNotNull null
            val rowNum = if (hasHeader) idx + 2 else idx + 1
            val rawName = cols.getOrNull(nameIdx)?.trim().orEmpty()
            val rawType = cols.getOrNull(typeIdx)?.trim().orEmpty()
            val rawColor = cols.getOrNull(colorIdx)?.trim().orEmpty()

            val normalizedName = StringUtils.toLowerTrim(rawName)
            val parsedType = when (rawType.lowercase()) {
                "income", "credit", "jama", "in" -> CategoryType.INCOME
                "expense", "debit", "kharcha", "out" -> CategoryType.EXPENSE
                else -> null
            }
            val colorHex = if (rawColor.isNotBlank() && StringUtils.isValidHexColor(rawColor)) {
                StringUtils.normalizeHexColor(rawColor)
            } else {
                defaultColors[idx % defaultColors.size]
            }

            val (status, reason) = when {
                normalizedName.isEmpty() -> CategoryCsvStatus.INVALID to "Missing category name"
                parsedType == null -> CategoryCsvStatus.INVALID to "Invalid type '$rawType' (expected income or expense)"
                normalizedName in existingKeys -> CategoryCsvStatus.DUPLICATE_DB to "Already exists in database"
                normalizedName in seenInCsv -> CategoryCsvStatus.DUPLICATE_CSV to "Duplicate row within CSV"
                else -> {
                    seenInCsv.add(normalizedName)
                    CategoryCsvStatus.VALID to "Ready to import"
                }
            }

            CategoryCsvRow(
                rowNumber = rowNum,
                rawName = rawName,
                normalizedName = normalizedName,
                type = parsedType,
                colorHex = colorHex,
                status = status,
                statusReason = reason
            )
        }

        return CategoryCsvPreview(fileName = fileName, rows = previewRows)
    }

    /**
     * Quote-aware RFC 4180 CSV parser supporting escaped quotes (`""`) and commas inside quotes.
     */
    fun parseCsvLines(text: String): List<List<String>> {
        val cleanText = text.removePrefix("\uFEFF") // Strip UTF-8 BOM if present
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < cleanText.length) {
            val ch = cleanText[i]
            when {
                inQuotes -> {
                    if (ch == '"') {
                        if (i + 1 < cleanText.length && cleanText[i + 1] == '"') {
                            currentField.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        currentField.append(ch)
                    }
                }
                ch == '"' -> inQuotes = true
                ch == ',' -> {
                    currentRow.add(currentField.toString())
                    currentField.clear()
                }
                ch == '\r' -> {
                    if (i + 1 < cleanText.length && cleanText[i + 1] == '\n') i++
                    currentRow.add(currentField.toString())
                    currentField.clear()
                    rows.add(currentRow.toList())
                    currentRow.clear()
                }
                ch == '\n' -> {
                    currentRow.add(currentField.toString())
                    currentField.clear()
                    rows.add(currentRow.toList())
                    currentRow.clear()
                }
                else -> currentField.append(ch)
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow.toList())
        }
        return rows
    }

    fun escapeCsvCell(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuote) "\"${value.replace("\"", "\"\"")}\"" else value
    }
}
