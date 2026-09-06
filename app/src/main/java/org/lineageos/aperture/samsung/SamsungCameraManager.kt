package org.lineageos.aperture.samsung

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log

/**
 * Manages Samsung-specific camera features on the S20 Ultra running LineageOS.
 * Detects vendor tag availability, reads Samsung characteristics,
 * and provides the bridge between Aperture and the Samsung vendor HAL.
 */
class SamsungCameraManager(context: Context) {

    companion object {
        private const val TAG = "SamsungCameraMgr"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    data class SamsungCameraInfo(
        val cameraId: String,
        val sensorName: String,
        val availableFeatures: Set<Int>,
        val hasHdr: Boolean,
        val hasBeauty: Boolean,
        val hasBokeh: Boolean,
        val hasNight: Boolean,
        val hasFood: Boolean,
        val hasSlowMotion: Boolean,
        val hasSuperNight: Boolean,
        val has108MP: Boolean,
        val hasSpaceZoom: Boolean,
        val hasProVideo: Boolean,
        val hasSingleTake: Boolean,
        val hasDirectorsView: Boolean,
        val hasSceneDetection: Boolean,
        val hasLiveHdr: Boolean,
        val hasBurstShot: Boolean,
        val hasGalaxyRaw: Boolean,
        val hasMetering: Boolean,
        val hasColorTemperature: Boolean,
        val hasMultiFrame: Boolean,
        val hasPaf: Boolean
    )

    private val cameraInfoCache = mutableMapOf<String, SamsungCameraInfo>()

    /**
     * Query Samsung vendor tag availability for a camera ID.
     */
    fun getSamsungInfo(cameraId: String): SamsungCameraInfo? {
        cameraInfoCache[cameraId]?.let { return it }

        return try {
            val chars = cameraManager.getCameraCharacteristics(cameraId)
            val features = chars.get(SamsungVendorKeys.KEY_AVAILABLE_FEATURES) ?: intArrayOf()
            val sensorName = chars.get(SamsungVendorKeys.KEY_SENSOR_NAME) ?: "Unknown"
            val featureSet = features.toSet()

            val info = SamsungCameraInfo(
                cameraId = cameraId,
                sensorName = sensorName,
                availableFeatures = featureSet,
                hasHdr = SamsungVendorKeys.FEATURE_HDR in featureSet,
                hasBeauty = SamsungVendorKeys.FEATURE_BEAUTY in featureSet,
                hasBokeh = SamsungVendorKeys.FEATURE_BOKEH in featureSet,
                hasNight = SamsungVendorKeys.FEATURE_NIGHT in featureSet,
                hasFood = SamsungVendorKeys.FEATURE_FOOD in featureSet,
                hasSlowMotion = SamsungVendorKeys.FEATURE_SLOW_MOTION in featureSet,
                hasSuperNight = SamsungVendorKeys.FEATURE_SUPER_NIGHT in featureSet,
                has108MP = SamsungVendorKeys.FEATURE_108MP in featureSet,
                hasSpaceZoom = SamsungVendorKeys.FEATURE_ZOOM in featureSet ||
                        SamsungVendorKeys.FEATURE_SPACE_ZOOM in featureSet,
                hasProVideo = SamsungVendorKeys.FEATURE_PRO_VIDEO in featureSet,
                hasSingleTake = SamsungVendorKeys.FEATURE_SINGLE_TAKE in featureSet,
                hasDirectorsView = SamsungVendorKeys.FEATURE_DIRECTORS_VIEW in featureSet,
                hasSceneDetection = SamsungVendorKeys.FEATURE_SCENE_DETECTION in featureSet ||
                        SamsungVendorKeys.FEATURE_AI_SCENE in featureSet,
                hasLiveHdr = SamsungVendorKeys.FEATURE_LIVE_HDR in featureSet,
                hasBurstShot = SamsungVendorKeys.FEATURE_BURST_SHOT in featureSet,
                hasGalaxyRaw = SamsungVendorKeys.FEATURE_GALAXY_RAW in featureSet,
                hasMetering = SamsungVendorKeys.FEATURE_METERING in featureSet,
                hasColorTemperature = SamsungVendorKeys.FEATURE_COLOR_TEMPERATURE in featureSet,
                hasMultiFrame = SamsungVendorKeys.FEATURE_MULTI_FRAME in featureSet,
                hasPaf = SamsungVendorKeys.FEATURE_PAF in featureSet
            )

            cameraInfoCache[cameraId] = info
            Log.i(TAG, "Samsung info for camera $cameraId: sensor=$sensorName, " +
                    "features=[${featureSet.joinToString()}]")

            info
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Samsung info for camera $cameraId", e)
            null
        }
    }

    /**
     * Dump all Samsung vendor tags for all cameras.
     */
    fun dumpAllSamsungTags() {
        Log.i(TAG, "=== Samsung Vendor Tag Dump ===")
        for (id in cameraManager.cameraIdList) {
            val info = getSamsungInfo(id) ?: continue
            Log.i(TAG, "Camera $id (${info.sensorName}):")
            Log.i(TAG, "  Features: ${info.availableFeatures.joinToString()}")
            Log.i(TAG, "  HDR=${info.hasHdr} Beauty=${info.hasBeauty} " +
                    "Bokeh=${info.hasBokeh} Night=${info.hasNight} " +
                    "Food=${info.hasFood} 108MP=${info.has108MP} " +
                    "SpaceZoom=${info.hasSpaceZoom} SuperNight=${info.hasSuperNight} " +
                    "SceneDetect=${info.hasSceneDetection} LiveHdr=${info.hasLiveHdr} " +
                    "ProVideo=${info.hasProVideo} SingleTake=${info.hasSingleTake} " +
                    "DirectorsView=${info.hasDirectorsView} BurstShot=${info.hasBurstShot} " +
                    "GalaxyRaw=${info.hasGalaxyRaw} Metering=${info.hasMetering} " +
                    "ColorTemp=${info.hasColorTemperature} MultiFrame=${info.hasMultiFrame} " +
                    "PAF=${info.hasPaf}")
        }
    }

    /**
     * Map a Samsung shooting mode to the best available equivalent.
     */
    fun resolveShootingMode(
        desiredMode: Int,
        info: SamsungCameraInfo?
    ): Int {
        if (info == null) return SamsungVendorKeys.MODE_SINGLE

        return when (desiredMode) {
            SamsungVendorKeys.MODE_BEAUTY ->
                if (info.hasBeauty) desiredMode else SamsungVendorKeys.MODE_SINGLE
            SamsungVendorKeys.MODE_NIGHT ->
                if (info.hasNight) desiredMode else SamsungVendorKeys.MODE_SINGLE
            SamsungVendorKeys.MODE_SUPER_NIGHT ->
                if (info.hasSuperNight) desiredMode
                else if (info.hasNight) SamsungVendorKeys.MODE_NIGHT
                else SamsungVendorKeys.MODE_SINGLE
            SamsungVendorKeys.MODE_HDR ->
                if (info.hasHdr) desiredMode else SamsungVendorKeys.MODE_SINGLE
            SamsungVendorKeys.MODE_FOOD ->
                if (info.hasFood) desiredMode else SamsungVendorKeys.MODE_SINGLE
            SamsungVendorKeys.MODE_LIVE_FOCUS ->
                if (info.hasBokeh) desiredMode else SamsungVendorKeys.MODE_SINGLE
            SamsungVendorKeys.MODE_SLOW_MOTION ->
                if (info.hasSlowMotion) desiredMode else SamsungVendorKeys.MODE_VIDEO
            SamsungVendorKeys.MODE_SINGLE_TAKE_PHOTO ->
                if (info.hasSingleTake) desiredMode else SamsungVendorKeys.MODE_SINGLE
            SamsungVendorKeys.MODE_DIRECTORS_VIEW ->
                if (info.hasDirectorsView) desiredMode else SamsungVendorKeys.MODE_VIDEO
            SamsungVendorKeys.MODE_PRO_VIDEO ->
                if (info.hasProVideo) desiredMode else SamsungVendorKeys.MODE_VIDEO
            SamsungVendorKeys.MODE_GALAXY_RAW ->
                if (info.hasGalaxyRaw) desiredMode else SamsungVendorKeys.MODE_SINGLE
            else -> desiredMode
        }
    }

    /**
     * Get supported Samsung modes for this camera.
     */
    fun getSupportedModes(info: SamsungCameraInfo?): List<Int> {
        if (info == null) return listOf(SamsungVendorKeys.MODE_SINGLE)

        val modes = mutableListOf(SamsungVendorKeys.MODE_SINGLE)
        if (info.has108MP) modes.add(SamsungVendorKeys.MODE_108MP)
        if (info.hasBeauty) modes.add(SamsungVendorKeys.MODE_BEAUTY)
        if (info.hasNight) modes.add(SamsungVendorKeys.MODE_NIGHT)
        if (info.hasSuperNight) modes.add(SamsungVendorKeys.MODE_SUPER_NIGHT)
        modes.add(SamsungVendorKeys.MODE_PRO)
        if (info.hasHdr) modes.add(SamsungVendorKeys.MODE_HDR)
        if (info.hasFood) modes.add(SamsungVendorKeys.MODE_FOOD)
        if (info.hasBokeh) modes.add(SamsungVendorKeys.MODE_LIVE_FOCUS)
        modes.add(SamsungVendorKeys.MODE_VIDEO)
        if (info.hasProVideo) modes.add(SamsungVendorKeys.MODE_PRO_VIDEO)
        if (info.hasSlowMotion) modes.add(SamsungVendorKeys.MODE_SLOW_MOTION)
        if (info.hasSingleTake) modes.add(SamsungVendorKeys.MODE_SINGLE_TAKE_PHOTO)
        if (info.hasDirectorsView) modes.add(SamsungVendorKeys.MODE_DIRECTORS_VIEW)
        if (info.hasGalaxyRaw) modes.add(SamsungVendorKeys.MODE_GALAXY_RAW)
        return modes
    }
}
