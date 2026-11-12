package com.example.animator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun hsvColor(h: Float, s: Float, v: Float, a: Float = 1f): Color =
    Color(
        android.graphics.Color.HSVToColor(
            (a.coerceIn(0f, 1f) * 255).toInt(),
            floatArrayOf(h.coerceIn(0f, 359.99f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
        )
    )

@Composable
fun HsvTab(initial: Color, onColorChange: (Color) -> Unit) {
    val hsv0 = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(hsv0[0]) }
    var sat by remember { mutableFloatStateOf(hsv0[1]) }
    var value by remember { mutableFloatStateOf(hsv0[2]) }
    var a by remember { mutableFloatStateOf(initial.alpha) }

    fun emit() = onColorChange(hsvColor(hue, sat, value, a))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2A2A3A))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Doygunluk / parlaklık karesi
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun upd(p: Offset) {
                            sat = (p.x / size.width).coerceIn(0f, 1f)
                            value = 1f - (p.y / size.height).coerceIn(0f, 1f)
                            emit()
                        }
                        upd(down.position)
                        drag(down.id) { upd(it.position); it.consume() }
                    }
                }
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hsvColor(hue, 1f, 1f))))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val c = Offset(sat * size.width, (1f - value) * size.height)
            drawCircle(Color.White, 10.dp.toPx(), c, style = Stroke(3.dp.toPx()))
            drawCircle(Color.Black, 12.dp.toPx(), c, style = Stroke(1.dp.toPx()))
        }

        // Ton çubuğu
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun upd(p: Offset) {
                            hue = (p.x / size.width).coerceIn(0f, 0.9999f) * 360f
                            emit()
                        }
                        upd(down.position)
                        drag(down.id) { upd(it.position); it.consume() }
                    }
                }
        ) {
            drawRect(Brush.horizontalGradient(List(7) { hsvColor((it * 60f) % 360f, 1f, 1f) }))
            val x = hue / 360f * size.width
            drawRect(Color.White, Offset(x - 3.dp.toPx(), 0f), Size(6.dp.toPx(), size.height))
            drawRect(Color.Black, Offset(x - 3.dp.toPx(), 0f), Size(6.dp.toPx(), size.height), style = Stroke(1.dp.toPx()))
        }

        // Opaklık + önizleme
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Opaklık %${(a * 100).toInt()}", color = Color.White, fontSize = 12.sp)
            Slider(
                value = a,
                onValueChange = { a = it; emit() },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00D9A0),
                    activeTrackColor = Color(0xFF00D9A0),
                    inactiveTrackColor = Color.Gray
                )
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(hsvColor(hue, sat, value, a))
                    .border(3.dp, Color(0xFF00D9A0), CircleShape)
            )
        }
    }
}