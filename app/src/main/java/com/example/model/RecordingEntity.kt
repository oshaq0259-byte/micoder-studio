package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val sampleRate: Int = 48000,
    val bitDepth: Int = 24,
    val fileSize: Long = 0L,
    val micProfileId: String,
    val modeProfileId: String,
    val isFavorite: Boolean = false
)
