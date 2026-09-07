package org.lineageos.aperture.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Raw Camera2 engine matching Samsung Camera app behavior exactly.
 * 
 * Samsung Camera flow:
 * 1. CameraManager.openCamera() 
 * 2. cameraDevice.setParameters("first-entrance=true;samsungcamera=true;...")
 * 3. Create capture session with preview surface
 * 4. Set SCALER_CROP_REGION + samsung.android.scaler.zoomRatio on every request
 * 5. HAL automatically switches physical sensors based on zoom ratio
 */
class SamsungCameraEngine(private val context: Context) {

    companion object {
        private const val TAG = "SamsungCamEngine"

        // Samsung Camera uses these camera IDs
        const val CAMERA_BACK = "0"       // 108MP main
        const val CAMERA_FRONT = "1"      // Front
        const val CAMERA_ULTRA_WIDE = "2" // Ultra-wide
        const val CAMERA_TELE = "3"       // Telephoto
        const val CAMERA_SEAMLESS = "20"  // Logical multi-camera (Samsung's seamless zoom)

        // Samsung vendor tag strings (from decompiled Samsung Camera app)
        private const val SEM_SCALER_ZOOM_RATIO = "samsung.android.scaler.zoomRatio"
        private const val SEM_SHOOTING_MODE = "samsung.android.control.shootingMode"
    }

    data class CameraState(
        val isOpen: Boolean = false,
        val isPreviewing: Boolean = false,
        val currentCameraId: String = CAMERA_BACK,
        val zoomRatio: Float = 1.0f,
        val minZoom: Float = 1.0f,
        val maxZoom: Float = 100f,
        val flashMode: Int = CameraCharacteristics.CONTROL_AE_MODE_ON,
        val statusMessage: String = "",
        val activeArray: Rect? = null,
        val samsungMaxZoom: Float = 8.0f,
        val aospMaxZoom: Float = 8.0f
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

    // Samsung vendor tag keys
    @Suppress("UNCHECKED_CAST")
    private val samsungZoomKey = CaptureRequest.Key(
        SEM_SCALER_ZOOM_RATIO, java.lang.Float::class.java
    ) as CaptureRequest.Key<Float>
    @Suppress("UNCHECKED_CAST")
    private val samsungShootingModeKey = CaptureRequest.Key(
        SEM_SHOOTING_MODE, java.lang.Integer::class.java
    ) as CaptureRequest.Key<Int>

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private var onImageCaptured: ((ByteArray) -> Unit)? = null

    fun setImageCaptureCallback(callback: (ByteArray) -> Unit) {
        onImageCaptured = callback
    }

    fun setTextureView(tv: TextureView) {
        textureView = tv
    }

    fun initialize() {
        startBackgroundThread()
        probeCamera()
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

    /**
     * Probe camera 0 to get active array size and zoom limits.
     */
    private fun probeCamera() {
        try {
            val chars = cameraManager.getCameraCharacteristics(CAMERA_BACK)
            val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            val aospMaxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 8.0f

            // Try Samsung max zoom
            var samsungMaxZoom = aospMaxZoom
            try {
                @Suppress("UNCHECKED_CAST")
                val key = CameraCharacteristics.Key(
                    "samsung.android.scaler.availableMaxDigitalZoom", java.lang.Float::class.java
                ) as CameraCharacteristics.Key<Float>
                samsungMaxZoom = chars.get(key) ?: aospMaxZoom
            } catch (_: Exception) {}

            Log.i(TAG, "Camera $CAMERA_BACK: activeArray=$activeArray, aospMax=$aospMaxZoom, samsungMax=$samsungMaxZoom")

            _state.value = _state.value.copy(
                activeArray = activeArray,
                aospMaxZoom = aospMaxZoom,
                samsungMaxZoom = samsungMaxZoom,
                maxZoom = samsungMaxZoom
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to probe camera", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(cameraId: String = CAMERA_BACK, onOpened: (() -> Unit)? = null) {
        if (!openCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
            _state.value = _state.value.copy(statusMessage = "Timeout opening camera")
            return
        }

        try {
            closeCamera()
            _state.value = _state.value.copy(statusMessage = "Opening camera $cameraId...")

            Log.i(TAG, "Opening camera $cameraId")

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "Camera $cameraId opened successfully")
                    cameraDevice = camera
                    _state.value = _state.value.copy(
                        isOpen = true,
                        currentCameraId = cameraId,
                        statusMessage = "Camera opened"
                    )

                    // Step 2: Samsung setParameters initialization
                    // This tells the HAL "I am Samsung Camera app"
                    initSamsungHAL(camera)

                    openCloseLock.release()
                    onOpened?.invoke()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    openCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    _state.value = _state.value.copy(
                        isOpen = false, isPreviewing = false,
                        statusMessage = "Camera disconnected"
                    )
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    openCloseLock.release()
                    camera.close()
                    cameraDevice = null
                    _state.value = _state.value.copy(
                        isOpen = false, isPreviewing = false,
                        statusMessage = "Camera error: $error (HAL rejected access)"
                    )
                    Log.e(TAG, "Camera $cameraId error: $error")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            openCloseLock.release()
            _state.value = _state.value.copy(statusMessage = "Failed: ${e.message}")
            Log.e(TAG, "Failed to open camera $cameraId", e)
        }
    }

    /**
     * Initialize Samsung HAL via setParameters.
     * This is the critical step that tells the HAL to accept Samsung vendor tags.
     */
    private fun initSamsungHAL(camera: CameraDevice) {
        // Try setParameters via reflection
        try {
            val setParams = camera.javaClass.getMethod("setParameters", String::class.java)
            val params = "first-entrance=true;samsungcamera=true;factorytest=false;" +
                    "shootingmode=0;recording-fps=0;sw-vdis=false;" +
                    "video-beautyface=false;vtmode=0;operation_mode=none;" +
                    "ssm_shot_mode=0;recording_dr_mode=sdr;sw-super_vdis=false;stream_type=0"
            setParams.invoke(camera, params)
            Log.i(TAG, "Samsung HAL initialized via setParameters")
            _state.value = _state.value.copy(statusMessage = "Samsung HAL ready")
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "setParameters method not found, trying alternatives...")
            // Try other method signatures Samsung might use
            tryAlternativeInit(camera)
        } catch (e: Exception) {
            Log.w(TAG, "setParameters failed: ${e.message}")
            tryAlternativeInit(camera)
        }
    }

    private fun tryAlternativeInit(camera: CameraDevice) {
        // List all available methods on CameraDevice
        try {
            val methods = camera.javaClass.declaredMethods
            for (m in methods) {
                Log.d(TAG, "CameraDevice method: ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})")
            }
        } catch (_: Exception) {}

        // Try sendCommand if available
        try {
            val sendCommand = camera.javaClass.getMethod("sendCommand", String::class.java)
            sendCommand.invoke(camera, "first-entrance=true;samsungcamera=true;shootingmode=0")
            Log.i(TAG, "Samsung HAL initialized via sendCommand")
        } catch (_: Exception) {}
    }

    /**
     * Create preview session. Step 3 in Samsung Camera flow.
     */
    fun createPreviewSession(onReady: (() -> Unit)? = null) {
        val camera = cameraDevice ?: return
        val tv = textureView ?: return

        try {
            val st = tv.surfaceTexture ?: return
            st.setDefaultBufferSize(1920, 1080)
            val surface = Surface(st)

            // Set up image reader for capture
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

            // Step 4: Build preview request with Samsung tags
            previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                imageReader?.surface?.let { addTarget(it) }

                // Standard Camera2 settings
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO)

                // Samsung vendor tags - CRITICAL
                set(samsungShootingModeKey, 0) // Photo mode
                set(samsungZoomKey, 1.0f)

                // SCALER_CROP_REGION - Samsung needs this set explicitly
                _state.value.activeArray?.let { rect ->
                    set(CaptureRequest.SCALER_CROP_REGION, rect)
                }
            }

            val surfaces = mutableListOf(surface)
            imageReader?.surface?.let { surfaces.add(it) }

            camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    _state.value = _state.value.copy(isPreviewing = true, statusMessage = "Preview active")
                    startPreview()
                    onReady?.invoke()
                    Log.i(TAG, "Preview session configured")
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    _state.value = _state.value.copy(statusMessage = "Session config FAILED")
                    Log.e(TAG, "Preview session config failed")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            _state.value = _state.value.copy(statusMessage = "Session failed: ${e.message}")
            Log.e(TAG, "Failed to create preview session", e)
        }
    }

