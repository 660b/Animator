package com.example.animator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// --- Silgi Mantığı ---

fun eraseNormalAt(strokes: MutableList<StrokeData>, p: Offset, radius: Float = 28f) {
    strokes.removeAll { s ->
        snapPoint(p, listOf(s), radius) != p ||
                (s.text != null && getPointsBounds(s.points).inflate(radius).contains(p))
    }
}

fun erasePartialAt(strokes: MutableList<StrokeData>, p: Offset, radius: Float = 28f) {
    val added = mutableListOf<StrokeData>()
    val iterator = strokes.iterator()
    while (iterator.hasNext()) {
        val stroke = iterator.next()
        if (stroke.fillOnly || stroke.text != null) continue
        if (stroke.points.none { dist(it, p) <= radius }) continue
        iterator.remove()
        val pr = stroke.pressures?.takeIf { it.size == stroke.points.size }
        var segPts = mutableListOf<Offset>()
        var segPr = mutableListOf<Float>()
        fun flush() {
            if (segPts.size >= 2) {
                added.add(stroke.copy(points = segPts.toList(), pressures = if (pr != null) segPr.toList() else null))
            }
            segPts = mutableListOf(); segPr = mutableListOf()
        }
        stroke.points.forEachIndexed { i, pt ->
            if (dist(pt, p) <= radius) flush()
            else { segPts.add(pt); if (pr != null) segPr.add(pr[i]) }
        }
        flush()
    }
    strokes.addAll(added)
}

// --- Doldurma (Fill) Mantığı ---

fun fillStrokeAt(strokes: MutableList<StrokeData>, p: Offset, fillColor: Color): Boolean {
    // Dokunulan çizgi veya kapalı şekli bul
    for (i in strokes.indices.reversed()) {
        val s = strokes[i]
        if (s.text != null) {
            if (getPointsBounds(s.points).inflate(10f).contains(p)) {
                strokes[i] = s.copy(color = fillColor)
                return true
            }
            continue
        }
        // Çizgiye yakın mı ya da noktaları içinde mi?
        val bounds = getPointsBounds(s.points)
        if (bounds.inflate(s.width + 20f).contains(p)) {
            val touched = snapPoint(p, listOf(s), s.width + 30f) != p || isPointInsidePolygon(p, s.points)
            if (touched) {
                strokes[i] = s.copy(isFilled = true, fillColor = fillColor)
                return true
            }
        }
    }
    return false
}

