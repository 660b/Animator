package com.example.animator

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun lerpOffset(a: Offset, b: Offset, t: Float) =
    Offset(lerpF(a.x, b.x, t), lerpF(a.y, b.y, t))

// İki karakter örneği arasında t (0..1) oranında ara poz üretir
fun tweenInstance(a: CharInstance, b: CharInstance, t: Float): CharInstance {
    val ch = a.character
    val result = CharInstance(ch, lerpOffset(a.offset, b.offset, t))
    if (b.character !== ch || ch.rootId == -1) {
        a.pose.forEach { (id, p) -> result.pose[id] = p }
        return result
    }

    val parent = parentMap(ch.bones, ch.rootId)
    val rootA = a.pose[ch.rootId]
    val rootB = b.pose[ch.rootId]
    if (rootA == null || rootB == null) {
        a.pose.forEach { (id, p) -> result.pose[id] = p }
        return result
    }
    result.pose[ch.rootId] = lerpOffset(rootA, rootB, t)

    // parentMap kökten başlayarak eklendiği için ebeveyn her zaman çocuktan önce gelir
    parent.forEach { (child, par) ->
        val pa = a.pose[par]
        val ca = a.pose[child]
        val pb = b.pose[par]
        val cb = b.pose[child]
        val pr = result.pose[par]
        if (pa == null || ca == null || pb == null || cb == null || pr == null) return@forEach

        val angA = atan2(ca.y - pa.y, ca.x - pa.x)
        val angB = atan2(cb.y - pb.y, cb.x - pb.x)
        var diff = angB - angA
        while (diff > PI) diff -= (2 * PI).toFloat()
        while (diff < -PI) diff += (2 * PI).toFloat()
        val ang = angA + diff * t

        val lenA = hypot(ca.x - pa.x, ca.y - pa.y)
        val lenB = hypot(cb.x - pb.x, cb.y - pb.y)
        val len = lerpF(lenA, lenB, t)

        result.pose[child] = Offset(pr.x + cos(ang) * len, pr.y + sin(ang) * len)
    }
    return result
}

// a ve b karesi arasına count kadar ara kare üretir
fun makeInBetweens(a: Frame, b: Frame, count: Int): List<Frame> {
    return (1..count).map { i ->
        val t = i / (count + 1f)
        val f = Frame()
        a.instances.forEachIndexed { k, instA ->
            val instB = b.instances.getOrNull(k)
            f.instances.add(
                if (instB != null && instB.character === instA.character)
                    tweenInstance(instA, instB, t)
                else instA.copy()
            )
        }
        f
    }
}