    private fun startPreview() {
        val session = captureSession ?: return
        val builder = previewBuilder ?: return
        try {
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
            Log.i(TAG, "Preview started, zoom=${_state.value.zoomRatio}x")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to start preview", e)
        }
    }

    /**
     * Set zoom. This is the key function - sets BOTH SCALER_CROP_REGION + samsung.zoomRatio.
     * This matches exactly what Samsung Camera's ZoomController does.
     */
    fun setZoom(zoomRatio: Float) {
        val newZoom = zoomRatio.coerceIn(_state.value.minZoom, _state.value.maxZoom)
        _state.value = _state.value.copy(zoomRatio = newZoom)

        val builder = previewBuilder ?: return
        val session = captureSession ?: return
        val activeArray = _state.value.activeArray ?: return

        try {
            // Calculate SCALER_CROP_REGION (same calculation as Samsung Camera)
            val cropRegion = calculateCropRegion(activeArray, newZoom)

            // Set BOTH tags together - Samsung Camera does this
            builder.set(samsungZoomKey, newZoom)
            builder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)

            // Also set standard zoom ratio for Camera2 compatibility
            builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, newZoom)

            session.setRepeatingRequest(builder.build(), null, backgroundHandler)

            Log.i(TAG, "Zoom: ${newZoom}x, cropRegion=$cropRegion, " +
                    "activeArray=$activeArray")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to set zoom", e)
        }
    }

    /**
     * Calculate SCALER_CROP_REGION from zoom ratio.
     * Exact same calculation as Samsung Camera's ZoomController.getScalerCropRegion().
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
            builder.set(CaptureRequest.CONTROL_AE_MODE, mode)
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

                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO)
                set(CaptureRequest.JPEG_QUALITY, 95.toByte())

                // Samsung tags on capture
                set(samsungShootingModeKey, 0)
                set(samsungZoomKey, _state.value.zoomRatio)

                // SCALER_CROP_REGION on capture
                _state.value.activeArray?.let { rect ->
                    set(CaptureRequest.SCALER_CROP_REGION, calculateCropRegion(rect, _state.value.zoomRatio))
                }
            }

            session.capture(builder.build(), null, backgroundHandler)
            Log.i(TAG, "Picture taken at zoom=${_state.value.zoomRatio}x")
            _state.value = _state.value.copy(statusMessage = "Photo captured!")
        } catch (e: CameraAccessException) {
            _state.value = _state.value.copy(statusMessage = "Capture failed: ${e.message}")
            Log.e(TAG, "Failed to take picture", e)
        }
    }

    fun switchCamera() {
        val current = _state.value.currentCameraId
        val next = when (current) {
            CAMERA_BACK -> CAMERA_ULTRA_WIDE
            CAMERA_ULTRA_WIDE -> CAMERA_TELE
            CAMERA_TELE -> CAMERA_BACK
            else -> CAMERA_BACK
        }
        openCamera(next) { createPreviewSession() }
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
