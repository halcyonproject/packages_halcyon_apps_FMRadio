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

package com.android.fmradio.ui.settings

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.fmradio.FmPreferences
import com.android.fmradio.R
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var autoplayEnabled by remember { mutableStateOf(FmPreferences.isAutoplayEnabled(context)) }
    var reduceMotion by remember { mutableStateOf(FmPreferences.isReduceMotionEnabled(context)) }
    var recordingFormat by remember { mutableStateOf(FmPreferences.getRecordingFormat(context)) }
    var recordingBitrate by remember { mutableIntStateOf(FmPreferences.getRecordingBitrate(context)) }
    var recordingLocation by remember { mutableStateOf(FmPreferences.getRecordingLocation(context)) }

    var showFormatDialog by remember { mutableStateOf(false) }
    var showBitrateDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    var predictiveProgress by remember { mutableFloatStateOf(0f) }
    val animatedScale by animateFloatAsState(targetValue = 1f - predictiveProgress * 0.08f, label = "back_scale")
    val animatedTranslationX by animateFloatAsState(targetValue = predictiveProgress * 120f, label = "back_trans_x")

    PredictiveBackHandler(enabled = true) { progress ->
        try {
            progress.collect { backEvent ->
                predictiveProgress = backEvent.progress
            }
            onBackClick()
        } catch (e: CancellationException) {
            predictiveProgress = 0f
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = animatedTranslationX
            },
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(android.R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Group 1: General & Playback
            SettingsCategoryHeader(title = "Playback & System")
            SettingsCardContainer {
                SettingsSwitchRow(
                    icon = Icons.Filled.PlayCircle,
                    title = "Autoplay on launch",
                    summary = "Automatically start radio playback when opening the app",
                    checked = autoplayEnabled,
                    onCheckedChange = {
                        autoplayEnabled = it
                        FmPreferences.setAutoplayEnabled(context, it)
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SettingsSwitchRow(
                    icon = Icons.Filled.MotionPhotosOff,
                    title = "Reduce motion",
                    summary = "Minimize motion effects, breathing font pulses, and radar animations",
                    checked = reduceMotion,
                    onCheckedChange = {
                        reduceMotion = it
                        FmPreferences.setReduceMotionEnabled(context, it)
                    }
                )
            }

            // Group 2: Recording
            SettingsCategoryHeader(title = "Recording Preferences")
            SettingsCardContainer {
                SettingsClickableRow(
                    icon = Icons.Filled.AudioFile,
                    title = "Audio format",
                    summary = if (recordingFormat == FmPreferences.FORMAT_3GPP) "3GPP (.3gpp)" else "AAC / M4A (.m4a)",
                    onClick = { showFormatDialog = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                val bitrateLabel = when (recordingBitrate) {
                    FmPreferences.BITRATE_128K -> "128 kbps (Standard)"
                    FmPreferences.BITRATE_256K -> "256 kbps (Ultra)"
                    FmPreferences.BITRATE_320K -> "320 kbps (Maximum)"
                    else -> "192 kbps (High Quality)"
                }
                SettingsClickableRow(
                    icon = Icons.Filled.GraphicEq,
                    title = "Audio bitrate",
                    summary = bitrateLabel,
                    onClick = { showBitrateDialog = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SettingsClickableRow(
                    icon = Icons.Filled.Folder,
                    title = "Storage folder",
                    summary = recordingLocation,
                    onClick = { showLocationDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Format Selector Dialog
    if (showFormatDialog) {
        val formats = listOf(
            FmPreferences.FORMAT_M4A to "AAC / M4A (MPEG-4 Container)",
            FmPreferences.FORMAT_3GPP to "3GPP (3GPP Container)"
        )
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text(text = "Select audio format", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    formats.forEach { (fmt, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    recordingFormat = fmt
                                    FmPreferences.setRecordingFormat(context, fmt)
                                    showFormatDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = recordingFormat == fmt,
                                onClick = {
                                    recordingFormat = fmt
                                    FmPreferences.setRecordingFormat(context, fmt)
                                    showFormatDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormatDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Bitrate Selector Dialog
    if (showBitrateDialog) {
        val bitrates = listOf(
            FmPreferences.BITRATE_128K to "128 kbps (Standard)",
            FmPreferences.BITRATE_192K to "192 kbps (High Quality)",
            FmPreferences.BITRATE_256K to "256 kbps (Ultra)",
            FmPreferences.BITRATE_320K to "320 kbps (Maximum)"
        )
        AlertDialog(
            onDismissRequest = { showBitrateDialog = false },
            title = { Text(text = "Select audio bitrate", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    bitrates.forEach { (rate, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    recordingBitrate = rate
                                    FmPreferences.setRecordingBitrate(context, rate)
                                    showBitrateDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = recordingBitrate == rate,
                                onClick = {
                                    recordingBitrate = rate
                                    FmPreferences.setRecordingBitrate(context, rate)
                                    showBitrateDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBitrateDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Storage Location Dialog
    if (showLocationDialog) {
        var tempLocation by remember { mutableStateOf(recordingLocation) }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text(text = "Storage folder name", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Folder name inside device Recordings / Music storage:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempLocation,
                        onValueChange = { tempLocation = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = tempLocation.trim()
                        if (trimmed.isNotEmpty()) {
                            recordingLocation = trimmed
                            FmPreferences.setRecordingLocation(context, trimmed)
                        }
                        showLocationDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        ),
        modifier = modifier.padding(start = 12.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
