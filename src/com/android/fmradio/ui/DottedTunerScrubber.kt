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

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.round

/**
 * Material 3 seekbar with discrete stops and vertical pill thumb indicator.
 * Provides smooth continuous dragging across FM frequencies with tactile haptics.
 */
@Composable
fun DottedTunerScrubber(
    currentFrequency: Float,
    minFrequency: Float = 87.5f,
    maxFrequency: Float = 108.0f,
    isPlaying: Boolean = false,
    enabled: Boolean = true,
    onFrequencySeek: (Float) -> Unit,
    onScrubbingStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeDotColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
    val inactiveDotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

    var lastHapticFreq by remember { mutableFloatStateOf(currentFrequency) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedThumbFraction by animateFloatAsState(
        targetValue = ((currentFrequency - minFrequency) / (maxFrequency - minFrequency)).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tuner_thumb"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 24.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .pointerInput(minFrequency, maxFrequency, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    val rawFreq = minFrequency + fraction * (maxFrequency - minFrequency)
                    val stepped = (round(rawFreq * 10f) / 10f).coerceIn(minFrequency, maxFrequency)
                    if (stepped != lastHapticFreq) {
                        lastHapticFreq = stepped
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    onFrequencySeek(stepped)
                }
            }
            .pointerInput(minFrequency, maxFrequency, enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onScrubbingStateChange(true)
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val rawFreq = minFrequency + fraction * (maxFrequency - minFrequency)
                        val stepped = (round(rawFreq * 10f) / 10f).coerceIn(minFrequency, maxFrequency)
                        if (stepped != lastHapticFreq) {
                            lastHapticFreq = stepped
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        onFrequencySeek(stepped)
                    },
                    onDragEnd = {
                        isDragging = false
                        onScrubbingStateChange(false)
                    },
                    onDragCancel = {
                        isDragging = false
                        onScrubbingStateChange(false)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val rawFreq = minFrequency + fraction * (maxFrequency - minFrequency)
                        val stepped = (round(rawFreq * 10f) / 10f).coerceIn(minFrequency, maxFrequency)
                        if (stepped != lastHapticFreq) {
                            lastHapticFreq = stepped
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onFrequencySeek(stepped)
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val trackHeight = 6.dp.toPx()
        val thumbWidth = 4.5.dp.toPx()
        val thumbHeight = 26.dp.toPx()
        val thumbCorner = thumbWidth / 2f

        val thumbX = (width * animatedThumbFraction).coerceIn(thumbWidth / 2f, width - thumbWidth / 2f)

        // 1. Inactive Track (Full background track)
        drawRoundRect(
            color = inactiveTrackColor,
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        )

        // 2. Active Track (From start to thumb position)
        if (thumbX > 0f) {
            drawRoundRect(
                color = activeTrackColor,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(thumbX, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )
        }

        // 3. Discrete Stops / Dots across the track (8 discrete evenly spaced points)
        val stopCount = 8
        val dotRadius = 2.dp.toPx()
        for (i in 0 until stopCount) {
            val stopFraction = i.toFloat() / (stopCount - 1)
            val stopX = (width * stopFraction).coerceIn(dotRadius * 2f, width - dotRadius * 2f)
            val isPassed = stopX <= thumbX

            // Avoid drawing the dot directly underneath the thumb bar
            if (kotlin.math.abs(stopX - thumbX) > (thumbWidth + dotRadius)) {
                drawCircle(
                    color = if (isPassed) activeDotColor else inactiveDotColor,
                    radius = dotRadius,
                    center = Offset(stopX, centerY)
                )
            }
        }

        // 4. Vertical Pill Thumb Indicator
        drawRoundRect(
            color = activeTrackColor,
            topLeft = Offset(thumbX - thumbWidth / 2f, centerY - thumbHeight / 2f),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbCorner, thumbCorner)
        )
    }
}
