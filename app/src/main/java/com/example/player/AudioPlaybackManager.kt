package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.database.LocalTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackUiState(
    val currentTrack: LocalTrack? = null,
    val isPlaying: Boolean = false,
    val playQueue: List<LocalTrack> = emptyList(),
    val queueIndex: Int = -1,
    val playbackPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isGaplessEnabled: Boolean = true,
    val isShuffleEnabled: Boolean = false,
    val isRepeatOneEnabled: Boolean = false,
    val castDeviceConnected: String? = null, // Name of the active Chromecast, null if local phone
    val activeSleepTimerMinutes: Int? = null, // Sleep timer countdown in minutes, null if disabled
    val wearControlSynced: Boolean = true,
    val playSpeed: Float = 1.0f
)

class AudioPlaybackManager(private val context: Context) {

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var currentPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var isPreparingNext = false

    private val handler = Handler(Looper.getMainLooper())
    private var positionTrackerRunnable: Runnable? = null
    private var sleepTimerHandler = Handler(Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null

    // For shuffling history retention
    private var originalQueue: List<LocalTrack> = emptyList()

    init {
        startPositionTracker()
    }

    // Connects or disconnects a simulated Chromecast device
    fun toggleCastDevice(deviceName: String?) {
        _uiState.update { it.copy(castDeviceConnected = deviceName) }
        Log.d("PlaybackManager", "Chromecast Cast target changed to: $deviceName")
    }

    // Set Sleep Timer countdown
    fun setSleepTimer(minutes: Int?) {
        sleepTimerRunnable?.let { sleepTimerHandler.removeCallbacks(it) }
        
        if (minutes == null) {
            _uiState.update { it.copy(activeSleepTimerMinutes = null) }
            return
        }

        _uiState.update { it.copy(activeSleepTimerMinutes = minutes) }

        var remaining = minutes
        sleepTimerRunnable = object : Runnable {
            override fun run() {
                if (remaining <= 1) {
                    stop()
                    _uiState.update { it.copy(activeSleepTimerMinutes = null) }
                } else {
                    remaining--
                    _uiState.update { it.copy(activeSleepTimerMinutes = remaining) }
                    sleepTimerHandler.postDelayed(this, 60000) // update every minute
                }
            }
        }
        sleepTimerHandler.postDelayed(sleepTimerRunnable!!, 60000)
    }

    // Standard control - PLAY immediate with initial list
    fun playTrackList(tracks: List<LocalTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        
        originalQueue = tracks
        val activeList = if (_uiState.value.isShuffleEnabled) {
            val shuffled = tracks.toMutableList()
            // Keep the selected track as first, shuffle others
            val selected = tracks[startIndex]
            shuffled.remove(selected)
            shuffled.shuffle()
            shuffled.add(0, selected)
            shuffled
        } else {
            tracks
        }

        val idx = if (_uiState.value.isShuffleEnabled) 0 else startIndex

        _uiState.update {
            it.copy(
                playQueue = activeList,
                queueIndex = idx,
                currentTrack = activeList[idx]
            )
        }

        playTrack(activeList[idx])
    }

    // Plays a specific track index
    fun selectTrackIndex(index: Int) {
        val q = _uiState.value.playQueue
        if (index in q.indices) {
            _uiState.update {
                it.copy(
                    queueIndex = index,
                    currentTrack = q[index]
                )
            }
            playTrack(q[index])
        }
    }

    // Clean initialization and playing of a single audio track
    private fun playTrack(track: LocalTrack) {
        try {
            // Release existing players
            releaseCurrentPlayer()
            releaseNextPlayer()

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(track.filePath)
                prepare()
                playbackParams = playbackParams.setSpeed(_uiState.value.playSpeed)
            }

            currentPlayer = player
            player.start()

            _uiState.update {
                it.copy(
                    isPlaying = true,
                    playbackPositionMs = 0L,
                    totalDurationMs = player.duration.toLong()
                )
            }

            setupCompletionListener()
            
            // Immediately chain the next player if gapless playback is enabled
            if (_uiState.value.isGaplessEnabled) {
                prepareNextGaplessPlayer()
            }

        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error preparing track: ${track.title}", e)
            // Auto skip if error
            skipNext()
        }
    }

