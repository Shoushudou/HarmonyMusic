package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.player.PlaybackUiState
import com.example.ui.MusicViewModel
import com.example.ui.components.LyricsSyncedPane
import com.example.ui.components.ReorderableQueueSheet
import java.util.Locale

@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onCollapse: () -> Unit,
    onShowCastDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val notificationState by viewModel.notificationState.collectAsState()

    val currentTrack = playbackState.currentTrack ?: return

    var activePaneIndex by remember { mutableIntStateOf(0) } // 0 = Player Controls, 1 = Synced Lyrics, 2 = Play Queue
    var showSleepTimerMenu by remember { mutableStateOf(false) }

    // Floating Vinyl spin animation when active playing
    val infiniteTransition = rememberInfiniteTransition(label = "Vinyl Spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Spin Rotation"
    )
    val activeSpin = if (playbackState.isPlaying) spinAngle else 0f

    // Format milliseconds beautifully
    fun formatTrackTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .testTag("now_playing_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = currentTrack.album,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showSleepTimerMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Sleep Timer",
                        tint = if (playbackState.activeSleepTimerMinutes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Sleep Timer countdown banner
            playbackState.activeSleepTimerMinutes?.let { minutes ->
                Card(
                    modifier = Modifier.padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Timer, "timer", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nap Timer: ${minutes}m left",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab bar selectors inside Player panel
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activePaneIndex == 0,
                    onClick = { activePaneIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Controller", fontSize = 11.sp)
                }
                SegmentedButton(
                    selected = activePaneIndex == 1,
                    onClick = { activePaneIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Lyrics", fontSize = 11.sp)
                }
                SegmentedButton(
                    selected = activePaneIndex == 2,
                    onClick = { activePaneIndex = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Queue (${playbackState.playQueue.size})", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Render matching sub pane
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activePaneIndex) {
                    0 -> {
                        // 1. ALBUM COVER CONTROLLER WORKSPACE
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Spinning vinyl record placeholder art
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .shadow(16.dp, CircleShape)
                                    .rotate(activeSpin)
                                    .clip(CircleShape)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = currentTrack.albumArtUrl,
                                    contentDescription = currentTrack.title,
                                    modifier = Modifier
                                        .fillMaxSize(0.95f)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                // Center vinyl golden circle spindle
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.Black, CircleShape)
                                        .border(2.dp, Color(0xFFFFD54F), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Details
                            Text(
                                text = currentTrack.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Text(
                                text = currentTrack.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom speed slider panel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Speed, "Speed", modifier = Modifier.size(14.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Music Velocity: ", fontSize = 10.sp, color = Color.Gray)
                                
                                val speedsList = listOf(0.8f, 1.0f, 1.25f, 1.5f)
                                speedsList.forEach { speed ->
                                                    val isSelected = playbackState.playSpeed == speed
                                                    Text(
                                                        text = "${speed}x",
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                        modifier = Modifier
                                                            .clickable { viewModel.playbackManager.setPlaybackSpeed(speed) }
                                                            .padding(horizontal = 6.dp)
                                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // 2. TIMED SCROLL LYRICS INTERFACE
                        LyricsSyncedPane(
                            lyricsText = currentTrack.lyricsSyncedText,
                            playbackPositionMs = playbackState.playbackPositionMs,
                            onTriggerAiSync = { viewModel.synchronizeLyricsForCurrentTrack() },
                            isSyncing = notificationState.isAiLyricsSyncing,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        // 3. REORDERABLE QUEUE MANAGER ARCH
                        ReorderableQueueSheet(
                            queue = playbackState.playQueue,
                            currentTrackIndex = playbackState.queueIndex,
                            onMove = { from, to -> viewModel.playbackManager.moveQueueItem(from, to) },
                            onRemove = { idx -> viewModel.playbackManager.removeTrackFromQueue(idx) },
                            onSelect = { idx -> viewModel.playbackManager.selectTrackIndex(idx) },
                            onDismissOrClose = { activePaneIndex = 0 },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // --- BOTTOM FIXED CONTROLS AREA ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seekbar slider
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTrackTime(playbackState.playbackPositionMs),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTrackTime(playbackState.totalDurationMs),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val position = playbackState.playbackPositionMs.toFloat()
                val duration = if (playbackState.totalDurationMs > 0) playbackState.totalDurationMs.toFloat() else 1f
                Slider(
                    value = position.coerceIn(0f, duration),
                    onValueChange = { viewModel.playbackManager.seekTo(it.toLong()) },
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth().testTag("player_seek_slider"),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Control core buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Toggle
                    IconButton(onClick = { viewModel.playbackManager.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // Backward SKIP
                    IconButton(onClick = { viewModel.playbackManager.skipPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Song",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Primary Play Toggle Box
                    FilledIconButton(
                        onClick = { viewModel.playbackManager.togglePlayPause() },
                        modifier = Modifier.size(64.dp).testTag("expanded_play_button")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play central remote",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Forward SKIP
                    IconButton(onClick = { viewModel.playbackManager.skipNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Song",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Gapless Playback Toggle
                    IconButton(onClick = { viewModel.playbackManager.toggleGapless(!playbackState.isGaplessEnabled) }) {
                        Icon(
                            imageVector = if (playbackState.isGaplessEnabled) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = "Gapless Toggle",
                            tint = if (playbackState.isGaplessEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // --- SLEEP COUNTDOWN CONFIGURATION DIALOG ---
    if (showSleepTimerMenu) {
        AlertDialog(
            onDismissRequest = { showSleepTimerMenu = false },
            confirmButton = {
                TextButton(onClick = { showSleepTimerMenu = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Set Sleep Nap Timer") },
            text = {
                Column {
                    Text(
                        "Stop music playback automatically. Perfect for falling asleep to rain sounds or relaxing acoustic tunes.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    val options = listOf(5, 15, 30, 45, 60)
                    options.forEach { minutes ->
                        Surface(
                            onClick = {
                                viewModel.playbackManager.setSleepTimer(minutes)
                                showSleepTimerMenu = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$minutes Minutes",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (playbackState.activeSleepTimerMinutes != null) {
                        Surface(
                            onClick = {
                                viewModel.playbackManager.setSleepTimer(null)
                                showSleepTimerMenu = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Cancel active timer",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        )
    }
}
