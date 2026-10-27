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
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.fmradio.FmPreferences
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.SettingsIcon
import com.hlcyn.tuner.R
import kotlinx.coroutines.CancellationException
import java.util.Locale

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
    var showAboutDialog by remember { mutableStateOf(false) }

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

    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val navControllerWrapper = remember(backDispatcherOwner) {
        object : NavControllerWrapper {
            override fun navigate(route: String, popUpCurrent: Boolean) {}
            override fun navigateBack() {
                onBackClick()
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = animatedTranslationX
            }
    ) {
        SettingsTheme {
            CompositionLocalProvider(LocalNavController provides navControllerWrapper) {
                SettingsScaffold(
                    title = stringResource(R.string.settings)
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(SettingsDimension.itemPaddingAround)
                    ) {
                        Category(title = stringResource(R.string.settings_cat_general)) {
                            SwitchPreference(
                                object : SwitchPreferenceModel {
                                    override val title = stringResource(R.string.pref_autoplay_title)
                                    override val summary = { context.getString(R.string.pref_autoplay_summary) }
                                    override val checked = { autoplayEnabled }
                                    override val onCheckedChange = { checked: Boolean ->
                                        autoplayEnabled = checked
                                        FmPreferences.setAutoplayEnabled(context, checked)
                                    }
                                    override val icon = @Composable {
                                        SettingsIcon(imageVector = Icons.Filled.PlayCircle)
                                    }
                                }
                            )

                            SwitchPreference(
                                object : SwitchPreferenceModel {
                                    override val title = stringResource(R.string.pref_reduce_motion_title)
                                    override val summary = { context.getString(R.string.pref_reduce_motion_summary) }
                                    override val checked = { reduceMotion }
                                    override val onCheckedChange = { checked: Boolean ->
                                        reduceMotion = checked
                                        FmPreferences.setReduceMotionEnabled(context, checked)
                                    }
                                    override val icon = @Composable {
                                        SettingsIcon(imageVector = Icons.Filled.MotionPhotosOff)
                                    }
                                }
                            )
                        }

                        Category(title = stringResource(R.string.settings_cat_audio)) {
                            Preference(
                                object : PreferenceModel {
                                    override val title = stringResource(R.string.pref_format_title)
                                    override val summary = {
                                        if (recordingFormat == FmPreferences.FORMAT_MP3) {
                                            context.getString(R.string.format_mp3_label)
                                        } else {
                                            context.getString(R.string.format_m4a_label)
                                        }
                                    }
                                    override val onClick = { showFormatDialog = true }
                                    override val icon = @Composable {
                                        SettingsIcon(imageVector = Icons.Filled.AudioFile)
                                    }
                                }
                            )

                            val bitrateSummary = when (recordingBitrate) {
                                FmPreferences.BITRATE_128K -> context.getString(R.string.bitrate_128k_label)
                                FmPreferences.BITRATE_256K -> context.getString(R.string.bitrate_256k_label)
                                FmPreferences.BITRATE_320K -> context.getString(R.string.bitrate_320k_label)
                                else -> context.getString(R.string.bitrate_192k_label)
                            }
                            Preference(
                                object : PreferenceModel {
                                    override val title = stringResource(R.string.pref_bitrate_title)
                                    override val summary = { bitrateSummary }
                                    override val onClick = { showBitrateDialog = true }
                                    override val icon = @Composable {
                                        SettingsIcon(imageVector = Icons.Filled.GraphicEq)
                                    }
                                }
                            )

                            Preference(
                                object : PreferenceModel {
                                    override val title = stringResource(R.string.pref_location_title)
                                    override val summary = { recordingLocation }
                                    override val onClick = { folderPickerLauncher.launch(null) }
                                    override val icon = @Composable {
                                        SettingsIcon(imageVector = Icons.Filled.Folder)
                                    }
                                }
                            )
                        }

                        Category(title = stringResource(R.string.settings_cat_about)) {
                            Preference(
                                object : PreferenceModel {
                                    override val title = stringResource(R.string.dialog_about_title)
                                    override val summary = { context.getString(R.string.pref_version_summary) }
                                    override val onClick = { showAboutDialog = true }
                                    override val icon = @Composable {
                                        SettingsIcon(imageVector = Icons.Outlined.Info)
                                    }
                                }
                            )

                            Preference(
                                object : PreferenceModel {
                                    override val title = stringResource(R.string.pref_license_title)
                                    override val summary = { context.getString(R.string.pref_license_summary) }
                                    override val onClick = {
                                        try {
                                            val browserIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://github.com/halcyonproject/packages_halcyon_apps_FMRadio/blob/17.0/LICENSE")
                                            )
                                            context.startActivity(browserIntent)
                                        } catch (e: Exception) {
                                            // Ignore activity launch failures
                                        }
                                    }
                                    override val icon = @Composable {
                                        SettingsIcon(imageVector = Icons.Outlined.Code)
                                    }
                                }
                            )
                        }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    }

    if (showFormatDialog) {
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_select_format),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    val formats = listOf(
                        Triple(FmPreferences.FORMAT_M4A, stringResource(R.string.format_m4a_label), stringResource(R.string.format_m4a_desc)),
                        Triple(FmPreferences.FORMAT_MP3, stringResource(R.string.format_mp3_label), stringResource(R.string.format_mp3_desc))
                    )

                    formats.forEach { (fmt, label, desc) ->
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
                            Column {
                                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormatDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (showBitrateDialog) {
        AlertDialog(
            onDismissRequest = { showBitrateDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_select_bitrate),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    val bitrates = listOf(
                        Triple(FmPreferences.BITRATE_128K, stringResource(R.string.bitrate_128k_label), stringResource(R.string.bitrate_128k_desc)),
                        Triple(FmPreferences.BITRATE_192K, stringResource(R.string.bitrate_192k_label), stringResource(R.string.bitrate_192k_desc)),
                        Triple(FmPreferences.BITRATE_256K, stringResource(R.string.bitrate_256k_label), stringResource(R.string.bitrate_256k_desc)),
                        Triple(FmPreferences.BITRATE_320K, stringResource(R.string.bitrate_320k_label), stringResource(R.string.bitrate_320k_desc))
                    )

                    bitrates.forEach { (rate, label, desc) ->
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
                                selected = (recordingBitrate == rate),
                                onClick = {
                                    recordingBitrate = rate
                                    FmPreferences.setRecordingBitrate(context, rate)
                                    showBitrateDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBitrateDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_about_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.about_app_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.about_crafted_by),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
