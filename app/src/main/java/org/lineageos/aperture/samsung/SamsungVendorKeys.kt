package org.lineageos.aperture.samsung

import android.hardware.camera2.CaptureRequest
import android.util.Log

/**
 * Complete Samsung vendor CaptureRequest keys for the Exynos 990 camera HAL.
 * These keys are processed by camera.exynos990.so via ExynosCameraMetadataConverterVendor.
 *
 * Source: Reverse engineering of Samsung SemCaptureRequest, SamsungCamera APK,
 *         tdrkDev vendor tag research, camera HAL dump.
 */
object SamsungVendorKeys {

    private const val TAG = "SamsungVendorKeys"

    @Suppress("UNCHECKED_CAST")
    private fun <T> key(name: String, type: Class<*>): CaptureRequest.Key<T> =
        CaptureRequest.Key(name, type as Class<T>)

    // ── Shooting Modes (UNIHAL) ──────────────────────────────────────
    const val MODE_SINGLE = 0
    const val MODE_AUTO = 1
    const val MODE_BEAUTY = 2
    const val MODE_VIDEO = 3
    const val MODE_PANORAMA = 4
    const val MODE_PRO = 5
    const val MODE_SELECTIVE_FOCUS = 6
    const val MODE_HDR = 7
    const val MODE_NIGHT = 8
    const val MODE_FOOD = 9
    const val MODE_DUAL = 10
    const val MODE_ANTIFOG = 11
    const val MODE_WIDE_SELFIE = 12
    const val MODE_SLOW_MOTION = 13
    const val MODE_INTERACTIVE = 14
    const val MODE_SPORTS = 15
    const val MODE_HYPER_MOTION = 16
    const val MODE_ANIMATED_GIF = 17
    const val MODE_COLOR_IRIS = 18
    const val MODE_AQUA_SCENE = 19
    const val MODE_SUPER_SLOW_MOTION = 20
    const val MODE_LIVE_FOCUS = 21
    const val MODE_FACE_LOCK = 22
    const val MODE_MULTI_BIOMETRIC = 23
    const val MODE_STICKER = 24
    const val MODE_SELFIE_FOCUS = 25
    const val MODE_LABS = 26
    const val MODE_REAR_SELFIE = 27
    const val MODE_BOKEH_VIDEO = 28
    const val MODE_INSTAGRAM_VIDEO = 29
    const val MODE_ILLUMINANCE = 30
    const val MODE_SUPER_NIGHT = 31
    const val MODE_DIRECTORS_VIEW = 32
    const val MODE_SINGLE_TAKE_PHOTO = 33
    const val MODE_SINGLE_TAKE_PHOTO_FRONT = 34
    const val MODE_PRO_VIDEO = 35
    const val MODE_QUICK_TAKE_VIDEO = 36
    const val MODE_MAX = 37
    const val MODE_GALAXY_RAW = 38
    const val MODE_INVALID = 39

    // 3rd party modes (from UNIHAL)
    const val MODE_3RD_PARTY_DEFAULT = 65537
    const val MODE_3RD_PARTY_DEFAULT_VDIS = 65538
    const val MODE_3RD_PARTY_VT = 65539
    const val MODE_3RD_PARTY_VT_VDIS = 65540
    const val MODE_3RD_PARTY_SIE = 65541
    const val MODE_3RD_PARTY_SIE_VDIS = 65542
    const val MODE_3RD_PARTY_SDK_VDIS = 65543
    const val MODE_3RD_PARTY_SDK_BOKEH = 65544
    const val MODE_3RD_PARTY_UDC_DEFAULT = 65545
    const val MODE_3RD_PARTY_INSTAGRAM = 65546
    const val MODE_3RD_PARTY_INSTAGRAM_VDIS = 65547

    // 108MP mode (non-standard, mapped from Samsung Camera app)
    const val MODE_108MP = 100

    // ── CaptureRequest Keys ──────────────────────────────────────────

