package com.unimagdalena.assistivevision.modules

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureFailure
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.ArrayDeque

/**
 * Camera2 capture module.
 *
 * CameraX only exposes the cameras returned by [CameraManager.getCameraIdList].
 * On some Xiaomi devices that list contains only the 1x back camera and the
 * front camera; the 0.5x ultra-wide is still queryable and openable by its
 * hidden physical camera id (usually `2`). This module probes those hidden ids
 * and opens the widest back camera directly, falling back to the normal public
 * back camera when the hidden one cannot be opened.
 *
 * Preview uses a TextureView (smooth camera stream). Detection uses a JPEG
 * still capture on demand, which avoids relying on YUV ImageReader output from
 * physical cameras that may not support it.
 */
class CaptureModule(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraThread = HandlerThread("CaptureModule").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var textureView: TextureView? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var jpegReader: ImageReader? = null
    private var yuvReader: ImageReader? = null

    private var currentChoice: CameraChoice? = null
    private val pendingFallbacks = ArrayDeque<CameraChoice>()
    private var isOpening = false

    private var sensorOrientation = 90
    private var stillSize: Size? = null
    private var isCapturingStill = false
    private var pendingStillCallback: ((Bitmap) -> Unit)? = null

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            openWhenReady()
        }

        override fun onStop(owner: LifecycleOwner) {
            closeCamera()
        }
    }

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: android.graphics.SurfaceTexture,
            width: Int,
            height: Int
        ) {
            openWhenReady()
        }

        override fun onSurfaceTextureSizeChanged(
            surface: android.graphics.SurfaceTexture,
            width: Int,
            height: Int
        ) = Unit

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            closeCamera()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) = Unit
    }

    fun start(textureView: TextureView) {
        this.textureView = textureView
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        textureView.surfaceTextureListener = surfaceListener
        if (textureView.isAvailable) {
            openWhenReady()
        }
    }

    /**
     * Captures a JPEG still from the active camera and delivers a rotated
     * bitmap on the main thread. Ignored while another still is in flight.
     */
    fun captureStill(onCaptured: (Bitmap) -> Unit) {
        val device = cameraDevice
        val choice = currentChoice
        val size = stillSize
        if (device == null || choice == null || size == null) return
        if (isCapturingStill) return

        isCapturingStill = true
        pendingStillCallback = onCaptured

        val reader = ImageReader.newInstance(
            size.width,
            size.height,
            ImageFormat.JPEG,
            1
        ).also {
            it.setOnImageAvailableListener(::onJpegAvailable, cameraHandler)
        }
        jpegReader = reader

        // A JPEG still needs its own session on this HAL; keeping the JPEG
        // surface in the preview session makes the preview black.
        captureSession?.close()
        captureSession = null

        try {
            device.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        issueStillCapture(device, choice, reader)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("CaptureModule", "Still session configuration failed")
                        resetStillCapture()
                        recreatePreviewSession()
                    }
                },
                mainHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("CaptureModule", "Cannot create still session", e)
            resetStillCapture()
            recreatePreviewSession()
        }
    }

    private fun issueStillCapture(
        device: CameraDevice,
        choice: CameraChoice,
        reader: ImageReader
    ) {
        try {
            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            request.addTarget(reader.surface)
            request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            request.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            request.set(CaptureRequest.JPEG_ORIENTATION, 0)
            applyAfMode(request, choice)

            captureSession?.capture(
                request.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        Log.e("CaptureModule", "Still capture failed: ${failure.reason}")
                        resetStillCapture()
                        recreatePreviewSession()
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("CaptureModule", "Cannot capture still", e)
            resetStillCapture()
            recreatePreviewSession()
        }
    }

    private fun resetStillCapture() {
        isCapturingStill = false
        pendingStillCallback = null
    }

    private fun openWhenReady() {
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(
                androidx.lifecycle.Lifecycle.State.STARTED
            )
        ) return
        val view = textureView ?: return
        if (!view.isAvailable || cameraDevice != null || isOpening) return

        val resolution = resolveCameras() ?: return
        pendingFallbacks.clear()
        pendingFallbacks.addAll(resolution.second)
        openCamera(pendingFallbacks.pollFirst() ?: resolution.first)
    }

    private fun openCamera(choice: CameraChoice) {
        isOpening = true
        Log.d("CaptureModule", "Opening camera ${choice.id} (focal=${choice.focalLength} mm)")

        try {
            cameraManager.openCamera(choice.id, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    isOpening = false
                    cameraDevice = device
                    startCaptureSession(device, choice)
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                    cameraDevice = null
                    isOpening = false
                }

                override fun onError(device: CameraDevice, error: Int) {
                    Log.e("CaptureModule", "Camera ${choice.id} error $error")
                    device.close()
                    cameraDevice = null
                    isOpening = false
                    openNextFallback()
                }
            }, mainHandler)
        } catch (e: CameraAccessException) {
            Log.e("CaptureModule", "Cannot open camera ${choice.id}", e)
            isOpening = false
            openNextFallback()
        } catch (e: SecurityException) {
            Log.e("CaptureModule", "Camera permission missing", e)
            isOpening = false
        }
    }

    private fun startCaptureSession(device: CameraDevice, choice: CameraChoice) {
        val view = textureView
        val characteristics = choice.characteristics
        val sizingCharacteristics = choice.physicalCharacteristics ?: characteristics
        val streamMap = sizingCharacteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )
        if (view?.surfaceTexture == null || streamMap == null) {
            Log.e("CaptureModule", "Camera ${choice.id} has no surface/stream configuration")
            closeCamera()
            openNextFallback()
            return
        }

        val previewSize = chooseSize(
            streamMap.getOutputSizes(android.graphics.SurfaceTexture::class.java),
            targetWidth = 1920,
            targetHeight = 1080
        )
        val stillSize = chooseSize(
            streamMap.getOutputSizes(ImageFormat.JPEG),
            targetWidth = 1920,
            targetHeight = 1080
        )
        this.stillSize = stillSize
        val analysisSize = chooseSize(
            streamMap.getOutputSizes(ImageFormat.YUV_420_888),
            targetWidth = 1280,
            targetHeight = 720
        )
        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

        view.surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        view.setTransform(Matrix())
        val preview = Surface(view.surfaceTexture)
        previewSurface = preview

        // Some HALs only keep the preview stream alive when a YUV output is
        // also configured. It is drained and ignored; stills use JPEG below.
        val yuv = ImageReader.newInstance(
            analysisSize.width,
            analysisSize.height,
            ImageFormat.YUV_420_888,
            2
        ).also {
            it.setOnImageAvailableListener(::drainYuv, cameraHandler)
        }
        yuvReader = yuv

        currentChoice = choice
        createPreviewSession(device, choice, preview, yuv.surface)
    }

    private fun createPreviewSession(
        device: CameraDevice,
        choice: CameraChoice,
        preview: Surface,
        analysisSurface: Surface
    ) {
        captureSession?.close()
        captureSession = null

        val sessionCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                Log.d("CaptureModule", "Preview session configured")
                captureSession = session
                try {
                    session.setRepeatingRequest(
                        buildPreviewRequest(device, choice, preview, analysisSurface),
                        null,
                        mainHandler
                    )
                } catch (e: CameraAccessException) {
                    Log.e("CaptureModule", "Cannot start preview", e)
                    closeCamera()
                    openNextFallback()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("CaptureModule", "Preview session configuration failed")
                closeCamera()
                openNextFallback()
            }
        }

        try {
            device.createCaptureSession(
                listOf(preview, analysisSurface),
                sessionCallback,
                mainHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("CaptureModule", "Cannot create preview session", e)
            closeCamera()
            openNextFallback()
        }
    }

    private fun recreatePreviewSession() {
        closeJpegReader()
        val device = cameraDevice ?: return
        val choice = currentChoice ?: return
        val preview = previewSurface ?: return
        val yuv = yuvReader ?: return
        createPreviewSession(device, choice, preview, yuv.surface)
    }

    private fun openNextFallback() {
        pendingFallbacks.pollFirst()?.let { openCamera(it) }
    }

    private fun buildPreviewRequest(
        device: CameraDevice,
        choice: CameraChoice,
        previewSurface: Surface,
        analysisSurface: Surface
    ): CaptureRequest {
        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        request.addTarget(previewSurface)
        request.addTarget(analysisSurface)
        request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        request.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        applyAfMode(request, choice)

        return request.build()
    }

    private fun applyAfMode(request: CaptureRequest.Builder, choice: CameraChoice) {
        val afModes = choice.physicalCharacteristics?.get(
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
        ) ?: choice.characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        val afMode = when {
            afModes?.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) == true ->
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            afModes?.contains(CaptureRequest.CONTROL_AF_MODE_AUTO) == true ->
                CaptureRequest.CONTROL_AF_MODE_AUTO
            else -> CaptureRequest.CONTROL_AF_MODE_OFF
        }
        request.set(CaptureRequest.CONTROL_AF_MODE, afMode)
    }

    private fun drainYuv(reader: ImageReader) {
        try {
            reader.acquireLatestImage()?.close()
        } catch (e: IllegalStateException) {
            // No image ready; nothing to drain.
        }
    }

    private fun onJpegAvailable(reader: ImageReader) {
        val callback = pendingStillCallback ?: return
        val image = try {
            reader.acquireNextImage()
        } catch (e: IllegalStateException) {
            null
        } ?: return

        val bytes = try {
            image.planes[0].buffer.let { buffer ->
                ByteArray(buffer.remaining()).also { buffer.get(it) }
            }
        } catch (t: Throwable) {
            Log.e("CaptureModule", "Failed to read JPEG still", t)
            resetStillCapture()
            recreatePreviewSession()
            return
        } finally {
            image.close()
        }

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (decoded == null) {
            Log.e("CaptureModule", "Could not decode JPEG still")
            resetStillCapture()
            closeJpegReader()
            recreatePreviewSession()
            return
        }

        val degrees = (sensorOrientation - displayRotationDegrees() + 360) % 360
        val rotated = rotateBitmap(decoded, degrees)
        mainHandler.post {
            callback(rotated)
            resetStillCapture()
            closeJpegReader()
            recreatePreviewSession()
        }
    }

    private fun closeJpegReader() {
        jpegReader?.setOnImageAvailableListener(null, null)
        jpegReader?.close()
        jpegReader = null
    }

    @Suppress("DEPRECATION")
    private fun displayRotationDegrees(): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    private fun resolveCameras(): Pair<CameraChoice, List<CameraChoice>>? {
        val publicIds = cameraManager.cameraIdList.toSet()
        val allIds = LinkedHashSet(publicIds)
        // Query ids that are hidden from getCameraIdList (physical ultra-wide,
        // macro, etc.). getCameraCharacteristics still works for those.
        for (id in 0..9) allIds.add(id.toString())

        val physicalCameras = mutableListOf<CameraChoice>()
        val logicalCameras = mutableListOf<CameraChoice>()

        for (id in allIds) {
            val characteristics = try {
                cameraManager.getCameraCharacteristics(id)
            } catch (e: CameraAccessException) {
                continue
            } catch (e: IllegalArgumentException) {
                continue
            }
            if (characteristics.get(CameraCharacteristics.LENS_FACING) !=
                CameraCharacteristics.LENS_FACING_BACK
            ) continue

            val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                characteristics.physicalCameraIds
            } else {
                emptySet()
            }

            if (physicalIds.isEmpty()) {
                val focalLength = characteristics
                    .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.minOrNull() ?: continue
                val sensorSize = characteristics
                    .get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    ?: continue
                physicalCameras += CameraChoice(
                    id = id,
                    focalLength = focalLength,
                    sensorArea = sensorSize.width * sensorSize.height,
                    isPublic = id in publicIds,
                    characteristics = characteristics
                )
            } else {
                for (physicalId in physicalIds) {
                    val physicalCharacteristics = try {
                        cameraManager.getCameraCharacteristics(physicalId)
                    } catch (e: CameraAccessException) {
                        continue
                    } catch (e: IllegalArgumentException) {
                        continue
                    }
                    if (physicalCharacteristics.get(CameraCharacteristics.LENS_FACING) !=
                        CameraCharacteristics.LENS_FACING_BACK
                    ) continue

                    val focalLength = physicalCharacteristics
                        .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        ?.minOrNull() ?: continue
                    val sensorSize = physicalCharacteristics
                        .get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                        ?: continue

                    logicalCameras += CameraChoice(
                        id = id,
                        focalLength = focalLength,
                        sensorArea = sensorSize.width * sensorSize.height,
                        isPublic = id in publicIds,
                        characteristics = characteristics,
                        physicalCameraId = physicalId,
                        physicalCharacteristics = physicalCharacteristics
                    )
                }
            }
        }

        val publicPhysical = physicalCameras.filter { it.isPublic }
        val main = (if (publicPhysical.isNotEmpty()) publicPhysical else physicalCameras)
            .maxByOrNull { it.sensorArea } ?: return null

        // Prefer a directly openable hidden physical camera, then a logical
        // multi-camera path, then the public main camera.
        val wideCandidates = (physicalCameras + logicalCameras)
            .filter {
                it.id != main.id &&
                    it.sensorArea >= 8f &&
                    it.focalLength < main.focalLength * 0.8f
            }
            .sortedWith(
                compareBy(
                    { it.focalLength },
                    { if (it.physicalCameraId == null) 0 else 1 }
                )
            )

        Log.d(
            "CaptureModule",
            "Main camera ${main.id} (${main.focalLength} mm); wide candidates: " +
                wideCandidates.joinToString {
                    "${it.id}/${it.physicalCameraId ?: "direct"}=${it.focalLength}mm"
                }
        )
        return main to wideCandidates
    }

    private fun chooseSize(sizes: Array<Size>?, targetWidth: Int, targetHeight: Int): Size {
        val available = sizes?.toList().orEmpty()
        if (available.isEmpty()) return Size(targetWidth, targetHeight)

        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
        return available
            .filter { kotlin.math.abs(it.width.toFloat() / it.height.toFloat() - targetRatio) < 0.02f }
            .minByOrNull { kotlin.math.abs(it.width - targetWidth) + kotlin.math.abs(it.height - targetHeight) }
            ?: available.minByOrNull { it.width * it.height }
            ?: Size(targetWidth, targetHeight)
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        jpegReader?.setOnImageAvailableListener(null, null)
        jpegReader?.close()
        jpegReader = null
        yuvReader?.setOnImageAvailableListener(null, null)
        yuvReader?.close()
        yuvReader = null
        previewSurface?.release()
        previewSurface = null
        isOpening = false
        resetStillCapture()
    }

    fun shutdown() {
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        closeCamera()
        cameraThread.quitSafely()
        textureView?.surfaceTextureListener = null
        textureView = null
    }

    private data class CameraChoice(
        val id: String,
        val focalLength: Float,
        val sensorArea: Float,
        val isPublic: Boolean,
        val characteristics: CameraCharacteristics,
        val physicalCameraId: String? = null,
        val physicalCharacteristics: CameraCharacteristics? = null
    )
}
