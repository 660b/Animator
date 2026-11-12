package com.example.animator

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

enum class LayerBlend(val label: String, val mode: BlendMode) {
    NORMAL("Normal", BlendMode.SrcOver),
    MULTIPLY("Çarp", BlendMode.Multiply),
    SCREEN("Ekran", BlendMode.Screen),
    OVERLAY("Bindir", BlendMode.Overlay)
}

private fun DrawScope.drawLayerContent(frame: Frame, l: Layer) {
    frame.peekStrokes(l).forEach { drawStrokeDataEnhanced(it) }
    frame.instances.forEach { if (it.layerId == l.id) drawInstance(it) }
}

fun DrawScope.drawFrameLayers(frame: Frame, layers: List<Layer>, sceneW: Float, sceneH: Float) {
    val bounds = Rect(0f, 0f, sceneW, sceneH)
    layers.forEach { l ->
        if (!l.visible) return@forEach
        val plain = l.blend == LayerBlend.NORMAL && l.opacity >= 0.999f
        if (plain) {
            drawLayerContent(frame, l)
        } else {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    alpha = l.opacity.coerceIn(0f, 1f)
                    blendMode = l.blend.mode
                }
                canvas.saveLayer(bounds, paint)
                drawLayerContent(frame, l)
                canvas.restore()
            }
        }
    }
}