package com.example.pspcontroller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/**
 * A custom left analog stick. Touch coordinates are normalized to the
 * -127..127 range expected by both the Bluetooth HID report and PC receiver.
 */
class AnalogStickView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMoveListener: ((x: Byte, y: Byte) -> Unit)? = null

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#14243A")
        style = Paint.Style.FILL
    }
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6686A8")
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34516F")
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#45617E")
        strokeWidth = dp(1f)
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val thumbEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A7F0E2")
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B9FFF1")
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var travelRadius = 0f
    private var stickRadius = 0f
    private var stickX = 0f
    private var stickY = 0f

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = minOf(w, h) * 0.43f
        stickRadius = baseRadius * 0.34f
        travelRadius = baseRadius - stickRadius * 0.55f
        stickX = centerX
        stickY = centerY
        thumbPaint.shader = RadialGradient(
            centerX - stickRadius * 0.22f,
            centerY - stickRadius * 0.28f,
            stickRadius * 1.55f,
            intArrayOf(
                Color.parseColor("#B5F7E9"),
                Color.parseColor("#62CDBD"),
                Color.parseColor("#287E82")
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, outerRingPaint)
        canvas.drawCircle(centerX, centerY, baseRadius * 0.67f, innerRingPaint)
        canvas.drawCircle(centerX, centerY, baseRadius * 0.34f, innerRingPaint)

        val guideReach = baseRadius * 0.72f
        canvas.drawLine(centerX - guideReach, centerY, centerX + guideReach, centerY, guidePaint)
        canvas.drawLine(centerX, centerY - guideReach, centerX, centerY + guideReach, guidePaint)

        canvas.drawCircle(stickX, stickY, stickRadius, thumbPaint)
        canvas.drawCircle(stickX, stickY, stickRadius, thumbEdgePaint)
        canvas.drawCircle(
            stickX - stickRadius * 0.27f,
            stickY - stickRadius * 0.30f,
            stickRadius * 0.13f,
            highlightPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = hypot(dx, dy)
                val scale = if (distance > travelRadius && distance > 0f) {
                    travelRadius / distance
                } else {
                    1f
                }

                stickX = centerX + dx * scale
                stickY = centerY + dy * scale

                val normX = (((stickX - centerX) / travelRadius) * 127f)
                    .toInt().coerceIn(-127, 127).toByte()
                val normY = (((stickY - centerY) / travelRadius) * 127f)
                    .toInt().coerceIn(-127, 127).toByte()

                onMoveListener?.invoke(normX, normY)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                stickX = centerX
                stickY = centerY
                onMoveListener?.invoke(0, 0)
                invalidate()
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
