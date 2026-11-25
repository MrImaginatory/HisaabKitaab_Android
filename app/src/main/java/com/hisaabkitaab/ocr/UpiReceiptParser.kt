package com.hisaabkitaab.ocr

import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.TransactionType
import com.hisaabkitaab.data.UpiReceiptDraft
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

data class OcrLine(
    val text: String,
    val heightPx: Int = 0,
    val topPx: Int = 0,
    val leftPx: Int = 0
)

/**
 * 100% offline deterministic parser for Indian UPI payment receipts
 * (GPay, PhonePe, Paytm, BHIM, Amazon Pay, CRED, Navi, WhatsApp Pay, and generic UPI).
 */
object UpiReceiptParser {

    private val draftIdCounter = AtomicLong(1L)
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val monthMap = mapOf(
        "jan" to Month.JANUARY, "january" to Month.JANUARY,
        "feb" to Month.FEBRUARY, "february" to Month.FEBRUARY,
        "mar" to Month.MARCH, "march" to Month.MARCH,
        "apr" to Month.APRIL, "april" to Month.APRIL,
        "may" to Month.MAY,
        "jun" to Month.JUNE, "june" to Month.JUNE,
        "jul" to Month.JULY, "july" to Month.JULY,
        "aug" to Month.AUGUST, "august" to Month.AUGUST,
        "sep" to Month.SEPTEMBER, "sept" to Month.SEPTEMBER, "september" to Month.SEPTEMBER,
        "oct" to Month.OCTOBER, "october" to Month.OCTOBER,
        "nov" to Month.NOVEMBER, "november" to Month.NOVEMBER,
        "dec" to Month.DECEMBER, "december" to Month.DECEMBER
    )

    private val bankAliases: List<Pair<String, List<String>>> = listOf(
        "sbi" to listOf("state bank of india", "state bank", "sbi"),
        "hdfc" to listOf("hdfc bank", "hdfc"),
        "icici" to listOf("icici bank", "icici"),
        "axis" to listOf("axis bank", "axis"),
        "kotak" to listOf("kotak mahindra", "kotak bank", "kotak"),
        "canara" to listOf("canara bank", "canara"),
        "pnb" to listOf("punjab national bank", "punjab national", "pnb"),
        "bob" to listOf("bank of baroda", "baroda", "bob"),
        "idfc" to listOf("idfc first bank", "idfc first", "idfc"),
        "yes" to listOf("yes bank"),
        "indusind" to listOf("indusind bank", "indusind"),
        "federal" to listOf("federal bank", "federal"),
        "union" to listOf("union bank of india", "union bank"),
        "indian" to listOf("indian bank"),
        "iob" to listOf("indian overseas bank", "iob"),
        "boi" to listOf("bank of india", "boi"),
        "bandhan" to listOf("bandhan bank", "bandhan"),
        "rbl" to listOf("rbl bank", "rbl"),
        "au" to listOf("au small finance", "au bank"),
        "idbi" to listOf("idbi bank", "idbi"),
        "uco" to listOf("uco bank", "uco"),
        "central" to listOf("central bank of india", "central bank"),
        "karnataka" to listOf("karnataka bank"),
        "southindian" to listOf("south indian bank"),
        "dbs" to listOf("dbs bank", "digibank", "dbs"),
        "hsbc" to listOf("hsbc bank", "hsbc"),
        "sc" to listOf("standard chartered"),
        "citi" to listOf("citibank", "citi bank"),
        "paytmpb" to listOf("paytm payments bank"),
        "airtelpb" to listOf("airtel payments bank"),
        "fi" to listOf("fi money", "fi savings"),
        "jupiter" to listOf("jupiter"),
        "maharashtra" to listOf("bank of maharashtra"),
        "kvb" to listOf("karur vysya", "kvb")
    )

    private val emailDomainBlocklist = setOf(
        "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com", "proton.me", "live.com"
    )

    private val boilerplateReasonLines = setOf(
        "paid to", "sent to", "payment to", "transfer to", "to", "to:",
        "received from", "from", "from:", "debited from", "credited to",
        "paid", "completed", "transaction successful", "payment successful",
        "paid successfully", "received successfully", "money sent", "money sent!",
        "money received", "money received!", "banking name", "banking name:",
        "verified merchant", "upi transaction id", "transaction id", "google transaction id",
        "upi ref no", "upi ref. no.", "utr", "view details", "share receipt",
        "split expense", "pay again", "check balance", "contact support", "need help",
        "powered by", "google pay", "phonepe", "paytm", "bhim", "upi"
    )

