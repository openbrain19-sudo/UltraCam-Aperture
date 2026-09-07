package com.ultracam.app.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Raw Camera2 engine with Samsung vendor tag support.
 * Sets SCALER_CROP_REGION + samsung.android.scaler.zoomRatio together
 * on every capture request, exactly like the Samsung Camera app does.
 *
 * The HAL automatically switches physical sensors based on zoom ratio.
 */
class SamsungCameraEngine(private val context: Context) {

    companion object {
        private const val TAG = "SamsungCamEngine"

        // Camera IDs on S20 Ultra
        const val CAMERA_BACK = "0"           // 108MP main S5KRM1
        const val CAMERA_FRONT = "1"          // 40MP front S5KGH1
        const val CAMERA_ULTRA_WIDE = "2"     // 12MP ultra-wide S5K2L3
        const val CAMERA_TELE = "3"           // 48MP telephoto
        const val CAMERA_DEPTH = "4"          // ToF depth
        const val CAMERA_SEAMLESS_ZOOM = "20" // Logical multi-camera (main+ultrawide+tele)

        // Samsung vendor tag strings
        private const val SAMSUNG_SCALER_ZOOM_RATIO = "samsung.android.scaler.zoomRatio"
        private const val SAMSUNG_SHOOTING_MODE = "samsung.android.control.shootingMode"
    }

    data class CameraInfo(
        val id: String,
        val sensorName: String,
        val facing: Int,
        val activeArraySize: Rect,
        val maxDigitalZoom: Float,
        val samsungMaxZoom: Float,
        val focalLengths: FloatArray,
        val apertures: FloatArray,
        val pixelArraySize: Size,
        val fpsRanges: Array<Range<Int>>
    )

    data class CameraState(
        val isInitialized: Boolean = false,
        val isOpen: Boolean = false,
        val isPreviewing: Boolean = false,
        val currentCameraId: String = CAMERA_BACK,
        val zoomRatio: Float = 1.0f,
        val minZoom: Float = 0.5f,
        val maxZoom: Float = 100f,
        val flashMode: Int = CaptureRequest.FLASH_MODE_OFF,
        val availableCameras: List<String> = emptyList(),
        val cameraInfo: Map<String, CameraInfo> = emptyMap()
    )

    private var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var imageReader: ImageReader? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val openCloseLock = Semaphore(1)

    private var textureView: TextureView? = null
    private var activeArray: Rect? = null

    // Samsung vendor tag keys
    @Suppress("UNCHECKED_CAST")
    private val samsungZoomRatioKey = CaptureRequest.Key(
        SAMSUNG_SCALER_ZOOM_RATIO, java.lang.Float::class.java
    ) as CaptureRequest.Key<Float>
    @Suppress("UNCHECKED_CAST")
    private val samsungShootingModeKey = CaptureRequest.Key(
        SAMSUNG_SHOOTING_MODE, java.lang.Integer::class.java
    ) as CaptureRequest.Key<Int>

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private var onImageCaptured: ((ByteArray) -> Unit)? = null

    fun setImageCaptureCallback(callback: (ByteArray) -> Unit) {
        onImageCaptured = callback
    }

    fun initialize() {
        startBackgroundThread()
        enumerateCameras()
    }

    fun shutdown() {
        try {
            openCloseLock.acquire()
            closeCamera()
            stopBackgroundThread()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Shutdown interrupted", e)
        } finally {
            openCloseLock.release()
        }
    }

    fun setTextureView(tv: TextureView) {
        textureView = tv
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("SamsungCamBg").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try { backgroundThread?.join() } catch (_: InterruptedException) {}
        backgroundThread = null
        backgroundHandler = null
    }

    private fun enumerateCameras() {
        val sensorMap = mutableMapOf<String, CameraInfo>()
        val available = mutableListOf<String>()

        for (id in cameraManager.cameraIdList) {
            try {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING) ?: continue
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: Rect(0, 0, 4000, 3000)
                val maxDigitalZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf()
                val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES) ?: floatArrayOf()
                val pixelArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) ?: Size(0, 0)
                val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()