private fun isPointInsidePolygon(p: Offset, points: List<Offset>): Boolean {
    if (points.size < 3) return false
    var inside = false
    var j = points.size - 1
    for (i in points.indices) {
        if ((points[i].y > p.y) != (points[j].y > p.y) &&
            p.x < (points[j].x - points[i].x) * (p.y - points[i].y) / (points[j].y - points[i].y + 0.0001f) + points[i].x
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

// --- Çizim Görselleştirme (Brush, Opacity, Fill, Variable Width) ---

private fun variableWidthPath(points: List<Offset>, widths: List<Float>): Path {
    val left = ArrayList<Offset>()
    val right = ArrayList<Offset>()
    for (i in points.indices) {
        val prev = points[(i - 1).coerceAtLeast(0)]
        val next = points[(i + 1).coerceAtMost(points.lastIndex)]
        var dx = next.x - prev.x
        var dy = next.y - prev.y
        val len = hypot(dx, dy)
        if (len < 0.0001f) { dx = 1f; dy = 0f } else { dx /= len; dy /= len }
        val nx = -dy
        val ny = dx
        val hw = widths[i] / 2f
        left.add(Offset(points[i].x + nx * hw, points[i].y + ny * hw))
        right.add(Offset(points[i].x - nx * hw, points[i].y - ny * hw))
    }
    return Path().apply {
        moveTo(left.first().x, left.first().y)
        for (i in 1 until left.size) lineTo(left[i].x, left[i].y)
        for (i in right.indices.reversed()) lineTo(right[i].x, right[i].y)
        close()
    }
}

fun DrawScope.drawStrokeDataEnhanced(stroke: StrokeData, alphaMultiplier: Float = 1f) {
    if (stroke.text != null) {
        drawTextStroke(stroke, alphaMultiplier)
        return
    }
    val points = stroke.points
    if (points.size < 2) return

    val finalAlpha = (stroke.alpha * alphaMultiplier).coerceIn(0f, 1f)
    val drawColor = stroke.color.copy(alpha = stroke.color.alpha * finalAlpha)
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }

    if (stroke.isFilled) {
        val fColor = (stroke.fillColor ?: stroke.color).copy(alpha = finalAlpha)
        val fillPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            close()
        }
        drawPath(path = fillPath, color = fColor, style = Fill)
        if (stroke.fillOnly) {
            if (stroke.width > 0f) {
                drawPath(fillPath, fColor, style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            return
        }
    }

    val pr = stroke.pressures?.takeIf { it.size == points.size }
    val avgP = pr?.average()?.toFloat() ?: 1f

    when (stroke.brushType) {
        BrushType.PEN -> {
            if (pr != null) {
                drawPath(variableWidthPath(points, pr.map { stroke.width * it }), drawColor, style = Fill)
                if (finalAlpha >= 0.999f) {
                    drawCircle(drawColor, stroke.width * pr.first() / 2f, points.first())
                    drawCircle(drawColor, stroke.width * pr.last() / 2f, points.last())
                }
            } else {
                drawPath(path, drawColor, style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        BrushType.MARKER -> drawPath(
            path = path,
            color = stroke.color.copy(alpha = 0.55f * finalAlpha),
            style = Stroke(width = stroke.width * avgP * 1.4f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
        BrushType.SOFT -> {
            val w = stroke.width * avgP
            drawPath(path, stroke.color.copy(alpha = 0.25f * finalAlpha),
                style = Stroke(width = w * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, stroke.color.copy(alpha = 0.5f * finalAlpha),
                style = Stroke(width = w * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, drawColor,
                style = Stroke(width = w * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// --- Seçim Mantığı ve Dönüştürmeler ---

fun getPointsBounds(pts: List<Offset>): Rect {
    if (pts.isEmpty()) return Rect.Zero
    var minX = pts.first().x
    var maxX = pts.first().x
    var minY = pts.first().y
    var maxY = pts.first().y
    for (p in pts) {
        if (p.x < minX) minX = p.x
        if (p.x > maxX) maxX = p.x
        if (p.y < minY) minY = p.y
        if (p.y > maxY) maxY = p.y
    }
    return Rect(minX, minY, maxX, maxY)
}

fun getStrokesBounds(strokes: List<StrokeData>): Rect {
    if (strokes.isEmpty()) return Rect.Zero
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE

    for (s in strokes) {
        for (p in s.points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
    }
    return Rect(minX, minY, maxX, maxY)
}

fun findStrokesInRect(strokes: List<StrokeData>, rect: Rect): List<StrokeData> {
    return strokes.filter { s ->
        s.points.any { p -> rect.contains(p) }
    }
}

fun transformStroke(
    stroke: StrokeData,
    delta: Offset = Offset.Zero,
    pivot: Offset = Offset.Zero,
    scale: Float = 1f,
    rotationRad: Float = 0f
): StrokeData {
    val cosA = cos(rotationRad)
    val sinA = sin(rotationRad)

    val newPoints = stroke.points.map { p ->
        var pt = p + delta
        if (scale != 1f || rotationRad != 0f) {
            val dx = (pt.x - pivot.x) * scale
            val dy = (pt.y - pivot.y) * scale
            pt = Offset(
                pivot.x + dx * cosA - dy * sinA,
                pivot.y + dx * sinA + dy * cosA
            )
        }
        pt
    }
    return stroke.copy(points = newPoints, width = (stroke.width * scale).coerceIn(1f, 100f))
}