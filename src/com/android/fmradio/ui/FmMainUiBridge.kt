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
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.android.fmradio.FmPreferences
import com.android.fmradio.FmRecorder
import com.android.fmradio.FmStation
import com.android.fmradio.FmUtils
import com.android.fmradio.ui.recording.RecordingItem
import java.io.File

/**
 * Functional interface for frequency seek events from UI.
 */
fun interface FrequencySeekListener {
    fun onFrequencySeek(freq: Float)
}

/**
 * Functional interface for station selection events from UI.
 */
fun interface StationSelectListener {
    fun onStationSelect(stationFreq: Int)
}

/**
 * Functional interface for saving a recording from UI.
 */
fun interface RecordingSaveListener {
    fun onSaveRecording(name: String)
}

/**
 * Functional interface for deleting a recording from UI.
 */
fun interface RecordingDeleteListener {
    fun onDeleteRecording(item: RecordingItem)
}

/**
 * State holder bridging FmMainActivity and the Material 3 Expressive Compose UI.
 */
class FmUiStateHolder {
    val currentFrequency = mutableFloatStateOf(87.5f)
    val stationName = mutableStateOf("")
    val radioText = mutableStateOf("")
    val isPlaying = mutableStateOf(false)
    val isFavorite = mutableStateOf(false)
    val isSpeakerUsed = mutableStateOf(false)
    val isAudioRoutingAvailable = mutableStateOf(false)
    val isRecording = mutableStateOf(false)
    val isSeeking = mutableStateOf(false)
    val isRecordingSheetVisible = mutableStateOf(false)
    val recordingState = mutableIntStateOf(FmRecorder.STATE_IDLE)
    val recordDurationMs = mutableLongStateOf(0L)
    val recordSizeBytes = mutableLongStateOf(0L)
    val recordingName = mutableStateOf("")
    val favoritesList = mutableStateOf<List<FavoriteStationItem>>(emptyList())
    val recordingsList = mutableStateOf<List<RecordingItem>>(emptyList())

    var onPlayPause: () -> Unit = {}
    var onPrevStation: () -> Unit = {}
    var onNextStation: () -> Unit = {}
    var onTuneDown: () -> Unit = {}
    var onTuneUp: () -> Unit = {}
    var onFrequencySeek: (Float) -> Unit = {}
    var onToggleFavorite: () -> Unit = {}
    var onToggleSpeaker: () -> Unit = {}
    var onRecord: () -> Unit = {}
    var onStationSelect: (Int) -> Unit = {}
    var onStartRecording: () -> Unit = {}
    var onStopRecording: () -> Unit = {}
    var onSaveRecording: (String) -> Unit = {}
    var onDiscardRecording: () -> Unit = {}
    var onDeleteRecording: (RecordingItem) -> Unit = {}
    var onReloadFavorites: () -> Unit = {}

    private val mainHandler = Handler(Looper.getMainLooper())

    fun setFrequency(freq: Float) {
        currentFrequency.floatValue = freq
    }

    fun setStationName(name: String?) {
        stationName.value = name ?: ""
    }

    fun setRadioText(text: String?) {
        radioText.value = text ?: ""
    }

    fun setPlaying(playing: Boolean) {
        isPlaying.value = playing
    }

    fun setFavorite(favorite: Boolean) {
        isFavorite.value = favorite
    }

    fun setSpeakerUsed(speaker: Boolean) {
        isSpeakerUsed.value = speaker
    }

    fun setAudioRoutingAvailable(available: Boolean) {
        isAudioRoutingAvailable.value = available
    }

    fun setRecording(recording: Boolean) {
        isRecording.value = recording
    }

    fun setSeeking(seeking: Boolean) {
        isSeeking.value = seeking
    }

    fun setRecordingSheetVisible(visible: Boolean) {
        isRecordingSheetVisible.value = visible
    }

    fun setRecordingState(state: Int) {
        recordingState.intValue = state
    }

    fun setRecordDuration(durationMs: Long) {
        recordDurationMs.longValue = durationMs
    }

    fun setRecordSize(sizeBytes: Long) {
        recordSizeBytes.longValue = sizeBytes
    }

    fun setRecordingName(name: String?) {
        recordingName.value = name ?: ""
    }

