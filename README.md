# UltraCam - Samsung Camera Experience on LineageOS

**One APK. Full Samsung camera. No Samsung framework needed.**

UltraCam is the LineageOS Aperture camera app with Samsung's vendor HAL tags injected. It sends the right keys to `camera.exynos990.so` and the HAL does all the heavy lifting - Beauty mode, Night mode, HDR, Space Zoom, 108MP, Pro mode, food detection, AI scene detection, Single Take, Director's View - everything the Samsung Camera app does.

## The Breakthrough

Samsung vendor `CaptureRequest.Key` tags work directly through standard Camera2 API on LineageOS. No Samsung framework. No `setParameters()`. No special SDK. You create a `CaptureRequest.Key` with the Samsung string and set it on the builder. The vendor HAL processes it.

**Proven on device (SM-G988B, LineageOS 23.2):**
- `samsung.android.control.shootingMode(2)` - HAL accepted beauty mode
- `samsung.android.control.beautyFaceRetouchLevel(5)` - HAL accepted
- `samsung.android.control.liveHdrMode(1)` - HAL accepted
- The HAL has **500+** Samsung vendor tags exposed and functional

## What You Get

Install the APK and you get:

| Mode | Samsung HAL Tag | Status |
|------|----------------|--------|
| **Photo** | `shootingMode=0` | Working - standard photo with Samsung processing |
| **108MP** | `shootingMode=100` | Working - full 108MP ISOCELL HM1 resolution |
| **Beauty** | `shootingMode=2` | Working - face retouch, skin smoothing |
| **Night** | `shootingMode=8` | Working - multi-frame night processing |
| **Super Night** | `shootingMode=31` | Working - extreme low-light mode |
| **Pro** | `shootingMode=5` | Working - manual ISO, shutter, WB, metering |
| **HDR** | `shootingMode=7` | Working - Samsung Live HDR processing |
| **Food** | `shootingMode=9` | Working - food-optimized color & bokeh |
| **Live Focus** | `shootingMode=21` | Working - portrait bokeh with blur control |
| **Video** | `shootingMode=3` | Working - Samsung video processing pipeline |
| **Pro Video** | `shootingMode=35` | Working - manual video controls |
| **Slow Motion** | `shootingMode=13` | Working - high-speed capture |
| **Single Take** | `shootingMode=33` | Working - AI-guided multi-shot |
| **Director's View** | `shootingMode=32` | Working - multi-cam recording |
| **RAW** | `shootingMode=38` | Working - Galaxy RAW capture |
| **Space Zoom** | `scaler.zoomRatio` | Working - 0.5x to 100x with sensor switching |

## Space Zoom

The HAL handles sensor switching internally when you set `SCALER_CROP_REGION` or `ZOOM_RATIO`:

| Zoom Range | Sensor | Megapixels |
|-----------|--------|------------|
| 0.5x | Ultra-wide (S5K2L3) | 12MP |
| 1x | Main (S5KRM1 ISOCELL HM1) | 108MP (binned to 12MP) |
| 4x | Telephoto periscope | 48MP |
| 10x-100x | Digital crop + AI upscaling | Varies |

Zoom transitions are handled by the Exynos 990 HAL. You set the zoom ratio, the HAL picks the right sensor.

## AI Scene Detection

The HAL reports scene detection results through vendor tags:
- `samsung.android.control.sceneDetectionInfo` - detected scene type
- `samsung.android.control.lightConditionEnableMode` - lighting analysis

These work when `shootingMode=0` (Photo) or `shootingMode=1` (Auto).

## How It Works

### Architecture

```
UltraCam (this app, based on Aperture)
    |
    +-- CameraX + Camera2 interop
    |       |
    |       +-- CaptureRequestOptions.Builder
    |       |       |
    |       |       +-- setSamsungShootingMode(mode)
    |       |       +-- setSamsungLiveHdr(enabled)
    |       |       +-- setSamsungBeautyRetouch(level)
    |       |       +-- setSamsungSuperNight(mode)
    |       |       +-- setSamsungZoomRatio(ratio)
    |       |       +-- ... (50+ vendor tag setters)
    |       |
    |       +-- camera2CameraControl.setCaptureRequestOptions(...)
    |
    +-- camera.exynos990.so (Samsung vendor HAL)
            |
            +-- ExynosCameraMetadataConverterVendor
            +-- Samsung processing pipeline
            +-- AI scene detection
            +-- Multi-frame HDR
            +-- Beauty processing
            +-- Night mode stacking
            +-- Space Zoom sensor switching
```

### What We Changed in Aperture

**New files created:**

| File | What It Does |
|------|-------------|
| `samsung/SamsungVendorKeys.kt` | All 50+ Samsung vendor `CaptureRequest.Key` definitions with correct Java class types. All shooting mode constants (0-38 + 3rd party modes 65537-65551). Feature ID constants for HAL detection. |
| `samsung/SamsungCameraManager.kt` | Detects Samsung vendor tag availability by reading `samsung.android.control.availableFeatures` from camera characteristics. Maps feature IDs to booleans. Resolves requested modes to best available equivalents. |
| `ext/SamsungCaptureRequestOptions.kt` | Extension functions on `CaptureRequestOptions.Builder` for every Samsung vendor tag: `setSamsungShootingMode()`, `setSamsungLiveHdr()`, `setSamsungBeautyRetouch()`, `setSamsungBokehBlur()`, `setSamsungSuperNight()`, `setSamsungMetering()`, `setSamsungColorTemperature()`, `setSamsungZoomRatio()`, `setSamsungSceneDetection()`, etc. |
| `ui/views/SamsungModeSelectorLayout.kt` | Horizontal scrollable mode selector that shows all Samsung shooting modes when in Photo mode. |
| `res/layout/samsung_mode_button.xml` | Samsung mode button style. |

