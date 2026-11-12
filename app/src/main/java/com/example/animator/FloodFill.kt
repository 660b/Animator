package com.example.animator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

private const val SCALE = 0.5f

private val DX = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
private val DY = intArrayOf(0, -1, -1, -1, 0, 1, 1, 1)

private fun dirIndex(dx: Int, dy: Int): Int {
    for (i in 0 until 8) if (DX[i] == dx && DY[i] == dy) return i
    return 0
}

// Dış sınır (Moore komşuluk izleme)
private fun traceOuter(region: BooleanArray, w: Int, h: Int): List<Offset> {
    var sx = -1
    var sy = -1
    loop@ for (y in 0 until h) for (x in 0 until w) {
        if (region[y * w + x]) { sx = x; sy = y; break@loop }
    }
    if (sx < 0) return emptyList()
    fun inside(x: Int, y: Int) = x in 0 until w && y in 0 until h && region[y * w + x]

    val out = ArrayList<Offset>()
    var cx = sx
    var cy = sy
    var bDir = 0                      // başlangıçta batı komşusu boştur
    out.add(Offset(cx + 0.5f, cy + 0.5f))
    var guard = 0
    while (guard++ < w * h * 4) {
        var moved = false
        for (k in 1..8) {
            val d = (bDir + k) % 8
            val nx = cx + DX[d]
            val ny = cy + DY[d]
            if (inside(nx, ny)) {
                val pd = (d + 7) % 8
                val bx = cx + DX[pd]
                val by = cy + DY[pd]
                cx = nx; cy = ny
                bDir = dirIndex(bx - cx, by - cy)
                moved = true
                break
            }
        }
        if (!moved) break
        if (cx == sx && cy == sy) break
        out.add(Offset(cx + 0.5f, cy + 0.5f))
    }
    return out
}

fun polygonArea(pts: List<Offset>): Float {
    var a = 0f
    for (i in pts.indices) {
        val p = pts[i]
        val q = pts[(i + 1) % pts.size]
        a += p.x * q.y - q.x * p.y
    }
    return abs(a) / 2f
}

// Dolgu şekillerini listenin başındaki "dolgu bloğu"na büyükten küçüğe doğru yerleştirir
fun insertFillStroke(list: MutableList<StrokeData>, fill: StrokeData) {
    val a = polygonArea(fill.points)
    var idx = 0
    while (idx < list.size && list[idx].fillOnly && polygonArea(list[idx].points) >= a) idx++
    list.add(idx, fill)
}

fun floodFillRegion(
    strokes: List<StrokeData>,
    seed: Offset,
    sceneW: Float,
    sceneH: Float,
    fillColor: Color,
    alpha: Float
): StrokeData? {
    val w = (sceneW * SCALE).toInt()
    val h = (sceneH * SCALE).toInt()
    val sx = (seed.x * SCALE).toInt()
    val sy = (seed.y * SCALE).toInt()
    if (sx !in 0 until w || sy !in 0 until h) return null

    // 1) Engel maskesi
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = android.graphics.Color.BLACK
    }
    for (s in strokes) {
        if (s.fillOnly || s.text != null || s.points.size < 2) continue
        val avgP = s.pressures?.average()?.toFloat() ?: 1f
        paint.strokeWidth = ((s.width * avgP + 3f) * SCALE).coerceAtLeast(2f)
        val path = Path()
        path.moveTo(s.points[0].x * SCALE, s.points[0].y * SCALE)
        for (i in 1 until s.points.size) path.lineTo(s.points[i].x * SCALE, s.points[i].y * SCALE)
        canvas.drawPath(path, paint)
    }
    val px = IntArray(w * h)
    bmp.getPixels(px, 0, w, 0, 0, w, h)
    bmp.recycle()
    val blocked = BooleanArray(w * h) { px[it] != 0 }

    // 2) Başlangıç noktası çizginin üstündeyse yakındaki boş pikseli bul
    var start = sy * w + sx
    if (blocked[start]) {
        var found = -1
        search@ for (r in 1..3) for (dy in -r..r) for (dx in -r..r) {
            val x = sx + dx
            val y = sy + dy
            if (x in 0 until w && y in 0 until h && !blocked[y * w + x]) { found = y * w + x; break@search }
        }
        if (found < 0) return null
        start = found
    }

    // 3) Scanline flood fill
    val region = BooleanArray(w * h)
    val stack = ArrayDeque<Int>()
    stack.add(start)
    var count = 0
    while (stack.isNotEmpty()) {
        val p = stack.removeLast()
        if (region[p] || blocked[p]) continue
        val y = p / w
        val x = p % w
        var l = x
        while (l > 0 && !blocked[y * w + l - 1] && !region[y * w + l - 1]) l--
        var r = x
        while (r < w - 1 && !blocked[y * w + r + 1] && !region[y * w + r + 1]) r++
        for (i in l..r) region[y * w + i] = true
        count += r - l + 1
        for (ny in intArrayOf(y - 1, y + 1)) {
            if (ny !in 0 until h) continue
            var i = l
            while (i <= r) {
                val q = ny * w + i
                if (!blocked[q] && !region[q]) {
                    stack.add(q)
                    while (i <= r && !blocked[ny * w + i] && !region[ny * w + i]) i++
                } else i++
            }
        }
    }
    if (count < 4) return null

    // 4) Dış çevre -> sadeleştirilmiş çokgen
    val loop = traceOuter(region, w, h)
    if (loop.size < 4) return null
    val simplified = simplifyPolyline(loop + loop.first(), 0.8f)
    if (simplified.size < 4) return null
    val poly = simplified.map { Offset(it.x / SCALE, it.y / SCALE) }

    return StrokeData(
        points = poly,
        color = fillColor,
        // Opak dolguda çizgilerin altına 3'er birim taşar (aradaki ince boşluğu kapatır)
        width = if (alpha >= 0.999f) 6f else 0f,
        alpha = alpha,
        isFilled = true,
        fillColor = fillColor,
        fillOnly = true
    )
}