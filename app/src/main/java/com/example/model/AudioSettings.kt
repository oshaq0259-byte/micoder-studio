package com.example.model

data class AudioSettings(
    val sampleRate: Int = 48000, // 44100, 48000, 96000
    val bitDepth: Int = 24, // 16, 24, 32
    val audioFormat: String = "WAV", // WAV, AAC, MP3
    val noiseReductionLevel: String = "High", // Low, Medium, High
    val adaptiveNoiseGate: Boolean = true,
    val voiceEnhance: Boolean = true,
    val silentInputAlert: Boolean = true,
    val monitoringEnabled: Boolean = false,
    val inputGainDb: Float = 0.0f
)
