package com.hisaabkitaab.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

object StringUtils {
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)

    fun toLowerTrim(input: String): String =
        input.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ENGLISH)

    fun displayName(input: String): String {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) return ""
        return cleaned.split(Regex("\\s+")).joinToString(" ") { word ->
            if (word.isEmpty()) ""
            else word.lowercase(Locale.ENGLISH)
                .replaceFirstChar { ch -> ch.titlecase(Locale.ENGLISH) }
        }
    }

    /**
     * Formats currency using Indian number grouping (1,23,456.78) matching web's `en-IN` formatting.
     */
    fun formatCurrency(
        amount: Double,
        symbol: String = "₹",
        showPositiveSign: Boolean = false
    ): String {
        val safeSymbol = symbol.ifBlank { "₹" }
        val isNegative = amount < -0.0001
        val absVal = abs(amount)
        val formattedNumber = formatIndianNumber(absVal)
        val sign = when {
            isNegative -> "-"
            showPositiveSign && absVal > 0.0001 -> "+"
            else -> ""
        }
        return "$sign$safeSymbol$formattedNumber"
    }

    fun formatIndianNumber(value: Double): String {
        val safe = if (value.isNaN() || value.isInfinite()) 0.0 else abs(value)
        val symbols = DecimalFormatSymbols(Locale.US)
        val twoDecimals = DecimalFormat("0.00", symbols).format(safe)
        val parts = twoDecimals.split(".")
        val intPart = parts[0]
        val fracPart = parts.getOrElse(1) { "00" }
        if (intPart.length <= 3) {
            return "$intPart.$fracPart"
        }
        val lastThree = intPart.takeLast(3)
        val remaining = intPart.dropLast(3)
        val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
        return "$grouped,$lastThree.$fracPart"
    }

    fun formatCompactCurrency(amount: Double, symbol: String = "₹"): String {
        val absVal = abs(amount)
        val sign = if (amount < -0.0001) "-" else ""
        val sym = symbol.ifBlank { "₹" }
        return when {
            absVal >= 10_000_000 -> String.format(Locale.US, "%s%s%.2fCr", sign, sym, absVal / 10_000_000)
            absVal >= 100_000 -> String.format(Locale.US, "%s%s%.2fL", sign, sym, absVal / 100_000)
            absVal >= 1_000 -> String.format(Locale.US, "%s%s%.1fK", sign, sym, absVal / 1_000)
            else -> formatCurrency(amount, sym)
        }
    }

    fun todayIso(): String = LocalDate.now().format(isoFormatter)

    fun daysAgoIso(days: Long): String = LocalDate.now().minusDays(days).format(isoFormatter)

    fun parseIsoDate(iso: String): LocalDate? = runCatching {
        LocalDate.parse(iso.trim().take(10), isoFormatter)
    }.getOrNull()

    fun formatDisplayDate(iso: String): String {
        val parsed = parseIsoDate(iso) ?: return iso
        return parsed.format(displayDateFormatter)
    }

    fun formatShortDate(iso: String): String {
        val parsed = parseIsoDate(iso) ?: return iso
        return parsed.format(shortDateFormatter)
    }

    fun epochMillisToIso(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
            .format(isoFormatter)
    }

    fun isoToEpochMillis(iso: String): Long? {
        val parsed = parseIsoDate(iso) ?: return null
        return parsed.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }

    fun computePeriodRange(
        preset: PeriodPreset,
        customStart: String = "",
        customEnd: String = ""
    ): DateRange {
        val today = LocalDate.now()
        return when (preset) {
            PeriodPreset.THIS_MONTH -> DateRange(
                start = today.withDayOfMonth(1).format(isoFormatter),
                end = today.format(isoFormatter)
            )
            PeriodPreset.LAST_MONTH -> {
                val prev = today.minusMonths(1)
                DateRange(
                    start = prev.withDayOfMonth(1).format(isoFormatter),
                    end = prev.withDayOfMonth(prev.lengthOfMonth()).format(isoFormatter)
                )
            }
            PeriodPreset.LAST_30 -> DateRange(
                start = today.minusDays(29).format(isoFormatter),
                end = today.format(isoFormatter)
            )
            PeriodPreset.LAST_7 -> DateRange(
                start = today.minusDays(6).format(isoFormatter),
                end = today.format(isoFormatter)
            )
            PeriodPreset.THIS_YEAR -> DateRange(
                start = today.withDayOfYear(1).format(isoFormatter),
                end = today.format(isoFormatter)
            )
            PeriodPreset.CUSTOM -> {
                val s = parseIsoDate(customStart) ?: today.withDayOfMonth(1)
                val e = parseIsoDate(customEnd) ?: today
                if (s.isAfter(e)) {
                    DateRange(e.format(isoFormatter), s.format(isoFormatter))
                } else {
                    DateRange(s.format(isoFormatter), e.format(isoFormatter))
                }
            }
        }
    }

    fun eachDay(startIso: String, endIso: String, maxDays: Int = 366): List<String> {
        val start = parseIsoDate(startIso) ?: return emptyList()
        val end = parseIsoDate(endIso) ?: return emptyList()
        if (start.isAfter(end)) return emptyList()
        val result = mutableListOf<String>()
        var cursor = start
        var count = 0
        while (!cursor.isAfter(end) && count < maxDays) {
            result.add(cursor.format(isoFormatter))
            cursor = cursor.plusDays(1)
            count++
        }
        return result
    }

    fun generateId(prefix: String): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val suffix = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "${prefix}_${System.currentTimeMillis()}_$suffix"
    }

    fun generateCaptcha(length: Int = 6): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    fun normalizeHexColor(input: String, fallback: String = "#3B82F6"): String {
        val trimmed = input.trim().uppercase(Locale.ENGLISH)
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        return if (Regex("^#[0-9A-F]{6}$").matches(withHash)) withHash else fallback
    }

    fun isValidHexColor(input: String): Boolean {
        val trimmed = input.trim()
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        return Regex("^#[0-9A-Fa-f]{6}$").matches(withHash)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        return String.format(Locale.US, "%.2f MB", kb / 1024.0)
    }
}