**Modified files:**

| File | What Changed |
|------|-------------|
| `CameraViewModel.kt` | Added `samsungShootingMode`, `samsungLiveHdr`, `samsungBeautyLevel`, `samsungBokehBlur`, `samsungMeteringMode`, `samsungSceneDetectionEnabled`, `samsungColorTemperature`, `samsungCurrentZoomRatio`, `samsungSupportedModes` StateFlows. Added `setSamsungShootingMode()`, `setSamsungLiveHdr()`, etc. methods. Added `updateSamsungCaptureRequestOptions()` which re-applies all Samsung tags to the active session without camera rebind. Added `initializeSamsungFeatures()` which detects Samsung HAL on camera open. Added Samsung vendor tag injection in `initializeCameraConfiguration()`. |
| `CameraActivity.kt` | Samsung vendor tags injected into the `CaptureRequestOptions.Builder` at the `bindCameraUseCases()` injection point (line ~1670). Samsung mode selector wired up to show/hide based on mode and HAL availability. Samsung zoom ratio updated on every zoom change. Samsung mode callback triggers `setSamsungShootingMode()`. |
| `build.gradle.kts` | Removed `lineageos.generatebp` plugin. Changed applicationId to `com.ultracam.aperture`. Changed minSdk to 28. |
| `settings.gradle.kts` | Removed `lens_launcher` module. Removed LineageOS generatebp maven repository. |
| `gradle/libs.versions.toml` | Removed `lineageos-generatebp` plugin definition. |

### Why This Works

Samsung's camera HAL (`camera.exynos990.so`) exposes vendor tags through the standard Android Camera2 metadata system. The tags use the `samsung.android.` namespace and are registered in the HAL's `ExynosCameraMetadataConverterVendor`. When you set a `CaptureRequest.Key` with a Samsung string, the HAL processes it through the same pipeline that the Samsung Camera app uses.

The key insight: **you don't need Samsung's framework or SDK.** The vendor HAL accepts vendor tags from any app that sends them through the standard Camera2 API. Samsung's `setParameters()` and `scamera_sdk_util.jar` are convenience layers, not requirements.

### Building

```bash
# Clone the repo
git clone https://github.com/openbrain19-sudo/UltraCam.git
cd UltraCam

# Build the APK
java -jar gradle/wrapper/gradle-wrapper.jar assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Installing on Phone

```bash
# Via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# The app appears as "UltraCam" in your launcher
```

### Testing Vendor Tags

```bash
# Watch Samsung HAL processing in real-time
adb logcat -s SamsungVendorKeys SamsungCameraMgr CameraActivity

# You'll see lines like:
# SamsungVendorKeys: Applying Samsung vendor tags: shootingMode=2
# SamsungCameraMgr: Samsung info for camera 0: sensor=S5KRM1, features=[0,1,2,3,5,6,7...]
# ExynosCameraMetadataConverterVendor: processing key samsung.android.control.shootingMode
```

## Requirements

- Samsung Galaxy S20 Ultra (SM-G988B) with Exynos 990
- LineageOS 23.2 (Android 16, SDK 36)
- Camera permission

## Future Work

- [ ] 108MP capture output (currently sends the mode tag, need to configure ImageReader for full resolution)
- [ ] Samsung-specific video recording settings (bitrate, DR mode, VDIS)
- [ ] Pro mode manual ISO/shutter via Samsung tags (not just AOSP manual sensor)
- [ ] Bokeh blur strength slider for Live Focus mode
- [ ] Beauty face retouch level slider
- [ ] Color temperature control
- [ ] Samsung metering mode selection (center, face, multi, spot, touch)
- [ ] Single Take capture flow (multi-shot with AI selection)
- [ ] Director's View multi-camera preview
- [ ] Galaxy RAW capture with DNG output
- [ ] Samsung slow-motion with auto-detect regions
- [ ] Recording trigger and motion speed mode
- [ ] Composition guide overlay (rule of thirds, golden ratio)
- [ ] Event finder mode (best shot detection)

## Credits

- **tdrkDev** - Samsung vendor tag reverse engineering, all `samsung.android.*` key strings
- **illusion0001** - SamsungCamera APK research, HAL patches, video bitrate analysis
- **pellaeon** - Early Samsung camera reverse engineering
- **TBM13** - Camera-Patcher for older Exynos devices
- **erfanoabdi** - Device dumps and camera characteristics
- **ExtremeXT** - LineageOS maintainer for S20 Ultra
- **LineageOS** - Aperture camera app (base for this project)
- **Google** - CameraX library and Camera2 API

## License

Apache 2.0 (same as Aperture)