        var samsungMaxZoom = maxDigitalZoom
                try {
                    @Suppress("UNCHECKED_CAST")
                    val samsungZoomKey = CameraCharacteristics.Key(
                        "samsung.android.scaler.availableMaxDigitalZoom", java.lang.Float::class.java
                    ) as CameraCharacteristics.Key<Float>
                    samsungMaxZoom = chars.get(samsungZoomKey) ?: maxDigitalZoom
                } catch (_: Exception) {}

                val sensorName = try {
                    val nameKey = CameraCharacteristics.Key<String>(
                        "samsung.android.sensor.info.sensorName", String::class.java
                    )
                    chars.get(nameKey) ?: "Unknown"
                } catch (_: Exception) { "Camera $id" }

                val info = CameraInfo(
                    id = id, sensorName = sensorName, facing = facing,
                    activeArraySize = activeArray, maxDigitalZoom = maxDigitalZoom,
                    samsungMaxZoom = samsungMaxZoom, focalLengths = focalLengths,
                    apertures = apertures, pixelArraySize = pixelArraySize, fpsRanges = fpsRanges
                )

                sensorMap[id] = info
                available.add(id)
                Log.i(TAG, "Camera $id: $sensorName, facing=$facing, maxZoom=$maxDigitalZoom, samsungMax=$samsungMaxZoom, activeArray=$activeArray")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enumerate camera $id", e)
            }
        }

        _state.value = _state.value.copy(
            isInitialized = true, availableCameras = available, cameraInfo = sensorMap
        )
    }

    @SuppressLint("MissingPermission")
    fun openCamera(cameraId: String = _state.value.currentCameraId, onOpened: (() -> Unit)? = null) {
        if (!openCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
            throw RuntimeException("Timeout waiting to lock camera opening")
        }

        try {
            closeCamera()
            val info = _state.value.cameraInfo[cameraId] ?: run {
                Log.e(TAG, "Camera $cameraId not found")
                openCloseLock.release()
                return
            }
            activeArray = info.activeArraySize

            Log.i(TAG, "Opening camera $cameraId (${info.sensorName}), activeArray=${info.activeArraySize}")

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    openCloseLock.release()
                    cameraDevice = camera
                    _state.value = _state.value.copy(
                        isOpen = true, currentCameraId = cameraId
                    )
                    Log.i(TAG, "Camera $cameraId opened")
                    onOpened?.invoke()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    openCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    _state.value = _state.value.copy(isOpen = false, isPreviewing = false)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    openCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    _state.value = _state.value.copy(isOpen = false, isPreviewing = false)
                    Log.e(TAG, "Camera $cameraId error: $error")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera $cameraId", e)
            openCloseLock.release()
        }
    }

    fun createPreviewSession(onReady: (() -> Unit)? = null) {
        val camera = cameraDevice ?: return
        val tv = textureView ?: return

        try {
            val st = tv.surfaceTexture ?: return
            st.setDefaultBufferSize(1920, 1080)
            val surface = Surface(st)

            // Set up image reader for capture
            val info = _state.value.cameraInfo[_state.value.currentCameraId]
            val captureSize = info?.let {
                val map = cameraManager.getCameraCharacteristics(it.id)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                map?.getOutputSizes(android.graphics.ImageFormat.JPEG)
                    ?.maxByOrNull { it.width * it.height }
            } ?: Size(4000, 3000)

            imageReader?.close()
            imageReader = ImageReader.newInstance(
                1920, 1080, android.graphics.ImageFormat.JPEG, 2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        onImageCaptured?.invoke(bytes)
                    } finally {
                        image.close()
                    }
                }, backgroundHandler)
            }

            previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                imageReader?.surface?.let { addTarget(it) }

                // Auto settings
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

                // Samsung tags
                set(samsungShootingModeKey, 0) // Photo mode
                set(samsungZoomRatioKey, _state.value.zoomRatio)

                // SCALER_CROP_REGION
                activeArray?.let { rect ->
                    set(CaptureRequest.SCALER_CROP_REGION, calculateCropRegion(rect, _state.value.zoomRatio))
                }
            }

            val surfaces = mutableListOf(surface)
            imageReader?.surface?.let { surfaces.add(it) }

            camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    _state.value = _state.value.copy(isPreviewing = true)
                    startPreview()
                    onReady?.invoke()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Preview session config failed")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create preview session", e)
        }
    }

    private fun startPreview() {
        val session = captureSession ?: return
        val builder = previewBuilder ?: return
        try {
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
            Log.i(TAG, "Preview started, zoom=${_state.value.zoomRatio}")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to start preview", e)
        }
    }

    fun setZoom(zoomRatio: Float) {
        val newZoom = zoomRatio.coerceIn(_state.value.minZoom, _state.value.maxZoom)
        _state.value = _state.value.copy(zoomRatio = newZoom)

        val builder = previewBuilder ?: return
        val session = captureSession ?: return
        val rect = activeArray ?: return

        try {
            // Set BOTH Samsung zoom ratio AND SCALER_CROP_REGION together
            // This is exactly what the Samsung Camera app does
            builder.set(samsungZoomRatioKey, newZoom)
            builder.set(CaptureRequest.SCALER_CROP_REGION, calculateCropRegion(rect, newZoom))

            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
            Log.d(TAG, "Zoom: ${newZoom}x, cropRegion=${calculateCropRegion(rect, newZoom)}")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to set zoom", e)
        }
    }

    /**
     * Calculate SCALER_CROP_REGION from zoom ratio.
     * This is the same calculation Samsung Camera uses.
     */
    private fun calculateCropRegion(activeArray: Rect, zoomRatio: Float): Rect {
        if (zoomRatio <= 1.0f) return activeArray

        val w = activeArray.width()
        val h = activeArray.height()
        val offsetX = ((w - (w / zoomRatio)) / 2).toInt()
        val offsetY = ((h - (h / zoomRatio)) / 2).toInt()

        return Rect(
            (activeArray.left + offsetX).coerceAtLeast(activeArray.left),
            (activeArray.top + offsetY).coerceAtLeast(activeArray.top),
            (activeArray.right - offsetX).coerceAtMost(activeArray.right),
            (activeArray.bottom - offsetY).coerceAtMost(activeArray.bottom)
        )
    }

    fun setFlashMode(mode: Int) {
        _state.value = _state.value.copy(flashMode = mode)
        val builder = previewBuilder ?: return
        val session = captureSession ?: return

        try {
            builder.set(CaptureRequest.FLASH_MODE, mode)
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to set flash mode", e)
        }
    }

    fun takePicture() {
        val camera = cameraDevice ?: return
        val session = captureSession ?: return

        try {
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                imageReader?.surface?.let { addTarget(it) }

                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                set(CaptureRequest.JPEG_QUALITY, 95.toByte())

            // Samsung tags on capture
            set(samsungShootingModeKey, 0)
            set(samsungZoomRatioKey, _state.value.zoomRatio)

            // SCALER_CROP_REGION on capture
            activeArray?.let { rect ->
                set(CaptureRequest.SCALER_CROP_REGION, calculateCropRegion(rect, _state.value.zoomRatio))
            }
            }

            session.capture(builder.build(), null, backgroundHandler)
            Log.i(TAG, "Picture taken at zoom=${_state.value.zoomRatio}x")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to take picture", e)
        }
    }

    fun switchCamera() {
        val cameras = _state.value.availableCameras
        val currentIdx = cameras.indexOf(_state.value.currentCameraId)
        val nextIdx = (currentIdx + 1) % cameras.size
        val nextCamera = cameras[nextIdx]

        openCamera(nextCamera) {
            textureView?.let { createPreviewSession() }
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        imageReader?.close()
        imageReader = null
        cameraDevice?.close()
        cameraDevice = null
        _state.value = _state.value.copy(isOpen = false, isPreviewing = false)
    }
}
