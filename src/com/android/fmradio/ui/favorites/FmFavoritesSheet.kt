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

package com.android.fmradio.ui.favorites

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.fmradio.FmPreferences
import com.android.fmradio.FmStation
import com.android.fmradio.FmUtils
import com.android.fmradio.R
import com.android.fmradio.ui.FavoriteStationItem
import kotlinx.coroutines.CancellationException
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmFavoritesSheet(
    favoritesList: List<FavoriteStationItem>,
    currentFrequency: Float,
    sheetState: SheetState,
    onStationSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    onReloadFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentFreqInt = (currentFrequency * 10f).roundToInt()

    // Dialog States
    var stationToRename by remember { mutableStateOf<FavoriteStationItem?>(null) }
    var stationToDelete by remember { mutableStateOf<FavoriteStationItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Predictive Back Handling
    var predictiveProgress by remember { mutableFloatStateOf(0f) }
    val animatedScale by animateFloatAsState(targetValue = 1f - predictiveProgress * 0.06f, label = "fav_scale")

    PredictiveBackHandler(enabled = true) { progress ->
        try {
            progress.collect { backEvent ->
                predictiveProgress = backEvent.progress
            }
            onDismissRequest()
        } catch (e: CancellationException) {
            predictiveProgress = 0f
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            // Header Row: Title, Badge, and Add Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.favorites),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    if (favoritesList.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${favoritesList.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Add Station", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (favoritesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_favorite_station),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(favoritesList, key = { _, item -> item.id }) { index, item ->
                        val isCurrent = item.frequency == currentFreqInt
                        val stationDisplayName = if (item.name.isNotBlank()) item.name else String.format(Locale.US, "%.1fFM", item.frequency / 10f)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onStationSelect(item.frequency) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stationDisplayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f MHz", item.frequency / 10f),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Actions: Move Up, Move Down, Rename, Delete
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Move Up Button
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = favoritesList.map { it.frequency }.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index - 1]
                                                mutable[index - 1] = temp
                                                FmPreferences.setFavoritesOrder(context, mutable)
                                                onReloadFavorites()
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
                                        )
                                    }

                                    // Move Down Button
                                    IconButton(
                                        onClick = {
                                            if (index < favoritesList.size - 1) {
                                                val mutable = favoritesList.map { it.frequency }.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index + 1]
                                                mutable[index + 1] = temp
                                                FmPreferences.setFavoritesOrder(context, mutable)
                                                onReloadFavorites()
                                            }
                                        },
                                        enabled = index < favoritesList.size - 1,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (index < favoritesList.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
                                        )
                                    }

                                    // Rename Button
                                    IconButton(
                                        onClick = { stationToRename = item },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Rename",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { stationToDelete = item },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (stationToRename != null) {
        val item = stationToRename!!
        var newName by remember { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { stationToRename = null },
            title = { Text(text = "Rename station", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Frequency: ${String.format(Locale.US, "%.1f MHz", item.frequency / 10f)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Station name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newName.trim()
                        FmStation.updateStationToDb(context, item.frequency, trimmed)
                        onReloadFavorites()
                        stationToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { stationToRename = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (stationToDelete != null) {
        val item = stationToDelete!!
        AlertDialog(
            onDismissRequest = { stationToDelete = null },
            title = { Text(text = "Remove from favorites?", fontWeight = FontWeight.Bold) },
            text = {
                Text(text = "Remove \"${item.name}\" (${String.format(Locale.US, "%.1f MHz", item.frequency / 10f)}) from your favorite stations?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        FmStation.removeFromFavorite(context, item.frequency)
                        val order = FmPreferences.getFavoritesOrder(context).toMutableList()
                        order.remove(item.frequency)
                        FmPreferences.setFavoritesOrder(context, order)
                        onReloadFavorites()
                        stationToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { stationToDelete = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Add Custom Station Dialog
    if (showAddDialog) {
        var freqInput by remember { mutableStateOf(String.format(Locale.US, "%.1f", currentFrequency)) }
        var nameInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Add favorite station", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = freqInput,
                        onValueChange = {
                            freqInput = it
                            isError = false
                        },
                        label = { Text("Frequency (87.5 - 108.0 MHz)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = isError,
                        supportingText = if (isError) {
                            { Text("Enter a frequency between 87.5 and 108.0") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Station name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedFreq = freqInput.trim().toFloatOrNull()
                        if (parsedFreq == null || parsedFreq < 87.5f || parsedFreq > 108.0f) {
                            isError = true
                        } else {
                            val freqInt = (parsedFreq * 10f).roundToInt()
                            FmStation.addToFavorite(context, freqInt)
                            val trimmedName = nameInput.trim()
                            if (trimmedName.isNotEmpty()) {
                                FmStation.updateStationToDb(context, freqInt, trimmedName)
                            }
                            val order = FmPreferences.getFavoritesOrder(context).toMutableList()
                            if (!order.contains(freqInt)) {
                                order.add(freqInt)
                                FmPreferences.setFavoritesOrder(context, order)
                            }
                            onReloadFavorites()
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
