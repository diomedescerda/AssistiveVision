package com.unimagdalena.assistivevision.modules

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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
                val now = System.currentTimeMillis()
                if (now - lastFrameTime >= frameIntervalMs) {
                    lastFrameTime = now
                    Log.d("CaptureModule", "Frame received: ${imageProxy.width}x${imageProxy.height}")
                    val bitmap = imageProxy.toBitmap()
                    onFrameReady(bitmap)
                }
                imageProxy.close()
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}