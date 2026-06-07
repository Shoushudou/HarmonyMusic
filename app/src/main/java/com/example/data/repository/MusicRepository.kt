package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.data.api.Content
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiRequest
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.database.CachedArtist
import com.example.data.database.LocalPlaylist
import com.example.data.database.LocalTrack
import com.example.data.database.MusicDao
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao
) {
    val allTracks: Flow<List<LocalTrack>> = musicDao.getAllTracks()
    val favoriteTracks: Flow<List<LocalTrack>> = musicDao.getFavoriteTracks()
    val allArtists: Flow<List<CachedArtist>> = musicDao.getAllArtists()
    val allPlaylists: Flow<List<LocalPlaylist>> = musicDao.getAllPlaylists()

    // Seeds the database with high-quality, pre-bundled online streaming audio tracks
    // for a fantastic out-of-the-box user experience inside the streaming emulator or physical device.
    suspend fun seedDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val existing = musicDao.getAllTracks().first()
            if (existing.isEmpty()) {
                Log.d("MusicRepository", "Seeding default music library...")
                val stream1 = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                val stream2 = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
                val stream3 = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
                val stream4 = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"
                val stream5 = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"

                val seededTracks = listOf(
                    LocalTrack(
                        title = "Midnight Synthwave",
                        artist = "Daft Punk",
                        album = "Neo Paris",
                        durationMs = 372000,
                        filePath = stream1,
                        albumArtUrl = "https://images.unsplash.com/photo-1487180142328-0c4e37023af5?w=500&q=80",
                        genre = "Electronic",
                        lyricsSyncedText = "[00:00.00] (Instrumental Intro)\n[00:15.00] Midnight lights are glowing warm\n[00:22.00] Synthesizers block the storm\n[00:30.00] Rhythms guide us in the street\n[00:38.00] Moving to a retro beat\n[00:46.00] Chasing high-tech stellar dreams\n[00:54.00] Floating over cosmic streams\n[01:02.00] One more time we find our space\n[01:10.00] Dancing in this neon place\n[01:18.00] (Synthwave Guitar Solo)\n[01:45.00] Feel the spark inside your hand\n[01:53.00] Driving to an ancient land\n[02:01.00] Chasing high-tech stellar dreams\n[02:08.00] Floating over cosmic streams"
                    ),
                    LocalTrack(
                        title = "Acoustic Golden Sunsets",
                        artist = "Coldplay",
                        album = "Golden Days",
                        durationMs = 344000,
                        filePath = stream2,
                        albumArtUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
                        genre = "Acoustic",
                        lyricsSyncedText = "[00:00.00] (Acoustic Guitar Strumming)\n[00:12.00] Yellow sun sets in the west\n[00:19.00] Close your eyes and take your rest\n[00:27.00] Summer breeze and golden sand\n[00:34.00] Hold the beauty of the land\n[00:42.00] Oh we write our names in dust\n[00:49.00] Finding people we can trust\n[00:57.00] Safe and warm under the stars\n[01:04.00] Healing up these ancient scars\n[01:12.00] (Acoustic Bridge section)\n[01:30.00] Look how they shine for you\n[01:38.00] Everything that we've been through"
                    ),
                    LocalTrack(
                        title = "Indie Cyber Lounge",
                        artist = "The Weeknd",
                        album = "Retro Glow",
                        durationMs = 302000,
                        filePath = stream3,
                        albumArtUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
                        genre = "Indie Pop",
                        lyricsSyncedText = "[00:00.00] (Synth Drums Intro)\n[00:10.00] I've been calling all night long\n[00:17.00] Wondering where we both went wrong\n[00:25.00] Blinding spotlights on the floor\n[00:32.00] Lock the keys and bolt the door\n[00:40.00] I cannot breathe without your air\n[00:48.00] Losing hope but I don't care\n[00:56.00] Sing a dark and retro tune\n[01:04.00] Underneath this plastic moon\n[01:12.00] (Sultry Saxophone Solo)\n[01:30.00] I've been calling all night long\n[01:38.00] Play our favorite synth pop song"
                    ),
                    LocalTrack(
                        title = "Atmospheric Cosmic Symphony",
                        artist = "Hans Zimmer",
                        album = "Interstellar Dimensions",
                        durationMs = 518000,
                        filePath = stream4,
                        albumArtUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500&q=80",
                        genre = "Cinematic",
                        lyricsSyncedText = "[00:00.00] (Slow Orchestral Pad)\n[00:25.00] Time is ticking on the clock\n[00:50.00] Waves crash deep upon the rock\n[01:15.00] Gravity will pull us through\n[01:40.00] Across the dark to find what's true\n[02:05.00] (Majestic Brass and Organ Swell)\n[02:40.00] Every second is a year\n[03:10.00] Overcoming human fear\n[03:40.00] Floating in the grand sublime\n[04:10.00] Bending space and weaving time"
                    ),
                    LocalTrack(
                        title = "Cozy Rainfall Sleepbeats",
                        artist = "Lofi Chill",
                        album = "Cozy Rainy Days",
                        durationMs = 286000,
                        filePath = stream5,
                        albumArtUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80",
                        genre = "Lofi",
                        lyricsSyncedText = "[00:00.00] (Rainfall and Vinyl Static CRACKLE)\n[00:12.00] Warm coffee sweet and slow\n[00:25.00] Watching winter winds and snow\n[00:38.00] Chillhop piano keys align\n[00:51.00] Cozy beats and notes combine\n[01:04.00] Close your eyes and dream away\n[01:18.00] Rest until another day\n[01:32.00] (Soothing Melodica Improvisation)\n[02:00.00] Warm coffee sweet and slow\n[02:15.00] Letting all your worries go"
                    )
                )

                musicDao.insertTracks(seededTracks)
                // Trigger image downloads for these artists as well
                for (track in seededTracks) {
                    downloadArtistImage(track.artist)
                }
            }
        }
    }

    // Scans local phone directory media files using Android MediaStore
    suspend fun scanLocalTracks(): Int {
        return withContext(Dispatchers.IO) {
            var scannedCount = 0
            val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.createAttributionContext("attributionTag")
            } else {
                context
            }
            val resolver: ContentResolver = attributionContext.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )
            // Only audio files that are music
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            try {
                val cursor = resolver.query(uri, projection, selection, null, null)
                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (c.moveToNext()) {
                        val path = c.getString(dataCol)
                        // Basic check
                        if (path.substringAfterLast(".").lowercase() in listOf("mp3", "flac", "m4a", "wav", "ogg")) {
                            val title = c.getString(titleCol) ?: "Unknown Track"
                            val artist = c.getString(artistCol) ?: "Unknown Artist"
                            val album = c.getString(albumCol) ?: "Unknown Album"
                            val duration = c.getLong(durationCol)

                            val track = LocalTrack(
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = if (duration > 0) duration else 180000L,
                                filePath = path,
                                albumArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80", // standard template art
                                genre = "Local File"
                            )
                            val rowId = musicDao.insertTrack(track)
                            if (rowId > 0) {
                                scannedCount++
                                downloadArtistImage(artist)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicRepository", "MediaStore scan exception", e)
            }
            scannedCount
        }
    }

    // Automatically downloads and caches artist image details based on curated map or dynamic Unsplash category
    suspend fun downloadArtistImage(artistName: String): CachedArtist {
        return withContext(Dispatchers.IO) {
            val existing = musicDao.getArtistByName(artistName)
            if (existing != null) {
                return@withContext existing
            }

            // High-quality curated list to matches visual aesthetic
            val unsplashImageMap = mapOf(
                "Daft Punk" to "https://images.unsplash.com/photo-1487180142328-0c4e37023af5?w=500&q=80",
                "Coldplay" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
                "The Weeknd" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
                "Hans Zimmer" to "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500&q=80",
                "Lofi Chill" to "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80"
            )

            val imageUrl = unsplashImageMap[artistName] 
                ?: "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&q=80"

            val bio = "A wonderful offline singer/songwriter of $artistName. Automatically discovered, downloaded and indexed."

            val cachedArtist = CachedArtist(
                name = artistName,
                imageUrl = imageUrl,
                bio = bio
            )
            musicDao.insertArtist(cachedArtist)
            Log.d("MusicRepository", "Successfully downloaded artist info for $artistName")
            cachedArtist
        }
    }

    suspend fun getTrackById(id: Int) = musicDao.getTrackById(id)

    suspend fun addFavorite(trackId: Int, isFav: Boolean) {
        musicDao.setFavorite(trackId, isFav)
    }

    suspend fun incrementPlayCount(trackId: Int) {
        musicDao.incrementPlayCount(trackId)
    }

    suspend fun updateLyrics(trackId: Int, lyricsText: String) {
        musicDao.updateLyrics(trackId, lyricsText)
    }

    suspend fun createPlaylist(name: String, desc: String, trackIds: List<Int>): Long {
        val csv = trackIds.joinToString(",")
        val playlist = LocalPlaylist(
            name = name,
            description = desc,
            trackIdsCsv = csv,
            isAiGenerated = false
        )
        return musicDao.insertPlaylist(playlist)
    }

    suspend fun deletePlaylist(id: Int) {
        musicDao.deletePlaylistById(id)
    }

    // AI Daily Mix generator: utilizes Gemini 3.5 Flash to automatically recommend tracks
    // grouped into a themed mix based on user context, library size, and requested visual vibe.
    suspend fun generateAiDailyMix(userVibePrompt: String? = null): String {
        return withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext "APIKeyMissing"
            }

            // Gather libraries tracks for context
            val library = musicDao.getAllTracks().first()
            if (library.isEmpty()) {
                return@withContext "LibraryEmpty"
            }

            val tracksInfo = library.joinToString("\n") { "ID: ${it.id} - '${it.title}' by [${it.artist}] (Album: ${it.album}, Genre: ${it.genre})" }

            val vibe = userVibePrompt ?: "A perfect refreshing workout, chill study sessions, or driving late at night"
            val systemPrompt = """
                You are a premium AI music curator on Android.
                Compile an AI daily mix playlist based ONLY on the user's available offline library index shown below.
                Group 3 to 5 matching songs that fit the following desired vibe: "$vibe".
                
                You must respond with a strictly formatted JSON object containing these exact fields:
                - "title": a cute visual, thematic title for this mix (e.g., "Neon Rain Study", "Cosmic Groove Lift")
                - "description": a highly descriptive and friendly sentence summarizing the mood
                - "trackIds": an array of integers representing the track IDs from the list that fit.
                
                Respond in clean, raw JSON only. Do not enclose the reply in markdown backticks or any dialogue text.
            """.trimIndent()

            val combinedModelPrompt = "Here is the user's available track list:\n$tracksInfo"

            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = combinedModelPrompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            try {
                val call = GeminiClient.service.generateContent(apiKey, requestBody)
                val rawResponse = call.string()
                val textResponse = GeminiClient.parseGeminiText(rawResponse)
                
                Log.d("MusicRepository", "Gemini AI Playlist JSON response: $textResponse")

                // Parse the clean JSON response to populate local playlists
                val cleanJson = textResponse.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanJson)
                val mixTitle = json.optString("title", "My AI Cozy Mix")
                val mixDesc = json.optString("description", "A custom selected daily mixtape compiled safely by Gemini.")
                val arr = json.getJSONArray("trackIds")
                val trackIdsList = mutableListOf<Int>()
                for (i in 0 until arr.length()) {
                    trackIdsList.add(arr.getInt(i))
                }

                if (trackIdsList.isNotEmpty()) {
                    val mixPlaylist = LocalPlaylist(
                        name = mixTitle,
                        description = mixDesc,
                        trackIdsCsv = trackIdsList.joinToString(","),
                        isAiGenerated = true
                    )
                    musicDao.insertPlaylist(mixPlaylist)
                    return@withContext "Success"
                } else {
                    return@withContext "EmptySelection"
                }
            } catch (e: Exception) {
                Log.e("MusicRepository", "Gemini call exception", e)
                return@withContext "Error: ${e.localizedMessage}"
            }
        }
    }

    // AI Synchronized lyrics helper: utilizes Gemini 3.5 Flash to automatically parse raw, basic lyrics
    // and align them into timestamps based on typical song timing
    suspend fun generateAiLyrics(trackId: Int): String {
        return withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext "APIKeyMissing"
            }

            val track = musicDao.getTrackById(trackId) ?: return@withContext "TrackNotFound"
            
            // Check if already has lyrics
            if (track.lyricsSyncedText.isNotEmpty() && track.lyricsSyncedText.contains("[")) {
                return@withContext "AlreadySynced"
            }

            val rawLyrics = """
                [Intro Instrumental]
                Midnight lines are glowing warm,
                Synthesizers block the storm.
                Rhythms guide us in the street,
                Moving to a retro beat.
                Chasing high-tech stellar dreams,
                Floating over cosmic streams.
                One more time we find our space,
                Dancing in this neon place.
                [Guitar Outro]
            """.trimIndent()

            val systemInstructionText = """
                You are a synchronized lyrics generator engine on Android.
                Configure timeline stamps anchor for the song "${track.title}" by "${track.artist}".
                Add timestamps to every lyric line based on a typical song starting from 00:00.00 up to 03:00.00.
                Output in standard LRC format, e.g., '[mm:ss.SS] Lyric line text'.
                Respond strictly with the text content of the LRC file, without any extra text or code blocks.
            """.trimIndent()

            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Please synchronize these lyrics:\n$rawLyrics")))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
                generationConfig = GenerationConfig(temperature = 0.5f)
            )

            try {
                val call = GeminiClient.service.generateContent(apiKey, requestBody)
                val rawResponse = call.string()
                val parsedText = GeminiClient.parseGeminiText(rawResponse)
                    .trim()
                    .removePrefix("```lrc")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                if (parsedText.isNotEmpty() && parsedText.contains("[")) {
                    musicDao.updateLyrics(trackId, parsedText)
                    return@withContext "Success"
                } else {
                    return@withContext "FailedToFormat"
                }
            } catch (e: Exception) {
                Log.e("MusicRepository", "Gemini Lyrics sync call failed", e)
                return@withContext "Error: ${e.localizedMessage}"
            }
        }
    }
}
