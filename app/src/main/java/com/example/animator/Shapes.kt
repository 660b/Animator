package com.example.animator

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

fun ellipsePoints(cx: Float, cy: Float, rx: Float, ry: Float): List<Offset> {
    val steps = 64
    return (0..steps).map { i ->
        val a = 2.0 * PI * i / steps
        Offset(cx + rx * cos(a).toFloat(), cy + ry * sin(a).toFloat())
    }
}

fun generateShape(tool: Tool, start: Offset, end: Offset): List<Offset> {
    val x1 = start.x
    val y1 = start.y
    val x2 = end.x
    val y2 = end.y
    return when (tool) {
        Tool.LINE -> listOf(start, end)

        Tool.RECT -> listOf(
            Offset(x1, y1), Offset(x2, y1),
            Offset(x2, y2), Offset(x1, y2),
            Offset(x1, y1)
        )

        Tool.TRIANGLE -> listOf(
            Offset((x1 + x2) / 2f, y1),
            Offset(x2, y2),
            Offset(x1, y2),
            Offset((x1 + x2) / 2f, y1)
        )

        Tool.CIRCLE -> ellipsePoints(
            (x1 + x2) / 2f, (y1 + y2) / 2f,
            abs(x2 - x1) / 2f, abs(y2 - y1) / 2f
        )

        else -> listOf(start, end)
    }
}
fun dist(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)
// p noktasına a-b doğru parçası üzerindeki en yakın nokta
private fun nearestOnSegment(p: Offset, a: Offset, b: Offset): Offset {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l2 = dx * dx + dy * dy
    if (l2 == 0f) return a
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0f, 1f)
    return Offset(a.x + t * dx, a.y + t * dy)
}
fun snapPoint(p: Offset, strokes: List<StrokeData>, threshold: Float): Offset {
    var best = p
    var bestD = threshold
    for (s in strokes) {
        for (i in 1 until s.points.size) {
            val q = nearestOnSegment(p, s.points[i - 1], s.points[i])
            val d = dist(p, q)
            if (d < bestD) {
                bestD = d
                best = q
            }
        }
    }
    return best
}
private fun perpDist(p: Offset, a: Offset, b: Offset): Float {
    val d = dist(a, b)
    if (d == 0f) return dist(p, a)
    return abs((b.x - a.x) * (a.y - p.y) - (a.x - p.x) * (b.y - a.y)) / d
}
private fun rdp(pts: List<Offset>, eps: Float): List<Offset> {
    if (pts.size < 3) return pts
    var maxD = 0f
    var idx = 0
    for (i in 1 until pts.size - 1) {
        val d = perpDist(pts[i], pts.first(), pts.last())
        if (d > maxD) {
            maxD = d
            idx = i
        }
    }
    return if (maxD > eps) {
        val left = rdp(pts.subList(0, idx + 1), eps)
        val right = rdp(pts.subList(idx, pts.size), eps)
        left.dropLast(1) + right
    } else {
        listOf(pts.first(), pts.last())
    }
}
private fun angleAt(prev: Offset, cur: Offset, next: Offset): Float {
    val v1x = prev.x - cur.x
    val v1y = prev.y - cur.y
    val v2x = next.x - cur.x
    val v2y = next.y - cur.y
    val l1 = hypot(v1x, v1y)
    val l2 = hypot(v2x, v2y)
    if (l1 == 0f || l2 == 0f) return 180f
    val c = ((v1x * v2x + v1y * v2y) / (l1 * l2)).coerceIn(-1f, 1f)
    return acos(c) * 180f / PI.toFloat()
}
fun recognizeShape(pts: List<Offset>): List<Offset>? {
    if (pts.size < 8) return null

    val minX = pts.minOf { it.x }
    val maxX = pts.maxOf { it.x }
    val minY = pts.minOf { it.y }
    val maxY = pts.maxOf { it.y }
    val w = maxX - minX
    val h = maxY - minY
    val diag = hypot(w, h)
    if (diag < 40f) return null

    var len = 0f
    for (i in 1 until pts.size) len += dist(pts[i - 1], pts[i])
    val gap = dist(pts.first(), pts.last())

    // 1) DÜZ ÇİZGİ: yol uzunluğu, uç uca mesafeye çok yakınsa
    if (len < 1.2f * gap) return listOf(pts.first(), pts.last())

    // Kapalı şekil değilse dokunma
    if (gap > 0.25f * len) return null

    // 2) DAİRE / OVAL
    if (w > 20f && h > 20f) {
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val rx = w / 2f
        val ry = h / 2f
        val dev = pts.map { p ->
            val nx = (p.x - cx) / rx
            val ny = (p.y - cy) / ry
            abs(sqrt(nx * nx + ny * ny) - 1f)
        }.average()
        if (dev < 0.12) {
            // en boy oranı çok yakınsa tam daire yap
            return if (abs(rx - ry) < 0.12f * maxOf(rx, ry)) {
                val r = (rx + ry) / 2f
                ellipsePoints(cx, cy, r, r)
            } else {
                ellipsePoints(cx, cy, rx, ry)
            }
        }
    }

    // 3) ÜÇGEN / DÖRTGEN: köşeleri bul
    val verts = rdp(pts, 0.06f * diag).toMutableList()
    if (verts.size >= 2) verts.removeAt(verts.size - 1) // kapanış noktasını at

    // Düze yakın (köşe olmayan) noktaları ele
    var changed = true
    while (changed && verts.size > 3) {
        changed = false
        for (i in verts.indices) {
            val prev = verts[(i - 1 + verts.size) % verts.size]
            val next = verts[(i + 1) % verts.size]
            if (angleAt(prev, verts[i], next) > 150f) {
                verts.removeAt(i)
                changed = true
                break
            }
        }
    }

    return when (verts.size) {
        3 -> verts + verts.first()
        4 -> {
            val corners = listOf(
                Offset(minX, minY), Offset(maxX, minY),
                Offset(maxX, maxY), Offset(minX, maxY)
            )
            val isAxisRect = verts.all { v -> corners.any { c -> dist(v, c) < 0.2f * diag } }
            if (isAxisRect) {
                listOf(
                    Offset(minX, minY), Offset(maxX, minY),
                    Offset(maxX, maxY), Offset(minX, maxY),
                    Offset(minX, minY)
                )
            } else {
                verts + verts.first()
            }
        }
        else -> null
    }
}

fun simplifyPolyline(pts: List<Offset>, eps: Float): List<Offset> = rdp(pts, eps)