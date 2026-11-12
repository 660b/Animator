package com.example.animator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform

@Composable
fun CharacterPreview(character: Character, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color.White)) {
        val pts = character.strokes.flatMap { it.points } + character.joints.map { it.second }
        if (pts.isEmpty()) return@Canvas
        val b = getPointsBounds(pts)
        val bw = b.width.coerceAtLeast(1f)
        val bh = b.height.coerceAtLeast(1f)
        val s = minOf(size.width / bw, size.height / bh) * 0.85f
        val ox = (size.width - bw * s) / 2f - b.left * s
        val oy = (size.height - bh * s) / 2f - b.top * s
        withTransform({
            translate(ox, oy)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawInstance(CharInstance(character))
        }
    }
}