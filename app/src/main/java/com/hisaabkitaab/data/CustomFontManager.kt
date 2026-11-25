package com.hisaabkitaab.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages user-imported custom .ttf and .otf font files in private internal app storage
 * (`filesDir/custom_fonts/`), so selected fonts persist permanently across unlimited app
 * restarts and only reset when app data is cleared or the app is uninstalled.
 */
object CustomFontManager {

    private const val DIR_NAME = "custom_fonts"
    private const val MAX_FONT_BYTES = 25L * 1024L * 1024L // 25 MB safety guard

    private val fontCache = ConcurrentHashMap<String, FontFamily>()

    private fun fontsDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun fontFile(context: Context, fileName: String): File {
        val safeName = File(fileName).name
        return File(fontsDir(context), safeName)
    }

    fun fontFileExists(context: Context, entry: CustomFontEntry): Boolean {
        val f = fontFile(context, entry.fileName)
        return f.isFile && f.length() > 12L
    }

    /**
     * Loads and caches a Compose [FontFamily] from an imported [CustomFontEntry].
     * Returns null safely if the file is missing or unreadable.
     */
    fun loadFontFamily(context: Context, entry: CustomFontEntry): FontFamily? {
        fontCache[entry.fileName]?.let { return it }
        val file = fontFile(context, entry.fileName)
        if (!file.isFile || file.length() <= 12L) return null
        val nativeTypeface = runCatching { Typeface.createFromFile(file) }.getOrNull() ?: return null
        if (nativeTypeface == Typeface.DEFAULT) return null
        val family = FontFamily(ComposeTypeface(nativeTypeface))
        fontCache[entry.fileName] = family
        return family
    }

    /**
     * Validates that the picked [uri] is a real `.ttf` or `.otf` font, copies it into private
     * internal storage, verifies Android's font engine can parse it, and returns the saved entry.
     */
    fun importFontFromUri(context: Context, uri: Uri): ImportFontResult {
        val appContext = context.applicationContext
        val rawName = resolveDisplayName(appContext, uri)
            ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            ?: ""

        val ext = rawName.substringAfterLast('.', missingDelimiterValue = "")
            .trim()
            .lowercase(Locale.US)

        if (ext != "ttf" && ext != "otf") {
            return ImportFontResult.Error("Only .ttf and .otf font files are supported.")
        }

        val dir = fontsDir(appContext)
        val tempFile = File(dir, "tmp_${UUID.randomUUID()}.$ext")

        return try {
            val copiedBytes = appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_FONT_BYTES) {
                            return ImportFontResult.Error("Font file is too large (max 25 MB).")
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                    total
                }
            } ?: return ImportFontResult.Error("Could not read the selected font file.")

            if (copiedBytes < 16L || !hasValidSfntHeader(tempFile)) {
                tempFile.delete()
                return ImportFontResult.Error("Invalid font file. Please choose a valid .ttf or .otf font.")
            }

            // Verify Android's native Typeface engine can actually parse this file
            val nativeTypeface = runCatching { Typeface.createFromFile(tempFile) }.getOrNull()
            if (nativeTypeface == null || nativeTypeface == Typeface.DEFAULT) {
                tempFile.delete()
                return ImportFontResult.Error("Android could not parse this font file. Try another .ttf or .otf file.")
            }

            val id = UUID.randomUUID().toString()
            val finalFileName = "font_${id.replace("-", "").take(12)}.$ext"
            val finalFile = File(dir, finalFileName)
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val verifiedTypeface = runCatching { Typeface.createFromFile(finalFile) }.getOrNull()
                ?: nativeTypeface
            fontCache[finalFileName] = FontFamily(ComposeTypeface(verifiedTypeface))

            val entry = CustomFontEntry(
                id = id,
                displayName = formatFriendlyFontName(rawName),
                fileName = finalFileName,
                extension = ext.uppercase(Locale.US),
                addedAtMillis = System.currentTimeMillis()
            )
            ImportFontResult.Success(entry)
        } catch (e: Exception) {
            tempFile.delete()
            ImportFontResult.Error("Could not import font: ${e.message ?: "unexpected error"}")
        }
    }

    fun deleteFontFile(context: Context, fileName: String) {
        fontCache.remove(fileName)
        runCatching {
            val file = fontFile(context, fileName)
            if (file.exists()) file.delete()
        }
    }

    private fun hasValidSfntHeader(file: File): Boolean {
        val header = ByteArray(4)
        val read = runCatching {
            file.inputStream().use { it.read(header) }
        }.getOrDefault(0)
        if (read < 4) return false

        val b0 = header[0].toInt() and 0xFF
        val b1 = header[1].toInt() and 0xFF
        val b2 = header[2].toInt() and 0xFF
        val b3 = header[3].toInt() and 0xFF

        // TrueType (0x00010000)
        val isTrueType = (b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00)
        // Apple TrueType ('true')
        val isAppleTrue = (b0 == 0x74 && b1 == 0x72 && b2 == 0x75 && b3 == 0x65)
        // OpenType CFF ('OTTO')
        val isOpenTypeCff = (b0 == 0x4F && b1 == 0x54 && b2 == 0x54 && b3 == 0x4F)

        return isTrueType || isAppleTrue || isOpenTypeCff
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun formatFriendlyFontName(rawFileName: String): String {
        val base = rawFileName
            .substringAfterLast('/')
            .substringAfterLast(':')
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (base.isBlank()) return "Custom Font"
        return base.split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
    }
}

sealed class ImportFontResult {
    data class Success(val entry: CustomFontEntry) : ImportFontResult()
    data class Error(val message: String) : ImportFontResult()
}
