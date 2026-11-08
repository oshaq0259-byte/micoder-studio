package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

sealed class ExportResult {
    data class Success(val savedPath: String, val uri: Uri?) : ExportResult()
    data class Failure(val errorMessage: String) : ExportResult()
}

class FileExportManager(
    private val context: Context
) {

    /**
     * Saves audio file directly to the device's public Downloads directory
     * so it appears in the user's Files app and USB storage without special permissions.
     */
    suspend fun saveToPublicDownloads(
        sourceFile: File,
        desiredDisplayName: String,
        mimeType: String
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext ExportResult.Failure("Source recording file not found.")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, desiredDisplayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MicoderStudio")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)
                    ?: return@withContext ExportResult.Failure("Could not create entry in Downloads folder.")

                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)

                ExportResult.Success(
                    savedPath = "Downloads/MicoderStudio/$desiredDisplayName",
                    uri = itemUri
                )
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "MicoderStudio")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val destinationFile = File(targetDir, desiredDisplayName)
                sourceFile.copyTo(destinationFile, overwrite = true)

                ExportResult.Success(
                    savedPath = destinationFile.absolutePath,
                    uri = Uri.fromFile(destinationFile)
                )
            }
        } catch (e: Exception) {
            ExportResult.Failure(e.localizedMessage ?: "Failed to save file to Downloads.")
        }
    }

    /**
     * Creates an Android Intent.ACTION_SEND chooser to share the audio file
     * to external cloud storage providers (Google Drive, Dropbox, Nextcloud, Box, OneDrive)
     * or communication apps (Gmail, Quick Share, etc.).
     */
    fun createShareIntent(
        sourceFile: File,
        mimeType: String,
        title: String
    ): Intent? {
        if (!sourceFile.exists()) return null

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sourceFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Recording from Micoder Studio: $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(sendIntent, "Share or Upload to Cloud").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
