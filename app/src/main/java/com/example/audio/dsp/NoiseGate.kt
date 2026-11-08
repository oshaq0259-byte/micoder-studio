package com.example.audio.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Studio-Grade Adaptive Noise Gate with Hysteresis and Envelope Follower.
 * Prevents cut-off syllables and eliminates background hiss and room tone during pauses.
 */
class NoiseGate(
    private val sampleRate: Float = 48000f
) {
    // Threshold in dBFS
    var thresholdDb: Float = -42.0f
        set(value) {
            field = value
            updateThresholds()
        }

    // Sensitivity adjustment multiplier (-6 to +6 dB offset)
    var sensitivityOffsetDb: Float = 0.0f
        set(value) {
            field = value
            updateThresholds()
        }

    var holdTimeMs: Long = 140L

    private var openThresholdLinear: Float = 0.0079f // approx -42 dB
    private var closeThresholdLinear: Float = 0.0050f // approx -46 dB (hysteresis)

    private val attackTimeSec = 0.002f // 2 ms instant open
    private val releaseTimeSec = 0.120f // 120 ms smooth exponential decay
    private val floorDb = -60.0f
    private val floorLinear = 10.0f.pow(floorDb / 20.0f) // 0.001f

    private var attackCoeff = exp(-1.0f / (attackTimeSec * sampleRate))
    private var releaseCoeff = exp(-1.0f / (releaseTimeSec * sampleRate))

    // State
    private var envelope: Float = 0.0f
    private var gain: Float = 1.0f
    private var holdCounter: Long = 0L
    private var isOpen: Boolean = true

    init {
        updateThresholds()
    }

    private fun updateThresholds() {
        val effectiveOpenDb = (thresholdDb + sensitivityOffsetDb).coerceIn(-65.0f, -20.0f)
        val effectiveCloseDb = effectiveOpenDb - 3.5f // 3.5 dB hysteresis margin
        openThresholdLinear = 10.0f.pow(effectiveOpenDb / 20.0f)
        closeThresholdLinear = 10.0f.pow(effectiveCloseDb / 20.0f)
    }

    fun reset() {
        envelope = 0.0f
        gain = 1.0f
        holdCounter = 0L
        isOpen = true
    }

    fun process(sample: Float): Float {
        val absVal = abs(sample)

        // Envelope detection
        envelope = if (absVal > envelope) {
            0.9f * envelope + 0.1f * absVal
        } else {
            0.999f * envelope + 0.001f * absVal
        }

        // State machine with hysteresis and hold timer
        if (isOpen) {
            if (envelope < closeThresholdLinear) {
                if (holdCounter > 0) {
                    holdCounter--
                } else {
                    isOpen = false
                }
            } else {
                // Refresh hold counter whenever signal is above close threshold
                holdCounter = (holdTimeMs * sampleRate / 1000L).toLong()
            }
        } else {
            if (envelope > openThresholdLinear) {
                isOpen = true
                holdCounter = (holdTimeMs * sampleRate / 1000L).toLong()
            }
        }

        // Smooth gain transition
        val targetGain = if (isOpen) 1.0f else floorLinear
        gain = if (targetGain > gain) {
            (1.0f - attackCoeff) * targetGain + attackCoeff * gain
        } else {
            (1.0f - releaseCoeff) * targetGain + releaseCoeff * gain
        }

        return sample * gain
    }
}
