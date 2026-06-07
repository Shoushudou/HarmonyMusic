package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MusicViewModel
import com.example.ui.components.ChromecastDialog
import com.example.ui.components.WearCompanionWidget
import com.example.ui.screens.AiMixScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val playbackState by viewModel.playbackState.collectAsState()
            val notificationState by viewModel.notificationState.collectAsState()

            // State indicators for page routers and dialog triggers
            var currentScreen by remember { mutableStateOf("library") } // "library" or "ai_mix"
            var isPlayerExpanded by remember { mutableStateOf(false) }
            var showCastDialog by remember { mutableStateOf(false) }
            var showWearDialog by remember { mutableStateOf(false) }

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            // Adaptive Visual Theme: adapts the entire app’s color layout dynamically to the active song genre!
            val dynamicGenreTheme = remember(playbackState.currentTrack) {
                playbackState.currentTrack?.genre
            }

            MyApplicationTheme(themeGenre = dynamicGenreTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHostState = { snackbarHostState }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (isPlayerExpanded) 0.dp else innerPadding.calculateBottomPadding())
                    ) {
                        // Main Page Router with slide transitions
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                slideInHorizontally { width -> if (targetState == "ai_mix") width else -width } togetherWith
                                slideOutHorizontally { width -> if (targetState == "ai_mix") -width else width }
                            },
                            label = "Page Navigation"
                        ) { screen ->
                            when (screen) {
                                "library" -> {
                                    LibraryScreen(
                                        viewModel = viewModel,
                                        onExpandPlayer = { isPlayerExpanded = true },
                                        onNavigateToAiMix = { currentScreen = "ai_mix" },
                                        onShowCastDialog = { showCastDialog = true },
                                        onShowWearDialog = { showWearDialog = true },
                                        modifier = Modifier.padding(
                                            top = innerPadding.calculateTopPadding()
                                        )
                                    )
                                }
                                "ai_mix" -> {
                                    AiMixScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "library" },
                                        modifier = Modifier.padding(
                                            top = innerPadding.calculateTopPadding()
                                        )
                                    )
                                }
                            }
                        }

                        // Expanded Now Playing Sheet Overlay
                        AnimatedVisibility(
                            visible = isPlayerExpanded,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            NowPlayingScreen(
                                viewModel = viewModel,
                                onCollapse = { isPlayerExpanded = false },
                                onShowCastDialog = { showCastDialog = true }
                            )
                        }

                        // Simulated Chromecast Target Selection dialog overlay
                        if (showCastDialog) {
                            ChromecastDialog(
                                connectedDevice = playbackState.castDeviceConnected,
                                onConnect = { deviceName -> viewModel.playbackManager.toggleCastDevice(deviceName) },
                                onDismiss = { showCastDialog = false }
                            )
                        }

                        // Simulated Wear OS tactile smartwatch dialog panel
                        if (showWearDialog) {
                            AlertDialog(
                                onDismissRequest = { showWearDialog = false },
                                confirmButton = {
                                    TextButton(onClick = { showWearDialog = false }) {
                                        Text("Ok")
                                    }
                                },
                                title = {
                                    Text(
                                        text = "Wrist Companion App",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        WearCompanionWidget(
                                            playbackState = playbackState,
                                            onWearAction = { action -> viewModel.playbackManager.syncFromWearOS(action) }
                                        )
                                    }
                                }
                            )
                        }

                        // Notification Toast listener
                        LaunchedEffect(notificationState.message) {
                            notificationState.message?.let { msg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = msg,
                                        duration = SnackbarDuration.Short
                                    )
                                    viewModel.clearNotificationMessage()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple legacy wrapper to prevent preview crashes on standard schemas
@Composable
 fun Scaffold(
     modifier: Modifier = Modifier,
     snackbarHostState: @Composable () -> SnackbarHostState = { remember { SnackbarHostState() } },
     bottomBar: @Composable () -> Unit = {},
     topBar: @Composable () -> Unit = {},
     content: @Composable (PaddingValues) -> Unit
 ) {
     Scaffold(
         modifier = modifier,
         topBar = topBar,
         bottomBar = bottomBar,
         snackbarHost = { SnackbarHost(hostState = snackbarHostState()) },
         content = content
     )
 }
