package com.hisaabkitaab.export

import android.util.Xml
import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.Category
import com.hisaabkitaab.data.PaymentMedium
import com.hisaabkitaab.data.StatementImportPreview
import com.hisaabkitaab.data.StatementImportRow
import com.hisaabkitaab.data.StatementImportStatus
import com.hisaabkitaab.data.StatementReport
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.TransactionType
import com.hisaabkitaab.data.UserProfile
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs

/**
 * Pure-Kotlin Spreadsheet Engine supporting:
 * 1. Valid multi-sheet `.xlsx` (OpenXML) Statement Export (Transactions + Accounts Summary + Profile)
 * 2. `.csv` Statement Export
 * 3. `.xlsx` and `.csv` Statement Import with duplicate detection and validation preview
 */
object StatementSpreadsheetEngine {

    // ==================== CSV EXPORT ====================

    fun exportStatementCsv(report: StatementReport, outputStream: OutputStream) {
        val sb = StringBuilder()
        sb.appendLine("Sr.No,Date,Account,Category,Payment Medium,Reason,Notes,Credit,Debit,Remaining Balance")
        report.rows.forEach { row ->
            sb.appendLine(
                listOf(
                    row.serialNo.toString(),
                    row.date,
                    CategoryCsvEngine.escapeCsvCell(row.accountName),
                    CategoryCsvEngine.escapeCsvCell(row.categoryName),
                    CategoryCsvEngine.escapeCsvCell(row.paymentMediumName),
                    CategoryCsvEngine.escapeCsvCell(row.reason),
                    CategoryCsvEngine.escapeCsvCell(row.notes),
                    String.format(Locale.US, "%.2f", row.credit),
                    String.format(Locale.US, "%.2f", row.debit),
                    String.format(Locale.US, "%.2f", row.remainingBalance)
                ).joinToString(",")
            )
        }
        outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    // ==================== XLSX EXPORT ====================

    fun exportStatementXlsx(
        report: StatementReport,
        profile: UserProfile,
        outputStream: OutputStream
    ) {
        val sheet1Rows = mutableListOf<List<Any>>()
        sheet1Rows.add(
            listOf("Sr.No", "Date", "Account", "Category", "Payment Medium", "Reason", "Notes", "Credit", "Debit", "Remaining Balance")
        )
        report.rows.forEach { row ->
            sheet1Rows.add(
                listOf(
                    row.serialNo,
                    row.date,
                    row.accountName,
                    row.categoryName,
                    row.paymentMediumName,
                    row.reason,
                    row.notes,
                    row.credit,
                    row.debit,
                    row.remainingBalance
                )
            )
        }
        sheet1Rows.add(
            listOf(
                "", "", "", "", "", "TOTALS", "",
                report.totalCredit, report.totalDebit, report.totalClosingBalance
            )
        )

        val sheet2Rows = mutableListOf<List<Any>>()
        sheet2Rows.add(listOf("Account", "A/C Number", "Opening Balance", "Total Credit", "Total Debit", "Closing Balance", "Status"))
        report.accountSummaries.forEach { acc ->
            sheet2Rows.add(
                listOf(
                    acc.accountName,
                    acc.accountNumber,
                    acc.openingBalanceBeforePeriod,
                    acc.periodCredit,
                    acc.periodDebit,
                    acc.closingBalance,
                    if (acc.isClosed) "Closed" else "Active"
                )
            )
        }

        val sheet3Rows = listOf(
            listOf("Field", "Value"),
            listOf("Name", profile.name),
            listOf("Email", profile.email),
            listOf("Contact", profile.contact),
            listOf("Address", profile.address),
            listOf("Currency", "${profile.effectiveCurrencyName} (${profile.effectiveSymbol})"),
            listOf("Period", "${report.range.start} to ${report.range.end}"),
            listOf("Generated On", StringUtils.todayIso())
        )

        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildWorksheetXml(sheet1Rows))
            writeZipEntry(zip, "xl/worksheets/sheet2.xml", buildWorksheetXml(sheet2Rows))
            writeZipEntry(zip, "xl/worksheets/sheet3.xml", buildWorksheetXml(sheet3Rows))
        }
    }

    // ==================== IMPORT (XLSX + CSV) ====================

    fun parseAndPreviewImport(
        inputStream: InputStream,
        fileName: String,
        accounts: List<Account>,
        categories: List<Category>,
        paymentMediums: List<PaymentMedium>,
        existingTransactions: List<Transaction>
    ): StatementImportPreview {
        val bytes = inputStream.readBytes()
        val isZipXlsx = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        val tableRows = if (isZipXlsx) {
            readFirstSheetFromXlsx(bytes)
        } else {
            CategoryCsvEngine.parseCsvLines(bytes.toString(Charsets.UTF_8))
        }
        return analyzeImportRows(
            fileName = fileName,
            rawRows = tableRows,
            accounts = accounts,
            categories = categories,
            paymentMediums = paymentMediums,
            existingTransactions = existingTransactions
        )
    }

    private fun analyzeImportRows(
        fileName: String,
        rawRows: List<List<String>>,
        accounts: List<Account>,
        categories: List<Category>,
        paymentMediums: List<PaymentMedium>,
        existingTransactions: List<Transaction>
    ): StatementImportPreview {
        if (rawRows.size < 2) {
            return StatementImportPreview(fileName = fileName, rows = emptyList())
        }

        val header = rawRows.first().map { it.trim().lowercase(Locale.ENGLISH) }
        val dateIdx = findCol(header, "date", "txn date", "transaction date")
        val typeIdx = findCol(header, "type", "txn type")
        val accountIdx = findCol(header, "account", "from account", "account name")
        val toAccountIdx = findCol(header, "to account", "destination account")
        val categoryIdx = findCol(header, "category", "category name")
        val mediumIdx = findCol(header, "payment medium", "medium", "mode")
        val reasonIdx = findCol(header, "reason", "description", "particulars", "title")
        val notesIdx = findCol(header, "notes", "remarks", "memo")
        val creditIdx = findCol(header, "credit", "income", "jama", "in")
        val debitIdx = findCol(header, "debit", "expense", "kharcha", "out")
        val amountIdx = findCol(header, "amount", "value")

        val activeAccountsByKey = accounts.filter { !it.isClosed }.associateBy { it.nameKey.lowercase() }
        val categoriesByKey = categories.filter { !it.isDeleted }.associateBy { it.nameKey.lowercase() }
        val mediumsByKey = paymentMediums.filter { !it.isDeleted }.associateBy { it.name.lowercase() }

        val result = mutableListOf<StatementImportRow>()

        rawRows.drop(1).forEachIndexed { idx, cols ->
            if (cols.all { it.isBlank() }) return@forEachIndexed
            val rowNum = idx + 2
            val rawReason = cols.getOrNull(reasonIdx)?.trim().orEmpty()
            if (rawReason.equals("TOTALS", ignoreCase = true) || rawReason.equals("PERIOD TOTALS", ignoreCase = true)) {
                return@forEachIndexed
            }

            val rawDate = cols.getOrNull(dateIdx)?.trim().orEmpty()
            val normalizedDate = normalizeDateString(rawDate)
            val rawAccount = cols.getOrNull(accountIdx)?.trim().orEmpty()
            val rawToAccount = cols.getOrNull(toAccountIdx)?.trim().orEmpty()
            val rawCategory = cols.getOrNull(categoryIdx)?.trim().orEmpty()
            val rawMedium = cols.getOrNull(mediumIdx)?.trim().orEmpty()
            val rawNotes = cols.getOrNull(notesIdx)?.trim().orEmpty()
            val rawType = cols.getOrNull(typeIdx)?.trim().orEmpty()

            val creditVal = parseDoubleSafe(cols.getOrNull(creditIdx))
            val debitVal = parseDoubleSafe(cols.getOrNull(debitIdx))
            val explicitAmt = parseDoubleSafe(cols.getOrNull(amountIdx))

            val resolvedType: TransactionType
            val resolvedAmount: Double
            when {
                rawType.equals("transfer", ignoreCase = true) -> {
                    resolvedType = TransactionType.TRANSFER
                    resolvedAmount = if (explicitAmt > 0) explicitAmt else maxOf(debitVal, creditVal)
                }
                rawType.equals("income", ignoreCase = true) -> {
                    resolvedType = TransactionType.INCOME
                    resolvedAmount = if (explicitAmt > 0) explicitAmt else creditVal
                }
                rawType.equals("expense", ignoreCase = true) -> {
                    resolvedType = TransactionType.EXPENSE
                    resolvedAmount = if (explicitAmt > 0) explicitAmt else debitVal
                }
                creditVal > 0 && debitVal <= 0 -> {
                    resolvedType = TransactionType.INCOME
                    resolvedAmount = creditVal
                }
                debitVal > 0 && creditVal <= 0 -> {
                    resolvedType = TransactionType.EXPENSE
                    resolvedAmount = debitVal
                }
                else -> {
                    resolvedType = TransactionType.EXPENSE
                    resolvedAmount = explicitAmt
                }
            }

            val matchedAcc = activeAccountsByKey[StringUtils.toLowerTrim(rawAccount)]
            val matchedToAcc = activeAccountsByKey[StringUtils.toLowerTrim(rawToAccount)]
            val matchedCat = categoriesByKey[StringUtils.toLowerTrim(rawCategory)]
            val matchedMed = mediumsByKey[StringUtils.toLowerTrim(rawMedium)]

            val isSelfTransferRow = rawCategory.equals("self transfer", ignoreCase = true)

            val (status, statusReason) = when {
                isSelfTransferRow && matchedToAcc == null ->
                    StatementImportStatus.INVALID to "Self-transfer leg requires To Account column"
                normalizedDate == null ->
                    StatementImportStatus.INVALID to "Invalid date '$rawDate' (use YYYY-MM-DD)"
                resolvedAmount <= 0.0 ->
                    StatementImportStatus.INVALID to "Amount must be greater than 0"
                matchedAcc == null ->
                    StatementImportStatus.INVALID to "Active account '${rawAccount.ifBlank { "?" }}' not found"
                resolvedType == TransactionType.TRANSFER && matchedToAcc == null ->
                    StatementImportStatus.INVALID to "Destination account '$rawToAccount' not found"
                resolvedType != TransactionType.TRANSFER && matchedCat == null ->
                    StatementImportStatus.INVALID to "Category '${rawCategory.ifBlank { "?" }}' not found"
                else -> {
                    val isDup = existingTransactions.any { existing ->
                        existing.date == normalizedDate &&
                            existing.type == resolvedType &&
                            existing.accountId == matchedAcc.id &&
                            abs(existing.amount - resolvedAmount) < 0.005 &&
                            existing.reason.equals(rawReason.ifBlank { "Imported entry" }, ignoreCase = true)
                    }
                    if (isDup) {
                        StatementImportStatus.DUPLICATE to "Matches an existing ledger entry"
                    } else {
                        StatementImportStatus.VALID to "Ready to import"
                    }
                }
            }

            result.add(
                StatementImportRow(
                    rowNumber = rowNum,
                    date = normalizedDate ?: rawDate,
                    type = resolvedType,
                    amount = resolvedAmount,
                    accountName = rawAccount,
                    toAccountName = rawToAccount,
                    categoryName = rawCategory,
                    paymentMediumName = rawMedium,
                    reason = rawReason.ifBlank { "Imported entry" },
                    notes = rawNotes,
                    status = status,
                    statusReason = statusReason,
                    resolvedAccountId = matchedAcc?.id.orEmpty(),
                    resolvedToAccountId = matchedToAcc?.id.orEmpty(),
                    resolvedCategoryId = matchedCat?.id.orEmpty(),
                    resolvedPaymentMediumId = matchedMed?.id.orEmpty()
                )
            )
        }

        return StatementImportPreview(fileName = fileName, rows = result)
    }

    // ==================== PURE-KOTLIN XLSX READER ====================

    private fun readFirstSheetFromXlsx(bytes: ByteArray): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = ByteArrayOutputStream()
                    zis.copyTo(out)
                    entries[entry.name] = out.toByteArray()
                }
                entry = zis.nextEntry
            }
        }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: entries.entries.firstOrNull { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }?.value
            ?: return emptyList()

        return parseSheetXml(sheetBytes, sharedStrings)
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var inSi = false
        var current = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        inSi = true
                        current = StringBuilder()
                    } else if (inSi && parser.name == "t") {
                        current.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si" && inSi) {
                        result.add(current.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var currentRow = mutableMapOf<Int, String>()
        var cellType: String? = null
        var cellColIndex = 0
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableMapOf()
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            cellColIndex = columnRefToIndex(ref)
                            cellType = parser.getAttributeValue(null, "t")
                        }
                        "v" -> {
                            val raw = parser.nextText()
                            val value = if (cellType == "s") {
                                raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: raw
                            } else {
                                raw
                            }
                            currentRow[cellColIndex] = value
                        }
                        "t" -> {
                            if (cellType == "inlineStr") {
                                currentRow[cellColIndex] = parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "row" && currentRow.isNotEmpty()) {
                        val maxCol = currentRow.keys.maxOrNull() ?: 0
                        val rowList = (0..maxCol).map { currentRow[it].orEmpty() }
                        rows.add(rowList)
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun columnRefToIndex(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }.uppercase(Locale.ENGLISH)
        var index = 0
        for (ch in letters) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return (index - 1).coerceAtLeast(0)
    }

    // ==================== OPENXML TEMPLATES ====================

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildWorksheetXml(rows: List<List<Any>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rIdx, row ->
            val rowNum = rIdx + 1
            sb.append("""<row r="$rowNum">""")
            row.forEachIndexed { cIdx, cellVal ->
                val ref = "${indexToColumnLetter(cIdx)}$rowNum"
                val styleAttr = if (rIdx == 0) """ s="1"""" else ""
                when (cellVal) {
                    is Int, is Long -> {
                        sb.append("""<c r="$ref"$styleAttr><v>$cellVal</v></c>""")
                    }
                    is Double, is Float -> {
                        val d = (cellVal as Number).toDouble()
                        val formatted = String.format(Locale.US, "%.2f", d)
                        sb.append("""<c r="$ref"$styleAttr><v>$formatted</v></c>""")
                    }
                    else -> {
                        val escaped = escapeXml(cellVal.toString())
                        sb.append("""<c r="$ref" t="inlineStr"$styleAttr><is><t>$escaped</t></is></c>""")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun indexToColumnLetter(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A'.code + rem).toChar())
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun contentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        </Types>
    """.trimIndent()

    private fun rootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>
                <sheet name="Transactions" sheetId="1" r:id="rId1"/>
                <sheet name="Accounts Summary" sheetId="2" r:id="rId2"/>
                <sheet name="Profile" sheetId="3" r:id="rId3"/>
            </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
            <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
            <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun stylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <fonts count="2">
                <font><sz val="11"/><name val="Calibri"/></font>
                <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
            </fonts>
            <fills count="3">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FF181A20"/></patternFill></fill>
            </fills>
            <borders count="1"><border/></borders>
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
            <cellXfs count="2">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
            </cellXfs>
        </styleSheet>
    """.trimIndent()

    private fun findCol(headers: List<String>, vararg aliases: String): Int {
        for (alias in aliases) {
            val idx = headers.indexOfFirst { it == alias || it.contains(alias) }
            if (idx >= 0) return idx
        }
        return -1
    }

    private fun parseDoubleSafe(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val cleaned = raw.replace(Regex("[^0-9.\\-]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun normalizeDateString(raw: String): String? {
        if (raw.isBlank()) return null
        val trimmed = raw.trim()
        // Already ISO YYYY-MM-DD
        if (Regex("^\\d{4}-\\d{2}-\\d{2}.*").matches(trimmed)) {
            val candidate = trimmed.take(10)
            return if (StringUtils.parseIsoDate(candidate) != null) candidate else null
        }
        // Handle DD/MM/YYYY or DD-MM-YYYY
        val dmy = Regex("^(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})$").matchEntire(trimmed)
        if (dmy != null) {
            val d = dmy.groupValues[1].padStart(2, '0')
            val m = dmy.groupValues[2].padStart(2, '0')
            val y = dmy.groupValues[3]
            val iso = "$y-$m-$d"
            return if (StringUtils.parseIsoDate(iso) != null) iso else null
        }
        // Handle Excel serial date numbers (days since 1899-12-30)
        val serial = trimmed.toDoubleOrNull()?.toLong()
        if (serial != null && serial in 25000..70000) {
            val date = java.time.LocalDate.of(1899, 12, 30).plusDays(serial)
            return date.toString()
        }
        return null
    }
}