    // Core shooting mode
    val KEY_SHOOTING_MODE = key<Int>("samsung.android.control.shootingMode", Integer::class.java)

    // ── Exposure & Color ──
    val KEY_AE_EXTRA_MODE = key<Int>("samsung.android.control.aeExtraMode", Integer::class.java)
    val KEY_COLOR_TEMPERATURE = key<Int>("samsung.android.control.colorTemperature", Integer::class.java)
    val KEY_WB_LEVEL = key<Int>("samsung.android.control.wbLevel", Integer::class.java)
    val KEY_LIVE_HDR_MODE = key<Int>("samsung.android.control.liveHdrMode", Integer::class.java)
    val KEY_METERING_MODE = key<Int>("samsung.android.control.meteringMode", Integer::class.java)

    // ── Beauty & Portrait ──
    val KEY_BEAUTY_FACE_RETOUCH_LEVEL = key<Int>("samsung.android.control.beautyFaceRetouchLevel", Integer::class.java)
    val KEY_BEAUTY_FACE_SKIN_COLOR = key<Int>("samsung.android.control.beautyFaceSkinColor", Integer::class.java)
    val KEY_BODY_BEAUTY_PARAMETERS = key<ByteArray>("samsung.android.control.bodyBeautyParameters", ByteArray::class.java)
    val KEY_BOKEH_BLUR_STRENGTH = key<Int>("samsung.android.control.bokehBlurStrength", Integer::class.java)
    val KEY_BOKEH_RELIGHT_LEVEL = key<Int>("samsung.android.control.bokehRelightLevel", Integer::class.java)
    val KEY_BOKEH_SPECIAL_EFFECT_INFO = key<ByteArray>("samsung.android.control.bokehSpecialEffectInfo", ByteArray::class.java)
    val KEY_DEPTH_FILTER_TYPE = key<Int>("samsung.android.control.depthFilterType", Integer::class.java)

    // ── Capture Control ──
    val KEY_CAPTURE_HINT = key<Int>("samsung.android.control.captureHint", Integer::class.java)
    val KEY_CAPTURE_PHYSICAL_ID = key<String>("samsung.android.control.capturePhysicalId", String::class.java)
    val KEY_DYNAMIC_SHOT_DEVICE_INFO = key<ByteArray>("samsung.android.control.dynamicShotDeviceInfo", ByteArray::class.java)
    val KEY_DYNAMIC_SHOT_EXTRA_INFO = key<ByteArray>("samsung.android.control.dynamicShotExtraInfo", ByteArray::class.java)
    val KEY_DYNAMIC_SHOT_HINT = key<Int>("samsung.android.control.dynamicShotHint", Integer::class.java)
    val KEY_BURST_SHOT_FPS = key<Int>("samsung.android.control.burstShotFps", Integer::class.java)

    // ── Zoom ──
    val KEY_ZOOM_RATIO = key<Float>("samsung.android.scaler.zoomRatio", java.lang.Float::class.java)
    val KEY_ZOOM_JUMP_TARGET_RATIO = key<Float>("samsung.android.control.zoomJumpTargetRatio", java.lang.Float::class.java)
    val KEY_ZOOM_LOCK_TRIGGER = key<Int>("samsung.android.control.zoomLockTrigger", Integer::class.java)

    // ── Stabilization ──
    val KEY_SW_VIDEO_STABILIZATION = key<Int>("samsung.android.control.swVideoStabilization", Integer::class.java)
    val KEY_SW_SUPER_VIDEO_STABILIZATION = key<Boolean>("samsung.android.control.swSuperVideoStabilization", java.lang.Boolean::class.java)
    val KEY_UNIHAL_VIDEO_VDIS_MODE = key<Int>("samsung.android.control.unihalVideoVdisMode", Integer::class.java)
    val KEY_LENS_OIS_OPERATION_MODE = key<Int>("samsung.android.lens.opticalStabilizationOperationMode", Integer::class.java)

