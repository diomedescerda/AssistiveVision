package com.unimagdalena.assistivevision.model

import android.graphics.RectF

data class Detection(
    val classId: Int,
    val className: String,
    val score: Float,
    val bbox: RectF,
    val zone: String = ""
)