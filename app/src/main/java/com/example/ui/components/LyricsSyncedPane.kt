package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class TimedLyricLine(
    val timestampMs: Long,
    val text: String
)

@Composable
fun LyricsSyncedPane(
    lyricsText: String?,
    playbackPositionMs: Long,
    onTriggerAiSync: () -> Unit,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 1. Parse LRC formatted lines
    val timedLyrics = remember(lyricsText) {
        parseLrcLyrics(lyricsText)
    }

    // 2. Identify the active lyric index based on the current track progress
    val activeIndex = remember(playbackPositionMs, timedLyrics) {
        var matchIdx = -1
        for (i in timedLyrics.indices) {
            if (playbackPositionMs >= timedLyrics[i].timestampMs) {
                matchIdx = i
            } else {
                break
            }
        }
        matchIdx
    }

    // 3. Auto-scroll to center the active item on update
    LaunchedEffect(activeIndex) {
        if (activeIndex != -1 && timedLyrics.isNotEmpty()) {
            coroutineScope.launch {
                // Scroll that item to the middle of the screen
                val targetScrollPos = (activeIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(targetScrollPos)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("lyrics_pane"),
        contentAlignment = Alignment.Center
    ) {
        if (timedLyrics.isEmpty()) {
            // Empty state prompting the user to hit Gemini lyrics synchronization
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No Synced Lyrics",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "No Synchronized Lyrics Found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Local files can be aligned automatically! Instruct Gemini to analyze the lyrics structure and generate real-time scrolling timed anchors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AI is aligning lyric lines...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Button(
                        onClick = onTriggerAiSync,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("ai_sync_lyrics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Sync",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Sync Lyrics Timestamps")
                    }
                }
            }
        } else {
            // High-fidelity active lyrics view
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(timedLyrics) { index, line ->
                    val isActive = index == activeIndex
                    
                    // Highlight active lyrics with Material You primary, others fade translucent
                    val textColor = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }

                    val scaleFactor by animateFloatAsState(
                        targetValue = if (isActive) 1.2f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "Font Scale"
                    )

                    Text(
                        text = line.text,
                        fontSize = (18 * scaleFactor).sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

// Inline LRC text timing line parser
fun parseLrcLyrics(lyricsText: String?): List<TimedLyricLine> {
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
