package com.example.animator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Surface1 = Color(0xFF1B1B27)
private val Surface2 = Color(0xFF2A2A3A)
private val Accent = Color(0xFF7C4DFF)
private val Accent2 = Color(0xFF00D9A0)
private val Danger = Color(0xFFFF5470)
private val TextDim = Color(0xFF9A9AB0)

@Composable
private fun TlBtn(text: String, enabled: Boolean = true, color: Color = Surface2, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) color else Surface1)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = if (enabled) Color.White else Color(0xFF666677), fontSize = 11.sp)
    }
}

@Composable
fun FrameThumbView(frame: Frame, layers: List<Layer>, canvasW: Float, template: CanvasTemplate = CanvasTemplate.BLANK_LANDSCAPE, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color.White)) {
        val s = if (canvasW > 0f) size.width / canvasW else 0.2f
        scale(s, s, pivot = Offset.Zero) {
            drawCanvasTemplatePattern(template, size.width / s, size.height / s)
            drawFrameLayers(frame, layers, size.width / s, size.height / s)
        }
    }
}

@Composable
fun TimelineView(
    frames: List<Frame>,
    currentIndex: Int,
    layers: List<Layer>,
    activeLayerId: Int,
    canvasW: Float,
    canvasH: Float,
    isPlaying: Boolean,
    fps: Float,
    isLooping: Boolean,
    onionSkin: Boolean,
    onionAlpha: Float,
    audioFileName: String?,
    isAudioMuted: Boolean,
    template: CanvasTemplate = CanvasTemplate.BLANK_LANDSCAPE,
    onSelectFrame: (Int) -> Unit,
    onSelectLayer: (Int) -> Unit,
    onAddFrame: () -> Unit,
    onDuplicateFrame: () -> Unit,
    onDeleteFrame: () -> Unit,
    onMoveFrame: (from: Int, to: Int) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleLoop: () -> Unit,
    onFpsChange: (Float) -> Unit,
    onToggleOnionSkin: () -> Unit,
    onOnionAlphaChange: (Float) -> Unit,
    onPickAudio: () -> Unit,
    onToggleMuteAudio: () -> Unit,
    onRemoveAudio: () -> Unit,
    waveform: AudioWaveform?,
    multiSelect: Boolean,
    selectedFrames: List<Frame>,
    clipboardCount: Int,
    onToggleMultiSelect: () -> Unit,
    onToggleFrameSelected: (Int) -> Unit,
    onSelectAllFrames: () -> Unit,
    onCopyFrames: () -> Unit,
    onCutFrames: () -> Unit,
    onPasteFrames: () -> Unit,
    onDeleteSelectedFrames: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspect = if (canvasW > 0f && canvasH > 0f) canvasW / canvasH else 1.4f
    var showLayerTracks by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(Surface1)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- Üst Satır: Oynatma Kontrolleri + FPS Ayarları ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol: Oynatma Düğmeleri
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .clickable(enabled = !isPlaying && currentIndex > 0) { onSelectFrame(0) },
                    contentAlignment = Alignment.Center
                ) { Text("⏮", color = Color.White, fontSize = 13.sp) }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .clickable(enabled = !isPlaying && currentIndex > 0) { onSelectFrame(currentIndex - 1) },
                    contentAlignment = Alignment.Center
                ) { Text("◀", color = Color.White, fontSize = 13.sp) }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Danger else Accent2)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isPlaying) "⏹" else "▶",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .clickable(enabled = !isPlaying && currentIndex < frames.size - 1) { onSelectFrame(currentIndex + 1) },
                    contentAlignment = Alignment.Center
                ) { Text("▶", color = Color.White, fontSize = 13.sp) }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .clickable(enabled = !isPlaying && currentIndex < frames.size - 1) { onSelectFrame(frames.size - 1) },
                    contentAlignment = Alignment.Center
                ) { Text("⏭", color = Color.White, fontSize = 13.sp) }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isLooping) Accent else Surface2)
                        .clickable { onToggleLoop() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLooping) "🔁 Döngü: AÇIK" else "➡️ Döngü: KAPALI",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sağ: FPS Ayarı & Katmanlı Görünüm Butonu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showLayerTracks) Accent else Surface2)
                        .clickable { showLayerTracks = !showLayerTracks }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (showLayerTracks) "🗂 Katman Görünümü" else "🖼 Kare Görünümü",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "${fps.toInt()} FPS",
                    color = Accent2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = fps,
                    onValueChange = onFpsChange,
                    valueRange = 1f..30f,
                    modifier = Modifier.width(90.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Accent2,
                        activeTrackColor = Accent2,
                        inactiveTrackColor = Surface2
                    )
                )
            }
        }

        // --- SES KATMANI SATIRI (AUDIO TRACK) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Surface2)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("🎵 Ses:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (audioFileName != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Accent.copy(alpha = 0.35f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "🎶 $audioFileName",
                            color = Accent2,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                } else {
                    Text("Ses eklenmedi", color = TextDim, fontSize = 11.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (audioFileName == null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Accent)
                            .clickable { onPickAudio() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("➕ Ses Ekle", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isAudioMuted) Danger else Accent2)
                            .clickable { onToggleMuteAudio() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (isAudioMuted) "🔇 Sessiz" else "🔊 Sesli", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Surface1)
                            .clickable { onRemoveAudio() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🗑", color = Danger, fontSize = 10.sp)
                    }
                }
            }
        }

        if (waveform != null) {
            WaveformStrip(
                wf = waveform,
                fps = fps,
                frameCount = frames.size,
                currentIndex = currentIndex,
                enabled = !isPlaying,
                onSeekFrame = onSelectFrame
            )
        }

        // --- Orta Satır: Kare İşlem Araçları & Onion Skin ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Accent)
                        .clickable(enabled = !isPlaying) { onAddFrame() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("➕ Yeni Kare", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2)
                        .clickable(enabled = !isPlaying) { onDuplicateFrame() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("📋 Çoğalt", color = Color.White, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2)
                        .clickable(enabled = !isPlaying && currentIndex > 0) {
                            onMoveFrame(currentIndex, currentIndex - 1)
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("⬅", color = Color.White, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2)
                        .clickable(enabled = !isPlaying && currentIndex < frames.size - 1) {
                            onMoveFrame(currentIndex, currentIndex + 1)
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("➡", color = Color.White, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (frames.size > 1) Danger else Surface2)
                        .clickable(enabled = !isPlaying && frames.size > 1) { onDeleteFrame() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("🗑 Sil", color = Color.White, fontSize = 11.sp)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TlBtn(
                    if (multiSelect) "☑ Seçim: AÇIK" else "☐ Seç",
                    color = if (multiSelect) Accent else Surface2
                ) { onToggleMultiSelect() }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (onionSkin) Accent2 else Surface2)
                        .clickable { onToggleOnionSkin() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "🧅 Silik: " + if (onionSkin) "AÇIK" else "KAPALI",
                        color = if (onionSkin) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (multiSelect) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${selectedFrames.size} seçili", color = Accent2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TlBtn("Tümü", enabled = !isPlaying) { onSelectAllFrames() }
                TlBtn("📋 Kopyala", enabled = !isPlaying && selectedFrames.isNotEmpty()) { onCopyFrames() }
                TlBtn("✂ Kes", enabled = !isPlaying && selectedFrames.isNotEmpty()) { onCutFrames() }
                TlBtn("📌 Yapıştır ($clipboardCount)", enabled = !isPlaying && clipboardCount > 0, color = Accent2) { onPasteFrames() }
                TlBtn("🗑 Sil", enabled = !isPlaying && selectedFrames.isNotEmpty(), color = Danger) { onDeleteSelectedFrames() }
            }
        }

        // --- Alt Satır: Sahneler / Kareler Yatay Zaman Çizelgesi ---
        if (showLayerTracks) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (waveform != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .width(75.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface1)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("🎵 Ses", color = Accent2, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        frames.indices.forEach { i ->
                            val amp = waveform.maxIn(i / fps, (i + 1) / fps)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Surface1)
                                    .clickable(enabled = !isPlaying) { onSelectFrame(i) },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((3 + amp * 21).dp)
                                        .background(if (i == currentIndex) Accent2 else Accent.copy(alpha = 0.8f))
                                )
                            }
                        }
                    }
                }

                layers.reversed().forEach { layer ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .width(75.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (layer.id == activeLayerId) Accent else Surface1)
                                .clickable { onSelectLayer(layer.id) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                layer.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        frames.forEachIndexed { i, f ->
                            val strokeCount = f.peekStrokes(layer).size
                            val isSelectedCell = i == currentIndex && layer.id == activeLayerId
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelectedCell) Accent2
                                        else if (i == currentIndex) Accent.copy(alpha = 0.5f)
                                        else if (strokeCount > 0) Color(0xFF3B82F6)
                                        else Surface1
                                    )
                                    .clickable(enabled = !isPlaying) {
                                        if (multiSelect) onToggleFrameSelected(i)
                                        else { onSelectFrame(i); onSelectLayer(layer.id) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (strokeCount > 0) {
                                    Text("$strokeCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("${i + 1}", color = TextDim, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                frames.forEachIndexed { i, f ->
                    val isSelected = i == currentIndex
                    val isPicked = selectedFrames.contains(f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(aspect)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected || isPicked) 3.dp else 1.dp,
                                color = when {
                                    isPicked -> Danger
                                    isSelected -> Accent2
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !isPlaying) {
                                if (multiSelect) onToggleFrameSelected(i) else onSelectFrame(i)
                            }
                    ) {
                        FrameThumbView(
                            frame = f,
                            layers = layers,
                            canvasW = canvasW,
                            template = template,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Accent2 else Color(0xAA000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${i + 1}",
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (waveform != null) {
                            val amp = waveform.maxIn(i / fps, (i + 1) / fps)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .width(5.dp)
                                    .height((4 + amp * 30).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Accent2)
                            )
                        }
                        if (isPicked) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(Danger)
                                    .padding(horizontal = 5.dp)
                            ) {
                                Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}