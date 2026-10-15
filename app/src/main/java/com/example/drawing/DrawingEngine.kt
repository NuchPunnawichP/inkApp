package com.example.drawing

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import com.example.data.model.DrawingStroke
import com.example.data.model.PointData
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object DrawingEngine {

    /**
     * Builds a smooth quadratic Bezier Path from a list of PointData points.
     * Uses midpoint smoothing to ensure zero latency and buttery smooth curves during rapid sketching.
     */
    fun createSmoothPath(points: List<PointData>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        if (points.size == 1) {
            val p = points[0]
            path.moveTo(p.x, p.y)
            path.lineTo(p.x + 0.5f, p.y + 0.5f)
            return path
        }

        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f

            if (i == 1) {
                path.lineTo(midX, midY)
            } else {
                path.quadraticTo(prev.x, prev.y, midX, midY)
            }
        }

        val last = points.last()
        path.lineTo(last.x, last.y)
        return path
    }

    /**
     * Calculates the effective stroke width applying stylus pressure sensitivity.
     * Stylus pressure is typically between 0.0 and 1.0.
     */
    fun calculateEffectiveWidth(baseWidth: Float, pressure: Float): Float {
        val validPressure = if (pressure > 0.05f) pressure else 1.0f
        val factor = 0.55f + (validPressure * 0.9f).coerceIn(0.2f, 1.45f)
        return (baseWidth * factor).coerceIn(1.5f, baseWidth * 2.5f)
    }

    /**
     * Checks if an eraser point intersects any segment of a stroke within radius.
     */
    fun isStrokeIntersectingEraser(
        stroke: DrawingStroke,
        eraserCenter: Offset,
        eraserRadius: Float
    ): Boolean {
        val points = stroke.points
        if (points.isEmpty()) return false

        val hitThreshold = eraserRadius + (stroke.strokeWidth / 2f)

        // Point check for very short strokes
        if (points.size == 1) {
            val p = points[0]
            return hypot(p.x - eraserCenter.x, p.y - eraserCenter.y) <= hitThreshold
        }

        // Bounding box early rejection check for performance
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }

        if (eraserCenter.x < minX - hitThreshold ||
            eraserCenter.x > maxX + hitThreshold ||
            eraserCenter.y < minY - hitThreshold ||
            eraserCenter.y > maxY + hitThreshold
        ) {
            return false
        }

        // Detailed segment-to-point distance check
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dist = distanceToSegment(
                px = eraserCenter.x,
                py = eraserCenter.y,
                x1 = p1.x,
                y1 = p1.y,
                x2 = p2.x,
                y2 = p2.y
            )
            if (dist <= hitThreshold) {
                return true
            }
        }
        return false
    }

    private fun distanceToSegment(
        px: Float,
        py: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Float {
        val l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
        if (l2 == 0f) return hypot(px - x1, py - y1)

        val t = max(0f, min(1f, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2))
        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)
        return hypot(px - projX, py - projY)
    }
}
