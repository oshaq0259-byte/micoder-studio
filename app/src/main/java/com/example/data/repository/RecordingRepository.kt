package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.db.RecordingDao
import com.example.model.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(
    private val context: Context,
    private val recordingDao: RecordingDao
) {

    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    fun getRecordingsDir(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveRecording(
        title: String,
        file: File,
        durationMs: Long,
        sampleRate: Int,
        bitDepth: Int,
        micProfileId: String,
        modeProfileId: String
    ): RecordingEntity = withContext(Dispatchers.IO) {
        val entity = RecordingEntity(
            title = title,
            filePath = file.absolutePath,
            durationMs = durationMs,
            createdAt = System.currentTimeMillis(),
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            fileSize = file.length(),
            micProfileId = micProfileId,
            modeProfileId = modeProfileId
        )
        val id = recordingDao.insert(entity)
        entity.copy(id = id)
    }

    suspend fun renameRecording(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        recordingDao.rename(id, newTitle)
    }

    suspend fun deleteRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
        recordingDao.delete(recording)
    }

    fun createShareIntent(recording: RecordingEntity): Intent? {
        val file = File(recording.filePath)
        if (!file.exists()) return null

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = if (recording.filePath.endsWith(".wav", ignoreCase = true)) {
            "audio/wav"
        } else {
            "audio/mp4"
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recording.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun getTotalStorageBytes(): Long = withContext(Dispatchers.IO) {
        val dir = getRecordingsDir()
        dir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
