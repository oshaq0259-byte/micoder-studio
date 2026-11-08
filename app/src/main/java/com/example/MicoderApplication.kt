package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.repository.RecordingRepository

class MicoderApplication : Application() {

    companion object {
        const val CHANNEL_RECORDING = "micoder_recording_channel"
        lateinit var instance: MicoderApplication
            private set
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: RecordingRepository by lazy { RecordingRepository(this, database.recordingDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_RECORDING,
                "Micoder Studio Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground recording status and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
