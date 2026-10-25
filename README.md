# Halcyon's FM Tuner a.k.a. Radio

Halcyon's FM Tuner a.k.a. Radio is an expressive FM radio application for Android (AOSP / LineageOS / Halcyon), forked from [iusmac's RevampedFMRadio](https://github.com/iusmac/RevampedFMRadio) and based on the upstream LineageOS / AOSP FMRadio codebase.

Built with Jetpack Compose, featuring dynamic typography, generative album art, direction-aware animations, and expanded device control.

---

## Features

### Expressive font physics on Layouts
- Dynamic title typography shifts across states (paused, seeking, playing, and recording) with subtle width breathing while tuning.
- Tactile spring physics give titles and controls a responsive, physical feel during interactions.
- Smooth typeface pre-caching prevents layout flicker and frame drops during live tuning.

### Directional transitions on main screen / FM player
- Direction-aware slide animations follow tuning gestures (slides left when tuning down, right when tuning up).
- Metadata and RDS radio text updates crossfade in place without shifting surrounding controls.
- Subtitle area reserves a fixed layout slot so station titles stay anchored when RDS metadata appears or disappears.

### Frequency-seeded album art generator
- Distinctive vector album art generated deterministically for each frequency.
- Dynamic combinations of Material 3 shapes (squircles, stars, scallops) and vector audio waves.
- Animated radar sweep indicator during channel scanning with lock-in when tuned.

### Better recording manager and recorded player
- Dedicated full-screen recording manager with creation dates, file sizes, and playback durations.
- Built-in audio player with a floating playback bar and scrubbing seekbar.
- Automatic broadcast interlock pauses live FM playback whenever a recorded file is played.

### Better station favourites management
- In-place station renaming and manual frequency entry with validation.
- Interactive reordering (Up / Down) with animated list transitions.
- Deletion confirmations to prevent accidental removals.

### Better preferences with system-like UI
- Clean preferences interface styled to match native system settings.
- Available preferences:
  - **Autoplay on launch**: Toggle whether radio playback starts immediately when opened.
  - **Recording format & bitrate**: Record in AAC (.m4a) or AMR (.3gpp) with bitrates up to 320 kbps.
  - **Custom storage location**: Choose where saved recordings are stored.
  - **Reduce motion**: Master toggle to disable font breathing, spring bounces, and radar animations.

### Platform Modernizations
- **Target SDK 35**: Modern Android 15+ platform behaviors.
- **Predictive Back Navigation**: Smooth back gesture integration.
- **LineageOS Vector Assets**: Replaced legacy raster assets with clean vector icons.
- **Wireless Playback**: Built-in antenna fallback when headphones are unplugged.

---

## Device Integration Guide

> [!NOTE]
> **Halcyon AOSP Source**: Halcyon's FM Tuner a.k.a. Radio (`FMTuner`) is included by default on Halcyon AOSP. If you are already building Halcyon AOSP from source, skip the manifest configuration step.

### 1. Manifest Configuration
To include the repository in external ROM manifests (e.g. `.repo/local_manifests/fmtuner.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <project name="halcyonproject/packages_halcyon_apps_FMTuner"
           path="packages/apps/FMTuner"
           remote="github"
           revision="17.0" />
</manifest>
```

*(Because `FMTuner` explicitly overrides `FMRadio` and `RevampedFMRadio` in its build configuration, removing existing `FMRadio` projects from your manifest is not required).*

### 2. Base Device Configuration
Add the package to your device makefile (`device.mk`):

```make
PRODUCT_PACKAGES += \
    FMTuner
```

---

### Qualcomm (QCOM) SoCs

1. **JNI Library**:
   Add the Qualcomm FM JNI library to `device.mk`:
   ```make
   PRODUCT_PACKAGES += \
       libqcomfmjni
   ```

2. **Vendor Properties** (`vendor.prop`):
   Set the Bluetooth SoC property so the FM stack can detect the hardware interface:
   ```properties
   vendor.qcom.bluetooth.soc=cherokee
   ```
   *(Replace `cherokee` with your SoC's Bluetooth interface, such as `hastings` or `pronto`).*

   For legacy audio routing pipelines (e.g. older audio HAL mixer paths), set:
   ```properties
   ro.vendor.fm.use_audio_session=true
   ```

3. **SELinux Policy** (`sepolicy/vendor/system_app.te`):
   Allow `system_app` to read the vendor Bluetooth property:
   ```
   get_prop(system_app, vendor_bluetooth_prop)
   ```

---

### MediaTek (MTK) SoCs

1. **JNI Library**:
   Add the MediaTek FM JNI library to `device.mk`:
   ```make
   PRODUCT_PACKAGES += \
       libmtkfmjni
   ```

2. **Prebuilt Library Cleanup & Configuration**:
   Remove any vendor prebuilt `libfmcust` or `libfmjni.so` libraries from your device tree:
   - `libmtkfmjni` already includes hardcoded configurations for common MediaTek SoCs previously provided by `libfmcust`.
   - For newer SoCs, configure parameters using system properties (see LineageOS change [Ia0e1f26820f4578d331abcc45e4c0a6db7d0a0da](https://review.lineageos.org/q/Ia0e1f26820f4578d331abcc45e4c0a6db7d0a0da) for reference).

---

## Credits

- [iusmac (Max)](https://github.com/iusmac) — Original RevampedFMRadio architecture and QCOM/MTK JNI ports.
- [The LineageOS Project](https://github.com/LineageOS) & AOSP — Base FMRadio implementation and vector assets.
- [The Halcyon Project](https://github.com/halcyonproject) — Expressive Compose UI, variable font physics, and platform modernizations.
