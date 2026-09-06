package org.lineageos.aperture.ext

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.CaptureRequestOptions
import org.lineageos.aperture.samsung.SamsungVendorKeys

/**
 * Samsung vendor tag extension functions for CaptureRequestOptions.Builder.
 *
 * These use the existing setOrClearCaptureRequestOption pattern to inject
 * Samsung vendor tags into the Camera2 capture request pipeline.
 * The HAL (camera.exynos990.so) processes these keys directly.
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungShootingMode(mode: Int?): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_SHOOTING_MODE, mode)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungLiveHdr(enabled: Boolean): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(
        SamsungVendorKeys.KEY_LIVE_HDR_MODE,
        if (enabled) 1 else 0
    )

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungBeautyRetouch(level: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_BEAUTY_FACE_RETOUCH_LEVEL, level)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungBokehBlur(strength: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_BOKEH_BLUR_STRENGTH, strength)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungSuperNight(mode: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_SUPER_NIGHT_SHOT_MODE, mode)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungMetering(mode: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_METERING_MODE, mode)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungColorTemperature(temp: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_COLOR_TEMPERATURE, temp)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungWbLevel(level: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_WB_LEVEL, level)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungZoomRatio(ratio: Float): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_ZOOM_RATIO, ratio)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungFlip(mode: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_FLIP_MODE, mode)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungVideoBeauty(enabled: Boolean): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_VIDEO_BEAUTY_FACE, enabled)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungRecordingMaxFps(fps: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_RECORDING_MAX_FPS, fps)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungRecordingDrMode(mode: String?): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_RECORDING_DR_MODE, mode)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungSwVideoStabilization(mode: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_SW_VIDEO_STABILIZATION, mode)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungSwSuperVideoStabilization(enabled: Boolean): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_SW_SUPER_VIDEO_STABILIZATION, enabled)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungBurstShotFps(fps: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_BURST_SHOT_FPS, fps)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungCaptureHint(hint: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_CAPTURE_HINT, hint)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungSceneDetection(enabled: Boolean): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(
        SamsungVendorKeys.KEY_LIGHT_CONDITION_ENABLE_MODE,
        if (enabled) 1 else 0
    )

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungCompositionGuide(mode: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_COMPOSITION_GUIDE_MODE, mode)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungEventFinder(mode: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_EVENT_FINDER_MODE, mode)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungStreamType(type: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_SENSOR_STREAM_TYPE, type)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungDepthFilter(type: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_DEPTH_FILTER_TYPE, type)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungDynamicShotHint(hint: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_DYNAMIC_SHOT_HINT, hint)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungRecordingTrigger(trigger: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_RECORDING_TRIGGER, trigger)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setSamsungAeExtraMode(mode: Int): CaptureRequestOptions.Builder =
    setOrClearCaptureRequestOption(SamsungVendorKeys.KEY_AE_EXTRA_MODE, mode)