    fun parseReceiptText(
        rawText: String,
        accounts: List<Account>,
        transactions: List<Transaction>,
        fallbackAccountId: String? = null,
        currencySymbol: String = "₹",
        today: LocalDate = LocalDate.now()
    ): UpiReceiptDraft {
        val lines = rawText.lines()
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
            .mapIndexed { idx, line -> OcrLine(text = line, heightPx = 0, topPx = idx * 40, leftPx = 0) }
        return parseReceiptLines(lines, accounts, transactions, fallbackAccountId, currencySymbol, today)
    }

    fun parseReceiptLines(
        rawLines: List<OcrLine>,
        accounts: List<Account>,
        transactions: List<Transaction>,
        fallbackAccountId: String? = null,
        currencySymbol: String = "₹",
        today: LocalDate = LocalDate.now()
    ): UpiReceiptDraft {
        val lines = rawLines
            .map { it.copy(text = it.text.trim().replace(Regex("\\s+"), " ")) }
            .filter { it.text.isNotBlank() }

        val fullText = lines.joinToString("\n") { it.text }
        val lowerFull = fullText.lowercase(Locale.ENGLISH)

        val detectedApp = detectUpiApp(lowerFull)
        val type = detectTransactionType(lines, lowerFull)
        val amount = extractAmount(lines)
        val dateIso = extractDateIso(lines, today)
        val utr = extractUtr(lines)
        val upiId = extractCounterpartyVpa(lines, type)
        val reason = extractMerchantReason(lines, type, upiId, detectedApp)
        val notes = buildNotes(detectedApp, utr, upiId)

        val activeAccounts = accounts.filter { !it.isClosed }
        val accountMatch = matchActiveAccount(lines, type, activeAccounts, fallbackAccountId)
        val resolvedAccountId = accountMatch?.account?.id ?: fallbackAccountId?.takeIf { id ->
            activeAccounts.any { it.id == id }
        }

        val duplicateMessage = detectDuplicate(
            transactions = transactions,
            type = type,
            amount = amount,
            dateIso = dateIso,
            resolvedAccountId = resolvedAccountId,
            utr = utr,
            currencySymbol = currencySymbol
        )

        val snippet = lines.take(4).joinToString(" • ") { it.text }.take(120)

        return UpiReceiptDraft(
            id = System.currentTimeMillis() * 1000L + (draftIdCounter.incrementAndGet() % 1000L),
            detectedApp = detectedApp,
            type = type,
            amount = amount,
            dateIso = dateIso,
            reason = reason,
            notes = notes,
            matchedAccountId = accountMatch?.account?.id,
            accountMatchedFromReceipt = accountMatch != null,
            matchedAccountHint = accountMatch?.hint,
            utrNumber = utr,
            upiId = upiId,
            possibleDuplicateMessage = duplicateMessage,
            rawExtractedSnippet = snippet
        )
    }

    private fun detectUpiApp(lowerFull: String): String {
        return when {
            lowerFull.contains("google pay") || lowerFull.contains("google transaction id") || lowerFull.contains("gpay") -> "GPay"
            lowerFull.contains("phonepe") || lowerFull.contains("phone pe") -> "PhonePe"
            lowerFull.contains("paytm") -> "Paytm"
            lowerFull.contains("bhim") -> "BHIM"
            lowerFull.contains("amazon pay") -> "Amazon Pay"
            lowerFull.contains("cred pay") || lowerFull.contains("cred club") -> "CRED"
            lowerFull.contains("navi upi") -> "Navi UPI"
            lowerFull.contains("whatsapp pay") -> "WhatsApp Pay"
            lowerFull.contains("@okhdfcbank") || lowerFull.contains("@okicici") ||
                lowerFull.contains("@oksbi") || lowerFull.contains("@okaxis") -> "GPay"
            lowerFull.contains("@ybl") || lowerFull.contains("@ibl") || lowerFull.contains("@axl") -> "PhonePe"
            lowerFull.contains("@ptyes") || lowerFull.contains("@pthdfc") ||
                lowerFull.contains("@ptsbi") || lowerFull.contains("@ptaxis") -> "Paytm"
            lowerFull.contains("@apl") -> "Amazon Pay"
            else -> "UPI"
        }
    }