    // ── Video ──
    val KEY_VIDEO_BEAUTY_FACE = key<Boolean>("samsung.android.control.videoBeautyFace", java.lang.Boolean::class.java)
    val KEY_RECORDING_DR_MODE = key<String>("samsung.android.control.recordingDrMode", String::class.java)
    val KEY_RECORDING_EXTRA_MODE = key<Int>("samsung.android.control.recordingExtraMode", Integer::class.java)
    val KEY_RECORDING_MAX_FPS = key<Int>("samsung.android.control.recordingMaxFps", Integer::class.java)
    val KEY_RECORDING_MIN_FPS = key<Int>("samsung.android.control.recordingMinFps", Integer::class.java)
    val KEY_RECORDING_MOTION_SPEED_MODE = key<Int>("samsung.android.control.recordingMotionSpeedMode", Integer::class.java)
    val KEY_RECORDING_TRIGGER = key<Int>("samsung.android.control.recordingTrigger", Integer::class.java)
    val KEY_UNIHAL_VIDEO_MODE = key<Int>("samsung.android.control.unihalVideoMode", Integer::class.java)
    val KEY_UNIHAL_VIDEO_AUTO_FRAMING_MODE = key<Int>("samsung.android.control.unihalVideoAutoFramingMode", Integer::class.java)
    val KEY_UNIHAL_VIDEO_BEAUTY_LEVEL = key<Int>("samsung.android.control.unihalVideoBeautyLevel", Integer::class.java)
    val KEY_UNIHAL_VIDEO_BOKEH_LEVEL = key<Int>("samsung.android.control.unihalVideoBokehLevel", Integer::class.java)
    val KEY_UNIHAL_VIDEO_BOKEH_MODE = key<Int>("samsung.android.control.unihalVideoBokehMode", Integer::class.java)

    // ── AI & Scene Detection ──
    val KEY_SCENE_DETECTION_INFO = key<ByteArray>("samsung.android.control.sceneDetectionInfo", ByteArray::class.java)
    val KEY_LIGHT_CONDITION_ENABLE_MODE = key<Int>("samsung.android.control.lightConditionEnableMode", Integer::class.java)
    val KEY_COMPOSITION_GUIDE_MODE = key<Int>("samsung.android.control.compositionGuideMode", Integer::class.java)
    val KEY_COMPOSITION_GUIDE_TRIGGER = key<Int>("samsung.android.control.compositionGuideTrigger", Integer::class.java)
    val KEY_EVENT_FINDER_MODE = key<Int>("samsung.android.control.eventFinderMode", Integer::class.java)

    // ── Super Slow Motion ──
    val KEY_SUPER_SLOW_MOTION_AUTO_DETECT_REGIONS = key<ByteArray>("samsung.android.control.superSlowMotionAutoDetectRegions", ByteArray::class.java)
    val KEY_SUPER_SLOW_MOTION_MODE = key<Int>("samsung.android.control.superSlowMotionMode", Integer::class.java)
    val KEY_SUPER_SLOW_MOTION_TRIGGER = key<Int>("samsung.android.control.superSlowMotionTrigger", Integer::class.java)

    // ── Night Mode ──
    val KEY_SUPER_NIGHT_SHOT_MODE = key<Int>("samsung.android.control.superNightShotMode", Integer::class.java)

    // ── Sensor ──
    val KEY_SENSOR_GAIN = key<Int>("samsung.android.sensor.gain", Integer::class.java)
    val KEY_SENSOR_FLIP_MODE = key<Int>("samsung.android.sensor.sensorFlipMode", Integer::class.java)
    val KEY_SENSOR_STREAM_TYPE = key<Int>("samsung.android.sensor.streamType", Integer::class.java)

    // ── Lens ──
    val KEY_LENS_FOCUS_LENS_POS = key<Int>("samsung.android.lens.focusLensPos", Integer::class.java)
    val KEY_LENS_FOCUS_LENS_POS_STALL = key<Int>("samsung.android.lens.focusLensPosStall", Integer::class.java)

    // ── Flip ──
    val KEY_FLIP_MODE = key<Int>("samsung.android.scaler.flipMode", Integer::class.java)

