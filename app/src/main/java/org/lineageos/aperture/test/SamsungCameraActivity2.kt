package org.lineageos.aperture.test

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.lineageos.aperture.camera.SamsungCameraEngine
import kotlin.math.sqrt

/**
 * Raw Camera2 camera activity matching Samsung Camera behavior.
 * No CameraX - direct Camera2 API with Samsung vendor tags.
 */
class SamsungCameraActivity2 : Activity() {
    companion object {
        private const val TAG = "UltraCam-Samsung2"
        private const val REQUEST_CAMERA = 100
    }

    private lateinit var engine: SamsungCameraEngine
    private lateinit var viewfinder: TextureView
    private lateinit var zoomText: TextView
    private lateinit var statusText: TextView
    private lateinit var zoomSlider: android.widget.SeekBar

    private var currentZoom = 1.0f
    private var lastSpan = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        engine = SamsungCameraEngine(applicationContext)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF000000.toInt())
        }

        viewfinder = TextureView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(viewfinder)

        // Status bar - shows zoom limits, camera info, errors
        statusText = TextView(this).apply {
            text = "Initializing..."
            setTextColor(0xFF4CAF50.toInt())
            textSize = 11f
            setPadding(16, 4, 16, 4)
            setBackgroundColor(0x33000000)
        }
        root.addView(statusText)

        zoomText = TextView(this).apply {
            text = "Zoom: 1.0x"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setPadding(16, 8, 16, 8)
        }
        root.addView(zoomText)

        zoomSlider = android.widget.SeekBar(this).apply {
            max = 10000
            progress = 1000
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentZoom = progressToZoom(progress.toFloat())
                        engine.setZoom(currentZoom)
                        updateZoomDisplay()
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }
        root.addView(zoomSlider)

        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
        }
        floatArrayOf(0.5f, 1f, 2f, 4f, 10f, 30f, 100f).forEach { zoom ->
            Button(this).apply {
                text = if (zoom < 1) "${zoom}x" else "${zoom.toInt()}x"
                textSize = 11f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x33FFFFFF)
                setPadding(12, 4, 12, 4)
                setOnClickListener {
                    currentZoom = zoom
                    engine.setZoom(zoom)
                    zoomSlider.progress = zoomToProgress(zoom)
                    updateZoomDisplay()
                }
            }.also { presetRow.addView(it) }
        }
        root.addView(presetRow)

        Button(this).apply {
            text = "CAPTURE"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFE94560.toInt())
            setOnClickListener { engine.takePicture() }
        }.also { root.addView(it) }

        val flashButton = Button(this).apply {
            text = "Flash OFF"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x40FFFFFF)
            setOnClickListener {
                val currentFlash = engine.state.value.flashMode
                val newFlash = if (currentFlash == android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON)
                    android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH
                else android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
                engine.setFlashMode(newFlash)
                text = if (newFlash == android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH) "Flash AUTO" else "Flash OFF"
            }
        }
        root.addView(flashButton)

        Button(this).apply {
            text = "Switch Camera"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x40FFFFFF)
            setOnClickListener { engine.switchCamera() }
        }.also { root.addView(it) }

        setContentView(root)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
            return
        }
        startCamera()
    }

    private fun startCamera() {
        engine.initialize()
        engine.setTextureView(viewfinder)

        // Observe engine state for status updates
        val handler = android.os.Handler(mainLooper)
        val checkState = object : Runnable {
            override fun run() {
                val s = engine.state.value
                updateStatusFromState(s)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(checkState)

        viewfinder.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                engine.openCamera(SamsungCameraEngine.CAMERA_BACK) {
                    engine.createPreviewSession()
                }
            }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }

        // Pinch to zoom
        viewfinder.setOnTouchListener { _, event ->
            if (event.pointerCount >= 2) {
                val dx = event.getX(0) - event.getX(1)
                val dy = event.getY(0) - event.getY(1)
                val span = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (lastSpan > 0) {
                    val scaleFactor = span / lastSpan
                    currentZoom = (currentZoom * scaleFactor).coerceIn(
                        engine.state.value.minZoom,
                        engine.state.value.maxZoom
                    )
                    engine.setZoom(currentZoom)
                    updateZoomDisplay()
                    zoomSlider.progress = zoomToProgress(currentZoom)
                }
                lastSpan = span
            } else {
                lastSpan = 0f
            }
            true
        }
    }

    private fun updateStatusFromState(s: SamsungCameraEngine.CameraState) {
        val lines = mutableListOf<String>()
        lines.add("Camera: ${s.currentCameraId} | Zoom: ${s.samsungMaxZoom}x max (Samsung) / ${s.aospMaxZoom}x (AOSP)")
        lines.add("Active array: ${s.activeArray}")
        lines.add("Status: ${s.statusMessage}")

        if (s.activeArray != null) {
            val crop = calculateCropRegion(s.activeArray, currentZoom)
            lines.add("Crop region at ${currentZoom}x: $crop")
            lines.add("Crop size: ${crop.width()}x${crop.height()} (${crop.width() * crop.height() / 1000}K pixels)")
        }

        if (currentZoom > s.aospMaxZoom) {
            lines.add("WARNING: Zoom ${currentZoom}x exceeds AOSP max ${s.aospMaxZoom}x")
            lines.add("Samsung tag set to ${currentZoom}x - HAL should process this")
        }

        statusText.text = lines.joinToString("\n")
    }

    private fun updateZoomDisplay() {
        val s = engine.state.value
        zoomText.text = "Zoom: ${"%.1f".format(currentZoom)}x (${s.currentCameraId})"
    }

    private fun calculateCropRegion(activeArray: android.graphics.Rect, zoomRatio: Float): android.graphics.Rect {
        if (zoomRatio <= 1.0f) return activeArray
        val w = activeArray.width()
        val h = activeArray.height()
        val offsetX = ((w - (w / zoomRatio)) / 2).toInt()
        val offsetY = ((h - (h / zoomRatio)) / 2).toInt()
        return android.graphics.Rect(
            (activeArray.left + offsetX).coerceAtLeast(activeArray.left),
            (activeArray.top + offsetY).coerceAtLeast(activeArray.top),
            (activeArray.right - offsetX).coerceAtMost(activeArray.right),
            (activeArray.bottom - offsetY).coerceAtMost(activeArray.bottom)
        )
    }

    private fun progressToZoom(progress: Float): Float {
        val logMin = Math.log(0.5)
        val logMax = Math.log(100.0)
        return Math.exp(logMin + progress / 10000.0 * (logMax - logMin)).toFloat()
    }

    private fun zoomToProgress(zoom: Float): Int {
        val logMin = Math.log(0.5)
        val logMax = Math.log(100.0)
        val logZoom = Math.log(zoom.toDouble().coerceIn(0.5, 100.0))
        return ((logZoom - logMin) / (logMax - logMin) * 10000).toInt().coerceIn(0, 10000)
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<String>, grants: IntArray) {
        if (req == REQUEST_CAMERA && grants.isNotEmpty() && grants[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.shutdown()
    }
}
