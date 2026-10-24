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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexTitle = FontFamily(
    Font(
        familyName = DeviceFontFamilyName("google-sans-flex"),
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 700f),
            FontVariation.Setting("wdth", 100f),
            FontVariation.Setting("ROND", 0f)
        )
    ),
    Font(
        familyName = DeviceFontFamilyName("google-sans-flex"),
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 600f),
            FontVariation.Setting("wdth", 100f),
            FontVariation.Setting("ROND", 0f)
        )
    ),
    Font(
        familyName = DeviceFontFamilyName("google-sans-flex"),
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 500f),
            FontVariation.Setting("wdth", 100f),
            FontVariation.Setting("ROND", 0f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexBody = FontFamily(
    Font(
        familyName = DeviceFontFamilyName("google-sans-flex"),
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
            FontVariation.Setting("wdth", 100f),
            FontVariation.Setting("ROND", 0f)
        )
    ),
    Font(
        familyName = DeviceFontFamilyName("google-sans-flex"),
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 500f),
            FontVariation.Setting("wdth", 100f),
            FontVariation.Setting("ROND", 0f)
        )
    )
)

val RevampedTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFlexTitle,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFlexBody,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun RevampedFmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RevampedTypography,
        content = content
    )
}
