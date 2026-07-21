package com.unimagdalena.assistivevision.modules

import android.content.Context
import android.content.res.AssetManager
import android.graphics.RectF
import android.util.Log
import com.unimagdalena.assistivevision.model.Detection
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class DetectionModule(context: Context) {
    private val interpreter: Interpreter

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.30f // tune as needed
        const val MAX_DETECTIONS = 15          // tune as needed
        // could be good to have like the best 15 IoUs or something
        const val INPUT_SIZE = 448
        val LABELS = listOf(
            "person", "chair", "couch", "bed", "tv",
            "sink", "toilet", "computer", "refrigerator", "table"
        )
    }

    init {
        val model = loadModelFile(context.assets, "efficientdet-lite2-detection-metadata.tflite")
        interpreter = Interpreter(model)
    }

    fun detect(inputBuffer: ByteBuffer): List<Detection> {
        val boxes = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
        val classes = Array(1) { FloatArray(MAX_DETECTIONS) }
        val scores = Array(1) { FloatArray(MAX_DETECTIONS) }
        val count = FloatArray(1)

        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer),
            mapOf(0 to boxes, 1 to classes, 2 to scores, 3 to count)
        )

        val numDetections = count[0].toInt().coerceAtMost(MAX_DETECTIONS)

        Log.d("DetectionModule", "Raw detections: $numDetections")

        return (0 until numDetections)
            .filter { scores[0][it] >= CONFIDENCE_THRESHOLD }
            .map { i ->
                Detection(
                    classId = classes[0][i].toInt(),
                    className = LABELS.getOrElse(classes[0][i].toInt()) { "unknown" },
                    score = scores[0][i],
                    bbox = RectF(
                        boxes[0][i][1] * INPUT_SIZE,  // xmin
                        boxes[0][i][0] * INPUT_SIZE,  // ymin
                        boxes[0][i][3] * INPUT_SIZE,  // xmax
                        boxes[0][i][2] * INPUT_SIZE   // ymax
                    )
                )
            }
    }

    private fun loadModelFile(assets: AssetManager, filename: String): MappedByteBuffer {
        val fd = assets.openFd(filename)
        val stream = FileInputStream(fd.fileDescriptor)
        val channel = stream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    fun close() {
        interpreter.close()
    }
}
