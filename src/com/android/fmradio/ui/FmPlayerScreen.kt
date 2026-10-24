/*
 * Copyright (C) 2026 RevampedFMRadio Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.fmradio.ui

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import java.util.Locale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.fmradio.FmPreferences
import com.android.fmradio.FmRecorder
import com.android.fmradio.R
import com.android.fmradio.ui.artwork.StationArtworkGenerator
import com.android.fmradio.ui.favorites.FmFavoritesSheet
import com.android.fmradio.ui.recording.FmRecordingsScreen
import com.android.fmradio.ui.recording.RecordingItem
import com.android.fmradio.ui.settings.FmSettingsScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class FavoriteStationItem(
    val id: Long,
    val frequency: Int,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmPlayerScreen(
    currentFrequency: Float,
    stationName: String,
    radioText: String,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isSpeakerUsed: Boolean,
    isAudioRoutingAvailable: Boolean = false,
    isRecording: Boolean,
    isSeeking: Boolean = false,
    isRecordingSheetVisible: Boolean = false,
    recordingState: Int = FmRecorder.STATE_IDLE,
    recordDurationMs: Long = 0L,
    recordSizeBytes: Long = 0L,
    defaultRecordingName: String = "",
    favoritesList: List<FavoriteStationItem>,
    recordingsList: List<RecordingItem> = emptyList(),
    onPlayPauseClick: () -> Unit,
    onPrevStationClick: () -> Unit,
    onNextStationClick: () -> Unit,
    onTuneDownClick: () -> Unit,
    onTuneUpClick: () -> Unit,
    onFrequencySeek: (Float) -> Unit,
    onToggleFavoriteClick: () -> Unit,
    onToggleSpeakerClick: () -> Unit,
    onRecordClick: () -> Unit,
    onStationSelect: (Int) -> Unit,
    onReloadFavorites: () -> Unit = {},
    onDismissRecordingSheet: () -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onSaveRecording: (String) -> Unit = {},
    onDiscardRecording: () -> Unit = {},
    onDeleteRecording: (RecordingItem) -> Unit = {}
) {
    val context = LocalContext.current
    val freqInt = (currentFrequency * 10).toInt()
    val artworkBitmap by produceState<Bitmap?>(initialValue = null, key1 = freqInt, key2 = stationName) {
        value = withContext(Dispatchers.Default) {
            StationArtworkGenerator.getStationArtwork(context, freqInt, stationName)
        }
    }

    var isScrubbing by remember { mutableStateOf(false) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var reduceMotion by remember { mutableStateOf(FmPreferences.isReduceMotionEnabled(context)) }

    // App Entrance Transition on Fresh Open
    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasEntered = true
    }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "entrance_alpha"
    )
    val entranceOffsetY by animateFloatAsState(
        targetValue = if (hasEntered) 0f else 28f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "entrance_offset"
    )

    // Dynamic Variable Font State Definitions (wght, wdth, slnt, ROND 100)
    @OptIn(ExperimentalTextApi::class)
    val pausedTitleFont = remember {
        FontFamily(
            Font(
                familyName = DeviceFontFamilyName("google-sans-flex"),
                weight = FontWeight.Normal,
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("wght", 400f),
                    FontVariation.Setting("wdth", 100f),
                    FontVariation.Setting("slnt", 0f),
                    FontVariation.Setting("ROND", 100f)
                )
            )
        )
    }

    @OptIn(ExperimentalTextApi::class)
    val playedTitleFont = remember {
        FontFamily(
            Font(
                familyName = DeviceFontFamilyName("google-sans-flex"),
                weight = FontWeight.Bold,
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("wght", 700f),
                    FontVariation.Setting("wdth", 135f),
                    FontVariation.Setting("slnt", 0f),
                    FontVariation.Setting("ROND", 100f)
                )
            )
        )
    }

    @OptIn(ExperimentalTextApi::class)
    val recordingTitleFont = remember {
        FontFamily(
            Font(
                familyName = DeviceFontFamilyName("google-sans-flex"),
                weight = FontWeight.Bold,
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("wght", 700f),
                    FontVariation.Setting("wdth", 135f),
                    FontVariation.Setting("slnt", -10f),
                    FontVariation.Setting("ROND", 100f)
                )
            )
        )
    }

    // 15-step quantized breathing list (wght 800, breathing wdth 100..135, slnt 0, ROND 100)
    @OptIn(ExperimentalTextApi::class)
    val breathingTitleFonts = remember {
        (0..14).map { step ->
            val width = 100f + step * 2.5f
            FontFamily(
                Font(
                    familyName = DeviceFontFamilyName("google-sans-flex"),
                    weight = FontWeight.ExtraBold,
                    variationSettings = FontVariation.Settings(
                        FontVariation.Setting("wght", 800f),
                        FontVariation.Setting("wdth", width),
                        FontVariation.Setting("slnt", 0f),
                        FontVariation.Setting("ROND", 100f)
                    )
                )
            )
        }
    }

    // Asynchronously pre-warm TypefaceRequestCache to eliminate any font load flicker
    val fontResolver = LocalFontFamilyResolver.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            fontResolver.resolve(pausedTitleFont)
            fontResolver.resolve(playedTitleFont)
            fontResolver.resolve(recordingTitleFont)
            breathingTitleFonts.forEach { fontResolver.resolve(it) }
        }
    }

    val isScrubbingOrSeeking = isScrubbing || isSeeking
    val breathingTransition = rememberInfiniteTransition(label = "title_breathing")
    val breathingStep by breathingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_step"
    )

    val currentBreathingFont = if (reduceMotion) {
        breathingTitleFonts[7] // static wdth 117.5f
    } else {
        breathingTitleFonts[breathingStep.roundToInt().coerceIn(0, 14)]
    }

    val currentTitleFont = when {
        isRecording -> recordingTitleFont
        isScrubbingOrSeeking -> currentBreathingFont
        isPlaying -> playedTitleFont
        else -> pausedTitleFont
    }

    // Tactile Spring RenderNode Physics between interactions
    val fontScaleX by animateFloatAsState(
        targetValue = if (reduceMotion) 1f else if (isScrubbingOrSeeking) 1.06f else if (isPlaying) 1.02f else 0.98f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "font_scale_x"
    )
    val fontScaleY by animateFloatAsState(
        targetValue = if (reduceMotion) 1f else if (isScrubbingOrSeeking) 1.03f else if (isPlaying) 1.01f else 0.99f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "font_scale_y"
    )

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Playback and channel change protection guards
    val controlsEnabled = isPlaying && !isSeeking
    var pendingChannelChange by remember { mutableStateOf<(() -> Unit)?>(null) }
    val safeChangeStation: (() -> Unit) -> Unit = { action ->
        if (isRecording) {
            pendingChannelChange = action
        } else {
            action()
        }
    }

    // Orientation Check for Adaptive UI & Fullscreen Immersive Mode
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(isLandscape) {
        val window = (context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        if (isLandscape) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .alpha(entranceAlpha)
                .graphicsLayer { translationY = entranceOffsetY.dp.toPx() }
        ) {
            if (isLandscape) {
                // Adaptive Landscape Layout (Symmetrical 50/50 two-pane presentation in pure fullscreen)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        ArtworkHeroCard(
                            artworkBitmap = artworkBitmap,
                            isSeeking = isSeeking,
                            isFavorite = isFavorite,
                            controlsEnabled = controlsEnabled,
                            onToggleFavoriteClick = onToggleFavoriteClick,
                            hasEntered = hasEntered,
                            modifier = Modifier
                                .fillMaxHeight(0.88f)
                                .aspectRatio(1.0f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TopStatusPillGroup(
                            isSpeakerUsed = isSpeakerUsed,
                            isAudioRoutingAvailable = isAudioRoutingAvailable,
                            isRecording = isRecording,
                            recordDurationMs = recordDurationMs,
                            onToggleSpeakerClick = onToggleSpeakerClick,
                            onRecordClick = onRecordClick,
                            onOpenSettingsClick = { isSettingsVisible = true }
                        )

                        StationTitleBlock(
                            currentFrequency = currentFrequency,
                            stationName = stationName,
                            radioText = radioText,
                            isPlaying = isPlaying,
                            titleFont = currentTitleFont,
                            fontScaleX = fontScaleX,
                            fontScaleY = fontScaleY,
                            reduceMotion = reduceMotion
                        )

                        DottedTunerScrubber(
                            currentFrequency = currentFrequency,
                            isPlaying = isPlaying,
                            enabled = controlsEnabled,
                            onFrequencySeek = { freq -> safeChangeStation { onFrequencySeek(freq) } },
                            onScrubbingStateChange = { isScrubbing = it },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        PlaybackControlsRow(
                            isPlaying = isPlaying,
                            isSeeking = isSeeking,
                            onPrevStationClick = { safeChangeStation(onPrevStationClick) },
                            onPlayPauseClick = onPlayPauseClick,
                            onNextStationClick = { safeChangeStation(onNextStationClick) }
                        )

                        BottomControlsBar(
                            isRecording = isRecording,
                            controlsEnabled = controlsEnabled,
                            onTuneDownClick = { safeChangeStation(onTuneDownClick) },
                            onTuneUpClick = { safeChangeStation(onTuneUpClick) },
                            onRecordClick = onRecordClick,
                            onStartRecording = onStartRecording,
                            onStopRecording = onStopRecording,
                            onOpenFavoritesClick = {
                                onReloadFavorites()
                                showBottomSheet = true
                            }
                        )
                    }
                }
            } else {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    TopStatusPillGroup(
                        isSpeakerUsed = isSpeakerUsed,
                        isAudioRoutingAvailable = isAudioRoutingAvailable,
                        isRecording = isRecording,
                        recordDurationMs = recordDurationMs,
                        onToggleSpeakerClick = onToggleSpeakerClick,
                        onRecordClick = onRecordClick,
                        onOpenSettingsClick = { isSettingsVisible = true },
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .weight(1.0f),
                        contentAlignment = Alignment.Center
                    ) {
                        ArtworkHeroCard(
                            artworkBitmap = artworkBitmap,
                            isSeeking = isSeeking,
                            isFavorite = isFavorite,
                            controlsEnabled = controlsEnabled,
                            onToggleFavoriteClick = onToggleFavoriteClick,
                            hasEntered = hasEntered,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(1.0f)
                        )
                    }

                    StationTitleBlock(
                        currentFrequency = currentFrequency,
                        stationName = stationName,
                        radioText = radioText,
                        isPlaying = isPlaying,
                        titleFont = currentTitleFont,
                        fontScaleX = fontScaleX,
                        fontScaleY = fontScaleY,
                        reduceMotion = reduceMotion,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )

                    DottedTunerScrubber(
                        currentFrequency = currentFrequency,
                        isPlaying = isPlaying,
                        enabled = controlsEnabled,
                        onFrequencySeek = { freq -> safeChangeStation { onFrequencySeek(freq) } },
                        onScrubbingStateChange = { isScrubbing = it },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    PlaybackControlsRow(
                        isPlaying = isPlaying,
                        isSeeking = isSeeking,
                        onPrevStationClick = { safeChangeStation(onPrevStationClick) },
                        onPlayPauseClick = onPlayPauseClick,
                        onNextStationClick = { safeChangeStation(onNextStationClick) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )

                    BottomControlsBar(
                        isRecording = isRecording,
                        controlsEnabled = controlsEnabled,
                        onTuneDownClick = { safeChangeStation(onTuneDownClick) },
                        onTuneUpClick = { safeChangeStation(onTuneUpClick) },
                        onRecordClick = onRecordClick,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onOpenFavoritesClick = {
                            onReloadFavorites()
                            showBottomSheet = true
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Favorites Modal Bottom Sheet with Reordering, Renaming, Adding, Deleting
        if (showBottomSheet) {
            FmFavoritesSheet(
                favoritesList = favoritesList,
                currentFrequency = currentFrequency,
                sheetState = sheetState,
                onStationSelect = { freq ->
                    safeChangeStation { onStationSelect(freq) }
                },
                onDismissRequest = { showBottomSheet = false },
                onReloadFavorites = onReloadFavorites
            )
        }

        // Fullscreen Saved Recordings with In-App Player & Seekbar
        AnimatedVisibility(
            visible = isRecordingSheetVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FmRecordingsScreen(
                recordingsList = recordingsList,
                onDeleteRecording = onDeleteRecording,
                onBackClick = onDismissRecordingSheet,
                onPauseRadio = { if (isPlaying) onPlayPauseClick() }
            )
        }

        // Settings Screen with Grouped Preferences & Reduce Motion Toggle
        AnimatedVisibility(
            visible = isSettingsVisible,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut()
        ) {
            FmSettingsScreen(
                onBackClick = {
                    reduceMotion = FmPreferences.isReduceMotionEnabled(context)
                    isSettingsVisible = false
                }
            )
        }

        // Confirmation Dialog when attempting to tune away while recording
        if (pendingChannelChange != null) {
            AlertDialog(
                onDismissRequest = { pendingChannelChange = null },
                title = {
                    Text(
                        text = stringResource(R.string.stop_recording_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.stop_recording_dialog_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = pendingChannelChange
                            pendingChannelChange = null
                            onStopRecording()
                            target?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(R.string.stop_recording_confirm))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { pendingChannelChange = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}

/**
 * Top status pill group containing the Audio Output selector and dynamic live Recording indicator.
 */
