package com.unimagdalena.assistivevision.modules

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class CaptureModule (
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val maxFps: Int = 9, // efficientdet-lite2 benchmarks at ~8.55 fps
    private val onFrameReady: (Bitmap) -> Unit
) {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var lastFrameTime = 0L
    private val frameIntervalMs get() = 1000L / maxFps

    fun start(previewView: PreviewView? = null) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build().also {
                it.surfaceProvider = previewView?.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastFrameTime >= frameIntervalMs) {
                        lastFrameTime = now
                        Log.d("CaptureModule", "Frame received: ${imageProxy.width}x${imageProxy.height}")
                        val bitmap = imageProxy.toBitmap()
                        val rotated = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
                        onFrameReady(rotated)
                    }
                } finally {
                    imageProxy.close()
                }
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                pickBackCameraSelector(cameraProvider),
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Builds a selector for the back camera with the lowest focal length
     * (0.5x ultra-wide when present, otherwise the 1x main lens).
     * Falls back to [CameraSelector.DEFAULT_BACK_CAMERA] when focal length
     * data is unavailable.
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun pickBackCameraSelector(cameraProvider: ProcessCameraProvider): CameraSelector {
        val backInfos = cameraProvider.availableCameraInfos.filter {
            it.lensFacing == CameraSelector.LENS_FACING_BACK
        }
        val candidates = backInfos.mapNotNull { info ->
            val camera2Info = Camera2CameraInfo.from(info)
            val focalLengths = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )
            if (focalLengths == null || focalLengths.isEmpty()) null
            else camera2Info.cameraId to focalLengths
        }
        val winner = CameraLensPolicy.pick(candidates)
            ?: return CameraSelector.DEFAULT_BACK_CAMERA

        return CameraSelector.Builder()
            .addCameraFilter(CameraFilter { infos ->
                infos.filter { Camera2CameraInfo.from(it).cameraId == winner }
                    .toMutableList()
            })
            .build()
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}