    fun reloadRecordings(context: Context) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val list = FmMainUiBridge.loadRecordings(context)
            mainHandler.post {
                recordingsList.value = list
            }
        }
    }

    fun reloadFavorites(context: Context) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val list = FmMainUiBridge.loadFavorites(context)
            mainHandler.post {
                favoritesList.value = list
            }
        }
    }

    fun setOnPlayPauseListener(listener: Runnable) {
        onPlayPause = { listener.run() }
    }

    fun setOnPrevStationListener(listener: Runnable) {
        onPrevStation = { listener.run() }
    }

    fun setOnNextStationListener(listener: Runnable) {
        onNextStation = { listener.run() }
    }

    fun setOnTuneDownListener(listener: Runnable) {
        onTuneDown = { listener.run() }
    }

    fun setOnTuneUpListener(listener: Runnable) {
        onTuneUp = { listener.run() }
    }

    fun setOnToggleFavoriteListener(listener: Runnable) {
        onToggleFavorite = { listener.run() }
    }

    fun setOnToggleSpeakerListener(listener: Runnable) {
        onToggleSpeaker = { listener.run() }
    }

    fun setOnRecordListener(listener: Runnable) {
        onRecord = { listener.run() }
    }

    fun setOnFrequencySeekListener(listener: FrequencySeekListener) {
        onFrequencySeek = { listener.onFrequencySeek(it) }
    }

    fun setOnStationSelectListener(listener: StationSelectListener) {
        onStationSelect = { listener.onStationSelect(it) }
    }

    fun setOnStartRecordingListener(listener: Runnable) {
        onStartRecording = { listener.run() }
    }

    fun setOnStopRecordingListener(listener: Runnable) {
        onStopRecording = { listener.run() }
    }

    fun setOnSaveRecordingListener(listener: RecordingSaveListener) {
        onSaveRecording = { listener.onSaveRecording(it) }
    }

    fun setOnDiscardRecordingListener(listener: Runnable) {
        onDiscardRecording = { listener.run() }
    }

    fun setOnDeleteRecordingListener(listener: RecordingDeleteListener) {
        onDeleteRecording = { listener.onDeleteRecording(it) }
    }

    fun setOnReloadFavoritesListener(listener: Runnable) {
        onReloadFavorites = { listener.run() }
    }
}

/**
 * Attaches the Material 3 Expressive player Composable into an Activity.
 */
object FmMainUiBridge {

    @JvmStatic
    fun loadFavorites(context: Context): List<FavoriteStationItem> {
        val list = mutableListOf<FavoriteStationItem>()
        try {
            val cursor = context.contentResolver.query(
                FmStation.Station.CONTENT_URI,
                arrayOf(
                    FmStation.Station._ID,
                    FmStation.Station.FREQUENCY,
                    FmStation.Station.STATION_NAME,
                    FmStation.Station.PROGRAM_SERVICE
                ),
                "${FmStation.Station.IS_FAVORITE}=1",
                null,
                "${FmStation.Station.FREQUENCY} ASC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(FmStation.Station._ID)
                val freqIdx = it.getColumnIndexOrThrow(FmStation.Station.FREQUENCY)
                val nameIdx = it.getColumnIndexOrThrow(FmStation.Station.STATION_NAME)
                val psIdx = it.getColumnIndexOrThrow(FmStation.Station.PROGRAM_SERVICE)
                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val freq = it.getInt(freqIdx)
                    var name = it.getString(nameIdx)
                    if (name.isNullOrEmpty()) {
                        name = it.getString(psIdx)
                    }
                    if (name.isNullOrEmpty()) {
                        name = FmUtils.formatStation(freq)
                    }
                    list.add(FavoriteStationItem(id, freq, name))
                }
            }
        } catch (e: Exception) {
            // Ignore query errors
        }

        val savedOrder = FmPreferences.getFavoritesOrder(context)
        if (savedOrder.isNotEmpty()) {
            val orderMap = savedOrder.withIndex().associate { it.value to it.index }
            list.sortBy { orderMap[it.frequency] ?: Int.MAX_VALUE }
        }

