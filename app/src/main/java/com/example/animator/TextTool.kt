package com.example.animator

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

fun textPaint(fontSize: Float, bold: Boolean): AndroidPaint =
    AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        textSize = fontSize
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

// Metin, 4 köşesi olan bir dikdörtgen olarak saklanır: points[0]=sol üst, points[1]=sağ üst.
// Döndürme/ölçekleme bu köşelerden okunur.
fun makeTextStroke(
    text: String, pos: Offset, fontSize: Float, bold: Boolean, color: Color, alpha: Float
): StrokeData {
    val paint = textPaint(fontSize, bold)
    val lines = text.split("\n")
    val w = lines.maxOf { paint.measureText(it) }.coerceAtLeast(1f)
    val fm = paint.fontMetrics
    val h = (fm.bottom - fm.top) * lines.size
    val pts = listOf(
        Offset(pos.x, pos.y), Offset(pos.x + w, pos.y),
        Offset(pos.x + w, pos.y + h), Offset(pos.x, pos.y + h),
        Offset(pos.x, pos.y)
    )
    return StrokeData(
        points = pts, color = color, width = 1f, alpha = alpha,
        text = text, fontSize = fontSize, textW = w, textBold = bold
    )
}

fun DrawScope.drawTextStroke(stroke: StrokeData, alphaMultiplier: Float) {
    val txt = stroke.text ?: return
    if (stroke.points.size < 2) return
    val p0 = stroke.points[0]
    val p1 = stroke.points[1]
    val sc = hypot(p1.x - p0.x, p1.y - p0.y) / stroke.textW.coerceAtLeast(1f)
    val ang = atan2(p1.y - p0.y, p1.x - p0.x) * 180f / PI.toFloat()

    val paint = textPaint(stroke.fontSize, stroke.textBold)
    val a = (stroke.alpha * alphaMultiplier * stroke.color.alpha).coerceIn(0f, 1f)
    paint.color = stroke.color.toArgb()
    paint.alpha = (a * 255).toInt()
    val fm = paint.fontMetrics
    val lineH = fm.bottom - fm.top

    withTransform({
        translate(p0.x, p0.y)
        rotate(ang, Offset.Zero)
        scale(sc, sc, Offset.Zero)
    }) {
        drawIntoCanvas { c ->
            txt.split("\n").forEachIndexed { i, line ->
                c.nativeCanvas.drawText(line, 0f, -fm.top + i * lineH, paint)
            }
        }
    }
}