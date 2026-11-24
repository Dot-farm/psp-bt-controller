package com.example.pspcontroller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class AnalogStickView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMoveListener: ((x: Byte, y: Byte) -> Unit)? = null

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.FILL
    }
    private val stickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var stickRadius = 0f
    private var stickX = 0f
    private var stickY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = (minOf(w, h) / 2f) * 0.85f
        stickRadius = baseRadius * 0.45f
        stickX = centerX
        stickY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(stickX, stickY, stickRadius, stickPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = hypot(dx, dy)

                if (distance < baseRadius) {
                    stickX = event.x
                    stickY = event.y
                } else {
                    val angle = atan2(dy, dx)
                    stickX = centerX + cos(angle) * baseRadius
                    stickY = centerY + sin(angle) * baseRadius
                }

                val normX = (((stickX - centerX) / baseRadius) * 127).toInt().coerceIn(-127, 127).toByte()
                val normY = (((stickY - centerY) / baseRadius) * 127).toInt().coerceIn(-127, 127).toByte()
                onMoveListener?.invoke(normX, normY)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stickX = centerX
                stickY = centerY
                onMoveListener?.invoke(0, 0)
                invalidate()
            }
        }
        return true
    }
}
