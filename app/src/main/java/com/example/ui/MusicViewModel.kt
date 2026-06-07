package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CachedArtist
import com.example.data.database.LocalPlaylist
import com.example.data.database.LocalTrack
import com.example.data.database.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlaybackManager
import com.example.player.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiNotificationState(
    val message: String? = null,
    val isError: Boolean = false,
    val isScanning: Boolean = false,
    val isAiGenerating: Boolean = false,
    val isAiLyricsSyncing: Boolean = false
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MusicDatabase.getDatabase(application)
    val repository = MusicRepository(application, db.musicDao())
    val playbackManager = AudioPlaybackManager(application)

    // Subscribed library flows
    val allTracks: StateFlow<List<LocalTrack>> = repository.allTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteTracks: StateFlow<List<LocalTrack>> = repository.favoriteTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allArtists: StateFlow<List<CachedArtist>> = repository.allArtists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPlaylists: StateFlow<List<LocalPlaylist>> = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Playback state subscription
    val playbackState: StateFlow<PlaybackUiState> = playbackManager.uiState

    // UI Toast and progress notifications
    private val _notificationState = MutableStateFlow(UiNotificationState())
    val notificationState: StateFlow<UiNotificationState> = _notificationState.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed default tracks on very first launch so the app is fully ready out-of-the-box
            repository.seedDatabaseIfEmpty()
        }
    }

    // Dynamic MediaStore Scanner: triggers actual background system indexing of local audio files (MP3/FLACs)
    fun scanDeviceForLocalAudio() {
        viewModelScope.launch {
            _notificationState.update { it.copy(isScanning = true, message = "Scanning audio files...") }
            try {
                val scannedCount = repository.scanLocalTracks()
                if (scannedCount > 0) {
                    _notificationState.update {
                        it.copy(
                            isScanning = false,
                            message = "Success! Scanned $scannedCount new music files from your phone.",
                            isError = false
                        )
                    }
                } else {
                    _notificationState.update {
                        it.copy(
                            isScanning = false,
                            message = "No new local MP3s or FLAC files were detected. (Playing seed library!)",
                            isError = false
                        )
                    }
                }
            } catch (e: Exception) {
                _notificationState.update {
                    it.copy(
                        isScanning = false,
                        message = "Scan failed: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    // AI Daily Mix generation handler: launches Gemini to analyze library and form thematic playlists
    fun generateAiMixtape(prompt: String) {
        viewModelScope.launch {
            _notificationState.update { it.copy(isAiGenerating = true, message = "Gemini is building your Daily Mix...") }
            val result = repository.generateAiDailyMix(prompt)
            when (result) {
                "Success" -> {
                    _notificationState.update {
                        it.copy(
                            isAiGenerating = false,
                            message = "AI Mix playlist created successfully!",
                            isError = false
                        )
                    }
                }
                "APIKeyMissing" -> {
                    _notificationState.update {
                        it.copy(
                            isAiGenerating = false,
                            message = "Gemini API key is missing. Please add your key to the SECRETS panel first!",
                            isError = true
                        )
                    }
                }
                "LibraryEmpty" -> {
                    _notificationState.update {
                        it.copy(
                            isAiGenerating = false,
                            message = "Your local music library is empty. Add or scan tracks first!",
                            isError = true
                        )
                    }
                }
                "EmptySelection" -> {
                    _notificationState.update {
                        it.copy(
                            isAiGenerating = false,
                            message = "Gemini could not associate any matching songs for this vibe.",
                            isError = true
                        )
                    }
                }
                else -> {
                    _notificationState.update {
                        it.copy(
                            isAiGenerating = false,
                            message = "AI playlist generation failed: $result",
                            isError = true
                        )
                    }
                }
            }
        }
    }

    // AI Dynamic Lyrics Synchronizer: triggers Gemini to analyze raw lyrics and produce timeline synchronization anchors
    fun synchronizeLyricsForCurrentTrack() {
        val currTrack = playbackState.value.currentTrack ?: return
        viewModelScope.launch {
            _notificationState.update { it.copy(isAiLyricsSyncing = true, message = "Gemini is synchronizing lyrics line timelines...") }
            val result = repository.generateAiLyrics(currTrack.id)
            when (result) {
                "Success" -> {
                    _notificationState.update {
                        it.copy(
                            isAiLyricsSyncing = false,
                            message = "Lyrics synchronized successfully!",
                            isError = false
                        )
                    }
                    // reload playing track inside manager
                    reloadPlayingTrackMetadata(currTrack.id)
                }
                "AlreadySynced" -> {
                    _notificationState.update {
                        it.copy(
                            isAiLyricsSyncing = false,
                            message = "This song already has timelines synced lyrics!",
                            isError = false
                        )
                    }
                }
                "APIKeyMissing" -> {
                    _notificationState.update {
                        it.copy(
                            isAiLyricsSyncing = false,
                            message = "Gemini API key is missing. Add your key to secrets in build first!",
                            isError = true
                        )
                    }
                }
                else -> {
                    _notificationState.update {
                        it.copy(
                            isAiLyricsSyncing = false,
                            message = "Failed syncing lyrics stream: $result",
                            isError = true
                        )
                    }
                }
            }
        }
    }

    private suspend fun reloadPlayingTrackMetadata(trackId: Int) {
        val updatedTrack = repository.getTrackById(trackId)
        if (updatedTrack != null) {
            val q = playbackState.value.playQueue.toMutableList()
            val currentIdx = playbackState.value.queueIndex
            if (currentIdx in q.indices) {
                q[currentIdx] = updatedTrack
                playbackManager.togglePlayPause() // refresh current play context
                playbackManager.playTrackList(q, currentIdx)
                playbackManager.togglePlayPause()
            }
        }
    }

    // Library management action bindings
    fun toggleFavorite(trackId: Int, isFav: Boolean) {
        viewModelScope.launch {
            repository.addFavorite(trackId, isFav)
        }
    }

    fun incrementPlayHistory(trackId: Int) {
        viewModelScope.launch {
            repository.incrementPlayCount(trackId)
        }
    }

    fun createPlaylist(name: String, desc: String, trackIds: List<Int>) {
        viewModelScope.launch {
            repository.createPlaylist(name, desc, trackIds)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun clearNotificationMessage() {
        _notificationState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.releaseAll()
    }
}