@Composable
private fun TopStatusPillGroup(
    isSpeakerUsed: Boolean,
    isAudioRoutingAvailable: Boolean,
    isRecording: Boolean,
    recordDurationMs: Long,
    onToggleSpeakerClick: () -> Unit,
    onRecordClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Audio Routing Pill (Disabled if no headset / bluetooth device is available)
        Surface(
            onClick = onToggleSpeakerClick,
            enabled = isAudioRoutingAvailable,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = if (isAudioRoutingAvailable) 1f else 0.38f
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .alpha(if (isAudioRoutingAvailable) 1f else 0.38f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isSpeakerUsed) Icons.Filled.Speaker else Icons.Filled.Headphones,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isSpeakerUsed) stringResource(R.string.playing_on_speaker) else stringResource(R.string.playing_on_headphone),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 2. Dynamic Live Recording Indicator Pill (Shown when recording)
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Surface(
                onClick = onRecordClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "rec_dot_pulse")
                    val recDotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "rec_dot_alpha"
                    )
                    Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        contentDescription = null,
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(recDotAlpha),
                        tint = MaterialTheme.colorScheme.error
                    )
                    val durationSec = recordDurationMs / 1000L
                    val timeStr = String.format(Locale.US, "%02d:%02d", durationSec / 60, durationSec % 60)
                    Text(
                        text = "REC $timeStr",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // 3. Settings Action Pill
        Surface(
            onClick = onOpenSettingsClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(
                modifier = Modifier.padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Hero artwork card with animated first-showing entrance scale, seeking radar overlay, and bottom-right favorite FAB.
 */
@Composable
private fun ArtworkHeroCard(
    artworkBitmap: Bitmap?,
    isSeeking: Boolean,
    isFavorite: Boolean,
    controlsEnabled: Boolean,
    onToggleFavoriteClick: () -> Unit,
    hasEntered: Boolean,
    modifier: Modifier = Modifier
) {
    val targetCardScale = if (isSeeking) {
        0.84f
    } else if (artworkBitmap != null && hasEntered) {
        1.0f
    } else {
        0.88f
    }
    val artworkEntranceScale by animateFloatAsState(
        targetValue = targetCardScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "artwork_entrance_scale"
    )

    Card(
        modifier = modifier.scale(artworkEntranceScale),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = artworkBitmap,
                animationSpec = tween(durationMillis = 350),
                label = "artwork_crossfade"
            ) { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            // Radar Scanning Overlay when seeking
            val radarAlpha by animateFloatAsState(
                targetValue = if (isSeeking) 1.0f else 0.0f,
                animationSpec = tween(durationMillis = 280),
                label = "radar_alpha"
            )
            if (radarAlpha > 0.01f) {
                val radarTransition = rememberInfiniteTransition(label = "radar_transition")
                val sweepAngle by radarTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "radar_sweep"
                )
                val waveProgress by radarTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "radar_wave"
                )

                val primaryColor = MaterialTheme.colorScheme.primary
                val scrimColor = MaterialTheme.colorScheme.scrim

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrimColor.copy(alpha = 0.45f * radarAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = size.minDimension / 2f * 0.88f

                        // Fading concentric radar pulse circles
                        for (i in 0..2) {
                            val phase = (waveProgress + i * 0.333f) % 1f
                            val r = maxRadius * phase
                            val ringAlpha = (1f - phase).coerceIn(0f, 1f) * 0.55f * radarAlpha
                            drawCircle(
                                color = primaryColor.copy(alpha = ringAlpha),
                                radius = r,
                                center = center,
                                style = Stroke(width = 2.5f)
                            )
                        }

                        // Seamless rotating radar sweep beam via canvas rotation (eliminates 0-degree seam clipping)
                        withTransform({
                            rotate(degrees = sweepAngle, pivot = center)
                        }) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    0.833f to Color.Transparent,
                                    1.0f to primaryColor.copy(alpha = 0.38f * radarAlpha),
                                    center = center
                                ),
                                startAngle = -60f,
                                sweepAngle = 60f,
                                useCenter = true,
                                topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                                size = Size(maxRadius * 2f, maxRadius * 2f)
                            )
                            drawLine(
                                color = primaryColor.copy(alpha = 0.85f * radarAlpha),
                                start = center,
                                end = Offset(center.x + maxRadius, center.y),
                                strokeWidth = 2.5f
                            )
                        }

                        // Central pulsing beacon
                        drawCircle(
                            color = primaryColor.copy(alpha = radarAlpha),
                            radius = 6.dp.toPx(),
                            center = center
                        )
                    }
                }
            }

            // Floating Favorite FAB in bottom-right corner of artwork
            val favScale by animateFloatAsState(
                targetValue = if (isFavorite) 1.0f else 0.95f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "fav_scale"
            )
            FilledIconButton(
                onClick = onToggleFavoriteClick,
                enabled = controlsEnabled,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .size(58.dp)
                    .scale(favScale),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isFavorite) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    contentColor = if (isFavorite) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.38f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.talk_add_favorite),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Centered Station Title & Subtitle with smooth slide + fade animated transition.
 * Uses hardware variable font variations for seamless weight/width breathing.
 */
