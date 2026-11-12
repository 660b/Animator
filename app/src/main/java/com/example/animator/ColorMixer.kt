package com.example.animator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

private val Surface1 = Color(0xFF1B1B27)
private val Surface2 = Color(0xFF2A2A3A)
private val Accent = Color(0xFF7C4DFF)
private val Accent2 = Color(0xFF00D9A0)

private fun colorToHex(c: Color): String {
    val a = (c.alpha * 255).roundToInt(); val r = (c.red * 255).roundToInt()
    val g = (c.green * 255).roundToInt(); val b = (c.blue * 255).roundToInt()
    return if (a == 255) "#%02X%02X%02X".format(r, g, b) else "#%02X%02X%02X%02X".format(a, r, g, b)
}

private fun parseHex(text: String): Color? {
    val s = text.trim().removePrefix("#")
    val v = s.toLongOrNull(16) ?: return null
    return when (s.length) {
        6 -> Color((0xFF000000L or v).toInt())
        8 -> Color(v.toInt())
        else -> null
    }
}

// Renkleri lineer oran (0.0 .. 1.0) ile karıştırır
fun mixColors(c1: Color, c2: Color, ratio: Float): Color {
    val r = c1.red + (c2.red - c1.red) * ratio
    val g = c1.green + (c2.green - c1.green) * ratio
    val b = c1.blue + (c2.blue - c1.blue) * ratio
    val a = c1.alpha + (c2.alpha - c1.alpha) * ratio
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), a.coerceIn(0f, 1f))
}

@Composable
fun ColorMixerDialog(
    initialColor: Color,
    palette: List<Color>,
    recentColors: List<Color> = emptyList(),
    onColorSelected: (Color) -> Unit,
    onAddColorToPalette: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var color1 by remember { mutableStateOf(initialColor) }
    var color2 by remember { mutableStateOf(Color.White) }
    var mixRatio by remember { mutableFloatStateOf(0.5f) }

    var red by remember { mutableFloatStateOf(initialColor.red * 255f) }
    var green by remember { mutableFloatStateOf(initialColor.green * 255f) }
    var blue by remember { mutableFloatStateOf(initialColor.blue * 255f) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    val mixedColor = remember(color1, color2, mixRatio) {
        mixColors(color1, color2, mixRatio)
    }

    val customColor = remember(red, green, blue, alpha) {
        Color(
            (red / 255f).coerceIn(0f, 1f),
            (green / 255f).coerceIn(0f, 1f),
            (blue / 255f).coerceIn(0f, 1f),
            alpha.coerceIn(0f, 1f)
        )
    }

    var hexText by remember { mutableStateOf(colorToHex(initialColor)) }
    LaunchedEffect(customColor) {
        val h = colorToHex(customColor)
        if (!hexText.equals(h, ignoreCase = true)) hexText = h
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Karışım, 1 = RGBA Ayarı, 2 = HSV

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .background(Surface1)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🎨 Renk Karıştırıcı & Palet",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Surface2)
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp)
                    }
                }

                // Son kullanılan renkler
                if (recentColors.isNotEmpty()) {
                    Text("Son kullanılanlar", color = Color(0xFF9A9AB0), fontSize = 11.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentColors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        red = c.red * 255f
                                        green = c.green * 255f
                                        blue = c.blue * 255f
                                        alpha = c.alpha
                                        selectedTab = 1
                                    }
                            )
                        }
                    }
                }

                // Sekme seçimi
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("🌈 Karıştır", "🎛 RGBA", "🎡 HSV").forEachIndexed { i, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == i) Accent else Color.Transparent)
                                .clickable { selectedTab = i }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                if (selectedTab == 0) {
                    // --- İKİ RENK KARIŞTIRMA ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface2)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("1. Renk Seç", color = Color.White, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            palette.take(9).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            if (c == color1) 3.dp else 1.dp,
                                            if (c == color1) Accent2 else Color.Gray,
                                            CircleShape
                                        )
                                        .clickable { color1 = c }
                                )
                            }
                        }

                        Text("2. Renk Seç", color = Color.White, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            palette.take(9).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            if (c == color2) 3.dp else 1.dp,
                                            if (c == color2) Accent2 else Color.Gray,
                                            CircleShape
                                        )
                                        .clickable { color2 = c }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Karışım Oranı: %${(mixRatio * 100).toInt()}", color = Color.White, fontSize = 13.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color1)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                            Slider(
                                value = mixRatio,
                                onValueChange = { mixRatio = it },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Accent2,
                                    activeTrackColor = Accent2,
                                    inactiveTrackColor = Color.Gray
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color2)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }

                        // Karışım Sonucu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Sonuç: ", color = Color.White, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(mixedColor)
                                    .border(3.dp, Accent2, CircleShape)
                            )
                        }
                    }
                } else if (selectedTab == 1) {
                    // --- RGBA RENK SLIDERLARI & HEX ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface2)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hexText,
                            onValueChange = { t ->
                                hexText = t
                                parseHex(t)?.let { c ->
                                    red = c.red * 255f; green = c.green * 255f; blue = c.blue * 255f; alpha = c.alpha
                                }
                            },
                            label = { Text("HEX (#RRGGBB)", color = Color(0xFF9A9AB0)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Accent2, unfocusedBorderColor = Surface2
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kırmızı: ${red.toInt()}", color = Color(0xFFFF5252), fontSize = 12.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = red, onValueChange = { red = it }, valueRange = 0f..255f, modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Yeşil: ${green.toInt()}", color = Color(0xFF69F0AE), fontSize = 12.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = green, onValueChange = { green = it }, valueRange = 0f..255f, modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF69F0AE), activeTrackColor = Color(0xFF69F0AE))
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mavi: ${blue.toInt()}", color = Color(0xFF448AFF), fontSize = 12.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = blue, onValueChange = { blue = it }, valueRange = 0f..255f, modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF448AFF), activeTrackColor = Color(0xFF448AFF))
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Opaklık: %${(alpha * 100).toInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                            Slider(
                                value = alpha, onValueChange = { alpha = it }, valueRange = 0f..1f, modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = Accent2, activeTrackColor = Accent2)
                            )
                        }

                        // RGBA Sonucu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Seçilen: ", color = Color.White, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(customColor)
                                    .border(3.dp, Accent2, CircleShape)
                            )
                        }
                    }
                } else {
                    HsvTab(
                        initial = customColor,
                        onColorChange = { c ->
                            red = c.red * 255f
                            green = c.green * 255f
                            blue = c.blue * 255f
                            alpha = c.alpha
                        }
                    )
                }

                val activeChosenColor = if (selectedTab == 0) mixedColor else customColor

                // İşlem Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface2)
                            .clickable {
                                onAddColorToPalette(activeChosenColor)
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("➕ Palete Ekle", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Accent2)
                            .clickable {
                                onColorSelected(activeChosenColor)
                                onDismiss()
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓ Kullan", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}