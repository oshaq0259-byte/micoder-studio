package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import com.example.model.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val currentTrack: RecordingEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f
)

class AudioPlayerManager(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun playTrack(track: RecordingEntity) {
        val file = File(track.filePath)
        if (!file.exists()) return

        if (_playbackState.value.currentTrack?.id == track.id && mediaPlayer != null) {
            mediaPlayer?.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracker()
            return
        }

        stop()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                prepare()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                player.playbackParams = PlaybackParams().apply {
                    speed = _playbackState.value.playbackSpeed
                }
            }

            player.setOnCompletionListener {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = 0L
                )
                progressJob?.cancel()
            }

            player.start()
            mediaPlayer = player

            _playbackState.value = _playbackState.value.copy(
                currentTrack = track,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = player.duration.toLong().coerceAtLeast(track.durationMs)
            )

            startProgressTracker()
        } catch (_: Exception) {
            stop()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            progressJob?.cancel()
        } else {
            player.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracker()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun seekRelative(offsetMs: Long) {
        val current = _playbackState.value.currentPositionMs
        val target = (current + offsetMs).coerceIn(0L, _playbackState.value.durationMs)
        seekTo(target)
    }

    fun setSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { player ->
                try {
                    player.playbackParams = PlaybackParams().apply { this.speed = speed }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0L)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                _playbackState.value = _playbackState.value.copy(currentPositionMs = current)
                delay(80)
            }
        }
    }

    fun release() {
        stop()
    }
}
