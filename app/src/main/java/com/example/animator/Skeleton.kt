package com.example.animator

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val BoneColor = Color(0xFFFB8C00)
val JointColor = Color(0xFF1E88E5)
val RootColor = Color(0xFFE53935)

fun DrawScope.drawSkeleton(frame: Frame) {
    frame.bones.forEach { bone ->
        val a = frame.joints.firstOrNull { it.id == bone.a }
        val b = frame.joints.firstOrNull { it.id == bone.b }
        if (a != null && b != null) {
            drawLine(BoneColor, a.pos, b.pos, strokeWidth = 8f, cap = StrokeCap.Round)
        }
    }
    frame.joints.forEach { j ->
        val isRoot = j.id == frame.rootJointId
        val r = if (isRoot) 20f else 12f
        drawCircle(Color.White, radius = r + 4f, center = j.pos)
        drawCircle(if (isRoot) RootColor else JointColor, radius = r, center = j.pos)
    }
}

fun findJoint(frame: Frame, p: Offset, radius: Float = 40f, excludeId: Int = -1): Joint? =
    frame.joints.firstOrNull { it.id != excludeId && dist(it.pos, p) < radius }

fun deleteJoint(frame: Frame, id: Int) {
    frame.bones.removeAll { it.a == id || it.b == id }
    frame.joints.removeAll { it.id == id }
    if (frame.rootJointId == id) frame.rootJointId = -1
}
val BindColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF6D4C41)
)

private fun segDist(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l2 = dx * dx + dy * dy
    if (l2 == 0f) return hypot(p.x - a.x, p.y - a.y)
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

// Her çizgi için en yakın kemiğin indeksini döndürür
fun computeBinding(
    strokes: List<StrokeData>,
    joints: List<Joint>,
    bones: List<Bone>
): List<Int> {
    val segs = bones.map { b ->
        val a = joints.firstOrNull { it.id == b.a }?.pos
        val c = joints.firstOrNull { it.id == b.b }?.pos
        if (a != null && c != null) a to c else null
    }
    return strokes.map { s ->
        var best = -1
        var bestD = Float.MAX_VALUE
        segs.forEachIndexed { i, seg ->
            if (seg != null) {
                val avg = s.points.map { segDist(it, seg.first, seg.second) }.average().toFloat()
                if (avg < bestD) {
                    bestD = avg
                    best = i
                }
            }
        }
        best
    }
}
// Her çizgiyi bağlı olduğu kemiğin referans pozdan güncel poza dönüşümüyle çizer
fun DrawScope.drawInstance(inst: CharInstance, alpha: Float = 1f) {
    val ch = inst.character
    val ref = ch.joints.toMap()
    ch.strokes.forEachIndexed { i, s ->
        val bi = ch.binding.getOrNull(i) ?: -1
        val bone = if (bi >= 0) ch.bones.getOrNull(bi) else null
        val a0 = bone?.let { ref[it.a] }
        val b0 = bone?.let { ref[it.b] }
        val a1 = bone?.let { inst.pose[it.a] }
        val b1 = bone?.let { inst.pose[it.b] }

        val pts = if (a0 != null && b0 != null && a1 != null && b1 != null) {
            val ang = atan2(b1.y - a1.y, b1.x - a1.x) - atan2(b0.y - a0.y, b0.x - a0.x)
            val c = cos(ang)
            val sn = sin(ang)
            s.points.map { p ->
                val dx = p.x - a0.x
                val dy = p.y - a0.y
                Offset(a1.x + dx * c - dy * sn, a1.y + dx * sn + dy * c) + inst.offset
            }
        } else {
            s.points.map { it + inst.offset }
        }
        drawStrokeDataEnhanced(s.copy(points = pts), alphaMultiplier = alpha)
    }
}

// Poz modunda karakterin iskeletini çizer
fun DrawScope.drawInstanceSkeleton(inst: CharInstance) {
    val ch = inst.character
    ch.bones.forEach { b ->
        val a = inst.pose[b.a]
        val c = inst.pose[b.b]
        if (a != null && c != null) {
            drawLine(BoneColor, a + inst.offset, c + inst.offset, strokeWidth = 6f, cap = StrokeCap.Round)
        }
    }
    inst.pose.forEach { (id, p) ->
        val isRoot = id == ch.rootId
        val r = if (isRoot) 18f else 11f
        drawCircle(Color.White, radius = r + 4f, center = p + inst.offset)
        drawCircle(if (isRoot) RootColor else JointColor, radius = r, center = p + inst.offset)
    }
}

// Ebeveyn haritası: kökten başlayarak her eklemin ebeveynini bulur
fun parentMap(bones: List<Bone>, root: Int): Map<Int, Int> {
    val parent = mutableMapOf<Int, Int>()
    val visited = mutableSetOf(root)
    val queue = ArrayDeque<Int>()
    queue.add(root)
    while (queue.isNotEmpty()) {
        val cur = queue.removeFirst()
        bones.forEach { b ->
            val n = if (b.a == cur) b.b else if (b.b == cur) b.a else null
            if (n != null && visited.add(n)) {
                parent[n] = cur
                queue.add(n)
            }
        }
    }
    return parent
}

// Bir eklem ve altındaki tüm eklemler
fun subtree(parent: Map<Int, Int>, id: Int): Set<Int> {
    val result = mutableSetOf(id)
    var added = true
    while (added) {
        added = false
        parent.forEach { (child, par) ->
            if (par in result && result.add(child)) added = true
        }
    }
    return result
}

// Eklemi sürüklerken: ebeveyn etrafında döndürür (kemik boyu sabit)
fun poseDrag(inst: CharInstance, jointId: Int, pointerLocal: Offset) {
    val ch = inst.character
    val parent = parentMap(ch.bones, ch.rootId)
    val par = parent[jointId] ?: return
    val pPos = inst.pose[par] ?: return
    val jPos = inst.pose[jointId] ?: return
    val ang = atan2(pointerLocal.y - pPos.y, pointerLocal.x - pPos.x) -
            atan2(jPos.y - pPos.y, jPos.x - pPos.x)
    val c = cos(ang)
    val sn = sin(ang)
    subtree(parent, jointId).forEach { id ->
        val p = inst.pose[id] ?: return@forEach
        val dx = p.x - pPos.x
        val dy = p.y - pPos.y
        inst.pose[id] = Offset(pPos.x + dx * c - dy * sn, pPos.y + dx * sn + dy * c)
    }
}

// Poz modunda dokunulan eklemi bulur
fun findPoseJoint(inst: CharInstance, p: Offset, radius: Float = 40f): Int? =
    inst.pose.entries.firstOrNull { dist(it.value + inst.offset, p) < radius }?.key