        return list
    }

    @JvmStatic
    fun loadRecordings(context: Context): List<RecordingItem> {
        val list = mutableListOf<RecordingItem>()
        val seenPaths = mutableSetOf<String>()

        try {
            val sdcard = FmUtils.getDefaultStoragePath()
            val folder = FmRecorder.getFmRecordFolder(context)
            val dir = File(sdcard, folder)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles { f ->
                    f.isFile && (f.name.endsWith(".mp3", ignoreCase = true) ||
                            f.name.endsWith(".m4a", ignoreCase = true) ||
                            f.name.endsWith(".aac", ignoreCase = true))
                }
                files?.sortedByDescending { it.lastModified() }?.forEach { f ->
                    val uri = Uri.fromFile(f)
                    seenPaths.add(f.absolutePath)
                    list.add(
                        RecordingItem(
                            id = f.hashCode().toLong(),
                            title = f.nameWithoutExtension,
                            filePath = f.absolutePath,
                            uri = uri,
                            durationMs = 0L,
                            sizeBytes = f.length(),
                            dateModified = f.lastModified()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("FmMainUiBridge", "Error listing recording directory: " + e.message)
        }

        try {
            val folder = FmRecorder.getFmRecordFolder(context)
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_MODIFIED
            )
            val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("%$folder%")
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataIdx)
                    val id = cursor.getLong(idIdx)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    if (path != null && seenPaths.contains(path)) {
                        val existingIndex = list.indexOfFirst { it.filePath == path }
                        if (existingIndex >= 0) {
                            val existing = list[existingIndex]
                            list[existingIndex] = existing.copy(
                                id = id,
                                uri = contentUri,
                                durationMs = cursor.getLong(durIdx)
                            )
                        }
                    } else if (path != null) {
                        seenPaths.add(path)
                        list.add(
                            RecordingItem(
                                id = id,
                                title = cursor.getString(titleIdx) ?: File(path).nameWithoutExtension,
                                filePath = path,
                                uri = contentUri,
                                durationMs = cursor.getLong(durIdx),
                                sizeBytes = cursor.getLong(sizeIdx),
                                dateModified = cursor.getLong(dateIdx) * 1000L
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FmMainUiBridge", "Error querying MediaStore: " + e.message)
        }

        return list.sortedByDescending { it.dateModified }
    }

    @JvmStatic
    fun deleteRecording(context: Context, item: RecordingItem): Boolean {
        var deleted = false
        try {
            if (item.filePath.isNotEmpty()) {
                val f = File(item.filePath)
                if (f.exists()) {
                    deleted = f.delete()
                }
            }
            if (item.id > 0) {
                val count = context.contentResolver.delete(item.uri, null, null)
                if (count > 0) deleted = true
            }
        } catch (e: Exception) {
            Log.e("FmMainUiBridge", "Error deleting recording: " + e.message)
        }
        return deleted
    }

    @JvmStatic
    fun createPlayerComposeView(
        activity: Activity,
        state: FmUiStateHolder
    ): View {
        return ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RevampedFmTheme {
                    FmPlayerScreen(
                        currentFrequency = state.currentFrequency.floatValue,
                        stationName = state.stationName.value,
                        radioText = state.radioText.value,
                        isPlaying = state.isPlaying.value,
                        isFavorite = state.isFavorite.value,
                        isSpeakerUsed = state.isSpeakerUsed.value,
                        isAudioRoutingAvailable = state.isAudioRoutingAvailable.value,
                        isRecording = state.isRecording.value,
                        isSeeking = state.isSeeking.value,
                        isRecordingSheetVisible = state.isRecordingSheetVisible.value,
                        recordingState = state.recordingState.intValue,
                        recordDurationMs = state.recordDurationMs.longValue,
                        recordSizeBytes = state.recordSizeBytes.longValue,
                        defaultRecordingName = state.recordingName.value,
                        favoritesList = state.favoritesList.value,
                        recordingsList = state.recordingsList.value,
                        onPlayPauseClick = { state.onPlayPause() },
                        onPrevStationClick = { state.onPrevStation() },
                        onNextStationClick = { state.onNextStation() },
                        onTuneDownClick = { state.onTuneDown() },
                        onTuneUpClick = { state.onTuneUp() },
                        onFrequencySeek = { freq -> state.onFrequencySeek(freq) },
                        onToggleFavoriteClick = { state.onToggleFavorite() },
                        onToggleSpeakerClick = { state.onToggleSpeaker() },
                        onRecordClick = {
                            state.reloadRecordings(activity)
                            state.onRecord()
                        },
                        onReloadFavorites = {
                            state.reloadFavorites(activity)
                            state.onReloadFavorites()
                        },
                        onStationSelect = { freq -> state.onStationSelect(freq) },
                        onDismissRecordingSheet = { state.setRecordingSheetVisible(false) },
                        onStartRecording = { state.onStartRecording() },
                        onStopRecording = { state.onStopRecording() },
                        onSaveRecording = { name -> state.onSaveRecording(name) },
                        onDiscardRecording = { state.onDiscardRecording() },
                        onDeleteRecording = { item -> state.onDeleteRecording(item) }
                    )
                }
            }
        }
    }
}
