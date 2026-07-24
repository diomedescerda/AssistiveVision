package com.unimagdalena.assistivevision.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.unimagdalena.assistivevision.model.Detection

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var detections: List<Detection> = emptyList()

    private val boxColor = 0xFF1D9E75.toInt()

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = boxColor
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val tagPaint = Paint().apply {
        style = Paint.Style.FILL
        color = boxColor
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val textPadding = 8f
    private val tagCornerRadius = 12f
    private val boxCornerRadius = 16f
    private val boxRect = RectF()
    private val tagRect = RectF()
    private val inputSize = 448

    fun setDetections(detections: List<Detection>) {
        this.detections = detections
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaleX = width.toFloat() / inputSize
        val scaleY = height.toFloat() / inputSize

        for (d in detections) {
            val left = d.bbox.left * scaleX
            val top = d.bbox.top * scaleY
            val right = d.bbox.right * scaleX
            val bottom = d.bbox.bottom * scaleY

            boxRect.set(left, top, right, bottom)
            canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, boxPaint)

            val label = "${d.className} ${(d.score * 100).toInt()}%"
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.fontMetrics.bottom - textPaint.fontMetrics.top
            val tagWidth = textWidth + textPadding * 2
            val tagHeight = textHeight + textPadding * 2

            tagRect.set(left, top - tagHeight, left + tagWidth, top)
            canvas.drawRoundRect(tagRect, tagCornerRadius, tagCornerRadius, tagPaint)

            val textX = left + (tagWidth - textWidth) / 2
            val textY = top - textPadding - textPaint.fontMetrics.descent
            canvas.drawText(label, textX, textY, textPaint)
        }
    }
}
