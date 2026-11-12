package com.example.animator

import androidx.compose.ui.geometry.Offset

enum class SymmetryMode(val label: String) {
    OFF("⚖ Simetri: KAPALI"),
    VERTICAL("↔ Dikey Eksen"),
    HORIZONTAL("↕ Yatay Eksen"),
    BOTH("✛ Çift Eksen")
}

private fun mirror(s: StrokeData, flipX: Boolean, flipY: Boolean, sw: Float, sh: Float) =
    s.copy(points = s.points.map {
        Offset(if (flipX) sw - it.x else it.x, if (flipY) sh - it.y else it.y)
    })

fun symmetryCopies(s: StrokeData, mode: SymmetryMode, sw: Float, sh: Float): List<StrokeData> = when (mode) {
    SymmetryMode.OFF -> emptyList()
    SymmetryMode.VERTICAL -> listOf(mirror(s, true, false, sw, sh))
    SymmetryMode.HORIZONTAL -> listOf(mirror(s, false, true, sw, sh))
    SymmetryMode.BOTH -> listOf(
        mirror(s, true, false, sw, sh),
        mirror(s, false, true, sw, sh),
        mirror(s, true, true, sw, sh)
    )
}

private fun lf(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun lo(a: Offset, b: Offset, t: Float) = Offset(lf(a.x, b.x, t), lf(a.y, b.y, t))

// Chaikin köşe kesme: noktalar arası titremeyi yumuşatır, basınç değerlerini de enterpole eder
fun smoothStroke(points: List<Offset>, pressures: List<Float>?, iterations: Int): Pair<List<Offset>, List<Float>?> {
    var pts = points
    var pr = pressures
    for (iter in 0 until iterations) {
        if (pts.size < 3) break
        val cur = pr
        val np = ArrayList<Offset>()
        val npr: ArrayList<Float>? = if (cur != null) ArrayList() else null
        np.add(pts.first()); if (cur != null) npr!!.add(cur.first())
        for (i in 0 until pts.size - 1) {
            np.add(lo(pts[i], pts[i + 1], 0.25f)); np.add(lo(pts[i], pts[i + 1], 0.75f))
            if (cur != null) {
                npr!!.add(lf(cur[i], cur[i + 1], 0.25f)); npr.add(lf(cur[i], cur[i + 1], 0.75f))
            }
        }
        np.add(pts.last()); if (cur != null) npr!!.add(cur.last())
        pts = np; pr = npr
    }
    return pts to pr
}