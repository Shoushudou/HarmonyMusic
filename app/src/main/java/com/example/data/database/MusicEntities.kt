package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [Index(value = ["title", "artist"], unique = true)]
)
data class LocalTrack(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val filePath: String, // Can be MediaStore file Uri or bundled Streaming audio URL
    val albumArtUrl: String, // Online image or media store thumb
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lyricsSyncedText: String = "", // LRC timed lyrics format
    val genre: String = "Unknown"
)

@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = true)]
)
data class CachedArtist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imageUrl: String,
    val bio: String
)

@Entity(tableName = "playlists")
data class LocalPlaylist(
    @PrimaryKey(autoGenerate = true) val playlistId: Int = 0,
    val name: String,
    val description: String,
    val trackIdsCsv: String, // Comma-separated track IDs, e.g., "1,2,5"
    val isAiGenerated: Boolean = false,
    val createdDate: Long = System.currentTimeMillis()
)
