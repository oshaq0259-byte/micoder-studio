package com.example.audio.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow

/**
 * Studio Vocal Dynamic Range Compressor with Soft Knee.
 * Tames loud spikes and delivers consistent, warm broadcast presence without pumping artifacts.
 */
class DynamicCompressor(
    private val sampleRate: Float = 48000f
) {
    var enabled: Boolean = true
    var thresholdDb: Float = -18.0f
    var ratio: Float = 2.5f // 2.5:1
    var attackTimeMs: Float = 6.0f
    var releaseTimeMs: Float = 140.0f
    var makeupGainDb: Float = 3.0f

    private val kneeWidthDb: Float = 6.0f
    private var envelopeDb: Float = -90.0f
    private var currentGainLinear: Float = 1.0f

    private var attackCoeff: Float = 0.0f
    private var releaseCoeff: Float = 0.0f
    private var makeupLinear: Float = 1.0f

    init {
        updateCoefficients()
    }

    fun updateCoefficients() {
        attackCoeff = exp(-1.0f / ((attackTimeMs / 1000f) * sampleRate))
        releaseCoeff = exp(-1.0f / ((releaseTimeMs / 1000f) * sampleRate))
        makeupLinear = 10.0f.pow(makeupGainDb / 20.0f)
    }

    fun reset() {
        envelopeDb = -90.0f
        currentGainLinear = 1.0f
    }

    fun process(sample: Float): Float {
        if (!enabled) return sample

        val absVal = abs(sample)
        val inputDb = if (absVal > 0.000001f) 20.0f * log10(absVal) else -120.0f

        // Ballistics envelope tracking
        envelopeDb = if (inputDb > envelopeDb) {
            (1.0f - attackCoeff) * inputDb + attackCoeff * envelopeDb
        } else {
            (1.0f - releaseCoeff) * inputDb + releaseCoeff * envelopeDb
        }

        // Soft-knee characteristic gain curve
        val outputDb = when {
            2.0f * (envelopeDb - thresholdDb) < -kneeWidthDb -> {
                envelopeDb
            }
            2.0f * abs(envelopeDb - thresholdDb) <= kneeWidthDb -> {
                envelopeDb + (1.0f / ratio - 1.0f) * (envelopeDb - thresholdDb + kneeWidthDb / 2.0f).pow(2.0f) / (2.0f * kneeWidthDb)
            }
            else -> {
                thresholdDb + (envelopeDb - thresholdDb) / ratio
            }
        }

        val gainReductionDb = outputDb - envelopeDb
        val targetGainLinear = 10.0f.pow(gainReductionDb / 20.0f)

        // Smooth gain transition
        currentGainLinear = 0.95f * currentGainLinear + 0.05f * targetGainLinear

        return sample * currentGainLinear * makeupLinear
    }
}