    // ── CameraCharacteristics Keys (read-only) ──
    val KEY_AVAILABLE_FEATURES = android.hardware.camera2.CameraCharacteristics.Key<IntArray>(
        "samsung.android.control.availableFeatures", IntArray::class.java
    )
    val KEY_SENSOR_NAME = android.hardware.camera2.CameraCharacteristics.Key<String>(
        "samsung.android.sensor.info.sensorName", String::class.java
    )

    // ── Samsung HAL feature IDs ──
    const val FEATURE_HDR = 0
    const val FEATURE_BEAUTY = 1
    const val FEATURE_BOKEH = 2
    const val FEATURE_NIGHT = 3
    const val FEATURE_FOOD = 4
    const val FEATURE_SLOW_MOTION = 5
    const val FEATURE_SUPER_SLOW_MOTION = 6
    const val FEATURE_PANORAMA = 7
    const val FEATURE_WIDE_SELFIE = 8
    const val FEATURE_PRO_VIDEO = 9
    const val FEATURE_SINGLE_TAKE = 10
    const val FEATURE_DIRECTORS_VIEW = 11
    const val FEATURE_108MP = 12
    const val FEATURE_SUPER_NIGHT = 13
    const val FEATURE_SCENE_DETECTION = 17
    const val FEATURE_AI_SCENE = 20
    const val FEATURE_LIVE_HDR = 21
    const val FEATURE_ZOOM = 22
    const val FEATURE_SPACE_ZOOM = 23
    const val FEATURE_GALAXY_RAW = 24
    const val FEATURE_METERING = 25
    const val FEATURE_COLOR_TEMPERATURE = 30
    const val FEATURE_BURST_SHOT = 31
    const val FEATURE_DYNAMIC_SHOT = 33
    const val FEATURE_PAF = 35
    const val FEATURE_MULTI_FRAME = 36

    // ── Metering modes ──
    const val METERING_CENTER = 0
    const val METERING_FACE = 1
    const val METERING_MULTI = 2
    const val METERING_SPOT = 3
    const val METERING_TOUCH = 4
    const val METERING_AUTO = 5
    const val METERING_MANUAL = 6

    // ── Recording DR modes ──
    const val RECORDING_DR_SDR = "sdr"
    const val RECORDING_DR_3HDR = "3hdr"
    const val RECORDING_DR_HDR10 = "hdr10"

    // ── Safe set helper ──
    fun <T> safeSet(builder: CaptureRequest.Builder, key: CaptureRequest.Key<T>, value: T) {
        try {
            builder.set(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set ${key.name}: ${e.message}")
        }
    }

    // ── Mode display names ──
    fun modeName(mode: Int): String = when (mode) {
        MODE_SINGLE -> "Photo"
        MODE_AUTO -> "Auto"
        MODE_BEAUTY -> "Beauty"
        MODE_VIDEO -> "Video"
        MODE_PANORAMA -> "Panorama"
        MODE_PRO -> "Pro"
        MODE_SELECTIVE_FOCUS -> "Selective Focus"
        MODE_HDR -> "HDR"
        MODE_NIGHT -> "Night"
        MODE_FOOD -> "Food"
        MODE_SLOW_MOTION -> "Slow-Mo"
        MODE_SPORTS -> "Sports"
        MODE_SUPER_SLOW_MOTION -> "Super Slow-Mo"
        MODE_LIVE_FOCUS -> "Live Focus"
        MODE_STICKER -> "Sticker"
        MODE_SUPER_NIGHT -> "Super Night"
        MODE_DIRECTORS_VIEW -> "Director's View"
        MODE_SINGLE_TAKE_PHOTO -> "Single Take"
        MODE_PRO_VIDEO -> "Pro Video"
        MODE_QUICK_TAKE_VIDEO -> "Quick Take"
        MODE_GALAXY_RAW -> "RAW"
        MODE_108MP -> "108MP"
        else -> "Mode_$mode"
    }
}
