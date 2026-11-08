package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import com.example.audio.dsp.AudioDspChain
import com.example.audio.dsp.MeterReading
import com.example.model.AudioSettings
import com.example.model.MicProfile
import com.example.model.RecordingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

enum class RecorderStatus {
    IDLE,
    RECORDING,
    PAUSED,
    ERROR
}

data class LiveAudioState(
    val status: RecorderStatus = RecorderStatus.IDLE,
    val elapsedMs: Long = 0L,
    val rmsDb: Float = -60f,
    val peakDb: Float = -60f,
    val isClipping: Boolean = false,
    val isSilentWarning: Boolean = false,
    val connectedDeviceName: String = "Built-in Microphone",
    val isHeadphoneConnected: Boolean = false,
    val waveformPoints: List<Float> = emptyList()
)

class AudioRecorderManager(
    private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _state = MutableStateFlow(LiveAudioState())
    val state: StateFlow<LiveAudioState> = _state.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var audioTrackMonitor: AudioTrack? = null
    private var wavWriter: WavWriter? = null
    private val dspChain = AudioDspChain(48000f)

    private var recordingJob: Job? = null
    private var currentFile: File? = null

    private var activeMicProfile: MicProfile = MicProfile.MIC_STUDIO
    private var activeRecordingMode: RecordingMode = RecordingMode.VOICE_RECORD
    private var currentSettings: AudioSettings = AudioSettings()

    private var recordingStartTime: Long = 0L
    private var accumulatedDurationMs: Long = 0L
    private var isPausedInternal: Boolean = false

    private val rollingWaveform = ArrayList<Float>(60)

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                pauseRecording()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume if was paused by system interruption
                if (_state.value.status == RecorderStatus.PAUSED && isPausedInternal) {
                    resumeRecording()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                pauseRecording()
            }
        }
    }

    init {
        updateAudioDeviceState()
    }

    fun configureProfiles(mic: MicProfile, mode: RecordingMode, settings: AudioSettings) {
        activeMicProfile = mic
        activeRecordingMode = mode
        currentSettings = settings
        dspChain.configure(mic, mode, settings)
        updateAudioDeviceState()
    }

    fun updateAudioDeviceState() {
        val isHeadphones = checkIfHeadphonesConnected()
        val deviceName = getActiveMicName()
        _state.value = _state.value.copy(
            isHeadphoneConnected = isHeadphones,
            connectedDeviceName = deviceName
        )
    }

    private fun checkIfHeadphonesConnected(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                val type = device.type
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    type == AudioDeviceInfo.TYPE_USB_HEADSET
                ) {
                    return true
                }
            }
        }
        @Suppress("DEPRECATION")
        return audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
    }

    private fun getActiveMicName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return "Bluetooth Headset Mic"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> return "Wired Headset Mic"
                    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> return "USB Studio Mic"
                }
            }
        }
        return "Built-in Studio Mic"
    }

    @SuppressLint("MissingPermission")
    fun startRecording(destinationFile: File): Boolean {
        if (_state.value.status == RecorderStatus.RECORDING) return true

        currentFile = destinationFile
        accumulatedDurationMs = 0L
        isPausedInternal = false
        rollingWaveform.clear()
        dspChain.reset()

        val sampleRate = currentSettings.sampleRate
        val bitDepth = currentSettings.bitDepth
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT // Hardware capture at 16-bit PCM, then DSP processed & saved in 24-bit PCM WAV

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        // Try VOICE_RECOGNITION source (optimized for speech), fallback to MIC
        var record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (_: Exception) {
            null
        }

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            } catch (_: Exception) {
                record = null
            }
        }

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            _state.value = _state.value.copy(status = RecorderStatus.ERROR)
            return false
        }

        audioRecord = record

        // Request audio focus
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )

        // Setup Monitor track if enabled and headphones connected
        setupMonitorTrack(sampleRate)

        // Setup Fail-Safe WAV writer
        wavWriter = WavWriter(
            outputFile = destinationFile,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            channels = 1
        )

        record.startRecording()
        recordingStartTime = System.currentTimeMillis()
        _state.value = _state.value.copy(
            status = RecorderStatus.RECORDING,
            elapsedMs = 0L,
            isClipping = false,
            isSilentWarning = false
        )

        // Background capture loop
        recordingJob = scope.launch(Dispatchers.IO) {
            val pcmShortBuffer = ShortArray(1024)
            val floatBuffer = FloatArray(1024)
            val monitorShortBuffer = ShortArray(1024)

            while (isActive && _state.value.status != RecorderStatus.IDLE) {
                if (_state.value.status == RecorderStatus.PAUSED) {
                    Thread.sleep(50)
                    continue
                }

                val readCount = record.read(pcmShortBuffer, 0, pcmShortBuffer.size)
                if (readCount > 0) {
                    // Convert to float [-1.0, 1.0]
                    for (i in 0 until readCount) {
                        floatBuffer[i] = pcmShortBuffer[i] / 32768.0f
                    }

                    // Process through DSP pipeline
                    val reading = dspChain.processBuffer(floatBuffer, readCount)

                    // Write to fail-safe WAV file
                    wavWriter?.writeSamples(floatBuffer, readCount)

                    // Real-time live headphone monitoring (if enabled)
                    if (currentSettings.monitoringEnabled && _state.value.isHeadphoneConnected) {
                        for (i in 0 until readCount) {
                            monitorShortBuffer[i] = (floatBuffer[i].coerceIn(-1.0f, 1.0f) * 32767f).toInt().toShort()
                        }
                        audioTrackMonitor?.write(monitorShortBuffer, 0, readCount)
                    }

                    // Update UI telemetry at ~25 Hz
                    val now = System.currentTimeMillis()
                    val totalElapsed = accumulatedDurationMs + (now - recordingStartTime)

                    // Compute normalized waveform amplitude (0.0 to 1.0)
                    val linearAmp = ((reading.rmsDb + 60f) / 60f).coerceIn(0.05f, 1.0f)
                    synchronized(rollingWaveform) {
                        rollingWaveform.add(linearAmp)
                        if (rollingWaveform.size > 50) {
                            rollingWaveform.removeAt(0)
                        }
                    }

                    _state.value = _state.value.copy(
                        elapsedMs = totalElapsed,
                        rmsDb = reading.rmsDb,
                        peakDb = reading.peakDb,
                        isClipping = reading.isClipping,
                        isSilentWarning = reading.isSilent,
                        waveformPoints = synchronized(rollingWaveform) { rollingWaveform.toList() }
                    )
                }
            }
        }

        return true
    }

    private fun setupMonitorTrack(sampleRate: Int) {
        try {
            audioTrackMonitor?.release()
            audioTrackMonitor = null

            if (currentSettings.monitoringEnabled && checkIfHeadphonesConnected()) {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf.coerceAtLeast(2048))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track.play()
                audioTrackMonitor = track
            }
        } catch (_: Exception) {
            audioTrackMonitor = null
        }
    }

    fun pauseRecording() {
        if (_state.value.status == RecorderStatus.RECORDING) {
            val now = System.currentTimeMillis()
            accumulatedDurationMs += (now - recordingStartTime)
            isPausedInternal = true
            _state.value = _state.value.copy(status = RecorderStatus.PAUSED)
        }
    }

    fun resumeRecording() {
        if (_state.value.status == RecorderStatus.PAUSED) {
            recordingStartTime = System.currentTimeMillis()
            isPausedInternal = false
            _state.value = _state.value.copy(status = RecorderStatus.RECORDING)
        }
    }

    fun stopRecording(): File? {
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null

        try {
            audioTrackMonitor?.stop()
            audioTrackMonitor?.release()
        } catch (_: Exception) {
        }
        audioTrackMonitor = null

        wavWriter?.close()
        wavWriter = null

        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(audioFocusChangeListener)

        val recordedFile = currentFile
        _state.value = _state.value.copy(
            status = RecorderStatus.IDLE,
            rmsDb = -60f,
            peakDb = -60f,
            isClipping = false,
            isSilentWarning = false
        )
        return recordedFile
    }

    fun clearClipping() {
        dspChain.clearClippingFlag()
        _state.value = _state.value.copy(isClipping = false)
    }
}