    private fun detectTransactionType(lines: List<OcrLine>, lowerFull: String): TransactionType {
        var incomeScore = 0
        var expenseScore = 0

        val strongIncome = listOf(
            "received from", "money received", "payment received", "received successfully",
            "paid you", "you received", "cashback received", "refund received"
        )
        val strongExpense = listOf(
            "paid to", "sent to", "debited from", "paid successfully", "money sent",
            "payment to", "you paid", "transfer to", "paid using"
        )

        strongIncome.forEach { cue -> if (lowerFull.contains(cue)) incomeScore += 3 }
        strongExpense.forEach { cue -> if (lowerFull.contains(cue)) expenseScore += 3 }

        if (lowerFull.contains("credited to") && !lowerFull.contains("debited from")) {
            incomeScore += 2
        }

        // Check top lines specifically (e.g. GPay top header "From Rahul" vs "To Zomato")
        lines.take(5).forEach { line ->
            val l = line.text.lowercase(Locale.ENGLISH)
            if (l.startsWith("from ") || l.startsWith("from:")) incomeScore += 2
            if (l.startsWith("to ") || l.startsWith("to:")) expenseScore += 2
        }

        return if (incomeScore > expenseScore && incomeScore > 0) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }
    }

    private fun extractAmount(lines: List<OcrLine>): Double? {
        val maxHeight = lines.maxOfOrNull { it.heightPx } ?: 0
        val currencyRegex = Regex(
            """(?i)(?:₹|rs\.?|inr)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
        )
        // OCR frequently misreads ₹ as ?, R, E, F, £, €, § right before digits on a standalone hero line
        val misreadCurrencyRegex = Regex(
            """^[?R£€§+\-]\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)$"""
        )
        val standaloneNumberRegex = Regex(
            """^([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)$"""
        )

        val ignoreLineKeywords = listOf(
            "utr", "ref", "transaction id", "txn id", "order id", "rrn", "balance",
            "avail", "limit", "+91", "mobile", "phone", "xxxx", "••••", "****",
            "a/c", "account", "ifsc", "upi id", "google transaction"
        )

        data class Candidate(val value: Double, val score: Int, val lineIndex: Int)
        val candidates = mutableListOf<Candidate>()

        lines.forEachIndexed { idx, line ->
            val text = line.text
            val lower = text.lowercase(Locale.ENGLISH)
            if (ignoreLineKeywords.any { lower.contains(it) }) return@forEachIndexed
            if (Regex("""\b\d{10,}\b""").containsMatchIn(text)) return@forEachIndexed

            val topBonus = if (idx < (lines.size.coerceAtLeast(1) + 1) / 2) 15 else 0
            val heightBonus = when {
                maxHeight > 0 && line.heightPx >= (maxHeight * 0.78f) -> 45
                maxHeight > 0 && line.heightPx >= (maxHeight * 0.55f) -> 20
                else -> 0
            }

            // 1. Explicit ₹ / Rs / INR match
            currencyRegex.findAll(text).forEach { match ->
                val rawNum = match.groupValues[1]
                val parsed = parseAmountValue(rawNum)
                if (parsed != null) {
                    val standaloneBonus = if (text.length <= rawNum.length + 5) 20 else 5
                    candidates.add(Candidate(parsed, 70 + topBonus + heightBonus + standaloneBonus, idx))
                }
            }

            // 2. Misread ₹ symbol on a standalone line (e.g., "?450" or "R1,200.00")
            val misread = misreadCurrencyRegex.matchEntire(text)
            if (misread != null) {
                val parsed = parseAmountValue(misread.groupValues[1])
                if (parsed != null) {
                    candidates.add(Candidate(parsed, 50 + topBonus + heightBonus, idx))
                }
            }

            // 3. Standalone numeric line (especially hero font size or right near Paid/Completed/To)
            val standalone = standaloneNumberRegex.matchEntire(text)
            if (standalone != null) {
                val rawNum = standalone.groupValues[1]
                val parsed = parseAmountValue(rawNum)
                // Avoid bare 4-digit years like 2025/2026 unless clearly tall hero text
                val looksLikeYear = rawNum.length == 4 && rawNum.startsWith("20") && !rawNum.contains(".") && !rawNum.contains(",")
                if (parsed != null && (!looksLikeYear || heightBonus >= 45)) {
                    val neighborBonus = if (hasPaymentNeighbor(lines, idx)) 15 else 0
                    val commaOrDecimalBonus = if (rawNum.contains(",") || rawNum.contains(".")) 10 else 0
                    candidates.add(Candidate(parsed, 25 + topBonus + heightBonus + neighborBonus + commaOrDecimalBonus, idx))
                }
            }
        }

        return candidates
            .sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.lineIndex })
            .firstOrNull()
            ?.value
    }

    private fun parseAmountValue(raw: String): Double? {
        val cleaned = raw.replace(",", "").trim()
        val intDigits = cleaned.substringBefore(".")
        if (intDigits.length > 7) return null // Exclude 8+ digit refs/phones
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value <= 0.0 || value > 10_000_000.0) return null
        return value
    }

    private fun hasPaymentNeighbor(lines: List<OcrLine>, idx: Int): Boolean {
        val window = listOfNotNull(lines.getOrNull(idx - 1), lines.getOrNull(idx + 1))
        return window.any { l ->
            val t = l.text.lowercase(Locale.ENGLISH)
            t.contains("paid") || t.contains("completed") || t.contains("successful") ||
                t.contains("received") || t.startsWith("to ") || t.startsWith("from ")
        }
    }

    private fun extractDateIso(lines: List<OcrLine>, today: LocalDate): String {
        val dmyWordRegex = Regex(
            """\b(\d{1,2})[\s\-/]+(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*(?:[\s,\-/]+(20\d{2})\b|[\-/]+(\d{2})\b(?!\s*:))""",
            RegexOption.IGNORE_CASE
        )
        val mdyWordRegex = Regex(
            """\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*[\s]+(\d{1,2})(?:st|nd|rd|th)?[\s,]+(20\d{2})\b(?!\s*:)""",
            RegexOption.IGNORE_CASE
        )
        val dmyNumRegex = Regex("""\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})\b(?!\s*:)""")
        val isoRegex = Regex("""\b(20\d{2})-(\d{2})-(\d{2})\b""")
        val dmNoYearRegex = Regex(
            """\b(\d{1,2})[\s\-]+(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\b""",
            RegexOption.IGNORE_CASE
        )
        val mdNoYearRegex = Regex(
            """\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*[\s]+(\d{1,2})\b""",
            RegexOption.IGNORE_CASE
        )

        for (line in lines) {
            val text = line.text

            isoRegex.find(text)?.let { m ->
                safeLocalDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), today)
                    ?.let { return it.format(isoFormatter) }
            }

            dmyWordRegex.find(text)?.let { m ->
                val day = m.groupValues[1].toIntOrNull()
                val month = monthMap[m.groupValues[2].lowercase(Locale.ENGLISH)]
                val rawYear = m.groupValues[3].ifBlank { m.groupValues[4] }
                val year = normalizeYear(rawYear.toIntOrNull())
                if (day != null && month != null && year != null) {
                    safeLocalDate(year, month.value, day, today)?.let { return it.format(isoFormatter) }
                }
            }

            mdyWordRegex.find(text)?.let { m ->
                val month = monthMap[m.groupValues[1].lowercase(Locale.ENGLISH)]
                val day = m.groupValues[2].toIntOrNull()
                val year = normalizeYear(m.groupValues[3].toIntOrNull())
                if (day != null && month != null && year != null) {
                    safeLocalDate(year, month.value, day, today)?.let { return it.format(isoFormatter) }
                }
            }

            dmyNumRegex.find(text)?.let { m ->
                val day = m.groupValues[1].toIntOrNull()
                val month = m.groupValues[2].toIntOrNull()
                val year = normalizeYear(m.groupValues[3].toIntOrNull())
                if (day != null && month != null && year != null) {
                    safeLocalDate(year, month, day, today)?.let { return it.format(isoFormatter) }
                }
            }
        }

        // Second pass: year-less dates ("14 Nov, 8:32 PM") or Today/Yesterday
        for (line in lines) {
            val text = line.text
            val lower = text.lowercase(Locale.ENGLISH)
            if (lower.contains("yesterday")) return today.minusDays(1).format(isoFormatter)
            if (lower.contains("today")) return today.format(isoFormatter)

            dmNoYearRegex.find(text)?.let { m ->
                val day = m.groupValues[1].toIntOrNull()
                val month = monthMap[m.groupValues[2].lowercase(Locale.ENGLISH)]
                if (day != null && month != null) {
                    inferYearDate(month.value, day, today)?.let { return it.format(isoFormatter) }
                }
            }
            mdNoYearRegex.find(text)?.let { m ->
                val month = monthMap[m.groupValues[1].lowercase(Locale.ENGLISH)]
                val day = m.groupValues[2].toIntOrNull()
                if (day != null && month != null) {
                    inferYearDate(month.value, day, today)?.let { return it.format(isoFormatter) }
                }
            }
        }

        return today.format(isoFormatter)
    }

    private fun normalizeYear(raw: Int?): Int? {
        if (raw == null) return null
        return when {
            raw in 2000..2100 -> raw
            raw in 0..99 -> 2000 + raw
            else -> null
        }
    }

    private fun safeLocalDate(year: Int, month: Int, day: Int, today: LocalDate): LocalDate? {
        if (year < 2000 || year > today.year + 1) return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun inferYearDate(month: Int, day: Int, today: LocalDate): LocalDate? {
        val candidate = safeLocalDate(today.year, month, day, today) ?: return null
        return if (candidate.isAfter(today.plusDays(1))) {
            safeLocalDate(today.year - 1, month, day, today) ?: candidate
        } else {
            candidate
        }
    }

    private fun extractUtr(lines: List<OcrLine>): String? {
        val utrLabelRegex = Regex(
            """(?i)\b(utr|upi\s*transaction\s*id|upi\s*ref\.?\s*(?:no\.?|id|number)?|transaction\s*reference\s*(?:id|no\.?)?|ref\.?\s*(?:no\.?|id)|rrn)\b"""
        )
        val twelveDigitRegex = Regex("""\b(\d{12})\b""")
        val genericTxnLabelRegex = Regex("""(?i)\b(transaction\s*id|txn\s*id|order\s*id)\b""")
        val alnumIdRegex = Regex("""\b([A-Z0-9]{10,28})\b""")

        // Pass 1: Look specifically for 12-digit NPCI UTR / UPI transaction ID
        for (i in lines.indices) {
            val line = lines[i].text
            if (utrLabelRegex.containsMatchIn(line)) {
                twelveDigitRegex.find(line)?.let { return it.groupValues[1] }
                val next = lines.getOrNull(i + 1)?.text
                if (next != null) {
                    twelveDigitRegex.find(next)?.let { return it.groupValues[1] }
                }
            }
        }

        // Pass 2: Any standalone 12-digit number (very characteristic of Indian UPI UTR/RRN)
        for (line in lines) {
            val cleaned = line.text.trim()
            if (Regex("""^\d{12}$""").matches(cleaned)) return cleaned
        }

        // Pass 3: Fallback to PhonePe/Paytm Transaction ID (e.g. T261114...)
        for (i in lines.indices) {
            val line = lines[i].text
            if (genericTxnLabelRegex.containsMatchIn(line) && !line.lowercase(Locale.ENGLISH).contains("google")) {
                val afterColon = line.substringAfter(":", "").trim()
                if (afterColon.length in 10..28 && afterColon.any { it.isDigit() }) {
                    alnumIdRegex.find(afterColon)?.let { return it.groupValues[1] }
                }
                val next = lines.getOrNull(i + 1)?.text
                if (next != null && next.any { it.isDigit() }) {
                    alnumIdRegex.find(next)?.let { return it.groupValues[1] }
                }
            }
        }

        return null
    }

    private fun extractCounterpartyVpa(lines: List<OcrLine>, type: TransactionType): String? {
        val vpaRegex = Regex("""\b([a-zA-Z0-9][a-zA-Z0-9._-]{1,63}@[a-zA-Z][a-zA-Z0-9]{1,24})\b""")
        val found = mutableListOf<Pair<Int, String>>()

        lines.forEachIndexed { idx, line ->
            vpaRegex.findAll(line.text).forEach { match ->
                val vpa = match.groupValues[1].lowercase(Locale.ENGLISH)
                val domain = vpa.substringAfter("@")
                if (domain !in emailDomainBlocklist && !domain.contains(".")) {
                    found.add(idx to vpa)
                }
            }
        }

        if (found.isEmpty()) return null
        if (found.size == 1) return found.first().second

        // Prefer VPA adjacent to "To:" for Expense or "From:" for Income
        val targetMarker = if (type == TransactionType.INCOME) "from" else "to"
        for ((idx, vpa) in found) {
            val contextLines = listOfNotNull(
                lines.getOrNull(idx - 2)?.text,
                lines.getOrNull(idx - 1)?.text,
                lines.getOrNull(idx)?.text
            ).joinToString(" ").lowercase(Locale.ENGLISH)
            if (Regex("""\b$targetMarker\b""").containsMatchIn(contextLines)) {
                return vpa
            }
        }

        return found.first().second
    }

    private fun extractMerchantReason(
        lines: List<OcrLine>,
        type: TransactionType,
        upiId: String?,
        detectedApp: String
    ): String {
        val primaryMarkers = if (type == TransactionType.INCOME) {
            listOf("received from", "from:", "from ")
        } else {
            listOf("paid to", "sent to", "payment to", "transfer to", "to:", "to ")
        }

        val candidates = mutableListOf<Pair<Int, String>>()

        lines.forEachIndexed { idx, line ->
            val raw = line.text
            val lower = raw.lowercase(Locale.ENGLISH)

            // Banking name label is often very clean
            if (lower.startsWith("banking name")) {
                val inline = cleanMerchantName(raw.substringAfter(":", ""))
                if (inline.isNotBlank()) {
                    candidates.add(85 to inline)
                } else {
                    val next = cleanMerchantName(lines.getOrNull(idx + 1)?.text.orEmpty())
                    if (next.isNotBlank()) candidates.add(80 to next)
                }
            }

            for ((markerIdx, marker) in primaryMarkers.withIndex()) {
                val exactLabel = marker.trimEnd(':', ' ')
                if (lower == exactLabel || lower == "$exactLabel:") {
                    val next = cleanMerchantName(lines.getOrNull(idx + 1)?.text.orEmpty())
                    if (next.isNotBlank()) {
                        candidates.add((100 - markerIdx * 5) to next)
                    }
                } else if (lower.startsWith(marker)) {
                    val remainder = raw.substring(marker.length).trim().trimStart(':', '-', '–')
                    val cleaned = cleanMerchantName(remainder)
                    if (cleaned.isNotBlank()) {
                        candidates.add((105 - markerIdx * 5) to cleaned)
                    } else {
                        val next = cleanMerchantName(lines.getOrNull(idx + 1)?.text.orEmpty())
                        if (next.isNotBlank()) candidates.add((95 - markerIdx * 5) to next)
                    }
                }
            }
        }

        val best = candidates.maxByOrNull { it.first }?.second
        if (!best.isNullOrBlank()) return best.take(50)

        // Fallback: derive friendly name from counterparty UPI ID prefix
        if (!upiId.isNullOrBlank()) {
            val prefix = upiId.substringBefore("@")
                .replace(Regex("[._-]+"), " ")
                .replace(Regex("\\d{6,}"), "")
                .trim()
            if (prefix.length >= 2 && prefix.any { it.isLetter() }) {
                return StringUtils.displayName(prefix).take(50)
            }
        }

        val actionWord = if (type == TransactionType.INCOME) "Receipt" else "Payment"
        return "$detectedApp $actionWord".take(50)
    }

    private fun cleanMerchantName(raw: String): String {
        var s = raw.trim()
        if (s.isBlank()) return ""

        // Remove inline amounts, VPAs, phone numbers, parenthesized bank tags
        s = s.replace(Regex("""(?i)(?:₹|rs\.?|inr)\s*[0-9,]+(?:\.[0-9]{1,2})?"""), "")
        s = s.replace(Regex("""\b[a-zA-Z0-9._-]+@[a-zA-Z0-9]+\b"""), "")
        s = s.replace(Regex("""(?:\+91[\s-]*)?\b[6-9]\d{9}\b"""), "")
        s = s.replace(Regex("""\([^)]*bank[^)]*\)""", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("""(?i)\bverified\s+merchant\b"""), "")
        s = s.replace(Regex("""\s+"""), " ").trim(' ', '-', '–', ':', '•', '|')

        if (s.length < 2) return ""
        val lower = s.lowercase(Locale.ENGLISH)
        if (lower in boilerplateReasonLines) return ""
        if (s.all { it.isDigit() || it == ',' || it == '.' || it == ' ' }) return ""
        if (lower.contains("transaction id") || lower.contains("utr") || lower.contains("ref no")) return ""

        // Title-case all-uppercase merchant names for cleaner ledger presentation
        return if (s == s.uppercase(Locale.ENGLISH) && s.any { it.isLetter() }) {
            StringUtils.displayName(s)
        } else {
            s
        }
    }

    private fun buildNotes(detectedApp: String, utr: String?, upiId: String?): String {
        val parts = mutableListOf<String>()
        parts.add("Via $detectedApp")
        if (!utr.isNullOrBlank()) parts.add("UTR: $utr")
        if (!upiId.isNullOrBlank()) parts.add("UPI ID: $upiId")
        return parts.joinToString(" • ").take(1000)
    }

    private data class AccountMatchResult(val account: Account, val hint: String)

    private fun matchActiveAccount(
        lines: List<OcrLine>,
        type: TransactionType,
        activeAccounts: List<Account>,
        fallbackAccountId: String?
    ): AccountMatchResult? {
        if (activeAccounts.isEmpty()) return null

        // Identify bank-focused lines (Debited from, Credited to, Paid from, From/To user bank, XXXX digits, Bank names)
        // Exclude "Powered by ..." PSP footers so they never masquerade as the user's bank
        val accountContextIndices = mutableSetOf<Int>()
        val bankCueRegex = Regex(
            """(?i)\b(debited\s*from|credited\s*to|paid\s*from|paid\s*using|a/c|account|bank|xxxx|xx\d{4}|ending\s*in)\b|•{2,}|\*{2,}"""
        )
        val userSectionCue = if (type == TransactionType.INCOME) {
            Regex("""(?i)^(to:|credited\s*to)""")
        } else {
            Regex("""(?i)^(from:|debited\s*from|paid\s*from|paid\s*using)""")
        }

        lines.forEachIndexed { idx, line ->
            val lower = line.text.lowercase(Locale.ENGLISH)
            if (lower.contains("powered by")) return@forEachIndexed
            if (bankCueRegex.containsMatchIn(line.text) || userSectionCue.containsMatchIn(line.text)) {
                accountContextIndices.add(idx)
                if (idx + 1 < lines.size && !lines[idx + 1].text.lowercase(Locale.ENGLISH).contains("powered by")) {
                    accountContextIndices.add(idx + 1)
                }
            }
        }

        val contextLines = accountContextIndices.sorted().map { lines[it].text }
        val nonFooterLines = lines.map { it.text }.filterNot { it.lowercase(Locale.ENGLISH).contains("powered by") }

        // Step 1: Extract masked / bank-associated 4–6 digit account suffixes
        val digitCandidates = extractMaskedAccountDigits(contextLines)
            .ifEmpty { extractMaskedAccountDigits(nonFooterLines) }

        if (digitCandidates.isNotEmpty()) {
            val digitMatches = activeAccounts.mapNotNull { acc ->
                val accDigits = extractAccountSignatureDigits(acc)
                val matched = digitCandidates.firstNotNullOfOrNull { cand ->
                    accDigits.firstOrNull { sig ->
                        sig == cand ||
                            (sig.length >= 4 && cand.length >= 4 && (sig.endsWith(cand.takeLast(4)) || cand.endsWith(sig.takeLast(4))))
                    }?.let { cand.takeLast(4) }
                }
                matched?.let { acc to it }
            }

            if (digitMatches.isNotEmpty()) {
                val chosen = digitMatches.firstOrNull { it.first.id == fallbackAccountId } ?: digitMatches.first()
                return AccountMatchResult(
                    account = chosen.first,
                    hint = "${StringUtils.displayName(chosen.first.name)} •••• ${chosen.second}"
                )
            }
        }

        // Step 2: Bank name / alias matching (strip VPAs first so merchant@hdfcbank doesn't match user's HDFC!)
        val cleanedContext = contextLines.joinToString("\n")
            .replace(Regex("""\b[a-zA-Z0-9._-]+@[a-zA-Z0-9]+\b"""), "")
            .lowercase(Locale.ENGLISH)
        val cleanedAll = nonFooterLines.joinToString("\n")
            .replace(Regex("""\b[a-zA-Z0-9._-]+@[a-zA-Z0-9]+\b"""), "")
            .lowercase(Locale.ENGLISH)

        val matchedByName = scoreAccountsByBankName(activeAccounts, cleanedContext, fallbackAccountId)
            ?: scoreAccountsByBankName(activeAccounts, cleanedAll, fallbackAccountId)

        if (matchedByName != null) {
            return AccountMatchResult(
                account = matchedByName,
                hint = StringUtils.displayName(matchedByName.name)
            )
        }

        return null
    }

    private fun extractMaskedAccountDigits(lines: List<String>): List<String> {
        val maskedRegex = Regex(
            """(?i)(?:x{2,}|•{2,}|\*{2,}|\.{2,}|a/c\s*(?:no\.?)?\s*x*|ending\s*in|bank\s*[-–•\s]+|account\s*[-–•\s]+)\s*[-–•]?\s*(\d{4,6})\b"""
        )
        val trailingBankDigitsRegex = Regex("""(?i)\b(?:bank|sbi|hdfc|icici|axis|kotak|canara|pnb|bob|idfc|indusind|federal)\b[^\n\d]{0,12}(\d{4,6})\b""")
        val results = linkedSetOf<String>()

        for (line in lines) {
            val lower = line.lowercase(Locale.ENGLISH)
            if (lower.contains("utr") || lower.contains("ref") || lower.contains("transaction id") || lower.contains("+91")) {
                continue
            }
            maskedRegex.findAll(line).forEach { m ->
                val d = m.groupValues[1]
                if (!isLikelyYear(d)) results.add(d)
            }
            trailingBankDigitsRegex.findAll(line).forEach { m ->
                val d = m.groupValues[1]
                if (!isLikelyYear(d)) results.add(d)
            }
        }
        return results.toList()
    }

    private fun isLikelyYear(digits: String): Boolean =
        digits.length == 4 && (digits.startsWith("202") || digits.startsWith("201") || digits.startsWith("199"))

    private fun extractAccountSignatureDigits(account: Account): List<String> {
        val out = mutableListOf<String>()
        val numDigits = account.accountNumber.filter { it.isDigit() }
        if (numDigits.length >= 4) out.add(numDigits)

        Regex("""\b(\d{4,6})\b""").findAll(account.name).forEach { m ->
            out.add(m.groupValues[1])
        }
        Regex("""\b(\d{4,6})\b""").findAll(account.description).forEach { m ->
            out.add(m.groupValues[1])
        }
        return out
    }

    private fun scoreAccountsByBankName(
        activeAccounts: List<Account>,
        receiptLowerText: String,
        fallbackAccountId: String?
    ): Account? {
        if (receiptLowerText.isBlank()) return null

        val detectedBankKeys = mutableSetOf<String>()
        for ((canonicalKey, aliases) in bankAliases) {
            if (aliases.any { alias -> containsWholePhrase(receiptLowerText, alias) }) {
                detectedBankKeys.add(canonicalKey)
            }
        }

        val scored = activeAccounts.mapNotNull { acc ->
            val accText = "${acc.name} ${acc.description}".lowercase(Locale.ENGLISH)
            var score = 0

            // Match via canonical bank alias table
            for ((canonicalKey, aliases) in bankAliases) {
                if (canonicalKey in detectedBankKeys && aliases.any { alias -> containsWholePhrase(accText, alias) }) {
                    score += 80
                }
            }

            // Direct match on full account name (if >= 4 chars and not overly generic)
            val accNameClean = acc.name.trim().lowercase(Locale.ENGLISH)
            if (accNameClean.length >= 4 && accNameClean !in setOf("cash", "bank", "main", "wallet", "savings", "salary")) {
                if (containsWholePhrase(receiptLowerText, accNameClean)) {
                    score += 100
                }
            }

            if (score > 0) {
                val tieBreak = if (acc.id == fallbackAccountId) 5 else 0
                acc to (score + tieBreak)
            } else {
                null
            }
        }

        return scored.maxByOrNull { it.second }?.first
    }

    private fun containsWholePhrase(haystack: String, phrase: String): Boolean {
        val escaped = Regex.escape(phrase)
        return Regex("""(?<![a-z0-9])$escaped(?![a-z0-9])""").containsMatchIn(haystack)
    }

    private fun detectDuplicate(
        transactions: List<Transaction>,
        type: TransactionType,
        amount: Double?,
        dateIso: String,
        resolvedAccountId: String?,
        utr: String?,
        currencySymbol: String
    ): String? {
        // 1. Exact UTR match in existing transaction notes
        if (!utr.isNullOrBlank() && utr.length >= 8) {
            val utrMatch = transactions.firstOrNull { txn ->
                txn.notes.contains(utr, ignoreCase = true)
            }
            if (utrMatch != null) {
                return "UTR $utr already logged in \"${utrMatch.reason}\" on ${StringUtils.formatDisplayDate(utrMatch.date)} (${StringUtils.formatCurrency(utrMatch.amount, currencySymbol)})."
            }
        }

        // 2. Exact Amount + Date + Type (+ Account if resolved) match
        if (amount != null && amount > 0.0) {
            val exactMatch = transactions.firstOrNull { txn ->
                txn.type == type &&
                    txn.date == dateIso &&
                    abs(txn.amount - amount) < 0.01 &&
                    (resolvedAccountId.isNullOrBlank() || txn.accountId == resolvedAccountId)
            }
            if (exactMatch != null) {
                return "A matching ${exactMatch.type.label.lowercase(Locale.ENGLISH)} of ${StringUtils.formatCurrency(exactMatch.amount, currencySymbol)} on ${StringUtils.formatDisplayDate(exactMatch.date)} (\"${exactMatch.reason}\") already exists."
            }
        }

        return null
    }
}
