package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.data.model.AudioRecitation
import com.example.data.repository.IslamicDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentTrack = MutableStateFlow<AudioRecitation?>(null)
    val currentTrack: StateFlow<AudioRecitation?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(1L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isRepeatOne = MutableStateFlow(false)
    val isRepeatOne: StateFlow<Boolean> = _isRepeatOne.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    fun playRecitation(context: Context, recitation: AudioRecitation) {
        _currentTrack.value = recitation
        stopPlayer()

        try {
            _isBuffering.value = true
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(recitation.audioUrl)
                setOnPreparedListener { mp ->
                    _isBuffering.value = false
                    _duration.value = mp.duration.toLong().coerceAtLeast(1L)
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracking()
                }
                setOnCompletionListener {
                    if (_isRepeatOne.value) {
                        playRecitation(context, recitation)
                    } else {
                        playNext(context)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    _isBuffering.value = false
                    _isPlaying.value = false
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to init player", e)
            _isBuffering.value = false
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.start()
            _isPlaying.value = true
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _currentPosition.value = positionMs
    }

    fun toggleRepeat() {
        _isRepeatOne.value = !_isRepeatOne.value
    }

    fun playNext(context: Context) {
        val current = _currentTrack.value ?: return
        val list = IslamicDataRepository.audioRecitations.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && list.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % list.size
            playRecitation(context, list[nextIndex])
        }
    }

    fun playPrevious(context: Context) {
        val current = _currentTrack.value ?: return
        val list = IslamicDataRepository.audioRecitations.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && list.isNotEmpty()) {
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            playRecitation(context, list[prevIndex])
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                try {
                    mediaPlayer?.currentPosition?.let { pos ->
                        _currentPosition.value = pos.toLong()
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }
    }

    fun stopPlayer() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _currentTrack.value = null
    }
}
