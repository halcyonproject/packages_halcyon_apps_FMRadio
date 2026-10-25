/*
 * Copyright (C) 2026 RevampedFMRadio Project
 * Copyright (C) 2026 Halcyon Project
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

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.fmradio.FmPreferences
import com.hlcyn.tuner.R
import kotlinx.coroutines.CancellationException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FmSettingsScreen(
    onBackClick: () -> Unit,
    onOpenRecordings: () -> Unit = {},
    recordingsCount: Int = 0,
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

    // AOSP Folder Picker Contract
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                // Ignore permission persistence errors
            }
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val relativePath = if (docId.contains(":")) {
                docId.substringAfter(":")
            } else {
                docId
            }
            val cleanPath = if (relativePath.isNotBlank()) relativePath else "FM Recording"
            recordingLocation = cleanPath
            FmPreferences.setRecordingLocation(context, cleanPath)
        }
    }

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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(android.R.string.cancel),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category 1: Playback & System
            SettingsCategoryHeader(title = "Playback & System")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                SettingsSegmentedSwitchRow(
                    icon = Icons.Filled.PlayCircle,
                    title = "Autoplay on launch",
                    summary = "Automatically start radio playback when opening the app",
                    checked = autoplayEnabled,
                    onCheckedChange = {
                        autoplayEnabled = it
                        FmPreferences.setAutoplayEnabled(context, it)
                    },
                    shapes = ListItemDefaults.segmentedShapes(0, 2)
                )

                SettingsSegmentedSwitchRow(
                    icon = Icons.Filled.MotionPhotosOff,
                    title = "Reduce motion",
                    summary = "Minimize motion effects, breathing font pulses, and animations",
                    checked = reduceMotion,
                    onCheckedChange = {
                        reduceMotion = it
                        FmPreferences.setReduceMotionEnabled(context, it)
                    },
                    shapes = ListItemDefaults.segmentedShapes(1, 2)
                )
            }

            // Category 2: Recording Preferences
            SettingsCategoryHeader(title = "Recording Preferences")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                SettingsSegmentedClickableRow(
                    icon = Icons.Filled.AudioFile,
                    title = "Audio format",
                    summary = if (recordingFormat == FmPreferences.FORMAT_3GPP) "3GPP (.3gpp)" else "AAC / M4A (.m4a)",
                    onClick = { showFormatDialog = true },
                    shapes = ListItemDefaults.segmentedShapes(0, 4)
                )

                val bitrateLabel = when (recordingBitrate) {
                    FmPreferences.BITRATE_128K -> "128 kbps (Standard)"
                    FmPreferences.BITRATE_256K -> "256 kbps (Ultra)"
                    FmPreferences.BITRATE_320K -> "320 kbps (Maximum)"
                    else -> "192 kbps (High Quality)"
                }
                SettingsSegmentedClickableRow(
                    icon = Icons.Filled.GraphicEq,
                    title = "Audio bitrate",
                    summary = bitrateLabel,
                    onClick = { showBitrateDialog = true },
                    shapes = ListItemDefaults.segmentedShapes(1, 4)
                )

                SettingsSegmentedClickableRow(
                    icon = Icons.Filled.Folder,
                    title = "Storage folder",
                    summary = recordingLocation,
                    onClick = { folderPickerLauncher.launch(null) },
                    shapes = ListItemDefaults.segmentedShapes(2, 4)
                )

                SettingsSegmentedClickableRow(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = stringResource(R.string.saved_recordings),
                    summary = if (recordingsCount > 0) String.format(Locale.getDefault(), "%d recordings", recordingsCount) else "View all recorded radio broadcasts",
                    onClick = onOpenRecordings,
                    shapes = ListItemDefaults.segmentedShapes(3, 4),
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // Category 3: About & Credits
            SettingsCategoryHeader(title = "About & Credits")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                SettingsSegmentedClickableRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    summary = "3.2.0 (com.hlcyn.tuner)",
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(0, 3)
                )

                SettingsSegmentedClickableRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = "Credits",
                    summary = "Halcyon Project, iusmac, LineageOS",
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(1, 3)
                )

                SettingsSegmentedClickableRow(
                    icon = Icons.Outlined.Code,
                    title = "License",
                    summary = "Apache License 2.0",
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(2, 3)
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
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (recordingFormat == fmt),
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
                    bitrates.forEach { (br, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    recordingBitrate = br
                                    FmPreferences.setRecordingBitrate(context, br)
                                    showBitrateDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (recordingBitrate == br),
                                onClick = {
                                    recordingBitrate = br
                                    FmPreferences.setRecordingBitrate(context, br)
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
}

@Composable
private fun SettingsCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        ),
        modifier = modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsSegmentedSwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = if (summary.isNotBlank()) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsSegmentedClickableRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    shapes: ListItemShapes,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = if (summary.isNotBlank()) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = trailingContent
    )
}
