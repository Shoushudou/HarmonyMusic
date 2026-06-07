package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.CachedArtist
import com.example.data.database.LocalTrack
import com.example.ui.MusicViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onExpandPlayer: () -> Unit,
    onNavigateToAiMix: () -> Unit,
    onShowCastDialog: () -> Unit,
    onShowWearDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Songs, 1 = Albums, 2 = Artists

    val tracks by viewModel.allTracks.collectAsState()
    val artists by viewModel.allArtists.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val notificationState by viewModel.notificationState.collectAsState()

    // 1. Filter tracks based on search bar
    val filteredTracks = remember(tracks, searchQuery) {
        if (searchQuery.isEmpty()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // 2. Identify unique albums from tracks
    val albumsList = remember(tracks) {
        tracks.map { it.album to it.albumArtUrl }.distinctBy { it.first }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("library_screen"),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Main Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Harmony Music",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row {
                        // Wear remote sync indicator
                        IconButton(onClick = onShowWearDialog) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = "Wear OS Remote Status",
                                tint = if (playbackState.wearControlSynced) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        
                        // Caste Chromecast connection button
                        IconButton(onClick = onShowCastDialog) {
                            Icon(
                                imageVector = if (playbackState.castDeviceConnected != null) Icons.Default.CastConnected else Icons.Default.Cast,
                                contentDescription = "Cast to Chromecast",
                                tint = if (playbackState.castDeviceConnected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Local scanner trigger
                        IconButton(
                            onClick = { viewModel.scanDeviceForLocalAudio() },
                            modifier = Modifier.testTag("scan_button")
                        ) {
                            if (notificationState.isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Scan Local Files"
                                )
                            }
                        }
                    }
                }

                // AI Playlist shortcut banner button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onNavigateToAiMix() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Mix",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Daily Mix AI",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Ask Gemini to build custom mixes tailored to any vibe!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs, artists, albums...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("search_bar"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Material You Tabs (Songs, Albums, Artists)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = { HorizontalDivider(thickness = 0.5.dp) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Songs", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Albums", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Artists", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                }
            }
        },
        bottomBar = {
            // Minimized media dynamic controller bottom bar
            playbackState.currentTrack?.let { currentTrack ->
                Surface(
                    onClick = onExpandPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("minimized_player_bar"),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp
                ) {
                    Column {
                        // Micro progress bar matching top speed play sync
                        val progress = if (playbackState.totalDurationMs > 0) {
                            playbackState.playbackPositionMs.toFloat() / playbackState.totalDurationMs.toFloat()
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = currentTrack.albumArtUrl,
                                contentDescription = "Min cover art",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTrack.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (playbackState.castDeviceConnected != null) {
                                        Icon(
                                            imageVector = Icons.Default.CastConnected,
                                            contentDescription = "Simulated cast icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp).padding(end = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = if (playbackState.castDeviceConnected != null) {
                                            "Casting to ${playbackState.castDeviceConnected}"
                                        } else {
                                            currentTrack.artist
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Quick player actions
                            IconButton(onClick = { viewModel.playbackManager.skipPrevious() }) {
                                Icon(Icons.Default.SkipPrevious, "Back")
                            }

                            IconButton(
                                onClick = { viewModel.playbackManager.togglePlayPause() },
                                modifier = Modifier.testTag("min_play_button")
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/pause togggle"
                                )
                            }

                            IconButton(onClick = { viewModel.playbackManager.skipNext() }) {
                                Icon(Icons.Default.SkipNext, "Next")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    // SONGS LIST
                    if (filteredTracks.isEmpty()) {
                        EmptyStatePrompt(
                            "Your audio folder is empty",
                            "Scan local memories or ask Gemini to compose tracks."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                        ) {
                            items(filteredTracks) { track ->
                                TrackItemRow(
                                    track = track,
                                    currentTrack = playbackState.currentTrack,
                                    onPlayClick = {
                                        val idx = filteredTracks.indexOf(track)
                                        viewModel.playbackManager.playTrackList(filteredTracks, idx)
                                        viewModel.incrementPlayHistory(track.id)
                                    },
                                    onFavToggle = { viewModel.toggleFavorite(track.id, !track.isFavorite) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // ALBUMS DATABASE GRID
                    if (albumsList.isEmpty()) {
                        EmptyStatePrompt("No Albums Cached", "Seeding or scanning audio file catalogs...")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(albumsList) { pair ->
                                val albumName = pair.first
                                val artUrl = pair.second

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Play all songs matching this album
                                            val albumTracks = tracks.filter { it.album == albumName }
                                            if (albumTracks.isNotEmpty()) {
                                                viewModel.playbackManager.playTrackList(albumTracks, 0)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Column {
                                        AsyncImage(
                                            model = artUrl,
                                            contentDescription = albumName,
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = albumName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        Text(
                                            text = "Album • Local Files",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // AUTOMATIC INTERNET DISCOVERED ARTISTS LIST
                    if (artists.isEmpty()) {
                        EmptyStatePrompt("Scanning Artists", "Please wait while we automatically index offline tracks...")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                        ) {
                            items(artists) { artist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            // Play all tracks by this artist
                                            val artistTracks = tracks.filter { it.artist == artist.name }
                                            if (artistTracks.isNotEmpty()) {
                                                viewModel.playbackManager.playTrackList(artistTracks, 0)
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = artist.imageUrl,
                                        contentDescription = artist.name,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = artist.bio,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Artist profile ok",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItemRow(
    track: LocalTrack,
    currentTrack: LocalTrack?,
    onPlayClick: () -> Unit,
    onFavToggle: () -> Unit
) {
    val isPlayingSelf = currentTrack?.id == track.id
    val tintColor = if (isPlayingSelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPlayClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number or visual Equalizer symbol
        if (isPlayingSelf) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Active Playing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Passive Music",
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Tracks properties details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = tintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist}  •  ${track.album}",
                fontSize = 11.sp,
                color = if (isPlayingSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action controls (Favorites indicator)
        IconButton(onClick = onFavToggle) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite Check",
                tint = if (track.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStatePrompt(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = "Cloud Queue",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
