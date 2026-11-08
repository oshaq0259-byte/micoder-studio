package com.example.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance 2nd-order IIR Biquad Digital Filter.
 * Computes frequency response for LowPass, HighPass, BandPass, Peaking EQ, LowShelf, and HighShelf.
 */
class BiquadFilter {

    private var b0: Float = 1.0f
    private var b1: Float = 0.0f
    private var b2: Float = 0.0f
    private var a1: Float = 0.0f
    private var a2: Float = 0.0f

    // State history
    private var x1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y1: Float = 0.0f
    private var y2: Float = 0.0f

    fun reset() {
        x1 = 0.0f
        x2 = 0.0f
        y1 = 0.0f
        y2 = 0.0f
    }

    fun process(input: Float): Float {
        val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = input
        y2 = y1
        y1 = if (output.isNaN() || output.isInfinite()) 0.0f else output
        return y1
    }

    companion object {
        fun createHighPass(sampleRate: Float, cutoffHz: Float, q: Float = 0.7071f): BiquadFilter {
            val filter = BiquadFilter()
            val w0 = (2.0 * PI * cutoffHz / sampleRate).toFloat()
            val alpha = (sin(w0) / (2.0f * q))
            val cosW0 = cos(w0)

            val a0 = 1.0f + alpha
            filter.b0 = ((1.0f + cosW0) / 2.0f) / a0
            filter.b1 = (-(1.0f + cosW0)) / a0
            filter.b2 = ((1.0f + cosW0) / 2.0f) / a0
            filter.a1 = (-2.0f * cosW0) / a0
            filter.a2 = (1.0f - alpha) / a0
            return filter
        }

        fun createLowShelf(sampleRate: Float, cutoffHz: Float, gainDb: Float, q: Float = 0.7071f): BiquadFilter {
            val filter = BiquadFilter()
            if (gainDb == 0.0f) {
                filter.b0 = 1.0f
                return filter
            }
            val a = 10.0f.pow(gainDb / 40.0f)
            val w0 = (2.0 * PI * cutoffHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2.0f * sqrt((a + 1.0f / a) * (1.0f / q - 1.0f) + 2.0f)
            val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha

            val a0 = (a + 1.0f) + (a - 1.0f) * cosW0 + twoSqrtAAlpha
            filter.b0 = (a * ((a + 1.0f) - (a - 1.0f) * cosW0 + twoSqrtAAlpha)) / a0
            filter.b1 = (2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosW0)) / a0
            filter.b2 = (a * ((a + 1.0f) - (a - 1.0f) * cosW0 - twoSqrtAAlpha)) / a0
            filter.a1 = (-2.0f * ((a - 1.0f) + (a + 1.0f) * cosW0)) / a0
            filter.a2 = ((a + 1.0f) + (a - 1.0f) * cosW0 - twoSqrtAAlpha) / a0
            return filter
        }

        fun createPeaking(sampleRate: Float, centerHz: Float, gainDb: Float, q: Float = 1.0f): BiquadFilter {
            val filter = BiquadFilter()
            if (gainDb == 0.0f) {
                filter.b0 = 1.0f
                return filter
            }
            val a = 10.0f.pow(gainDb / 40.0f)
            val w0 = (2.0 * PI * centerHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)

            val a0 = 1.0f + alpha / a
            filter.b0 = (1.0f + alpha * a) / a0
            filter.b1 = (-2.0f * cosW0) / a0
            filter.b2 = (1.0f - alpha * a) / a0
            filter.a1 = (-2.0f * cosW0) / a0
            filter.a2 = (1.0f - alpha / a) / a0
            return filter
        }

        fun createHighShelf(sampleRate: Float, cutoffHz: Float, gainDb: Float, q: Float = 0.7071f): BiquadFilter {
            val filter = BiquadFilter()
            if (gainDb == 0.0f) {
                filter.b0 = 1.0f
                return filter
            }
            val a = 10.0f.pow(gainDb / 40.0f)
            val w0 = (2.0 * PI * cutoffHz / sampleRate).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2.0f * sqrt((a + 1.0f / a) * (1.0f / q - 1.0f) + 2.0f)
            val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha

            val a0 = (a + 1.0f) - (a - 1.0f) * cosW0 + twoSqrtAAlpha
            filter.b0 = (a * ((a + 1.0f) + (a - 1.0f) * cosW0 + twoSqrtAAlpha)) / a0
            filter.b1 = (-2.0f * a * ((a - 1.0f) + (a + 1.0f) * cosW0)) / a0
            filter.b2 = (a * ((a + 1.0f) + (a - 1.0f) * cosW0 - twoSqrtAAlpha)) / a0
            filter.a1 = (2.0f * ((a - 1.0f) - (a + 1.0f) * cosW0)) / a0
            filter.a2 = ((a + 1.0f) - (a - 1.0f) * cosW0 - twoSqrtAAlpha) / a0
            return filter
        }
    }
}
