package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // --- Tracks Queries ---
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<LocalTrack>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Int): LocalTrack?

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<LocalTrack>>

    @Query("SELECT * FROM tracks WHERE title LIKE :query OR artist LIKE :query OR album LIKE :query")
    fun searchTracks(query: String): Flow<List<LocalTrack>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(track: LocalTrack): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<LocalTrack>)

    @Update
    suspend fun updateTrack(track: LocalTrack)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: Int, isFavorite: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Int)

    @Query("UPDATE tracks SET lyricsSyncedText = :lyrics WHERE id = :trackId")
    suspend fun updateLyrics(trackId: Int, lyrics: String)

    // --- Artists Queries ---
    @Query("SELECT * FROM artists WHERE name = :name LIMIT 1")
    suspend fun getArtistByName(name: String): CachedArtist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: CachedArtist)

    @Query("SELECT * FROM artists")
    fun getAllArtists(): Flow<List<CachedArtist>>

    // --- Playlists Queries ---
    @Query("SELECT * FROM playlists ORDER BY createdDate DESC")
    fun getAllPlaylists(): Flow<List<LocalPlaylist>>

    @Query("SELECT * FROM playlists WHERE playlistId = :id")
    suspend fun getPlaylistById(id: Int): LocalPlaylist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: LocalPlaylist): Long

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylistById(playlistId: Int)
}
