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

package com.android.fmradio

import android.content.Context
import android.content.SharedPreferences

object FmPreferences {
    const val KEY_AUTOPLAY_ON_START = "pref_autoplay_on_start"
    const val KEY_REDUCE_MOTION = "pref_reduce_motion"
    const val KEY_RECORDING_FORMAT = "pref_recording_format"
    const val KEY_RECORDING_BITRATE = "pref_recording_bitrate"
    const val KEY_RECORDING_LOCATION = "pref_recording_location"
    const val KEY_FAVORITES_ORDER = "pref_favorites_order"

    const val FORMAT_M4A = "m4a"
    const val FORMAT_3GPP = "3gpp"

    const val BITRATE_128K = 128000
    const val BITRATE_192K = 192000
    const val BITRATE_256K = 256000
    const val BITRATE_320K = 320000

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun isAutoplayEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTOPLAY_ON_START, true)
    }

    @JvmStatic
    fun setAutoplayEnabled(context: Context, enabled: Boolean) {
        getPrefs(context)
            .edit()
            .putBoolean(KEY_AUTOPLAY_ON_START, enabled)
            .apply()
    }

    @JvmStatic
    fun isReduceMotionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REDUCE_MOTION, false)
    }

    @JvmStatic
    fun setReduceMotionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context)
            .edit()
            .putBoolean(KEY_REDUCE_MOTION, enabled)
            .apply()
    }

    @JvmStatic
    fun getRecordingFormat(context: Context): String {
        return getPrefs(context).getString(KEY_RECORDING_FORMAT, FORMAT_M4A) ?: FORMAT_M4A
    }

    @JvmStatic
    fun setRecordingFormat(context: Context, format: String) {
        getPrefs(context)
            .edit()
            .putString(KEY_RECORDING_FORMAT, format)
            .apply()
    }

    @JvmStatic
    fun getRecordingBitrate(context: Context): Int {
        return getPrefs(context).getInt(KEY_RECORDING_BITRATE, BITRATE_192K)
    }

    @JvmStatic
    fun setRecordingBitrate(context: Context, bitrate: Int) {
        getPrefs(context)
            .edit()
            .putInt(KEY_RECORDING_BITRATE, bitrate)
            .apply()
    }

    @JvmStatic
    fun getRecordingLocation(context: Context): String {
        return getPrefs(context).getString(KEY_RECORDING_LOCATION, "FM Recording") ?: "FM Recording"
    }

    @JvmStatic
    fun setRecordingLocation(context: Context, location: String) {
        getPrefs(context)
            .edit()
            .putString(KEY_RECORDING_LOCATION, location)
            .apply()
    }

    @JvmStatic
    fun getFavoritesOrder(context: Context): List<Int> {
        val serialized = getPrefs(context).getString(KEY_FAVORITES_ORDER, "") ?: ""
        if (serialized.isBlank()) return emptyList()
        return serialized.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }

    @JvmStatic
    fun setFavoritesOrder(context: Context, order: List<Int>) {
        val serialized = order.joinToString(",")
        getPrefs(context)
            .edit()
            .putString(KEY_FAVORITES_ORDER, serialized)
            .apply()
    }
}