    // Pre-loads the next song in the background and chains it gaplessly
    private fun prepareNextGaplessPlayer() {
        if (isPreparingNext) return
        val currentIdx = _uiState.value.queueIndex
        val q = _uiState.value.playQueue
        
        if (q.isEmpty()) return
        
        val nextIdx = if (_uiState.value.isRepeatOneEnabled) {
            currentIdx
        } else {
            (currentIdx + 1) % q.size
        }

        val nextTrack = q[nextIdx]
        isPreparingNext = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(nextTrack.filePath)
                    prepare()
                }

                withContext(Dispatchers.Main) {
                    nextPlayer = player
                    currentPlayer?.setNextMediaPlayer(player)
                    isPreparingNext = false
                    Log.d("PlaybackManager", "Successfully chained gapless next song: ${nextTrack.title}")
                }
            } catch (e: Exception) {
                isPreparingNext = false
                Log.e("PlaybackManager", "Failed to preheat next gapless player", e)
            }
        }
    }

    private fun setupCompletionListener() {
        currentPlayer?.setOnCompletionListener {
            Log.d("PlaybackManager", "Primary Track Completed! Loading chained next track...")
            handleTrackTransition()
        }
    }

    // Moves play context to the pre-loaded next player gaplessly
    private fun handleTrackTransition() {
        val q = _uiState.value.playQueue
        if (q.isEmpty()) return

        var nextIdx = _uiState.value.queueIndex
        if (!_uiState.value.isRepeatOneEnabled) {
            nextIdx = (nextIdx + 1) % q.size
        }

        val nextTrack = q[nextIdx]

        if (_uiState.value.isGaplessEnabled && nextPlayer != null) {
            // Swap player pointers
            val oldPlayer = currentPlayer
            currentPlayer = nextPlayer
            nextPlayer = null

            oldPlayer?.setOnCompletionListener(null)
            oldPlayer?.release()

            currentPlayer?.playbackParams = currentPlayer?.playbackParams!!.setSpeed(_uiState.value.playSpeed)
            currentPlayer?.start()

            _uiState.update {
                it.copy(
                    queueIndex = nextIdx,
                    currentTrack = nextTrack,
                    isPlaying = true,
                    playbackPositionMs = 0L,
                    totalDurationMs = currentPlayer?.duration?.toLong() ?: 0L
                )
            }

            setupCompletionListener()
            prepareNextGaplessPlayer()
        } else {
            // Hard transition if gapless disabled or next player wasn't heated in time
            selectTrackIndex(nextIdx)
        }
    }

    // Set play speed (useful for podcasts or customized listening)
    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playSpeed = speed) }
        try {
            currentPlayer?.let {
                if (it.isPlaying) {
                     it.playbackParams = it.playbackParams.setSpeed(speed)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        val player = currentPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            player.start()
            _uiState.update { it.copy(isPlaying = true) }
        }
    }

    fun skipNext() {
        val q = _uiState.value.playQueue
        if (q.isEmpty()) return
        
        val nextIdx = (_uiState.value.queueIndex + 1) % q.size
        selectTrackIndex(nextIdx)
    }

    fun skipPrevious() {
        val q = _uiState.value.playQueue
        if (q.isEmpty()) return

        // If played more than 3 seconds, restart current track
        val pos = _uiState.value.playbackPositionMs
        if (pos > 3000L) {
            seekTo(0L)
            return
        }

        var prevIdx = _uiState.value.queueIndex - 1
        if (prevIdx < 0) {
            prevIdx = q.size - 1
        }
        selectTrackIndex(prevIdx)
    }

    fun seekTo(positionMs: Long) {
        currentPlayer?.let {
            it.seekTo(positionMs.toInt())
            _uiState.update { state -> state.copy(playbackPositionMs = positionMs) }
        }
    }

    fun stop() {
        currentPlayer?.pause()
        currentPlayer?.seekTo(0)
        _uiState.update { it.copy(isPlaying = false, playbackPositionMs = 0L) }
    }

    // Toggle gapless feature
    fun toggleGapless(enabled: Boolean) {
        _uiState.update { it.copy(isGaplessEnabled = enabled) }
        if (enabled) {
            prepareNextGaplessPlayer()
        } else {
            releaseNextPlayer()
            currentPlayer?.setNextMediaPlayer(null)
        }
    }

    // Toggle shuffle
    fun toggleShuffle() {
        val enabled = !_uiState.value.isShuffleEnabled
        _uiState.update { it.copy(isShuffleEnabled = enabled) }

        val currentTrack = _uiState.value.currentTrack
        val qList = _uiState.value.playQueue

        if (enabled) {
            val shuffled = originalQueue.toMutableList()
            if (currentTrack != null) {
                shuffled.remove(currentTrack)
                shuffled.shuffle()
                shuffled.add(0, currentTrack)
            } else {
                shuffled.shuffle()
            }
            _uiState.update {
                it.copy(
                    playQueue = shuffled,
                    queueIndex = if (currentTrack != null) 0 else -1
                )
            }
        } else {
            val idx = if (currentTrack != null) originalQueue.indexOf(currentTrack) else -1
            _uiState.update {
                it.copy(
                    playQueue = originalQueue,
                    queueIndex = idx
                )
            }
        }
    }

    // Toggle repeat one
    fun toggleRepeatOne() {
        val enabled = !_uiState.value.isRepeatOneEnabled
        _uiState.update { it.copy(isRepeatOneEnabled = enabled) }
        // Re-buffer the chained gapless next player to reflect repeat one state
        if (_uiState.value.isGaplessEnabled) {
            releaseNextPlayer()
            prepareNextGaplessPlayer()
        }
    }

    // Move track indexes in active queue list
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val q = _uiState.value.playQueue.toMutableList()
        val currentTrack = _uiState.value.currentTrack
        if (fromIndex in q.indices && toIndex in q.indices) {
            val item = q.removeAt(fromIndex)
            q.add(toIndex, item)

            val newIdx = if (currentTrack != null) q.indexOf(currentTrack) else -1

            _uiState.update {
                it.copy(
                    playQueue = q,
                    queueIndex = newIdx
                )
            }

            // Repass background gapless chain
            if (_uiState.value.isGaplessEnabled) {
                releaseNextPlayer()
                prepareNextGaplessPlayer()
            }
        }
    }

    // Remove track from queue
    fun removeTrackFromQueue(index: Int) {
        val q = _uiState.value.playQueue.toMutableList()
        if (q.size <= 1) return // Keep at least one track
        if (index in q.indices) {
            val currentIdx = _uiState.value.queueIndex
            val listIndexRemoved = index == currentIdx
            
            q.removeAt(index)
            val newIdx = if (listIndexRemoved) {
                currentIdx % q.size
            } else {
                val currentTrack = _uiState.value.currentTrack
                if (currentTrack != null) q.indexOf(currentTrack) else -1
            }

            _uiState.update {
                it.copy(
                    playQueue = q,
                    queueIndex = newIdx,
                    currentTrack = if (newIdx != -1) q[newIdx] else null
                )
            }

            if (listIndexRemoved && newIdx != -1) {
                playTrack(q[newIdx])
            } else {
                if (_uiState.value.isGaplessEnabled) {
                    releaseNextPlayer()
                    prepareNextGaplessPlayer()
                }
            }
        }
    }

    // Synchronize playbacks from our wearable Remote Controls
    fun syncFromWearOS(action: String) {
        Log.d("PlaybackManager", "Wear OS synchronized action callback: $action")
        _uiState.update { it.copy(wearControlSynced = false) }
        when (action) {
            "PLAY_PAUSE" -> togglePlayPause()
            "NEXT" -> skipNext()
            "PREVIOUS" -> skipPrevious()
        }
        // bounce visual sync tracker
        handler.postDelayed({
            _uiState.update { it.copy(wearControlSynced = true) }
        }, 800)
    }

    private fun startPositionTracker() {
        positionTrackerRunnable = object : Runnable {
            override fun run() {
                try {
                    currentPlayer?.let { player ->
                        if (player.isPlaying) {
                            val currentPos = player.currentPosition.toLong()
                            _uiState.update { it.copy(playbackPositionMs = currentPos) }
                        }
                    }
                } catch (e: Exception) {
                    // media player can get into invalid state during transitions
                }
                handler.postDelayed(this, 300) // update UI every 300ms
            }
        }
        handler.postDelayed(positionTrackerRunnable!!, 300)
    }

    private fun releaseCurrentPlayer() {
        currentPlayer?.let {
            try {
                it.stop()
            } catch (e: Exception) {}
            it.release()
        }
        currentPlayer = null
    }

    private fun releaseNextPlayer() {
        nextPlayer?.let {
            it.release()
        }
        nextPlayer = null
    }

    fun releaseAll() {
        releaseCurrentPlayer()
        releaseNextPlayer()
        positionTrackerRunnable?.let { handler.removeCallbacks(it) }
        sleepTimerRunnable?.let { sleepTimerHandler.removeCallbacks(it) }
    }
}
