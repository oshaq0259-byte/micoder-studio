package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MicoderApplication
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.AudioTranscoder
import com.example.audio.ExportAudioFormat
import com.example.audio.LiveAudioState
import com.example.audio.PlaybackState
import com.example.audio.RecorderStatus
import com.example.data.repository.ExportResult
import com.example.data.repository.FileExportManager
import com.example.model.AudioSettings
import com.example.model.MicProfile
import com.example.model.RecordingEntity
import com.example.model.RecordingMode
import com.example.service.RecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MicoderApplication).repository
    val recorderManager = AudioRecorderManager(application)
    val playerManager = AudioPlayerManager(application)
    val audioTranscoder = AudioTranscoder(application)
    val fileExportManager = FileExportManager(application)

    // State flows
    val recorderState: StateFlow<LiveAudioState> = recorderManager.state
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Studio Configuration
    private val _selectedMic = MutableStateFlow(MicProfile.MIC_STUDIO)
    val selectedMic: StateFlow<MicProfile> = _selectedMic.asStateFlow()

    private val _selectedMode = MutableStateFlow(RecordingMode.VOICE_RECORD)
    val selectedMode: StateFlow<RecordingMode> = _selectedMode.asStateFlow()

    private val _settings = MutableStateFlow(AudioSettings())
    val settings: StateFlow<AudioSettings> = _settings.asStateFlow()

    // Dialog & UI interaction states
    private val _renameTarget = MutableStateFlow<RecordingEntity?>(null)
    val renameTarget: StateFlow<RecordingEntity?> = _renameTarget.asStateFlow()

    private val _deleteTarget = MutableStateFlow<RecordingEntity?>(null)
    val deleteTarget: StateFlow<RecordingEntity?> = _deleteTarget.asStateFlow()

    private val _exportTarget = MutableStateFlow<RecordingEntity?>(null)
    val exportTarget: StateFlow<RecordingEntity?> = _exportTarget.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        syncAudioConfig()
    }

    private fun syncAudioConfig() {
        recorderManager.configureProfiles(
            mic = _selectedMic.value,
            mode = _selectedMode.value,
            settings = _settings.value
        )
    }

    fun selectMic(mic: MicProfile) {
        _selectedMic.value = mic
        syncAudioConfig()
    }

    fun selectMode(mode: RecordingMode) {
        _selectedMode.value = mode
        syncAudioConfig()
    }

    fun updateSettings(newSettings: AudioSettings) {
        _settings.value = newSettings
        syncAudioConfig()
    }

    fun startRecording(): Boolean {
        playerManager.stop()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Micoder_${_selectedMic.value.displayName.replace(" ", "")}_$timeStamp.wav"
        val destination = File(repository.getRecordingsDir(), fileName)

        val success = recorderManager.startRecording(destination)
        if (success) {
            startForegroundService()
        } else {
            _userMessage.value = "Could not initialize microphone. Please check permissions."
        }
        return success
    }

    fun pauseRecording() {
        recorderManager.pauseRecording()
    }

    fun resumeRecording() {
        recorderManager.resumeRecording()
    }

    fun stopRecording() {
        val recordedFile = recorderManager.stopRecording()
        stopForegroundService()

        if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 44) {
            val durationMs = recorderState.value.elapsedMs
            val sampleRate = _settings.value.sampleRate
            val bitDepth = _settings.value.bitDepth
            val title = "Take " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())

            viewModelScope.launch {
                repository.saveRecording(
                    title = title,
                    file = recordedFile,
                    durationMs = durationMs,
                    sampleRate = sampleRate,
                    bitDepth = bitDepth,
                    micProfileId = _selectedMic.value.id,
                    modeProfileId = _selectedMode.value.id
                )
                _userMessage.value = "Recording saved safely to vault."
            }
        }
    }

    private fun startForegroundService() {
        val app = getApplication<Application>()
        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra("mic_name", _selectedMic.value.displayName)
            putExtra("mode_name", _selectedMode.value.displayName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }

    private fun stopForegroundService() {
        val app = getApplication<Application>()
        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        app.startService(intent)
    }

    fun playTrack(track: RecordingEntity) {
        if (recorderState.value.status == RecorderStatus.RECORDING) {
            _userMessage.value = "Cannot play while recording is active."
            return
        }
        playerManager.playTrack(track)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        playerManager.seekRelative(offsetMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setSpeed(speed)
    }

    fun requestRename(recording: RecordingEntity) {
        _renameTarget.value = recording
    }

    fun dismissRename() {
        _renameTarget.value = null
    }

    fun confirmRename(newTitle: String) {
        val target = _renameTarget.value ?: return
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                repository.renameRecording(target.id, newTitle.trim())
                _renameTarget.value = null
            }
        }
    }

    fun requestDelete(recording: RecordingEntity) {
        _deleteTarget.value = recording
    }

    fun dismissDelete() {
        _deleteTarget.value = null
    }

    fun confirmDelete() {
        val target = _deleteTarget.value ?: return
        if (playerManager.playbackState.value.currentTrack?.id == target.id) {
            playerManager.stop()
        }
        viewModelScope.launch {
            repository.deleteRecording(target)
            _deleteTarget.value = null
            _userMessage.value = "Recording deleted."
        }
    }

    fun requestExport(recording: RecordingEntity) {
        _exportTarget.value = recording
    }

    fun dismissExport() {
        _exportTarget.value = null
    }

    /**
     * Transcodes recording to target format and launches the system share chooser
     * allowing direct upload to Google Drive, Dropbox, Gmail, Quick Share, etc.
     */
    fun exportAndShare(recording: RecordingEntity, targetFormat: ExportAudioFormat) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val sourceFile = File(recording.filePath)
                val sanitizedName = recording.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val outputFile = audioTranscoder.transcode(sourceFile, targetFormat, sanitizedName)

                val shareIntent = fileExportManager.createShareIntent(
                    sourceFile = outputFile,
                    mimeType = targetFormat.mimeType,
                    title = "${recording.title}.${targetFormat.extension}"
                )

                if (shareIntent != null) {
                    getApplication<Application>().startActivity(shareIntent)
                    _exportTarget.value = null
                } else {
                    _userMessage.value = "Unable to prepare audio file for sharing."
                }
            } catch (e: Exception) {
                _userMessage.value = "Export failed: ${e.localizedMessage}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    /**
     * Transcodes recording to target format and writes directly to the device's
     * public Downloads folder (Downloads/MicoderStudio/).
     */
    fun exportAndDownload(recording: RecordingEntity, targetFormat: ExportAudioFormat) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val sourceFile = File(recording.filePath)
                val sanitizedName = recording.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val outputFile = audioTranscoder.transcode(sourceFile, targetFormat, sanitizedName)

                val targetDisplayName = "${sanitizedName}.${targetFormat.extension}"
                val result = fileExportManager.saveToPublicDownloads(
                    sourceFile = outputFile,
                    desiredDisplayName = targetDisplayName,
                    mimeType = targetFormat.mimeType
                )

                when (result) {
                    is ExportResult.Success -> {
                        _userMessage.value = "Saved to ${result.savedPath}"
                        _exportTarget.value = null
                    }
                    is ExportResult.Failure -> {
                        _userMessage.value = "Download failed: ${result.errorMessage}"
                    }
                }
            } catch (e: Exception) {
                _userMessage.value = "Download failed: ${e.localizedMessage}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearClipping() {
        recorderManager.clearClipping()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
