package com.poolaimguide.app.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.poolaimguide.app.geometry.Point2D
import com.poolaimguide.app.geometry.PoolGeometry
import com.poolaimguide.app.geometry.ShotResult
import kotlin.math.hypot

/**
 * Transparent Full-Screen Canvas View that renders:
 * 1. Line 1: Long Aim Line (Cue ball to Ghost Ball and extended)
 * 2. Line 2: Object Ball Path (Ghost ball to Target Ball center and extended)
 * 3. Line 3: Cue Ball 90° Tangent Deflection Line
 * 4. Ghost Ball visualization and Draggable Handles
 * 5. Floating mini control panel with sliders for length, thickness, opacity, color
 */
class OverlayCanvasView(context: Context) : View(context) {

    // Calibration Ball Positions
    var cueBall = Point2D(300f, 800f)
    var targetBall = Point2D(600f, 500f)
    var ballRadius = 36f

    // Line Properties
    var lineLength = 700f
    var lineThickness = 6f
    var lineOpacity = 0.9f
    var aimColor = Color.CYAN
    var objectColor = Color.YELLOW
    var deflectionColor = Color.MAGENTA

    // Mode: 0 = Idle/Draggable, 1 = Tap Cue Ball, 2 = Tap Target Ball
    var tapCalibrationStep = 0

    // Paints
    private val paintAimLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val paintObjectLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val paintDeflectionLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }
    private val paintGhostBall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(180, 255, 255, 255)
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    private val paintHandle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }

    var onPassThroughToggleListener: ((Boolean) -> Unit)? = null
    var onCloseListener: (() -> Unit)? = null

    private var activeDraggingBall: Int = 0 // 1 = cue, 2 = target

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate 3-line pool geometry
        val shot = PoolGeometry.calculateShot(
            cueBall = cueBall,
            targetBall = targetBall,
            ballRadius = ballRadius,
            lineLength = lineLength
        )

        // Configure Paint alpha and stroke
        val alphaInt = (lineOpacity * 255).toInt()
        paintAimLine.strokeWidth = lineThickness
        paintAimLine.color = aimColor
        paintAimLine.alpha = alphaInt

        paintObjectLine.strokeWidth = lineThickness
        paintObjectLine.color = objectColor
        paintObjectLine.alpha = alphaInt

        paintDeflectionLine.strokeWidth = lineThickness * 0.85f
        paintDeflectionLine.color = deflectionColor
        paintDeflectionLine.alpha = alphaInt

        // 1. Draw LINE 1: Long Aim Line
        canvas.drawLine(shot.cueBall.x, shot.cueBall.y, shot.aimLineEnd.x, shot.aimLineEnd.y, paintAimLine)

        // 2. Draw LINE 2: Object Ball Trajectory Path
        canvas.drawLine(shot.contactPoint.x, shot.contactPoint.y, shot.objectTrajectoryEnd.x, shot.objectTrajectoryEnd.y, paintObjectLine)

        // 3. Draw LINE 3: Cue Ball 90° Tangent Deflection
        canvas.drawLine(shot.ghostBall.x, shot.ghostBall.y, shot.deflectionEnd.x, shot.deflectionEnd.y, paintDeflectionLine)

        // Draw Ghost Ball Circle at Impact
        canvas.drawCircle(shot.ghostBall.x, shot.ghostBall.y, ballRadius, paintGhostBall)

        // Draw Contact Point Dot
        paintHandle.color = Color.RED
        canvas.drawCircle(shot.contactPoint.x, shot.contactPoint.y, 6f, paintHandle)

        // Draw Cue Ball Ring and Handle
        paintHandle.color = Color.argb(120, 255, 255, 255)
        canvas.drawCircle(cueBall.x, cueBall.y, ballRadius, paintHandle)
        paintHandle.color = Color.WHITE
        canvas.drawCircle(cueBall.x, cueBall.y, 8f, paintHandle)

        // Draw Target Ball Ring and Handle
        paintHandle.color = Color.argb(120, 255, 200, 0)
        canvas.drawCircle(targetBall.x, targetBall.y, ballRadius, paintHandle)
        paintHandle.color = Color.YELLOW
        canvas.drawCircle(targetBall.x, targetBall.y, 8f, paintHandle)

        // Display Cut Angle Information
        val angleStr = String.format("Cut: %.1f° | 90° Tangent", shot.cutAngleDeg)
        canvas.drawText(angleStr, shot.ghostBall.x + 40f, shot.ghostBall.y - 40f, paintText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if tapping in 2-point manual mode
                if (tapCalibrationStep == 1) {
                    cueBall.x = touchX
                    cueBall.y = touchY
                    tapCalibrationStep = 2
                    invalidate()
                    return true
                } else if (tapCalibrationStep == 2) {
                    targetBall.x = touchX
                    targetBall.y = touchY
                    tapCalibrationStep = 0
                    invalidate()
                    return true
                }

                // Dragging existing balls
                val touchThreshold = ballRadius * 1.8f
                if (cueBall.distanceTo(Point2D(touchX, touchY)) < touchThreshold) {
                    activeDraggingBall = 1
                    return true
                } else if (targetBall.distanceTo(Point2D(touchX, touchY)) < touchThreshold) {
                    activeDraggingBall = 2
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeDraggingBall == 1) {
                    cueBall.x = touchX
                    cueBall.y = touchY
                    invalidate()
                    return true
                } else if (activeDraggingBall == 2) {
                    targetBall.x = touchX
                    targetBall.y = touchY
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeDraggingBall = 0
            }
        }
        return super.onTouchEvent(event)
    }
}