@Composable
private fun StationTitleBlock(
    currentFrequency: Float,
    stationName: String,
    radioText: String,
    isPlaying: Boolean,
    titleFont: FontFamily,
    fontScaleX: Float = 1.0f,
    fontScaleY: Float = 1.0f,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val formattedFreq = String.format(Locale.US, "%.1fFM", currentFrequency)
    val appName = stringResource(R.string.app_name)
    val hasRds = stationName.isNotBlank() && stationName != appName
    val displayTitle = if (hasRds) stationName else formattedFreq
    val displaySubtitle = if (hasRds) {
        if (radioText.isNotBlank()) radioText else formattedFreq
    } else {
        if (radioText.isNotBlank()) radioText else formattedFreq
    }

    // Deduplicate subtitle if identical to station title
    val showSubtitle = displaySubtitle.isNotBlank() && !displaySubtitle.equals(displayTitle, ignoreCase = true)

    var lastFrequency by remember { mutableFloatStateOf(currentFrequency) }
    val direction = when {
        currentFrequency > lastFrequency + 0.01f -> 1
        currentFrequency < lastFrequency - 0.01f -> -1
        else -> 0
    }
    SideEffect {
        lastFrequency = currentFrequency
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title with contextual directional slide + fade transition
        AnimatedContent(
            targetState = displayTitle,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                } else if (direction < 0) {
                    // Previous scrubbing / lower freq: moves to left
                    (slideInHorizontally(animationSpec = tween(300)) { width -> width / 2 } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width / 2 } + fadeOut(tween(300)))
                } else if (direction > 0) {
                    // Next scrubbing / higher freq: moves to right
                    (slideInHorizontally(animationSpec = tween(300)) { width -> -width / 2 } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width / 2 } + fadeOut(tween(300)))
                } else {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                }
            },
            label = "title_directional_slide"
        ) { targetTitle ->
            Text(
                text = targetTitle,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = titleFont,
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = fontScaleX
                        scaleY = fontScaleY
                    }
                    .then(if (isPlaying) Modifier.basicMarquee() else Modifier)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Reserved Subtitle Box to prevent title jumping when subtitle appears / disappears
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            this@Column.AnimatedVisibility(
                visible = showSubtitle,
                enter = if (reduceMotion) fadeIn(tween(200)) else fadeIn(tween(250)) + slideInVertically(animationSpec = tween(250)) { it / 2 },
                exit = if (reduceMotion) fadeOut(tween(200)) else fadeOut(tween(200)) + slideOutVertically(animationSpec = tween(200)) { -it / 2 }
            ) {
                // Subtitle with smooth directional slide + fade transition when frequency or RDS updates
                AnimatedContent(
                    targetState = displaySubtitle,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                        } else if (direction < 0) {
                            // Previous scrubbing / lower freq: moves to left
                            (slideInHorizontally(animationSpec = tween(300)) { width -> width / 2 } + fadeIn(tween(300)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width / 2 } + fadeOut(tween(300)))
                        } else if (direction > 0) {
                            // Next scrubbing / higher freq: moves to right
                            (slideInHorizontally(animationSpec = tween(300)) { width -> -width / 2 } + fadeIn(tween(300)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width / 2 } + fadeOut(tween(300)))
                        } else {
                            (slideInVertically(animationSpec = tween(250)) { height -> height / 2 } + fadeIn(tween(250)))
                                .togetherWith(slideOutVertically(animationSpec = tween(250)) { height -> -height / 2 } + fadeOut(tween(250)))
                        }
                    },
                    label = "subtitle_directional_slide"
                ) { targetSubtitle ->
                    Text(
                        text = targetSubtitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Expressive Playback Controls Row (Disabled on pause, morphing center squircle to stadium pill).
 */
@Composable
private fun PlaybackControlsRow(
    isPlaying: Boolean,
    isSeeking: Boolean,
    onPrevStationClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextStationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seekPulseScale by if (isSeeking) {
        val infiniteTransition = rememberInfiniteTransition(label = "seek_btn_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "seek_btn_scale"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    val centerBtnWidth by animateDpAsState(
        targetValue = if (isPlaying) 156.dp else 84.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "center_btn_width"
    )
    val centerBtnCorner by animateDpAsState(
        targetValue = if (isPlaying) 40.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "center_btn_corner"
    )
    val sideBtnWidth by animateDpAsState(
        targetValue = if (isPlaying) 74.dp else 84.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "side_btn_width"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Station (Disabled when paused)
        FilledIconButton(
            onClick = onPrevStationClick,
            enabled = isPlaying && !isSeeking,
            modifier = Modifier
                .width(sideBtnWidth)
                .height(80.dp)
                .scale(if (isSeeking) seekPulseScale else 1.0f),
            shape = RoundedCornerShape(24.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.talk_prev_station),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Central Primary Play / Pause (Morphs between squircle and stadium)
        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .width(centerBtnWidth)
                .height(80.dp),
            shape = RoundedCornerShape(centerBtnCorner),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) {
                    stringResource(R.string.accessibility_pause)
                } else {
                    stringResource(R.string.accessibility_play)
                },
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Next Station (Disabled when paused)
        FilledIconButton(
            onClick = onNextStationClick,
            enabled = isPlaying && !isSeeking,
            modifier = Modifier
                .width(sideBtnWidth)
                .height(80.dp)
                .scale(if (isSeeking) seekPulseScale else 1.0f),
            shape = RoundedCornerShape(24.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.talk_next_station),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Bottom Auxiliary Row: Granular Stepping & Mini Button Groups.
 */
@Composable
private fun BottomControlsBar(
    isRecording: Boolean,
    controlsEnabled: Boolean,
    onTuneDownClick: () -> Unit,
    onTuneUpClick: () -> Unit,
    onRecordClick: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onOpenFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recordBtnEnabled = isRecording || controlsEnabled

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Granular -0.1 Step Button
        Surface(
            onClick = onTuneDownClick,
            enabled = controlsEnabled,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = if (controlsEnabled) 1f else 0.38f
            ),
            modifier = Modifier.height(38.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .alpha(if (controlsEnabled) 1f else 0.38f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "0.1",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Center Mini Button Group (Recording List, Record/Stop Button, Favorites List)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Mini Recording History List Button
            Surface(
                onClick = onRecordClick,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(width = 46.dp, height = 38.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = stringResource(R.string.fm_over_record_list),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Mini Record / Stop Button (Alert red when recording, disabled when paused/seeking)
            Surface(
                onClick = {
                    if (isRecording) {
                        onStopRecording()
                    } else {
                        onStartRecording()
                    }
                },
                enabled = recordBtnEnabled,
                shape = RoundedCornerShape(14.dp),
                color = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(
                        alpha = if (controlsEnabled) 1f else 0.38f
                    )
                },
                modifier = Modifier.size(width = 52.dp, height = 38.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (recordBtnEnabled) 1f else 0.38f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                        contentDescription = if (isRecording) stringResource(R.string.stop_record) else stringResource(R.string.record_title),
                        tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(if (isRecording) 18.dp else 12.dp)
                    )
                }
            }

            // 3. Mini Favorites List Button (Disabled when paused/seeking)
            Surface(
                onClick = onOpenFavoritesClick,
                enabled = controlsEnabled,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                    alpha = if (controlsEnabled) 1f else 0.38f
                ),
                modifier = Modifier.size(width = 46.dp, height = 38.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (controlsEnabled) 1f else 0.38f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = stringResource(R.string.favorites),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Granular +0.1 Step Button
        Surface(
            onClick = onTuneUpClick,
            enabled = controlsEnabled,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = if (controlsEnabled) 1f else 0.38f
            ),
            modifier = Modifier.height(38.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .alpha(if (controlsEnabled) 1f else 0.38f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "0.1",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
