package com.example.model

enum class RecordingMode(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val description: String,
    val dynamicRangePreset: String,
    val gateHoldTimeMs: Long,
    val compressorReleaseMs: Long,
    val highPassCutoffHz: Float,
    val autoLevelBalancing: Boolean
) {
    STORY_TELLING(
        id = "mode_story",
        displayName = "Story Telling",
        subtitle = "Gentle & Natural",
        description = "Gentle compression, warm tone, relaxed dynamics. Long-session friendly for audiobooks and narratives.",
        dynamicRangePreset = "Relaxed",
        gateHoldTimeMs = 180L,
        compressorReleaseMs = 240L,
        highPassCutoffHz = 75f,
        autoLevelBalancing = false
    ),
    VOICE_RECORD(
        id = "mode_voice",
        displayName = "Voice Record",
        subtitle = "General Purpose",
        description = "General purpose, balanced settings. Faithful capture for voice notes, ideas, and lectures.",
        dynamicRangePreset = "Balanced",
        gateHoldTimeMs = 120L,
        compressorReleaseMs = 150L,
        highPassCutoffHz = 80f,
        autoLevelBalancing = false
    ),
    VOICE_OVER(
        id = "mode_voiceover",
        displayName = "Voice Over",
        subtitle = "Tight & Broadcast",
        description = "Tight compression, de-esser, broadcast-style EQ. High punch for commercials and animations.",
        dynamicRangePreset = "Broadcast Tight",
        gateHoldTimeMs = 80L,
        compressorReleaseMs = 65L,
        highPassCutoffHz = 85f,
        autoLevelBalancing = false
    ),
    PODCAST_TALK(
        id = "mode_podcast",
        displayName = "Podcast Talk",
        subtitle = "Speech-Optimized",
        description = "Speech-optimized, auto level balancing. Keeps multiple speakers or varied mic distances even.",
        dynamicRangePreset = "Level Balanced",
        gateHoldTimeMs = 140L,
        compressorReleaseMs = 180L,
        highPassCutoffHz = 80f,
        autoLevelBalancing = true
    ),
    INTERVIEW_MODE(
        id = "mode_interview",
        displayName = "Interview Mode",
        subtitle = "High Sensitivity",
        description = "Strong noise suppression, high sensitivity. Isolates voices in field recordings and cafe meetings.",
        dynamicRangePreset = "High Isolation",
        gateHoldTimeMs = 200L,
        compressorReleaseMs = 120L,
        highPassCutoffHz = 90f,
        autoLevelBalancing = true
    );

    companion object {
        fun fromId(id: String): RecordingMode = values().firstOrNull { it.id == id } ?: VOICE_RECORD
    }
}
