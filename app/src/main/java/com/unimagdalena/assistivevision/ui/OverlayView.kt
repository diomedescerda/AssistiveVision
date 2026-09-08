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
    private var frameWidth = 0
    private var frameHeight = 0

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

    fun setDetections(detections: List<Detection>) {
        this.detections = detections
        invalidate()
    }

    fun setFrameSize(width: Int, height: Int) {
        frameWidth = width
        frameHeight = height
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (detections.isEmpty() || frameWidth <= 0 || frameHeight <= 0) return

        // Fit the captured frame inside this view with a uniform scale
        val viewScale = minOf(width.toFloat() / frameWidth, height.toFloat() / frameHeight)
        val offsetX = (width - frameWidth * viewScale) / 2f
        val offsetY = (height - frameHeight * viewScale) / 2f

        for (d in detections) {
            val left = d.bbox.left * viewScale + offsetX
            val top = d.bbox.top * viewScale + offsetY
            val right = d.bbox.right * viewScale + offsetX
            val bottom = d.bbox.bottom * viewScale + offsetY

            boxRect.set(left, top, right, bottom)
            canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, boxPaint)

            val label = "${d.className} ${(d.score * 100).toInt()}%"
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.fontMetrics.bottom - textPaint.fontMetrics.top
            val tagWidth = textWidth + textPadding * 2
            val tagHeight = textHeight + textPadding * 2

            // Place the tag above the box, or inside it when the box is too high
            val tagTop = if (top - tagHeight >= 0f) top - tagHeight else top
            tagRect.set(left, tagTop, left + tagWidth, tagTop + tagHeight)
            canvas.drawRoundRect(tagRect, tagCornerRadius, tagCornerRadius, tagPaint)

            val textX = left + (tagWidth - textWidth) / 2
            val textY = tagTop + textPadding - textPaint.fontMetrics.ascent
            canvas.drawText(label, textX, textY, textPaint)
        }
    }
}