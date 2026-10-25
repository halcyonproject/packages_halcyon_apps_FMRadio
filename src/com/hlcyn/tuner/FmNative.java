/*
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

package com.hlcyn.tuner;

/**
 * High-level native bridge delegating to the low-level HAL interop class.
 */
public final class FmNative {
    private FmNative() {}

    public static boolean openDev() { return com.android.fmradio.FmNative.openDev(); }
    public static boolean closeDev() { return com.android.fmradio.FmNative.closeDev(); }
    public static boolean powerUp(float frequency) { return com.android.fmradio.FmNative.powerUp(frequency); }
    public static boolean powerDown(int type) { return com.android.fmradio.FmNative.powerDown(type); }
    public static boolean tune(float frequency) { return com.android.fmradio.FmNative.tune(frequency); }
    public static float seek(float frequency, boolean isUp) { return com.android.fmradio.FmNative.seek(frequency, isUp); }
    public static short[] autoScan() { return com.android.fmradio.FmNative.autoScan(); }
    public static boolean stopScan() { return com.android.fmradio.FmNative.stopScan(); }
    public static int setRds(boolean rdson) { return com.android.fmradio.FmNative.setRds(rdson); }
    public static short readRds() { return com.android.fmradio.FmNative.readRds(); }
    public static byte[] getPs() { return com.android.fmradio.FmNative.getPs(); }
    public static byte[] getLrText() { return com.android.fmradio.FmNative.getLrText(); }
    public static short activeAf() { return com.android.fmradio.FmNative.activeAf(); }
    public static int setMute(boolean mute) { return com.android.fmradio.FmNative.setMute(mute); }
    public static int isRdsSupport() { return com.android.fmradio.FmNative.isRdsSupport(); }
    public static int switchAntenna(int antenna) { return com.android.fmradio.FmNative.switchAntenna(antenna); }
    public static boolean setLowPowerMode() { return com.android.fmradio.FmNative.setLowPowerMode(); }
    public static boolean setNormalPowerMode() { return com.android.fmradio.FmNative.setNormalPowerMode(); }
}
