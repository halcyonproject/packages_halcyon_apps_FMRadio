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

package com.android.fmradio.ui.artwork;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.LruCache;
import java.io.File;

import java.util.Locale;
import java.util.Random;

/**
 * Procedurally generates high-definition vector station album art seeded by
 * station frequency and styled using Material You tonal color roles.
 */
public final class StationArtworkGenerator {
    private static final int ARTWORK_SIZE = 1024;
    private static final LruCache<Integer, Bitmap> sArtworkCache = new LruCache<>(16);

    private StationArtworkGenerator() {
    }

    /**
     * Retrieve or generate an HD artwork bitmap for a specific radio frequency.
     *
     * @param context Application or UI context
     * @param frequency Station frequency in tenths (e.g. 875 for 87.5 MHz)
     * @param stationName Optional station name/callsign
     * @return 1024x1024 high-definition Bitmap
     */
    public static synchronized Bitmap getStationArtwork(Context context, int frequency, String stationName) {
        Bitmap cached = sArtworkCache.get(frequency);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        Bitmap bitmap = generateArtwork(context, frequency, stationName);
        sArtworkCache.put(frequency, bitmap);
        return bitmap;
    }

    private static Bitmap generateArtwork(Context context, int frequency, String stationName) {
        Bitmap bitmap = Bitmap.createBitmap(ARTWORK_SIZE, ARTWORK_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        long seed = (long) frequency * 31337L;
        Random rng = new Random(seed);

        Palette palette = extractMaterialPalette(context, rng);
        int archetype = Math.abs(frequency / 5) % 5;

        switch (archetype) {
            case 0:
                renderAtmosphericHorizon(canvas, palette, frequency, rng);
                break;
            case 1:
                renderGeometricWaveforms(canvas, palette, frequency, rng);
                break;
            case 2:
                renderRetroRadioDial(canvas, palette, frequency, rng);
                break;
            case 3:
                renderMaterial3Shapes(canvas, palette, frequency, rng);
                break;
            default:
                renderModernistAbstract(canvas, palette, frequency, rng);
                break;
        }

        return bitmap;
    }

    private static Typeface sGoogleSansFlex;

    private static Typeface getGoogleSansFlex(Context context) {
        if (sGoogleSansFlex == null) {
            try {
                File fontFile = new File("/product/fonts/GoogleSansFlex-Regular.ttf");
                if (!fontFile.exists()) {
                    fontFile = new File("/system/fonts/GoogleSansFlex-Regular.ttf");
                }
                if (fontFile.exists()) {
                    sGoogleSansFlex = new Typeface.Builder(fontFile).build();
                } else {
                    sGoogleSansFlex = Typeface.create("google-sans-flex", Typeface.NORMAL);
                }
            } catch (Throwable t) {
                sGoogleSansFlex = Typeface.create("google-sans", Typeface.NORMAL);
                if (sGoogleSansFlex == null || sGoogleSansFlex == Typeface.DEFAULT) {
                    sGoogleSansFlex = Typeface.DEFAULT_BOLD;
                }
            }
        }
        return sGoogleSansFlex;
    }

    private static void renderCornerBadges(Canvas canvas, Context context, Palette palette, int freq, String stationName) {
        Typeface font = getGoogleSansFlex(context);

        Paint pillBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillBgPaint.setStyle(Paint.Style.FILL);
        pillBgPaint.setColor(adjustAlpha(Color.BLACK, 0.45f));

        Paint pillBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillBorderPaint.setStyle(Paint.Style.STROKE);
        pillBorderPaint.setStrokeWidth(3f);
        pillBorderPaint.setColor(adjustAlpha(Color.WHITE, 0.25f));

        // 1. Top-Left Pill: "FM RADIO"
        RectF tlPill = new RectF(56f, 56f, 310f, 136f);
        canvas.drawRoundRect(tlPill, 40f, 40f, pillBgPaint);
        canvas.drawRoundRect(tlPill, 40f, 40f, pillBorderPaint);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(palette.accentPrimary);
        canvas.drawCircle(96f, 96f, 10f, dotPaint);

        Paint tlTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tlTextPaint.setTypeface(font);
        tlTextPaint.setFontVariationSettings("'ROND' 100, 'wght' 750, 'wdth' 100");
        tlTextPaint.setColor(Color.WHITE);
        tlTextPaint.setTextSize(32f);
        canvas.drawText("FM RADIO", 124f, 107f, tlTextPaint);

        // 2. Top-Right Pill: Frequency callout e.g. "104.5 MHz"
        String freqText = String.format(Locale.US, "%.1f", freq / 10.0f);
        String unitText = "MHz";

        Paint trNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trNumberPaint.setTypeface(font);
        trNumberPaint.setFontVariationSettings("'ROND' 100, 'wght' 800, 'wdth' 105");
        trNumberPaint.setColor(Color.WHITE);
        trNumberPaint.setTextSize(64f);

        Paint trUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trUnitPaint.setTypeface(font);
        trUnitPaint.setFontVariationSettings("'ROND' 100, 'wght' 600, 'wdth' 100");
        trUnitPaint.setColor(palette.accentSecondary);
        trUnitPaint.setTextSize(32f);

        float numWidth = trNumberPaint.measureText(freqText);
        float unitWidth = trUnitPaint.measureText(unitText);
        float trPillWidth = numWidth + unitWidth + 64f;
        float trLeft = ARTWORK_SIZE - 56f - trPillWidth;

        RectF trPill = new RectF(trLeft, 56f, ARTWORK_SIZE - 56f, 146f);
        canvas.drawRoundRect(trPill, 45f, 45f, pillBgPaint);
        canvas.drawRoundRect(trPill, 45f, 45f, pillBorderPaint);

        canvas.drawText(freqText, trLeft + 28f, 118f, trNumberPaint);
        canvas.drawText(unitText, trLeft + 36f + numWidth, 118f, trUnitPaint);

        // 3. Bottom-Left Pill: Station Name / Program Service
        String bottomLabel = (stationName != null && !stationName.trim().isEmpty() && !stationName.equals("FM Radio"))
                ? stationName.trim()
                : "BROADCAST STEREO";

        Paint blTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blTextPaint.setTypeface(font);
        blTextPaint.setFontVariationSettings("'ROND' 100, 'wght' 600, 'wdth' 100");
        blTextPaint.setColor(Color.WHITE);
        blTextPaint.setTextSize(32f);

        float blTextWidth = blTextPaint.measureText(bottomLabel);
        float blPillWidth = Math.min(blTextWidth + 60f, ARTWORK_SIZE - 112f);
        RectF blPill = new RectF(56f, ARTWORK_SIZE - 136f, 56f + blPillWidth, ARTWORK_SIZE - 56f);
        canvas.drawRoundRect(blPill, 40f, 40f, pillBgPaint);
        canvas.drawRoundRect(blPill, 40f, 40f, pillBorderPaint);

        canvas.save();
        canvas.clipRect(blPill.left + 24f, blPill.top, blPill.right - 24f, blPill.bottom);
        canvas.drawText(bottomLabel, 56f + 28f, ARTWORK_SIZE - 85f, blTextPaint);
        canvas.restore();
    }

    private static void renderAtmosphericHorizon(Canvas canvas, Palette palette, int freq, Random rng) {
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Shader skyShader = new LinearGradient(
                0, 0, 0, ARTWORK_SIZE,
                new int[]{palette.accentPrimary, palette.accentSecondary, palette.surfaceBase},
                new float[]{0.0f, 0.65f, 1.0f},
                Shader.TileMode.CLAMP
        );
        bgPaint.setShader(skyShader);
        canvas.drawRect(0, 0, ARTWORK_SIZE, ARTWORK_SIZE, bgPaint);

        // Radiant Sun / Beacon Disc
        Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float sunCx = ARTWORK_SIZE * 0.5f;
        float sunCy = ARTWORK_SIZE * 0.45f;
        float sunRadius = ARTWORK_SIZE * 0.22f;
        sunPaint.setShader(new RadialGradient(
                sunCx, sunCy, sunRadius * 1.5f,
                new int[]{palette.glowColor, Color.TRANSPARENT},
                new float[]{0.3f, 1.0f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(sunCx, sunCy, sunRadius * 1.5f, sunPaint);

        sunPaint.setShader(null);
        sunPaint.setColor(palette.accentTertiary);
        canvas.drawCircle(sunCx, sunCy, sunRadius, sunPaint);

        // Sinusoidal Radio Waves / Mountain Ridges
        Paint ridgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ridgePaint.setStyle(Paint.Style.FILL);

        int ridges = 3;
        for (int r = 0; r < ridges; r++) {
            float baseY = ARTWORK_SIZE * (0.60f + r * 0.12f);
            Path path = new Path();
            path.moveTo(0, ARTWORK_SIZE);
            path.lineTo(0, baseY);

            float waveFreq = 0.005f + (r * 0.002f);
            float amplitude = 35f - (r * 6f);
            for (float x = 0; x <= ARTWORK_SIZE; x += 16) {
                float y = baseY + (float) Math.sin((x + freq * 10f) * waveFreq) * amplitude;
                path.lineTo(x, y);
            }
            path.lineTo(ARTWORK_SIZE, ARTWORK_SIZE);
            path.close();

            ridgePaint.setColor(adjustAlpha(palette.surfaceBase, 0.70f + (r * 0.12f)));
            canvas.drawPath(path, ridgePaint);
        }
    }

    private static void renderGeometricWaveforms(Canvas canvas, Palette palette, int freq, Random rng) {
        // Deep background
        canvas.drawColor(palette.surfaceBase);

        // Layered concentric waveforms spanning full canvas width
        Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);

        int waveCount = 14;
        float centerY = ARTWORK_SIZE * 0.5f;
        for (int i = 0; i < waveCount; i++) {
            float progress = (float) i / waveCount;
            wavePaint.setStrokeWidth(8f + progress * 16f);
            wavePaint.setColor(lerpColor(palette.accentPrimary, palette.accentSecondary, progress));
            wavePaint.setAlpha((int) (140 + progress * 115));

            Path wave = new Path();
            float startX = -20f;
            float endX = ARTWORK_SIZE + 20f;
            wave.moveTo(startX, centerY);

            float amp = (float) Math.sin(progress * Math.PI) * (ARTWORK_SIZE * 0.24f);
            float freqFactor = 2.0f + (freq % 4);
            for (float x = startX; x <= endX; x += 16) {
                float normX = (x - startX) / (endX - startX);
                float y = centerY + (float) Math.sin(normX * Math.PI * freqFactor + (i * 0.4f)) * amp;
                wave.lineTo(x, y);
            }
            canvas.drawPath(wave, wavePaint);
        }
    }

    private static void renderRetroRadioDial(Canvas canvas, Palette palette, int freq, Random rng) {
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new RadialGradient(
                ARTWORK_SIZE * 0.5f, ARTWORK_SIZE * 0.5f, ARTWORK_SIZE * 0.7f,
                palette.surfaceBase, darkenColor(palette.surfaceBase, 0.4f),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, ARTWORK_SIZE, ARTWORK_SIZE, bgPaint);

        // Concentric vinyl / frequency rings
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        float cx = ARTWORK_SIZE * 0.5f;
        float cy = ARTWORK_SIZE * 0.5f;

        for (float r = 120; r <= 440; r += 28) {
            ringPaint.setStrokeWidth(3f);
            ringPaint.setColor(adjustAlpha(palette.accentSecondary, 0.20f));
            canvas.drawCircle(cx, cy, r, ringPaint);
        }

        // Prominent tuning arc
        ringPaint.setStrokeWidth(18f);
        ringPaint.setColor(palette.accentPrimary);
        RectF arcBounds = new RectF(cx - 320, cy - 320, cx + 320, cy + 320);
        float startAngle = 135f;
        float sweepAngle = 270f * ((freq % 205) / 205.0f);
        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, ringPaint);

        // Center hub
        Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setStyle(Paint.Style.FILL);
        hubPaint.setColor(palette.accentTertiary);
        canvas.drawCircle(cx, cy, 90f, hubPaint);
    }

    private static void renderModernistAbstract(Canvas canvas, Palette palette, int freq, Random rng) {
        canvas.drawColor(palette.surfaceBase);

        Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shapePaint.setStyle(Paint.Style.FILL);

        // M3 Puffy Diamond base
        shapePaint.setColor(adjustAlpha(palette.accentPrimary, 0.85f));
        Path diamond = createPuffyDiamondPath(ARTWORK_SIZE * 0.42f, ARTWORK_SIZE * 0.46f, 320f);
        canvas.drawPath(diamond, shapePaint);

        // Overlapping Stadium Arch
        shapePaint.setColor(adjustAlpha(palette.accentSecondary, 0.75f));
        Path arch = createArchPath(ARTWORK_SIZE * 0.62f, ARTWORK_SIZE * 0.58f, 320f, 440f);
        canvas.drawPath(arch, shapePaint);

        // Accent M3 Flower node
        shapePaint.setColor(palette.accentTertiary);
        Path flower = createHarmonicShapePath(ARTWORK_SIZE * 0.72f, ARTWORK_SIZE * 0.28f, 90f, 0.22f, 6);
        canvas.drawPath(flower, shapePaint);
    }

    /**
     * Procedural Material 3 Expressive Shapes composition (Cookie, Clover, Burst, Gem, Flower).
     */
    private static void renderMaterial3Shapes(Canvas canvas, Palette palette, int freq, Random rng) {
        // Deep background with subtle tonal vignette
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new RadialGradient(
                ARTWORK_SIZE * 0.5f, ARTWORK_SIZE * 0.5f, ARTWORK_SIZE * 0.75f,
                darkenColor(palette.surfaceBase, 0.1f), darkenColor(palette.surfaceBase, 0.55f),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, ARTWORK_SIZE, ARTWORK_SIZE, bgPaint);

        Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shapePaint.setStyle(Paint.Style.FILL);

        float cx = ARTWORK_SIZE * 0.5f;
        float cy = ARTWORK_SIZE * 0.5f;
        int shapeStyle = Math.abs(freq % 6);

        // Outer ambient glow disk
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setShader(new RadialGradient(
                cx, cy, 420f,
                new int[]{palette.glowColor, Color.TRANSPARENT},
                new float[]{0.2f, 1.0f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, 420f, glowPaint);

        // 1. Secondary Backing M3 Shape (Soft Layer)
        shapePaint.setColor(adjustAlpha(palette.accentSecondary, 0.40f));
        Path backingShape;
        if (shapeStyle % 2 == 0) {
            backingShape = createHarmonicShapePath(cx, cy, 330f, 0.16f, 8); // 8-sided cookie
        } else {
            backingShape = createBurstPath(cx, cy, 360f, 260f, 12); // 12-point burst
        }
        canvas.drawPath(backingShape, shapePaint);

        // 2. Primary Hero M3 Expressive Shape
        shapePaint.setColor(palette.accentPrimary);
        Path heroShape;
        switch (shapeStyle) {
            case 0: // 4-leaf clover
                heroShape = createHarmonicShapePath(cx, cy, 270f, 0.32f, 4);
                break;
            case 1: // 6-sided cookie
                heroShape = createHarmonicShapePath(cx, cy, 280f, 0.18f, 6);
                break;
            case 2: // Puffy Diamond
                heroShape = createPuffyDiamondPath(cx, cy, 280f);
                break;
            case 3: // 8-petal Flower
                heroShape = createHarmonicShapePath(cx, cy, 270f, 0.28f, 8);
                break;
            case 4: // Faceted Gem
                heroShape = createGemPath(cx, cy, 270f);
                break;
            default: // Expressive Heart
                heroShape = createHeartPath(cx, cy - 30f, 380f);
                break;
        }
        canvas.drawPath(heroShape, shapePaint);

        // 3. Central Core M3 Accent
        shapePaint.setColor(palette.accentTertiary);
        Path coreShape;
        if (shapeStyle == 5) {
            coreShape = createHarmonicShapePath(cx, cy - 30f, 110f, 0.15f, 6);
        } else {
            coreShape = createHarmonicShapePath(cx, cy, 110f, 0.25f, 4);
        }
        canvas.drawPath(coreShape, shapePaint);

        // Inner micro dot
        shapePaint.setColor(Color.WHITE);
        shapePaint.setAlpha(200);
        canvas.drawCircle(cx, (shapeStyle == 5) ? cy - 30f : cy, 22f, shapePaint);
    }

    private static Path createHarmonicShapePath(float cx, float cy, float baseR, float modAmp, int lobes) {
        Path path = new Path();
        int steps = 180;
        for (int i = 0; i <= steps; i++) {
            double angle = (2.0 * Math.PI * i) / steps;
            float r = baseR * (1.0f + modAmp * (float) Math.cos(lobes * angle));
            float x = cx + r * (float) Math.cos(angle);
            float y = cy + r * (float) Math.sin(angle);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        return path;
    }

    private static Path createBurstPath(float cx, float cy, float outerR, float innerR, int points) {
        Path path = new Path();
        int totalVertices = points * 2;
        for (int i = 0; i <= totalVertices; i++) {
            double angle = (2.0 * Math.PI * i) / totalVertices - Math.PI / 2.0;
            float r = (i % 2 == 0) ? outerR : innerR;
            float x = cx + r * (float) Math.cos(angle);
            float y = cy + r * (float) Math.sin(angle);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        return path;
    }

    private static Path createPuffyDiamondPath(float cx, float cy, float r) {
        Path path = new Path();
        float topX = cx, topY = cy - r;
        float rightX = cx + r, rightY = cy;
        float bottomX = cx, bottomY = cy + r;
        float leftX = cx - r, leftY = cy;
        float ctrl = r * 0.45f;
        path.moveTo(topX, topY);
        path.quadTo(cx + ctrl, cy - ctrl, rightX, rightY);
        path.quadTo(cx + ctrl, cy + ctrl, bottomX, bottomY);
        path.quadTo(cx - ctrl, cy + ctrl, leftX, leftY);
        path.quadTo(cx - ctrl, cy - ctrl, topX, topY);
        path.close();
        return path;
    }

    private static Path createArchPath(float cx, float cy, float width, float height) {
        Path path = new Path();
        float left = cx - width / 2f;
        float right = cx + width / 2f;
        float bottom = cy + height / 2f;
        float top = cy - height / 2f;
        RectF dome = new RectF(left, top, right, top + width);
        path.moveTo(left, bottom);
        path.lineTo(left, top + width / 2f);
        path.arcTo(dome, 180f, 180f);
        path.lineTo(right, bottom);
        path.close();
        return path;
    }

    private static Path createGemPath(float cx, float cy, float r) {
        Path path = new Path();
        int sides = 6;
        for (int i = 0; i <= sides; i++) {
            double angle = (2.0 * Math.PI * i) / sides - Math.PI / 2.0;
            float x = cx + r * (float) Math.cos(angle);
            float y = cy + r * (float) Math.sin(angle);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        return path;
    }

    private static Path createHeartPath(float cx, float cy, float size) {
        Path path = new Path();
        float s = size * 0.5f;
        path.moveTo(cx, cy + s * 0.75f);
        path.cubicTo(cx - s * 1.25f, cy, cx - s * 1.25f, cy - s * 0.8f, cx, cy - s * 0.25f);
        path.cubicTo(cx + s * 1.25f, cy - s * 0.8f, cx + s * 1.25f, cy, cx, cy + s * 0.75f);
        path.close();
        return path;
    }

    private static Palette extractMaterialPalette(Context context, Random rng) {
        int primary;
        int secondary;
        int tertiary;
        int surface;

        try {
            // Android 12+ System Palette Tokens
            primary = context.getColor(android.R.color.system_accent1_400);
            secondary = context.getColor(android.R.color.system_accent2_300);
            tertiary = context.getColor(android.R.color.system_accent3_400);
            surface = context.getColor(android.R.color.system_neutral1_900);
        } catch (Throwable ignored) {
            // Curated fallback palette matching M3 specs
            primary = Color.rgb(103, 80, 164);
            secondary = Color.rgb(98, 91, 113);
            tertiary = Color.rgb(125, 82, 96);
            surface = Color.rgb(28, 27, 31);
        }

        // Tonal variation based on RNG
        float hueShift = rng.nextFloat() * 40f - 20f;
        primary = shiftHue(primary, hueShift);
        secondary = shiftHue(secondary, -hueShift);

        int glow = adjustAlpha(primary, 0.45f);
        return new Palette(primary, secondary, tertiary, surface, glow);
    }

    private static int shiftHue(int color, float degrees) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + degrees + 360f) % 360f;
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    private static int darkenColor(int color, float factor) {
        return Color.argb(
                Color.alpha(color),
                (int) (Color.red(color) * factor),
                (int) (Color.green(color) * factor),
                (int) (Color.blue(color) * factor)
        );
    }

    private static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(Math.min(255, Math.max(0, alpha)), Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int lerpColor(int from, int to, float t) {
        float inv = 1.0f - t;
        int a = (int) (Color.alpha(from) * inv + Color.alpha(to) * t);
        int r = (int) (Color.red(from) * inv + Color.red(to) * t);
        int g = (int) (Color.green(from) * inv + Color.green(to) * t);
        int b = (int) (Color.blue(from) * inv + Color.blue(to) * t);
        return Color.argb(a, r, g, b);
    }

    private static class Palette {
        final int accentPrimary;
        final int accentSecondary;
        final int accentTertiary;
        final int surfaceBase;
        final int glowColor;

        Palette(int p, int s, int t, int surf, int glow) {
            this.accentPrimary = p;
            this.accentSecondary = s;
            this.accentTertiary = t;
            this.surfaceBase = surf;
            this.glowColor = glow;
        }
    }
}
