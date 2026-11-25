package com.hisaabkitaab.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.UpiReceiptDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device 100% offline ML Kit OCR bridge for UPI screenshots.
 * Zero network calls, zero storage permissions (reads shared or Photo Picker URIs via ContentResolver).
 */
object UpiReceiptScanner {

    suspend fun scanFromUri(
        context: Context,
        uri: Uri,
        accounts: List<Account>,
        transactions: List<Transaction>,
        fallbackAccountId: String?,
        currencySymbol: String
    ): Result<UpiReceiptDraft> = withContext(Dispatchers.Default) {
        runCatching {
            val inputImage = withContext(Dispatchers.IO) {
                InputImage.fromFilePath(context, uri)
            }
            val visionText = recognizeTextOffline(inputImage)
            val orderedLines = extractLayoutOrderedLines(visionText)
            if (orderedLines.isEmpty()) {
                throw IllegalStateException("No readable text found in this screenshot.")
            }
            UpiReceiptParser.parseReceiptLines(
                rawLines = orderedLines,
                accounts = accounts,
                transactions = transactions,
                fallbackAccountId = fallbackAccountId,
                currencySymbol = currencySymbol
            )
        }
    }

    suspend fun scanFromFile(
        context: Context,
        file: File,
        accounts: List<Account>,
        transactions: List<Transaction>,
        fallbackAccountId: String?,
        currencySymbol: String
    ): Result<UpiReceiptDraft> {
        return scanFromUri(
            context = context,
            uri = Uri.fromFile(file),
            accounts = accounts,
            transactions = transactions,
            fallbackAccountId = fallbackAccountId,
            currencySymbol = currencySymbol
        )
    }

    /**
     * Copies a shared content URI into app cache so it remains safely readable
     * if the user needs to connect/create a database before scanning completes.
     */
    suspend fun copyUriToTempCache(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "pending_upi_scans").apply { mkdirs() }
            val outFile = File(dir, "receipt_${System.currentTimeMillis()}.img")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@runCatching null
            outFile
        }.getOrNull()
    }

    private suspend fun recognizeTextOffline(image: InputImage): Text {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        if (cont.isActive) cont.resume(result)
                    }
                    .addOnFailureListener { err ->
                        if (cont.isActive) cont.resumeWithException(err)
                    }
            }
        } finally {
            runCatching { recognizer.close() }
        }
    }

    /**
     * Orders ML Kit text lines in natural top-to-bottom, left-to-right reading order
     * while preserving bounding-box height so hero amounts are prioritized accurately.
     */
    private fun extractLayoutOrderedLines(visionText: Text): List<OcrLine> {
        val raw = mutableListOf<OcrLine>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val text = line.text.trim()
                if (text.isBlank()) continue
                val box = line.boundingBox
                raw.add(
                    OcrLine(
                        text = text,
                        heightPx = box?.height() ?: 0,
                        topPx = box?.top ?: 0,
                        leftPx = box?.left ?: 0
                    )
                )
            }
        }
        if (raw.isEmpty()) return emptyList()

        // Sort top-to-bottom, then left-to-right within the same horizontal band
        val sorted = raw.sortedWith(compareBy<OcrLine> { it.topPx }.thenBy { it.leftPx })
        val merged = mutableListOf<OcrLine>()
        var currentRow = mutableListOf<OcrLine>()

        fun flushRow() {
            if (currentRow.isEmpty()) return
            val orderedRow = currentRow.sortedBy { it.leftPx }
            // Keep individual lines if heights differ significantly (e.g., merchant name vs hero amount)
            val maxH = orderedRow.maxOf { it.heightPx }
            val minH = orderedRow.minOf { it.heightPx }
            if (orderedRow.size > 1 && maxH > 0 && minH >= (maxH * 0.65f)) {
                merged.add(
                    OcrLine(
                        text = orderedRow.joinToString(" ") { it.text },
                        heightPx = maxH,
                        topPx = orderedRow.minOf { it.topPx },
                        leftPx = orderedRow.first().leftPx
                    )
                )
            } else {
                merged.addAll(orderedRow)
            }
            currentRow = mutableListOf()
        }

        for (line in sorted) {
            if (currentRow.isEmpty()) {
                currentRow.add(line)
            } else {
                val anchor = currentRow.first()
                val bandTolerance = maxOf(anchor.heightPx, line.heightPx, 24) * 0.55f
                if (kotlin.math.abs(line.topPx - anchor.topPx) <= bandTolerance) {
                    currentRow.add(line)
                } else {
                    flushRow()
                    currentRow.add(line)
                }
            }
        }
        flushRow()

        return merged
    }
}
