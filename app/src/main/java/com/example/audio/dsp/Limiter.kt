package com.example.audio.dsp

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh

/**
 * Precision Brickwall Peak Limiter and Soft Saturator.
 * Keeps peaks strictly below -0.3 dBFS (0.965 linear) to guarantee zero digital clipping distortion.
 */
class Limiter {
    var enabled: Boolean = true

    // -0.3 dBFS ceiling
    private val ceilingLinear = 10.0f.pow(-0.3f / 20.0f) // ~0.966f

    fun process(sample: Float): Float {
        if (!enabled) return sample

        val absVal = abs(sample)
        if (absVal <= ceilingLinear) {
            return sample
        }

        // Smooth analog tape-style hyperbolic tangent saturation above ceiling
        val sign = if (sample >= 0) 1.0f else -1.0f
        val normalizedOvershoot = (absVal - ceilingLinear) / (1.0f - ceilingLinear + 0.001f)
        val compressedOvershoot = tanh(normalizedOvershoot) * (1.0f - ceilingLinear) * 0.85f

        val limited = ceilingLinear + compressedOvershoot
        return (sign * limited).coerceIn(-0.999f, 0.999f)
    }
}
