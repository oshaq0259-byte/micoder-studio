package com.example.audio.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

/**
 * Dynamic De-Esser.
 * Uses a bandpass sidechain (centered around 6.5 kHz) to detect harsh sibilants ("s", "sh", "ch")
 * and applies dynamic ducking to the treble band without dulling the overall voice tone.
 */
class DeEsser(
    private val sampleRate: Float = 48000f
) {
    var enabled: Boolean = true
    var intensity: Float = 0.5f // 0.0 (off) to 1.0 (aggressive)

    // Sidechain bandpass filter for sibilance detection
    private val sidechainFilter = BiquadFilter.createPeaking(sampleRate, 6500f, 6.0f, 2.5f)

    private val attackTimeSec = 0.001f // 1 ms fast attack
    private val releaseTimeSec = 0.040f // 40 ms fast release
    private val attackCoeff = exp(-1.0f / (attackTimeSec * sampleRate))
    private val releaseCoeff = exp(-1.0f / (releaseTimeSec * sampleRate))

    private var sidechainEnvelope: Float = 0.0f
    private var widebandEnvelope: Float = 0.0f
    private var attenuationGain: Float = 1.0f

    fun reset() {
        sidechainFilter.reset()
        sidechainEnvelope = 0.0f
        widebandEnvelope = 0.0f
        attenuationGain = 1.0f
    }

    fun process(sample: Float): Float {
        if (!enabled || intensity <= 0.01f) return sample

        val sidechainSample = sidechainFilter.process(sample)
        val sibilanceEnergy = abs(sidechainSample)
        val widebandEnergy = abs(sample)

        // Envelope smoothers
        sidechainEnvelope = (1.0f - attackCoeff) * sibilanceEnergy + attackCoeff * sidechainEnvelope
        widebandEnvelope = (1.0f - releaseCoeff) * widebandEnergy + releaseCoeff * widebandEnvelope

        // Check if high frequency content significantly exceeds average voice energy
        val threshold = 0.015f * (1.2f - intensity * 0.6f)
        val targetGain = if (sidechainEnvelope > threshold && sidechainEnvelope > widebandEnvelope * 0.85f) {
            val excess = (sidechainEnvelope - threshold) / (threshold + 0.0001f)
            val maxReductionDb = -8.0f * intensity
            val reductionDb = (excess * -3.0f).coerceIn(maxReductionDb, 0.0f)
            10.0f.pow(reductionDb / 20.0f)
        } else {
            1.0f
        }

        // Apply gain smoothing
        attenuationGain = if (targetGain < attenuationGain) {
            (1.0f - attackCoeff) * targetGain + attackCoeff * attenuationGain
        } else {
            (1.0f - releaseCoeff) * targetGain + releaseCoeff * attenuationGain
        }

        // Apply reduction only to the harsh component
        return sample * attenuationGain
    }
}
