package com.poolaimguide.app.geometry

import kotlin.math.*

/**
 * 2D Point coordinate in screen pixels.
 */
data class Point2D(var x: Float, var y: Float) {
    fun distanceTo(other: Point2D): Float = hypot(other.x - x, other.y - y)
}

/**
 * Calculated 3-line shot geometry for the aim guide.
 */
data class ShotResult(
    val cueBall: Point2D,
    val targetBall: Point2D,
    val ghostBall: Point2D,
    val contactPoint: Point2D,
    val cutAngleDeg: Float,
    val aimLineEnd: Point2D,         // Line 1: Long Aim Line
    val objectTrajectoryEnd: Point2D, // Line 2: Object Ball Path
    val deflectionEnd: Point2D       // Line 3: Cue Ball Deflection (Tangent)
)

/**
 * Pool physics geometry solver:
 * - Line 1 (long aim line): straight line from cue ball center through target ball center, extended well beyond it.
 * - Line 2 (object ball path): from the contact point, along the line through the object ball center (ghost-ball method).
 * - Line 3 (cue ball deflection): the tangent line, perpendicular to Line 2, on the correct side of impact.
 */
object PoolGeometry {

    fun calculateShot(
        cueBall: Point2D,
        targetBall: Point2D,
        ballRadius: Float,
        lineLength: Float,
        cutAngleOffsetDeg: Float = 0f
    ): ShotResult {
        val dist = cueBall.distanceTo(targetBall)
        if (dist < 1f) {
            return fallbackShot(cueBall, targetBall, ballRadius, lineLength)
        }

        // Angle from target ball to cue ball
        val baseAngle = atan2(cueBall.y - targetBall.y, cueBall.x - targetBall.x)
        val cutRad = Math.toRadians(cutAngleOffsetDeg.toDouble()).toFloat()
        val ghostAngle = baseAngle + cutRad

        // Ghost ball is at distance 2 * ballRadius from target ball
        val ghostBall = Point2D(
            x = targetBall.x + cos(ghostAngle) * (2f * ballRadius),
            y = targetBall.y + sin(ghostAngle) * (2f * ballRadius)
        )

        // Line of centers normal vector: from Ghost Ball to Target Ball
        val dxNorm = targetBall.x - ghostBall.x
        val dyNorm = targetBall.y - ghostBall.y
        val normLen = hypot(dxNorm, dyNorm).coerceAtLeast(0.001f)
        val normalX = dxNorm / normLen
        val normalY = dyNorm / normLen

        // Contact point on the surface of both balls
        val contactPoint = Point2D(
            x = targetBall.x - normalX * ballRadius,
            y = targetBall.y - normalY * ballRadius
        )

        // Incoming cue ball vector towards ghost ball
        val dxIn = ghostBall.x - cueBall.x
        val dyIn = ghostBall.y - cueBall.y
        val inLen = hypot(dxIn, dyIn).coerceAtLeast(0.001f)
        val inDirX = dxIn / inLen
        val inDirY = dyIn / inLen

        // Actual cut angle calculation
        val dotNorm = (inDirX * normalX + inDirY * normalY).coerceIn(-1f, 1f)
        val cutAngleDeg = Math.toDegrees(acos(dotNorm.toDouble())).toFloat()

        // Line 1: Long Aim Line from Cue Ball through Target area and extended beyond
        val aimLength = max(lineLength, dist + lineLength * 0.6f)
        val aimLineEnd = Point2D(
            x = cueBall.x + inDirX * aimLength,
            y = cueBall.y + inDirY * aimLength
        )

        // Line 2: Object Ball Path along center-to-center line
        val objectTrajectoryEnd = Point2D(
            x = targetBall.x + normalX * lineLength,
            y = targetBall.y + normalY * lineLength
        )

        // Line 3: Cue Ball Deflection (Stun / 90° Tangent Line)
        // Tangent candidates perpendicular to normal
        val t1X = -normalY
        val t1Y = normalX
        val t2X = normalY
        val t2Y = -normalX

        // Pick tangent vector on correct side of incoming momentum
        val dot1 = inDirX * t1X + inDirY * t1Y
        val tangentX = if (dot1 >= 0f) t1X else t2X
        val tangentY = if (dot1 >= 0f) t1Y else t2Y

        // Deflection length is proportional to cut angle
        val deflFactor = sin(Math.toRadians(cutAngleDeg.toDouble())).toFloat().absoluteValue
        val deflLength = max(lineLength * 0.35f, lineLength * deflFactor)

        val deflectionEnd = Point2D(
            x = ghostBall.x + tangentX * deflLength,
            y = ghostBall.y + tangentY * deflLength
        )

        return ShotResult(
            cueBall = cueBall,
            targetBall = targetBall,
            ghostBall = ghostBall,
            contactPoint = contactPoint,
            cutAngleDeg = cutAngleDeg,
            aimLineEnd = aimLineEnd,
            objectTrajectoryEnd = objectTrajectoryEnd,
            deflectionEnd = deflectionEnd
        )
    }

    private fun fallbackShot(
        c: Point2D, t: Point2D, r: Float, len: Float
    ): ShotResult = ShotResult(
        cueBall = c,
        targetBall = t,
        ghostBall = Point2D(t.x - 2 * r, t.y),
        contactPoint = Point2D(t.x - r, t.y),
        cutAngleDeg = 0f,
        aimLineEnd = Point2D(t.x + len, t.y),
        objectTrajectoryEnd = Point2D(t.x + len, t.y),
        deflectionEnd = Point2D(t.x, t.y + len * 0.4f)
    )
}