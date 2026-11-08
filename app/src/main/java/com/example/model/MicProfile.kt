package com.example.model

enum class MicProfile(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val description: String,
    val bassBoostDb: Float,
    val presenceBoostDb: Float,
    val airBoostDb: Float,
    val noiseGateThresholdDb: Float,
    val deEsserIntensity: Float, // 0.0 to 1.0
    val compressionRatio: Float,
    val warmthGainDb: Float
) {
    MIC_CLEAR(
        id = "mic_clear",
        displayName = "Mic Clear",
        subtitle = "Neutral & Natural",
        description = "Neutral, natural voice with light noise reduction. Ideal for calls, interviews, and crisp acoustics.",
        bassBoostDb = 0.0f,
        presenceBoostDb = 1.0f,
        airBoostDb = 1.5f,
        noiseGateThresholdDb = -46.0f,
        deEsserIntensity = 0.3f,
        compressionRatio = 1.8f,
        warmthGainDb = 0.0f
    ),
    MIC_STUDIO(
        id = "mic_studio",
        displayName = "Mic Studio",
        subtitle = "Warm & Broadcast",
        description = "Warm, rich, close-mic sound with bass boost, de-esser, and compressor. Ideal for voice-over and podcasts.",
        bassBoostDb = 3.5f,
        presenceBoostDb = 2.0f,
        airBoostDb = 2.0f,
        noiseGateThresholdDb = -42.0f,
        deEsserIntensity = 0.7f,
        compressionRatio = 3.0f,
        warmthGainDb = 2.0f
    ),
    MIC_CRISP(
        id = "mic_crisp",
        displayName = "Mic Crisp",
        subtitle = "Bright & High-Clarity",
        description = "Bright, high-clarity voice with presence boost and strong noise gate. Ideal for vlogs and noisy rooms.",
        bassBoostDb = -1.0f,
        presenceBoostDb = 4.0f,
        airBoostDb = 4.5f,
        noiseGateThresholdDb = -36.0f,
        deEsserIntensity = 0.5f,
        compressionRatio = 3.5f,
        warmthGainDb = -0.5f
    );

    companion object {
        fun fromId(id: String): MicProfile = values().firstOrNull { it.id == id } ?: MIC_STUDIO
    }
}
