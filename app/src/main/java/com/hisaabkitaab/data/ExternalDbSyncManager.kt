package com.hisaabkitaab.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

/**
 * Handles zero-permission Android Storage Access Framework (SAF) persistent read/write
 * synchronization to the user's chosen `.db` SQLite file.
 */
class ExternalDbSyncManager(private val context: Context) {

    private val syncMutex = Mutex()

    /**
     * Attempts to persist both READ and WRITE URI grants across device restarts.
     */
    fun takePersistableReadWritePermission(uri: Uri): Boolean {
        val rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val rwTaken = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, rwFlags)
            true
        }.getOrDefault(false)
        if (rwTaken) return true

        // Fallback: try taking read permission individually if provider restricts combined flags
        return runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            false
        }.getOrDefault(false)
    }

    /**
     * Atomically writes the complete self-contained SQLite `.db` bytes to the linked SAF URI,
     * truncating any previous contents so no stale trailing bytes remain.
     */
    suspend fun writeBytesToUri(uri: Uri, bytes: ByteArray): Result<Unit> = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                if (bytes.size < 16) {
                    throw IllegalStateException("Refusing to write an empty or invalid SQLite snapshot.")
                }

                // Strategy 1: Truncating write stream ("wt") — works on most local & SAF providers
                val wroteWithWt = runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(bytes)
                        out.flush()
                        true
                    } ?: false
                }.getOrDefault(false)
                if (wroteWithWt) return@runCatching

                // Strategy 2: ParcelFileDescriptor ("rwt") with explicit FileChannel truncation
                val wroteWithPfd = runCatching {
                    context.contentResolver.openFileDescriptor(uri, "rwt")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            runCatching { fos.channel.truncate(0L) }
                            fos.write(bytes)
                            fos.flush()
                            runCatching { fos.fd.sync() }
                        }
                        true
                    } ?: false
                }.getOrDefault(false)
                if (wroteWithPfd) return@runCatching

                // Strategy 3: Plain write mode ("w") for third-party cloud providers that disallow "wt"
                val out = context.contentResolver.openOutputStream(uri, "w")
                    ?: throw IOException("Write permission was revoked or the file is no longer accessible.")
                out.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                }
            }
        }
    }

    fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }
}
