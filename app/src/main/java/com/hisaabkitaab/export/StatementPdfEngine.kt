package com.hisaabkitaab.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.hisaabkitaab.R
import com.hisaabkitaab.data.StatementReport
import com.hisaabkitaab.data.StringUtils
import com.hisaabkitaab.data.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object StatementPdfEngine {

    private const val PAGE_WIDTH = 842  // Landscape A4 in points
    private const val PAGE_HEIGHT = 595
    private const val MARGIN_X = 32f
    private const val MARGIN_TOP = 30f
    private const val FOOTER_Y = PAGE_HEIGHT - 22f
    private const val CONTENT_BOTTOM = PAGE_HEIGHT - 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    private val colorInk = Color.parseColor("#181A20")
    private val colorMuted = Color.parseColor("#64748B")
    private val colorHairline = Color.parseColor("#E2E8F0")
    private val colorHeaderBg = Color.parseColor("#181A20")
    private val colorZebra = Color.parseColor("#F8FAFC")
    private val colorSoftCard = Color.parseColor("#F1F5F9")
    private val colorAccent = Color.parseColor("#F0B90B")
    private val colorGreen = Color.parseColor("#0B9E62")
    private val colorRed = Color.parseColor("#DC2626")
    private val colorBrandLime = Color.parseColor("#6BDA0A")

    fun generatePdf(
        report: StatementReport,
        profile: UserProfile,
        includeWatermark: Boolean,
        outputStream: OutputStream,
        context: Context? = null
    ) {
        val doc = PdfDocument()
        val pages = mutableListOf<PdfDocument.Page>()
        val symbol = profile.effectiveSymbol
        val logoBitmap = context?.let {
            runCatching {
                BitmapFactory.decodeResource(it.resources, R.drawable.ic_hisaab_logo_transparent)
            }.getOrNull()
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInk
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorMuted
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInk
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInk
            textSize = 8.3f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInk
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInk
            textSize = 8.2f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val monoBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInk
            textSize = 8.4f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = colorHairline
        }

        // Helper to start a new page with watermarks
        var pageNumber = 0
        fun startPage(): Pair<PdfDocument.Page, Float> {
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            drawWatermark(canvas, profile, includeWatermark, logoBitmap)
            return page to MARGIN_TOP
        }

        var (currentPage, y) = startPage()
        var canvas = currentPage.canvas

        // ---- PAGE 1 HEADER ----
        drawLogoEmblem(canvas, MARGIN_X, y + 2f, 28f, logoBitmap)
        canvas.drawText("HisaabKitaab", MARGIN_X + 34f, y + 14f, titlePaint)
        canvas.drawText(
            "Financial Statement  •  ${StringUtils.formatDisplayDate(report.range.start)} to ${StringUtils.formatDisplayDate(report.range.end)}",
            MARGIN_X + 34f,
            y + 27f,
            subtitlePaint
        )

        val rightMetaPaint = Paint(subtitlePaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(
            "Generated: ${StringUtils.formatDisplayDate(StringUtils.todayIso())}",
            PAGE_WIDTH - MARGIN_X,
            y + 13f,
            rightMetaPaint
        )
        canvas.drawText(
            "Currency: ${profile.effectiveCurrencyName} ($symbol)",
            PAGE_WIDTH - MARGIN_X,
            y + 26f,
            rightMetaPaint
        )
        y += 38f

        // Accent bar
        bgPaint.color = colorAccent
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 2.5f, bgPaint)
        y += 12f

        // ---- PROFILE BLOCK ----
        val hasProfile = profile.name.isNotBlank() || profile.email.isNotBlank() ||
            profile.contact.isNotBlank() || profile.address.isNotBlank()
        if (hasProfile) {
            val boxTop = y
            val boxHeight = 46f
            bgPaint.color = colorSoftCard
            canvas.drawRoundRect(
                RectF(MARGIN_X, boxTop, PAGE_WIDTH - MARGIN_X, boxTop + boxHeight),
                6f, 6f, bgPaint
            )
            canvas.drawRoundRect(
                RectF(MARGIN_X, boxTop, PAGE_WIDTH - MARGIN_X, boxTop + boxHeight),
                6f, 6f, strokePaint
            )
            val leftColX = MARGIN_X + 12f
            val midColX = MARGIN_X + CONTENT_WIDTH * 0.45f
            canvas.drawText(
                profile.name.ifBlank { "Account Holder" },
                leftColX,
                boxTop + 17f,
                sectionPaint
            )
            if (profile.address.isNotBlank()) {
                canvas.drawText(
                    truncateText(profile.address, cellPaint, CONTENT_WIDTH * 0.40f),
                    leftColX,
                    boxTop + 33f,
                    subtitlePaint
                )
            }
            if (profile.email.isNotBlank()) {
                canvas.drawText("Email: ${profile.email}", midColX, boxTop + 17f, cellPaint)
            }
            if (profile.contact.isNotBlank()) {
                canvas.drawText("Contact: ${profile.contact}", midColX, boxTop + 33f, cellPaint)
            }
            y += boxHeight + 14f
        }

        // ---- ACCOUNTS SUMMARY TABLE ----
        if (report.accountSummaries.isNotEmpty()) {
            canvas.drawText("Accounts Summary", MARGIN_X, y + 10f, sectionPaint)
            y += 16f

            val accColWidths = floatArrayOf(180f, 95f, 125f, 125f, 125f, 128f)
            val accHeaders = listOf("Account", "A/C No.", "Opening Bal.", "Total Credit", "Total Debit", "Closing Bal.")
            drawTableHeader(canvas, MARGIN_X, y, accColWidths, accHeaders, headerTextPaint, bgPaint, rightAlignFrom = 2)
            y += 20f

            report.accountSummaries.forEachIndexed { idx, summary ->
                if (y + 18f > CONTENT_BOTTOM) {
                    pages.add(currentPage)
                    doc.finishPage(currentPage)
                    val next = startPage()
                    currentPage = next.first
                    canvas = currentPage.canvas
                    y = next.second
                    drawTableHeader(canvas, MARGIN_X, y, accColWidths, accHeaders, headerTextPaint, bgPaint, rightAlignFrom = 2)
                    y += 20f
                }
                if (idx % 2 == 1) {
                    bgPaint.color = colorZebra
                    canvas.drawRect(MARGIN_X, y, MARGIN_X + accColWidths.sum(), y + 18f, bgPaint)
                }
                var x = MARGIN_X
                val label = summary.accountName + if (summary.isClosed) " (Closed)" else ""
                canvas.drawText(truncateText(label, cellPaint, accColWidths[0] - 8f), x + 4f, y + 12.5f, cellPaint)
                x += accColWidths[0]
                val acNo = if (summary.accountNumber.isNotBlank()) "••••${summary.accountNumber}" else "—"
                canvas.drawText(acNo, x + 4f, y + 12.5f, cellPaint)
                x += accColWidths[1]

                monoPaint.color = colorInk
                canvas.drawText(StringUtils.formatCurrency(summary.openingBalanceBeforePeriod, symbol), x + accColWidths[2] - 6f, y + 12.5f, monoPaint)
                x += accColWidths[2]

                monoPaint.color = colorGreen
                canvas.drawText(StringUtils.formatCurrency(summary.periodCredit, symbol), x + accColWidths[3] - 6f, y + 12.5f, monoPaint)
                x += accColWidths[3]

                monoPaint.color = colorRed
                canvas.drawText(StringUtils.formatCurrency(summary.periodDebit, symbol), x + accColWidths[4] - 6f, y + 12.5f, monoPaint)
                x += accColWidths[4]

                monoBoldPaint.color = if (summary.closingBalance < 0) colorRed else colorInk
                canvas.drawText(StringUtils.formatCurrency(summary.closingBalance, symbol), x + accColWidths[5] - 6f, y + 12.5f, monoBoldPaint)

                canvas.drawLine(MARGIN_X, y + 18f, MARGIN_X + accColWidths.sum(), y + 18f, strokePaint)
                y += 18f
            }
            y += 16f
        }

        // ---- TRANSACTIONS TABLE (9 columns, total width = 778f) ----
        val txnColWidths = floatArrayOf(28f, 66f, 96f, 88f, 72f, 158f, 88f, 88f, 94f)
        val txnHeaders = listOf("#", "Date", "Account", "Category", "Medium", "Reason", "Credit", "Debit", "Balance")

        if (y + 44f > CONTENT_BOTTOM) {
            pages.add(currentPage)
            doc.finishPage(currentPage)
            val next = startPage()
            currentPage = next.first
            canvas = currentPage.canvas
            y = next.second
        }

        canvas.drawText("Ledger Entries (${report.rows.size})", MARGIN_X, y + 10f, sectionPaint)
        y += 16f
        drawTableHeader(canvas, MARGIN_X, y, txnColWidths, txnHeaders, headerTextPaint, bgPaint, rightAlignFrom = 6)
        y += 20f

        if (report.rows.isEmpty()) {
            canvas.drawText("No transactions recorded in the selected period.", MARGIN_X + 8f, y + 16f, subtitlePaint)
            y += 24f
        } else {
            report.rows.forEachIndexed { idx, row ->
                if (y + 18f > CONTENT_BOTTOM) {
                    pages.add(currentPage)
                    doc.finishPage(currentPage)
                    val next = startPage()
                    currentPage = next.first
                    canvas = currentPage.canvas
                    y = next.second
                    drawTableHeader(canvas, MARGIN_X, y, txnColWidths, txnHeaders, headerTextPaint, bgPaint, rightAlignFrom = 6)
                    y += 20f
                }
                if (idx % 2 == 1) {
                    bgPaint.color = colorZebra
                    canvas.drawRect(MARGIN_X, y, MARGIN_X + txnColWidths.sum(), y + 18f, bgPaint)
                }

                var x = MARGIN_X
                canvas.drawText(row.serialNo.toString(), x + 4f, y + 12.5f, cellPaint)
                x += txnColWidths[0]

                canvas.drawText(row.date, x + 4f, y + 12.5f, cellPaint)
                x += txnColWidths[1]

                canvas.drawText(truncateText(row.accountName, cellPaint, txnColWidths[2] - 8f), x + 4f, y + 12.5f, cellPaint)
                x += txnColWidths[2]

                canvas.drawText(truncateText(row.categoryName, cellPaint, txnColWidths[3] - 8f), x + 4f, y + 12.5f, cellPaint)
                x += txnColWidths[3]

                canvas.drawText(truncateText(row.paymentMediumName, cellPaint, txnColWidths[4] - 8f), x + 4f, y + 12.5f, cellPaint)
                x += txnColWidths[4]

                canvas.drawText(truncateText(row.reason, cellPaint, txnColWidths[5] - 8f), x + 4f, y + 12.5f, cellPaint)
                x += txnColWidths[5]

                if (row.credit > 0.0) {
                    monoPaint.color = colorGreen
                    canvas.drawText(StringUtils.formatCurrency(row.credit, symbol), x + txnColWidths[6] - 5f, y + 12.5f, monoPaint)
                } else {
                    monoPaint.color = colorMuted
                    canvas.drawText("—", x + txnColWidths[6] - 5f, y + 12.5f, monoPaint)
                }
                x += txnColWidths[6]

                if (row.debit > 0.0) {
                    monoPaint.color = colorRed
                    canvas.drawText(StringUtils.formatCurrency(row.debit, symbol), x + txnColWidths[7] - 5f, y + 12.5f, monoPaint)
                } else {
                    monoPaint.color = colorMuted
                    canvas.drawText("—", x + txnColWidths[7] - 5f, y + 12.5f, monoPaint)
                }
                x += txnColWidths[7]

                monoPaint.color = if (row.remainingBalance < 0) colorRed else colorInk
                canvas.drawText(StringUtils.formatCurrency(row.remainingBalance, symbol), x + txnColWidths[8] - 5f, y + 12.5f, monoPaint)

                canvas.drawLine(MARGIN_X, y + 18f, MARGIN_X + txnColWidths.sum(), y + 18f, strokePaint)
                y += 18f
            }

            // Totals Row
            if (y + 22f > CONTENT_BOTTOM) {
                pages.add(currentPage)
                doc.finishPage(currentPage)
                val next = startPage()
                currentPage = next.first
                canvas = currentPage.canvas
                y = next.second
            }
            bgPaint.color = colorSoftCard
            canvas.drawRect(MARGIN_X, y, MARGIN_X + txnColWidths.sum(), y + 22f, bgPaint)
            val totalsLabelX = MARGIN_X + txnColWidths.take(5).sum() + 4f
            canvas.drawText("PERIOD TOTALS", totalsLabelX, y + 14.5f, cellBoldPaint)

            val creditEndX = MARGIN_X + txnColWidths.take(7).sum() - 5f
            val debitEndX = MARGIN_X + txnColWidths.take(8).sum() - 5f
            val balEndX = MARGIN_X + txnColWidths.sum() - 5f

            monoBoldPaint.color = colorGreen
            canvas.drawText(StringUtils.formatCurrency(report.totalCredit, symbol), creditEndX, y + 14.5f, monoBoldPaint)
            monoBoldPaint.color = colorRed
            canvas.drawText(StringUtils.formatCurrency(report.totalDebit, symbol), debitEndX, y + 14.5f, monoBoldPaint)
            monoBoldPaint.color = if (report.totalClosingBalance < 0) colorRed else colorInk
            canvas.drawText(StringUtils.formatCurrency(report.totalClosingBalance, symbol), balEndX, y + 14.5f, monoBoldPaint)
        }

        // Draw footer on last page (and simple page numbers via helper)
        drawFooter(canvas, pageNumber, strokePaint, subtitlePaint)
        doc.finishPage(currentPage)

        doc.writeTo(outputStream)
        outputStream.flush()
        doc.close()
    }

    fun writeTempPdfForShare(
        context: Context,
        report: StatementReport,
        profile: UserProfile,
        includeWatermark: Boolean
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val suffix = if (includeWatermark) "watermarked" else "statement"
        val file = File(dir, "HisaabKitaab_${report.range.start}_to_${report.range.end}_$suffix.pdf")
        FileOutputStream(file).use { out ->
            generatePdf(report, profile, includeWatermark, out, context)
        }
        return file
    }

    fun buildShareIntent(context: Context, file: File, mimeType: String, chooserTitle: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, chooserTitle)
    }

    private fun drawTableHeader(
        canvas: Canvas,
        startX: Float,
        y: Float,
        widths: FloatArray,
        labels: List<String>,
        paint: Paint,
        bgPaint: Paint,
        rightAlignFrom: Int
    ) {
        bgPaint.color = colorHeaderBg
        canvas.drawRect(startX, y, startX + widths.sum(), y + 20f, bgPaint)
        var x = startX
        labels.forEachIndexed { idx, label ->
            val p = Paint(paint)
            if (idx >= rightAlignFrom) {
                p.textAlign = Paint.Align.RIGHT
                canvas.drawText(label, x + widths[idx] - 5f, y + 13.5f, p)
            } else {
                p.textAlign = Paint.Align.LEFT
                canvas.drawText(label, x + 4f, y + 13.5f, p)
            }
            x += widths[idx]
        }
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, strokePaint: Paint, textPaint: Paint) {
        canvas.drawLine(MARGIN_X, FOOTER_Y - 12f, PAGE_WIDTH - MARGIN_X, FOOTER_Y - 12f, strokePaint)
        canvas.drawText("HisaabKitaab • Local Offline Ledger", MARGIN_X, FOOTER_Y, textPaint)
        val right = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Page $pageNum", PAGE_WIDTH - MARGIN_X, FOOTER_Y, right)
    }

    private fun drawWatermark(
        canvas: Canvas,
        profile: UserProfile,
        includeTextWatermark: Boolean,
        logoBitmap: Bitmap?
    ) {
        // Subtle brand emblem watermark in center
        canvas.save()
        drawLogoWatermark(canvas, PAGE_WIDTH / 2f - 85f, PAGE_HEIGHT / 2f - 85f, 170f, logoBitmap)
        canvas.restore()

        if (includeTextWatermark) {
            val wmText = profile.watermark.ifBlank { profile.name.ifBlank { "HISAABKITAAB CONFIDENTIAL" } }
            canvas.save()
            canvas.rotate(-28f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
            val wmTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                alpha = 38
                textSize = 46f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(wmText, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f + 16f, wmTextPaint)
            canvas.restore()
        }
    }

    private fun drawLogoEmblem(canvas: Canvas, left: Float, top: Float, size: Float, logoBitmap: Bitmap?) {
        drawBrandBitmap(canvas, left, top, size, alphaVal = 255, logoBitmap = logoBitmap)
    }

    private fun drawLogoWatermark(canvas: Canvas, left: Float, top: Float, size: Float, logoBitmap: Bitmap?) {
        drawBrandBitmap(canvas, left, top, size, alphaVal = 24, logoBitmap = logoBitmap)
    }

    private fun drawBrandBitmap(
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float,
        alphaVal: Int,
        logoBitmap: Bitmap?
    ) {
        if (logoBitmap != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = alphaVal
            }
            canvas.drawBitmap(logoBitmap, null, RectF(left, top, left + size, top + size), paint)
        } else {
            val fallback = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D3926")
                alpha = alphaVal
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(left, top, left + size, top + size), size * 0.22f, size * 0.22f, fallback)
        }
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }
}
