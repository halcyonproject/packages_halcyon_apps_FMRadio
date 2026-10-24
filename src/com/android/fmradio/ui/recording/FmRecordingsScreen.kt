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

package com.android.fmradio.ui.recording

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.fmradio.R
import java.util.Date
import java.util.Locale

data class RecordingItem(
    val id: Long,
    val title: String,
    val filePath: String,
    val uri: Uri,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateModified: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun FmRecordingsScreen(
    recordingsList: List<RecordingItem>,
    onDeleteRecording: (RecordingItem) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // Handle system predictive and hardware back gestures
    BackHandler(onBack = onBackClick)

    // Variable Typography for Title & Metadata
    val titleFont = remember {
        FontFamily(
            Font(
                familyName = DeviceFontFamilyName("google-sans-flex"),
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("wght", 750f),
                    FontVariation.Setting("wdth", 100f),
                    FontVariation.Setting("ROND", 100f)
                )
            )
        )
    }

    val bodyFont = remember {
        FontFamily(
            Font(
                familyName = DeviceFontFamilyName("google-sans-flex"),
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("wght", 500f),
                    FontVariation.Setting("wdth", 100f),
                    FontVariation.Setting("ROND", 0f)
                )
            )
        )
    }

    // Media Preview Player
    val mediaPlayer = remember { MediaPlayer() }
    var currentlyPlayingUri by remember { mutableStateOf<Uri?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    // Deletion confirmation dialog target
    var itemToDelete by remember { mutableStateOf<RecordingItem?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    fun togglePlayback(item: RecordingItem) {
        if (currentlyPlayingUri == item.uri && isAudioPlaying) {
            try {
                mediaPlayer.pause()
                isAudioPlaying = false
            } catch (e: Exception) {
                // Ignore
            }
        } else {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, item.uri)
                mediaPlayer.prepare()
                mediaPlayer.start()
                currentlyPlayingUri = item.uri
                isAudioPlaying = true
                mediaPlayer.setOnCompletionListener {
                    isAudioPlaying = false
                    currentlyPlayingUri = null
                }
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(item.uri, "audio/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
        }
    }

    fun shareRecording(item: RecordingItem) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, item.title))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.fm_over_record_list),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = titleFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (recordingsList.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${recordingsList.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = bodyFont),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            if (recordingsList.isEmpty()) {
                // Empty State View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.toast_cannot_search),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = titleFont),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Audio recorded from live radio broadcasts will appear here.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Full-Screen Recordings List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recordingsList, key = { it.filePath }) { item ->
                        val isThisPlaying = (currentlyPlayingUri == item.uri && isAudioPlaying)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isThisPlaying) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play / Pause Button
                                FilledTonalIconButton(
                                    onClick = { togglePlayback(item) },
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (isThisPlaying) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        },
                                        contentColor = if (isThisPlaying) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Information details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = bodyFont,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val formattedDate = DateFormat.format("MMM dd, yyyy • hh:mm a", Date(item.dateModified)).toString()
                                    val formattedFileSize = Formatter.formatFileSize(context, item.sizeBytes)
                                    val durationStr = if (item.durationMs > 0) {
                                        val m = (item.durationMs / 1000) / 60
                                        val s = (item.durationMs / 1000) % 60
                                        String.format(Locale.US, "%02d:%02d • ", m, s)
                                    } else {
                                        ""
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "$durationStr$formattedFileSize",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )

                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = bodyFont),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }

                                // Share Button
                                IconButton(onClick = { shareRecording(item) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Delete Button
                                IconButton(onClick = { itemToDelete = item }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (itemToDelete != null) {
            val item = itemToDelete!!
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = {
                    Text(
                        text = "Delete recording?",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = titleFont),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete \"${item.title}\"? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = bodyFont)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (currentlyPlayingUri == item.uri) {
                                try {
                                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                    isAudioPlaying = false
                                    currentlyPlayingUri = null
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                            onDeleteRecording(item)
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { itemToDelete = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}
