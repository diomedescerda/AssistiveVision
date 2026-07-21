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
    private val maxModelDetections: Int

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.30f // tune as needed
        const val MAX_DETECTIONS = 15          // tune as needed
        const val INPUT_SIZE = 448
        // COCO class ID → label name (model outputs 0-indexed, add 1 for COCO ID)
        val TARGET_LABELS = mapOf(
            1 to "person",
            62 to "chair",
            63 to "couch",
            65 to "bed",
            67 to "table",
            70 to "toilet",
            72 to "tv",
            73 to "computer",
            81 to "sink",
            82 to "refrigerator"
        )
    }

    init {
        val model = loadModelFile(context.assets)
        interpreter = Interpreter(model)
        maxModelDetections = interpreter.getOutputTensor(0).shape()[1]
    }

    fun detect(inputBuffer: ByteBuffer): List<Detection> {
        val boxes = Array(1) { Array(maxModelDetections) { FloatArray(4) } }
        val classes = Array(1) { FloatArray(maxModelDetections) }
        val scores = Array(1) { FloatArray(maxModelDetections) }
        val count = FloatArray(1)

        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer),
            mapOf(0 to boxes, 1 to classes, 2 to scores, 3 to count)
        )

        val numDetections = count[0].toInt().coerceAtMost(maxModelDetections)

        Log.d("DetectionModule", "Raw detections: $numDetections")

        return (0 until numDetections)
            .asSequence()
            .filter { scores[0][it] >= CONFIDENCE_THRESHOLD }
            .filter { TARGET_LABELS.containsKey(classes[0][it].toInt() + 1) }
            .map { i ->
                val classId = classes[0][i].toInt() + 1
                Log.d("DetectionModule", "classId raw=${classes[0][i]} adjusted=$classId label=${TARGET_LABELS[classId]}")
                Detection(
                    classId = classId,
                    className = TARGET_LABELS[classId]!!,
                    score = scores[0][i],
                    bbox = RectF(
                        boxes[0][i][1] * INPUT_SIZE,  // xmin
                        boxes[0][i][0] * INPUT_SIZE,  // ymin
                        boxes[0][i][3] * INPUT_SIZE,  // xmax
                        boxes[0][i][2] * INPUT_SIZE   // ymax
                    )
                )
            }
            .sortedByDescending { it.score }
            .take(MAX_DETECTIONS)
            .toList()
    }

    private fun loadModelFile(assets: AssetManager): MappedByteBuffer {
        val fd = assets.openFd("efficientdet-lite2-detection-metadata.tflite")
        val stream = FileInputStream(fd.fileDescriptor)
        val channel = stream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    fun close() {
        interpreter.close()
    }
}
