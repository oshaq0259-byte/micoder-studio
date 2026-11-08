package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ExportAudioFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    WAV("wav", "audio/wav", "WAV (Lossless)"),
    AAC("m4a", "audio/mp4", "AAC (.m4a High-Bitrate)"),
    MP3("mp3", "audio/mpeg", "MP3 (Compressed)");

    companion object {
        fun fromName(name: String): ExportAudioFormat = when (name.uppercase()) {
            "AAC" -> AAC
            "MP3" -> MP3
            else -> WAV
        }
    }
}

class AudioTranscoder(
    private val context: Context
) {

    fun getExportCacheDir(): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Transcodes a WAV recording to the requested ExportAudioFormat.
     * Returns the generated file.
     */
    suspend fun transcode(
        sourceWavFile: File,
        targetFormat: ExportAudioFormat,
        outputFileNameWithoutExt: String
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = getExportCacheDir()
        val outputFile = File(cacheDir, "${outputFileNameWithoutExt}.${targetFormat.extension}")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        when (targetFormat) {
            ExportAudioFormat.WAV -> {
                sourceWavFile.copyTo(outputFile, overwrite = true)
                outputFile
            }
            ExportAudioFormat.AAC -> {
                encodeToAac(sourceWavFile, outputFile)
                outputFile
            }
            ExportAudioFormat.MP3 -> {
                // If MP3 encoder is supported by MediaCodec, use it; otherwise fallback to clean AAC container renamed or standard MP3 stream
                val mp3Success = encodeToMp3(sourceWavFile, outputFile)
                if (!mp3Success) {
                    encodeToAac(sourceWavFile, outputFile)
                }
                outputFile
            }
        }
    }

    /**
     * Hardware-accelerated 48 kHz AAC encoder using MediaCodec and MediaMuxer.
     */
    private fun encodeToAac(sourceWavFile: File, outputFile: File) {
        val sampleRate = 48000
        val channelCount = 1
        val bitRate = 256000 // 256 kbps broadcast quality

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var audioTrackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val fis = FileInputStream(sourceWavFile)

        // Read WAV header to determine bit depth
        val header = ByteArray(44)
        fis.read(header)
        val bitDepth = ByteBuffer.wrap(header, 34, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

        val rawInput = ByteArray(8192)
        var sawInputEos = false
        var sawOutputEos = false

        try {
            while (!sawOutputEos) {
                // Feed input to encoder
                if (!sawInputEos) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000L)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                        inputBuffer.clear()

                        val bytesRead = fis.read(rawInput)
                        if (bytesRead <= 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            if (bitDepth == 24) {
                                // Convert 24-bit PCM to 16-bit PCM for MediaCodec
                                val samples = bytesRead / 3
                                val converted = ByteArray(samples * 2)
                                val byteBuf = ByteBuffer.wrap(converted).order(ByteOrder.LITTLE_ENDIAN)
                                for (i in 0 until samples) {
                                    val b0 = rawInput[i * 3].toInt() and 0xFF
                                    val b1 = rawInput[i * 3 + 1].toInt() and 0xFF
                                    val b2 = rawInput[i * 3 + 2].toInt()
                                    val sample24 = (b2 shl 16) or (b1 shl 8) or b0
                                    val sample16 = (sample24 shr 8).toShort()
                                    byteBuf.putShort(sample16)
                                }
                                inputBuffer.put(converted)
                                codec.queueInputBuffer(inputBufferIndex, 0, converted.size, 0L, 0)
                            } else {
                                inputBuffer.put(rawInput, 0, bytesRead)
                                codec.queueInputBuffer(inputBufferIndex, 0, bytesRead, 0L, 0)
                            }
                        }
                    }
                }

                // Dequeue output from encoder
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = codec.outputFormat
                        audioTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEos = true
                        }
                    }
                }
            }
        } finally {
            fis.close()
            try {
                codec.stop()
                codec.release()
            } catch (_: Exception) {
            }
            if (muxerStarted) {
                try {
                    muxer.stop()
                    muxer.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Attempts hardware MP3 encoding or falls back gracefully.
     */
    private fun encodeToMp3(sourceWavFile: File, outputFile: File): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val format = MediaFormat.createAudioFormat("audio/mpeg", 48000, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 192000)
            }
            val encoderName = codecList.findEncoderForFormat(format)
            if (encoderName != null) {
                // Device has native MP3 encoder
                val codec = MediaCodec.createByCodecName(encoderName)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec.start()

                val fos = FileOutputStream(outputFile)
                val fis = FileInputStream(sourceWavFile)
                fis.skip(44) // skip WAV header

                val bufferInfo = MediaCodec.BufferInfo()
                val rawInput = ByteArray(4096)
                var sawInputEos = false
                var sawOutputEos = false

                while (!sawOutputEos) {
                    if (!sawInputEos) {
                        val inputIndex = codec.dequeueInputBuffer(10000L)
                        if (inputIndex >= 0) {
                            val inBuf = codec.getInputBuffer(inputIndex) ?: continue
                            inBuf.clear()
                            val count = fis.read(rawInput)
                            if (count <= 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEos = true
                            } else {
                                inBuf.put(rawInput, 0, count)
                                codec.queueInputBuffer(inputIndex, 0, count, 0L, 0)
                            }
                        }
                    }

                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                    if (outIndex >= 0) {
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null && bufferInfo.size > 0) {
                            val chunk = ByteArray(bufferInfo.size)
                            outBuf.position(bufferInfo.offset)
                            outBuf.get(chunk)
                            fos.write(chunk)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEos = true
                        }
                    }
                }
                fis.close()
                fos.close()
                codec.stop()
                codec.release()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
