package com.example

import com.example.audio.ExportAudioFormat
import com.example.audio.dsp.BiquadFilter
import com.example.audio.dsp.Limiter
import com.example.audio.dsp.NoiseGate
import com.example.model.MicProfile
import com.example.model.RecordingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {

    @Test
    fun `verify mic profiles exist with correct tuning`() {
        val clear = MicProfile.MIC_CLEAR
        val studio = MicProfile.MIC_STUDIO
        val crisp = MicProfile.MIC_CRISP

        assertEquals("Mic Clear", clear.displayName)
        assertEquals("Mic Studio", studio.displayName)
        assertEquals("Mic Crisp", crisp.displayName)

        assertTrue(studio.bassBoostDb > clear.bassBoostDb)
        assertTrue(crisp.presenceBoostDb > clear.presenceBoostDb)
    }

    @Test
    fun `verify all 5 recording modes exist`() {
        assertEquals(5, RecordingMode.values().size)
        assertNotNull(RecordingMode.fromId("mode_story"))
        assertNotNull(RecordingMode.fromId("mode_voice"))
        assertNotNull(RecordingMode.fromId("mode_voiceover"))
        assertNotNull(RecordingMode.fromId("mode_podcast"))
        assertNotNull(RecordingMode.fromId("mode_interview"))
    }

    @Test
    fun `verify export audio formats and mime types`() {
        val wav = ExportAudioFormat.WAV
        val aac = ExportAudioFormat.AAC
        val mp3 = ExportAudioFormat.MP3

        assertEquals("wav", wav.extension)
        assertEquals("audio/wav", wav.mimeType)

        assertEquals("m4a", aac.extension)
        assertEquals("audio/mp4", aac.mimeType)

        assertEquals("mp3", mp3.extension)
        assertEquals("audio/mpeg", mp3.mimeType)

        assertEquals(ExportAudioFormat.WAV, ExportAudioFormat.fromName("WAV"))
        assertEquals(ExportAudioFormat.AAC, ExportAudioFormat.fromName("AAC"))
        assertEquals(ExportAudioFormat.MP3, ExportAudioFormat.fromName("MP3"))
    }

    @Test
    fun `verify limiter prevents digital clipping`() {
        val limiter = Limiter()
        val overshootSample = 1.8f
        val limited = limiter.process(overshootSample)
        assertTrue(abs(limited) <= 1.0f)
    }

    @Test
    fun `verify noise gate suppresses low energy background hiss`() {
        val gate = NoiseGate(48000f)
        gate.thresholdDb = -40f
        gate.holdTimeMs = 0L

        var quietSample = 0.0003f
        repeat(500) {
            quietSample = gate.process(quietSample)
        }
        assertTrue(quietSample < 0.0001f)
    }

    @Test
    fun `verify biquad high pass attenuation`() {
        val hpf = BiquadFilter.createHighPass(48000f, 80f)
        var dcSample = 0.5f
        repeat(500) {
            dcSample = hpf.process(0.5f)
        }
        assertTrue(abs(dcSample) < 0.01f)
    }
}
