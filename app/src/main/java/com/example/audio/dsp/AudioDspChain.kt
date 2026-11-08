package com.example.audio.dsp

import com.example.model.AudioSettings
import com.example.model.MicProfile
import com.example.model.RecordingMode
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

data class MeterReading(
    val rmsDb: Float,
    val peakDb: Float,
    val isClipping: Boolean,
    val isSilent: Boolean
)

class AudioDspChain(
    private val sampleRate: Float = 48000f
) {
    private var activeMic: MicProfile = MicProfile.MIC_CLEAR
    private var activeMode: RecordingMode = RecordingMode.STORY_TELLING
    private var settings: AudioSettings = AudioSettings()

    // DSP components
    private var highPassFilter = BiquadFilter.createHighPass(sampleRate, 80f)
    private val noiseGate = NoiseGate(sampleRate)
    private var lowShelfWarmth = BiquadFilter.createLowShelf(sampleRate, 120f, 0f)
    private var peakingPresence = BiquadFilter.createPeaking(sampleRate, 3400f, 0f)
    private var highShelfAir = BiquadFilter.createHighShelf(sampleRate, 8500f, 0f)
    private val deEsser = DeEsser(sampleRate)
    private val compressor = DynamicCompressor(sampleRate)
    private val limiter = Limiter()

    private var inputGainLinear: Float = 1.0f

    private var currentRmsDb: Float = -60f
    private var currentPeakDb: Float = -60f
    private var isClippingFlag: Boolean = false
    private var silentSampleCount: Long = 0L
    private val silenceThresholdSamples = (sampleRate * 3.0f).toLong()

    init {
        rebuildChain()
    }

    fun configure(mic: MicProfile, mode: RecordingMode, newSettings: AudioSettings) {
        activeMic = mic
        activeMode = mode
        settings = newSettings
        rebuildChain()
    }

    private fun rebuildChain() {
        val highPassCutoff = activeMode.highPassCutoffHz
        highPassFilter = BiquadFilter.createHighPass(sampleRate, highPassCutoff)

        // Noise Reduction Level mapping
        val baseThreshold = when (settings.noiseReductionLevel.lowercase()) {
            "low" -> -48.0f
            "medium" -> -42.0f
            "high" -> -36.0f
            else -> -40.0f
        }
        noiseGate.thresholdDb = baseThreshold
        noiseGate.sensitivityOffsetDb = 0f
        noiseGate.holdTimeMs = activeMode.gateHoldTimeMs

        // EQ curves from active Mic and Mode
        val totalWarmthDb = activeMic.bassBoostDb + activeMic.warmthGainDb
        lowShelfWarmth = BiquadFilter.createLowShelf(sampleRate, 120f, totalWarmthDb)
        peakingPresence = BiquadFilter.createPeaking(sampleRate, 3400f, activeMic.presenceBoostDb)
        highShelfAir = BiquadFilter.createHighShelf(sampleRate, 8500f, activeMic.airBoostDb)

        deEsser.enabled = settings.voiceEnhance
        deEsser.intensity = activeMic.deEsserIntensity

        compressor.enabled = settings.voiceEnhance
        compressor.ratio = activeMic.compressionRatio
        compressor.releaseTimeMs = activeMode.compressorReleaseMs.toFloat()
        compressor.makeupGainDb = if (activeMode.autoLevelBalancing) 4.0f else 2.0f
        compressor.updateCoefficients()

        limiter.enabled = true
        inputGainLinear = 10.0f.pow(settings.inputGainDb / 20.0f)
    }

    fun reset() {
        highPassFilter.reset()
        noiseGate.reset()
        lowShelfWarmth.reset()
        peakingPresence.reset()
        highShelfAir.reset()
        deEsser.reset()
        compressor.reset()
        silentSampleCount = 0L
        isClippingFlag = false
    }

    fun processBuffer(buffer: FloatArray, length: Int): MeterReading {
        var sumSquares = 0.0
        var maxPeak = 0.0f
        var bufferIsSilent = true

        for (i in 0 until length) {
            var sample = buffer[i] * inputGainLinear

            // 1. High-Pass rumble filter
            sample = highPassFilter.process(sample)

            // 2. Adaptive Noise Gate
            if (settings.adaptiveNoiseGate) {
                sample = noiseGate.process(sample)
            }

            // 3. Voice Enhance (EQ, De-Esser, Compressor)
            if (settings.voiceEnhance) {
                sample = lowShelfWarmth.process(sample)
                sample = peakingPresence.process(sample)
                sample = highShelfAir.process(sample)
                sample = deEsser.process(sample)
                sample = compressor.process(sample)
            }

            // 4. Brickwall Peak Limiter
            sample = limiter.process(sample)

            buffer[i] = sample

            val absSample = abs(sample)
            if (absSample > maxPeak) {
                maxPeak = absSample
            }
            sumSquares += (sample * sample)

            if (absSample > 0.002f) { // ~ -54 dBFS
                bufferIsSilent = false
            }
        }

        val rmsLinear = if (length > 0) sqrt(sumSquares / length).toFloat() else 0.0f
        currentRmsDb = if (rmsLinear > 0.00001f) (20.0f * log10(rmsLinear)).coerceIn(-60f, 0f) else -60f
        currentPeakDb = if (maxPeak > 0.00001f) (20.0f * log10(maxPeak)).coerceIn(-60f, 0f) else -60f

        val isClipping = maxPeak >= 0.99f
        if (isClipping) {
            isClippingFlag = true
        }

        if (bufferIsSilent) {
            silentSampleCount += length
        } else {
            silentSampleCount = 0L
        }

        val isPersistentlySilent = settings.silentInputAlert && (silentSampleCount >= silenceThresholdSamples)

        return MeterReading(
            rmsDb = currentRmsDb,
            peakDb = currentPeakDb,
            isClipping = isClippingFlag,
            isSilent = isPersistentlySilent
        )
    }

    fun clearClippingFlag() {
        isClippingFlag = false
    }
}
