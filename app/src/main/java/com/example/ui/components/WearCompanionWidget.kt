package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlaybackUiState

@Composable
fun WearCompanionWidget(
    playbackState: PlaybackUiState,
    onWearAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current wrist volume simulation
    var watchVolume by remember { mutableFloatStateOf(0.7f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Watch,
                contentDescription = "Watch Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Wear OS Companion (Virtual Watch)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = "Your wrist controller is automatically linked. Tap the watch keys below to remotely direct the phone player, simulating dual devices!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        // Physical Watch Case Outer Frame
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(16.dp, shape = CircleShape, clip = false)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2C2C2C), Color(0xFF151515))
                    ),
                    shape = CircleShape
                )
                .border(6.dp, Color(0xFF424242), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Bezel highlight
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    style = Stroke(width = 4f)
                )
            }

            // Wear App screen Canvas (circular OLED black background)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Tracking play progress ring
                val progress = if (playbackState.totalDurationMs > 0) {
                    playbackState.playbackPositionMs.toFloat() / playbackState.totalDurationMs.toFloat()
                } else 0f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // background grey arc
                    drawArc(
                        color = Color(0xFF222222),
                        startAngle = -210f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // active colorful progress arc representing song play
                    drawArc(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF81C784), Color(0xFF4DD0E1))
                        ),
                        startAngle = -210f,
                        sweepAngle = 240f * progress,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Internal Wear UI Layout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Watch Header: status or sync icon
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.wearControlSynced) Icons.Default.BluetoothConnected else Icons.Default.Sync,
                            contentDescription = "Syncing",
                            tint = if (playbackState.wearControlSynced) Color(0xFF4DD0E1) else Color(0xFFFFD54F),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (playbackState.wearControlSynced) "REMOTE ON" else "SYNCING...",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playbackState.wearControlSynced) Color(0xFF4DD0E1) else Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Content - Now Playing details
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = playbackState.currentTrack?.title ?: "Wrist Standby",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Text(
                            text = playbackState.currentTrack?.artist ?: "Harmony Player",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Active live Lyrics fragment streaming on the wrist!
                        val currentLyric = remember(playbackState.playbackPositionMs, playbackState.currentTrack) {
                            parseActiveLyricLine(playbackState.currentTrack?.lyricsSyncedText, playbackState.playbackPositionMs)
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentLyric.ifEmpty { "🎵 Instrumental" },
                                fontSize = 8.sp,
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Control Row - Wear tactile keys
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous remote key
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222222))
                                .clickable { onWearAction("PREVIOUS") }
                                .testTag("wear_prev"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Prev remote",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Play pause remote key
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onWearAction("PLAY_PAUSE") }
                                .testTag("wear_play"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play remote",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Next remote key
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222222))
                                .clickable { onWearAction("NEXT") }
                                .testTag("wear_next"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next remote",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Bottom Wrist Volume Meter
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = "Vol",
                            tint = Color.Gray,
                            modifier = Modifier.size(10.dp).clickable {
                                watchVolume = (watchVolume - 0.1f).coerceAtLeast(0f)
                            }
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        LinearProgressIndicator(
                            progress = { watchVolume },
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = Color(0xFF4DD0E1),
                            trackColor = Color(0xFF222222)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Vol+",
                            tint = Color.Gray,
                            modifier = Modifier.size(10.dp).clickable {
                                watchVolume = (watchVolume + 0.1f).coerceAtMost(1f)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Helper methods to match lyric timeline of the active track
private fun parseActiveLyricLine(lyricsText: String?, playbackPositionMs: Long): String {
    val timedLyrics = parseLrcLines(lyricsText)
    if (timedLyrics.isEmpty()) return ""
    var matchIdx = -1
    for (i in timedLyrics.indices) {
        if (playbackPositionMs >= timedLyrics[i].timestampMs) {
            matchIdx = i
        } else {
            break
        }
    }
    return if (matchIdx != -1) timedLyrics[matchIdx].text else ""
}

private fun parseLrcLines(lyricsText: String?): List<TimedLyricLine> {
    if (lyricsText.isNullOrEmpty()) return emptyList()
    val list = mutableListOf<TimedLyricLine>()
    val lines = lyricsText.split("\n")

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("[")) {
            val bracketIndex = trimmed.indexOf("]")
            if (bracketIndex != -1) {
                val timeStr = trimmed.substring(1, bracketIndex)
                val lyricPart = trimmed.substring(bracketIndex + 1).trim()

                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val minutes = parts[0].toLongOrNull() ?: 0L
                    val secondsParts = parts[1].split(".")
                    val seconds = secondsParts.getOrNull(0)?.toLongOrNull() ?: 0L
                    val millis = secondsParts.getOrNull(1)?.toLongOrNull() ?: 0L

                    // Calculate ms
                    val totalMs = (minutes * 60 * 1000) + (seconds * 1000) + (millis * 10)
                    list.add(TimedLyricLine(totalMs, lyricPart))
                }
            }
        }
    }
    return list.sortedBy { it.timestampMs }
}
