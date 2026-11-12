package com.example.animator

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

// ---------- Tema ----------
private val BgDark = Color(0xFF101018)
private val Surface1 = Color(0xFF1B1B27)
private val Surface2 = Color(0xFF2A2A3A)
private val Accent = Color(0xFF7C4DFF)
private val Accent2 = Color(0xFF00D9A0)
private val Danger = Color(0xFFFF5470)
private val TextDim = Color(0xFF9A9AB0)

private enum class Tab(val label: String) {
    DRAW("🎨 Çiz"),
    SHAPE("◯ Şekil"),
    SKEL("🦴 İskelet"),
    TWEEN("🎞 Animasyon"),
    EDITOR("🛠 Editör"),
    REF("🖼 Referans"),
    GUIDE("📐 Kılavuz")
}

@Composable
private fun Chip(
    text: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    color: Color = Accent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) color else Surface2)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color.White else Color(0xFF666677),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun RoundBtn(
    text: String,
    enabled: Boolean = true,
    color: Color = Surface2,
    size: Int = 36,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (enabled) color else Surface1)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color.White else Color(0xFF555566),
            fontSize = if (size < 36) 12.sp else 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = TextDim, fontSize = 11.sp)
}

@Composable
private fun PanelRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun AppSlider(value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, w: Int = 130, steps: Int = 0) {
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        steps = steps,
        modifier = Modifier.width(w.dp),
        colors = SliderDefaults.colors(
            thumbColor = Accent2,
            activeTrackColor = Accent2,
            inactiveTrackColor = Surface2
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LayerRow(layer: Layer, selected: Boolean, onSelect: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Accent else Surface2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clickable { layer.visible = !layer.visible }
                .padding(horizontal = 7.dp, vertical = 8.dp)
        ) { Text(if (layer.visible) "👁" else "🚫", fontSize = 12.sp) }
        Box(
            modifier = Modifier
                .clickable { layer.locked = !layer.locked }
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) { Text(if (layer.locked) "🔒" else "🔓", fontSize = 12.sp) }
        Box(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = onSelect, onLongClick = onSettings)
                .padding(end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(
                layer.name +
                        (if (layer.shared) " (ortak)" else "") +
                        (if (layer.opacity < 0.999f) " ${(layer.opacity * 100).toInt()}%" else ""),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .clickable { onSettings() }
                .padding(horizontal = 7.dp, vertical = 8.dp)
        ) { Text("⚙", color = Color.White, fontSize = 12.sp) }
    }
}

fun DrawScope.drawCanvasTemplatePattern(
    template: CanvasTemplate,
    width: Float,
    height: Float
) {
    drawRect(color = Color.White, size = Size(width, height))

    when (template.pattern) {
        CanvasPattern.GRID -> {
            val step = 36f
            val gridColor = Color(0xFFE2E8F0)
            var x = step
            while (x < width) { drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f); x += step }
            var y = step
            while (y < height) { drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f); y += step }
        }
        CanvasPattern.LINE -> {
            val step = 36f
            drawLine(Color(0xFFFF8A80), Offset(50f, 0f), Offset(50f, height), strokeWidth = 2f)
            var y = step
            while (y < height) { drawLine(Color(0xFFCBD5E1), Offset(0f, y), Offset(width, y), strokeWidth = 1f); y += step }
        }
        CanvasPattern.BLANK -> {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawingScreen(
    activeProjectMeta: ProjectMeta = ProjectMeta("proj_default", "Proje 1", CanvasTemplate.BLANK_LANDSCAPE),
    onBackToProjects: () -> Unit = {}
) {
    val frames = remember { mutableStateListOf(Frame()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var onionSkin by remember { mutableStateOf(true) }
    var onionAlpha by remember { mutableFloatStateOf(0.35f) }

    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }
    var fps by remember { mutableFloatStateOf(12f) }

    val history = remember { History() }
    var pressureEnabled by remember { mutableStateOf(true) }
    var smoothing by remember { mutableFloatStateOf(0f) }
    var symmetry by remember { mutableStateOf(SymmetryMode.OFF) }
    var saveBadge by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Character?>(null) }
    var deleteTarget by remember { mutableStateOf<Character?>(null) }
    var layerSettings by remember { mutableStateOf<Layer?>(null) }
    val currentPressures = remember { mutableListOf<Float>() }

    // metin
    var textDialogOpen by remember { mutableStateOf(false) }
    var textEditPos by remember { mutableStateOf(Offset.Zero) }
    var textEditTarget by remember { mutableStateOf<StrokeData?>(null) }
    var textSize by remember { mutableFloatStateOf(48f) }
    var textBold by remember { mutableStateOf(false) }

    // kılavuz / cetvel
    var gridSnap by remember { mutableStateOf(false) }
    var gridStep by remember { mutableFloatStateOf(36f) }
    var guideSnap by remember { mutableStateOf(true) }
    var guideEdit by remember { mutableStateOf(false) }
    var showGuides by remember { mutableStateOf(true) }
    val guides = remember { mutableStateListOf<Guide>() }
    var rulerOn by remember { mutableStateOf(false) }
    var rulerA by remember { mutableStateOf(Offset.Zero) }
    var rulerB by remember { mutableStateOf(Offset.Zero) }

    // referans
    val refs = remember { mutableStateListOf<RefImage>() }
    var selectedRefId by remember { mutableIntStateOf(-1) }

    // çoklu seçim
    var multiSelect by remember { mutableStateOf(false) }
    val selectedFrames = remember { mutableStateListOf<Frame>() }
    val frameClipboard = remember { mutableStateListOf<Frame>() }
    val pickedFrames = frames.filter { selectedFrames.contains(it) }

    var topPanelOpen by remember { mutableStateOf(true) }
    var touchDrawingEnabled by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioFileName by remember { mutableStateOf<String?>(null) }
    var isAudioMuted by remember { mutableStateOf(false) }
    var waveform by remember { mutableStateOf<AudioWaveform?>(null) }

    var showExportDialog by remember { mutableStateOf(false) }

    var tool by remember { mutableStateOf(Tool.FREE) }
    var brushType by remember { mutableStateOf(BrushType.PEN) }
    var eraserMode by remember { mutableStateOf(EraserMode.NORMAL) }
    var eraserWidth by remember { mutableFloatStateOf(32f) }

    var shapeStart by remember { mutableStateOf(Offset.Zero) }
    var autoCorrect by remember { mutableStateOf(false) }
    var magnetSnap by remember { mutableStateOf(false) }
    var skeletonMode by remember { mutableStateOf(false) }
    var skelTool by remember { mutableStateOf(SkelTool.BONE) }
    var showBinding by remember { mutableStateOf(false) }
    var moveCharMode by remember { mutableStateOf(false) }
    var poseMode by remember { mutableStateOf(false) }
    var tweenCount by remember { mutableFloatStateOf(3f) }
    var tab by remember { mutableStateOf(Tab.DRAW) }
    var layerPanelOpen by remember { mutableStateOf(true) }
    var timelineOpen by remember { mutableStateOf(true) }
    var focusMode by remember { mutableStateOf(false) }
    var canvasW by remember { mutableFloatStateOf(0f) }
    var canvasH by remember { mutableFloatStateOf(0f) }

    var fitScale by remember { mutableFloatStateOf(1f) }
    var fitOffset by remember { mutableStateOf(Offset.Zero) }

    val selectedStrokes = remember { mutableStateListOf<StrokeData>() }
    var selectionBox by remember { mutableStateOf<Rect?>(null) }

    val context = LocalContext.current
    var statusText by remember { mutableStateOf("") }

    fun snapScene(p: Offset, useGrid: Boolean): Offset {
        var q = p
        if (useGrid && gridSnap) {
            q = Offset((q.x / gridStep).roundToInt() * gridStep, (q.y / gridStep).roundToInt() * gridStep)
        }
        if (guideSnap) {
            val th = 14f / zoomScale
            guides.forEach { g ->
                if (g.horizontal) { if (abs(q.y - g.pos) < th) q = Offset(q.x, g.pos) }
                else if (abs(q.x - g.pos) < th) q = Offset(g.pos, q.y)
            }
        }
        return q
    }

    fun projectOnRuler(p: Offset): Offset {
        val d = rulerB - rulerA
        val l2 = d.x * d.x + d.y * d.y
        if (l2 < 1f) return p
        val t = (((p.x - rulerA.x) * d.x + (p.y - rulerA.y) * d.y) / l2).coerceIn(0f, 1f)
        return Offset(rulerA.x + d.x * t, rulerA.y + d.y * t)
    }

    fun distToRuler(p: Offset): Float = dist(p, projectOnRuler(p))

    fun persistExtras() = saveExtras(context, activeProjectMeta.id, refs, guides)

    val gridDots = remember(gridSnap, gridStep, activeProjectMeta.template) {
        if (!gridSnap) emptyList<Offset>() else {
            val t = activeProjectMeta.template
            val out = ArrayList<Offset>()
            var y = 0f
            while (y <= t.sceneH) {
                var x = 0f
                while (x <= t.sceneW) { out.add(Offset(x, y)); x += gridStep }
                y += gridStep
            }
            out
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val t = activeProjectMeta.template
            val nextId = (refs.maxOfOrNull { it.id } ?: -1) + 1
            val r = importRefImage(context, activeProjectMeta.id, uri, t.sceneW, t.sceneH, nextId)
            if (r != null) {
                refs.add(r)
                selectedRefId = r.id
                persistExtras()
                statusText = "Referans eklendi 🖼"
            } else {
                statusText = "Görsel yüklenemedi"
            }
        }
    }

    fun deleteFrames(list: List<Frame>) {
        if (list.isEmpty()) return
        if (list.size >= frames.size) { statusText = "En az bir kare kalmalı"; return }
        val removed = list.map { frames.indexOf(it) to it }.sortedBy { it.first }
        val oldCurrent = currentIndex
        removed.asReversed().forEach { frames.removeAt(it.first) }
        currentIndex = currentIndex.coerceIn(0, frames.size - 1)
        selectedFrames.clear()
        history.push(
            undo = {
                removed.forEach { (i, f) -> frames.add(i, f) }
                currentIndex = oldCurrent.coerceIn(0, frames.size - 1)
            },
            redo = {
                removed.forEach { frames.remove(it.second) }
                currentIndex = oldCurrent.coerceIn(0, frames.size - 1)
            }
        )
    }

    fun pasteFrames() {
        if (frameClipboard.isEmpty()) { statusText = "Panoda kare yok"; return }
        val at = (if (pickedFrames.isNotEmpty()) frames.indexOf(pickedFrames.last()) else currentIndex) + 1
        val copies = frameClipboard.map { it.copy() }
        frames.addAll(at, copies)
        currentIndex = at
        history.push(
            undo = { frames.removeAll(copies); currentIndex = (at - 1).coerceIn(0, frames.size - 1) },
            redo = { frames.addAll(at, copies); currentIndex = at }
        )
        selectedFrames.clear()
        selectedFrames.addAll(copies)
        statusText = "${copies.size} kare yapıştırıldı 📌"
    }

    fun doUndo() { if (history.undo()) { selectedStrokes.clear(); statusText = "Geri alındı ↶" } }
    fun doRedo() { if (history.redo()) { selectedStrokes.clear(); statusText = "Yinelendi ↷" } }
    fun pressureFactor(c: PointerInputChange): Float =
        if (pressureEnabled && c.type == PointerType.Stylus) 0.25f + 0.75f * c.pressure.coerceIn(0f, 1f) else 1f

    var showColorMixer by remember { mutableStateOf(false) }

    val layers = remember { mutableStateListOf(Layer(0, "Arka Plan", shared = true), Layer(1, "Katman 1")) }
    var activeLayerId by remember { mutableIntStateOf(1) }
    val library = remember { mutableStateListOf<Character>() }

    val defaultPalette = listOf(
        Color.Black, Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
        Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFF6D4C41), Color.White
    )
    val palette = remember {
        mutableStateListOf<Color>().also { it.addAll(PalettePrefs.loadColors(context, PalettePrefs.KEY_PALETTE, defaultPalette)) }
    }
    val recentColors = remember {
        mutableStateListOf<Color>().also { it.addAll(PalettePrefs.loadColors(context, PalettePrefs.KEY_RECENT, emptyList())) }
    }
    fun pushRecent(c: Color) {
        recentColors.remove(c)
        recentColors.add(0, c)
        while (recentColors.size > 10) recentColors.removeAt(recentColors.lastIndex)
        PalettePrefs.saveColors(context, PalettePrefs.KEY_RECENT, recentColors)
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri
            audioFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Müzik.mp3"
            statusText = "Ses Eklendi: $audioFileName 🎵"
        }
    }

    LaunchedEffect(audioUri) {
        waveform = null
        val u = audioUri
        if (u != null) waveform = withContext(Dispatchers.IO) { computeWaveform(context, u) }
    }

    val mediaPlayer = remember { MediaPlayer() }
    DisposableEffect(audioUri) {
        if (audioUri != null) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, audioUri!!)
                mediaPlayer.prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            try { mediaPlayer.release() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(isPlaying, isAudioMuted, audioUri) {
        if (audioUri != null && !isAudioMuted) {
            if (isPlaying) {
                val targetMs = ((currentIndex / fps) * 1000f).toInt()
                try {
                    mediaPlayer.seekTo(targetMs)
                    mediaPlayer.start()
                } catch (e: Exception) { e.printStackTrace() }
            } else {
                try { if (mediaPlayer.isPlaying) mediaPlayer.pause() } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    fun loadInto(p: LoadedProject) {
        library.clear()
        library.addAll(p.library)
        frames.clear()
        frames.addAll(p.frames)
        if (frames.isEmpty()) frames.add(Frame())
        layers.clear()
        layers.addAll(p.layers)
        if (layers.isEmpty()) layers.add(Layer(0, "Katman 1"))
        activeLayerId = layers.last().id
        fps = p.fps
        currentIndex = 0
        history.clear()
    }

    LaunchedEffect(activeProjectMeta.id) {
        loadProjectById(context, activeProjectMeta.id)?.let { loadInto(it) }
        val (loadedRefs, loadedGuides) = loadExtras(context, activeProjectMeta.id)
        refs.clear(); refs.addAll(loadedRefs)
        guides.clear(); guides.addAll(loadedGuides)

        while (true) {
            delay(10_000)
            if (!isPlaying) {
                val ok = saveProjectById(context, activeProjectMeta, frames, layers, library, fps)
                persistExtras()
                saveBadge = if (ok) "☁ Otomatik kaydedildi ✓" else "⚠ Otomatik kayıt başarısız"
                delay(2_500)
                saveBadge = ""
            }
        }
    }

    LaunchedEffect(statusText) {
        if (statusText.isNotEmpty()) { delay(2_500); statusText = "" }
    }

    var editorMode by remember { mutableStateOf(false) }
    val editorFrame = remember { Frame() }
    val activeFrame = if (editorMode) editorFrame else frames[currentIndex]
    val activeLayer = layers.firstOrNull { it.id == activeLayerId } ?: layers.last()

    fun activeStrokes(): MutableList<StrokeData> =
        if (editorMode) editorFrame.strokes else frames[currentIndex].strokesOf(activeLayer)

    fun layerIndex(id: Int) = layers.indexOfFirst { it.id == id }
    fun orderedInstances(f: Frame): List<CharInstance> =
        f.instances
            .filter { inst -> layers.firstOrNull { it.id == inst.layerId }?.visible == true }
            .sortedBy { layerIndex(it.layerId) }

    var previewFrom by remember { mutableStateOf<Offset?>(null) }
    var previewTo by remember { mutableStateOf<Offset?>(null) }
    var eraserPos by remember { mutableStateOf<Offset?>(null) }

    val currentPoints = remember { mutableStateListOf<Offset>() }
    var penColor by remember { mutableStateOf(Color.Black) }
    var penWidth by remember { mutableFloatStateOf(6f) }
    var penAlpha by remember { mutableFloatStateOf(1f) }

    fun toLocal(p: Offset): Offset = (p - panOffset) / zoomScale

    fun fitToScene() {
        if (canvasW <= 0f || canvasH <= 0f) return
        val t = activeProjectMeta.template
        val s = minOf(canvasW / t.sceneW, canvasH / t.sceneH) * 0.99f
        fitScale = s
        zoomScale = s
        fitOffset = Offset((canvasW - t.sceneW * s) / 2f, (canvasH - t.sceneH * s) / 2f)
        panOffset = fitOffset
    }

    // Sahne alanı boyutu değişince (panel aç/kapa, odak modu) kullanıcı elle zoom/kaydırma
    // yapmadıysa sahneyi yeniden sığdır.
    LaunchedEffect(canvasW, canvasH) {
        val wasAtFit = abs(zoomScale - fitScale) <= 0.001f && (panOffset - fitOffset).getDistance() <= 1f
        if (wasAtFit) fitToScene()
    }

    LaunchedEffect(isPlaying, fps, isLooping) {
        while (isPlaying) {
            delay((1000f / fps).toLong())
            if (currentIndex < frames.size - 1) {
                currentIndex++
            } else {
                if (isLooping) {
                    currentIndex = 0
                } else {
                    isPlaying = false
                }
            }
        }
    }

    fun selectTab(t: Tab) {
        tab = t
        editorMode = t == Tab.EDITOR
        skeletonMode = t == Tab.SKEL || (t == Tab.EDITOR && skeletonMode)
        if (t == Tab.SKEL || t == Tab.EDITOR) {
            poseMode = false
            moveCharMode = false
        }
    }

    if (showColorMixer) {
        ColorMixerDialog(
            initialColor = penColor,
            palette = palette.toList(),
            recentColors = recentColors.toList(),
            onColorSelected = { selected ->
                penColor = selected
                penAlpha = selected.alpha
                pushRecent(selected)
            },
            onAddColorToPalette = { newColor ->
                if (!palette.contains(newColor)) {
                    palette.add(newColor)
                    PalettePrefs.saveColors(context, PalettePrefs.KEY_PALETTE, palette)
                }
            },
            onDismiss = { showColorMixer = false }
        )
    }

    if (showExportDialog) {
        var exportProgress by remember { mutableFloatStateOf(-1f) }
        var exportMsg by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { if (exportProgress < 0f) showExportDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .padding(18.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📤 Dışa Aktar", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Proje: ${activeProjectMeta.name} (${frames.size} Kare, ${fps.toInt()} FPS)",
                        color = TextDim,
                        fontSize = 12.sp
                    )
                    Text(
                        if (audioUri != null && !isAudioMuted) "🎵 $audioFileName videoya eklenecek"
                        else "🔇 Videoda ses olmayacak",
                        color = TextDim,
                        fontSize = 12.sp
                    )

                    if (exportProgress >= 0f) {
                        Text(
                            "Galerie Aktarılıyor... %${(exportProgress * 100).toInt()}",
                            color = Accent2,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        AppSlider(exportProgress, {}, 0f..1f, 180)
                    }

                    if (exportMsg.isNotEmpty()) {
                        Text(exportMsg, color = Color.White, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (exportProgress < 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Surface2)
                                    .clickable { showExportDialog = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("İptal", color = Color.White, fontSize = 13.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Accent2)
                                    .clickable {
                                        exportProgress = 0f
                                        exportProjectAsVideo(
                                            context = context,
                                            meta = activeProjectMeta,
                                            frames = frames,
                                            layers = layers,
                                            fps = fps,
                                            audioUri = if (isAudioMuted) null else audioUri,
                                            onProgress = { p -> exportProgress = p },
                                            onComplete = { msg ->
                                                exportProgress = -1f
                                                exportMsg = msg
                                            },
                                            onError = { err ->
                                                exportProgress = -1f
                                                exportMsg = err
                                            }
                                        )
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎥 MP4 Video Aktar", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Accent2)
                                    .clickable { showExportDialog = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tamam", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (textDialogOpen) {
        var txt by remember { mutableStateOf(textEditTarget?.text ?: "") }
        Dialog(onDismissRequest = { textDialogOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (textEditTarget != null) "🔤 Metni Düzenle" else "🔤 Metin Ekle",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = txt,
                    onValueChange = { txt = it },
                    label = { Text("Metin (birden çok satır olabilir)", color = TextDim) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Accent2, unfocusedBorderColor = Surface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Label("Boyut ${textSize.toInt()}")
                AppSlider(textSize, { textSize = it }, 12f..240f, 240)
                Chip("B  Kalın", selected = textBold, color = Accent2) { textBold = !textBold }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("İptal", modifier = Modifier.weight(1f)) { textDialogOpen = false }
                    if (textEditTarget != null) {
                        Chip("Sil", color = Danger, selected = true, modifier = Modifier.weight(1f)) {
                            val t = activeStrokes(); val before = t.toList()
                            t.remove(textEditTarget)
                            history.pushStrokes(t, before)
                            selectedStrokes.clear()
                            textDialogOpen = false
                        }
                    }
                    Chip("Tamam", color = Accent2, selected = true, modifier = Modifier.weight(1f)) {
                        val t = activeStrokes(); val before = t.toList()
                        val old = textEditTarget
                        if (txt.isNotBlank()) {
                            val ns = makeTextStroke(
                                txt, old?.points?.first() ?: textEditPos, textSize, textBold,
                                old?.color ?: penColor, old?.alpha ?: penAlpha
                            )
                            val i = if (old != null) t.indexOf(old) else -1
                            if (i >= 0) t[i] = ns else t.add(ns)
                        } else if (old != null) {
                            t.remove(old)
                        }
                        history.pushStrokes(t, before)
                        selectedStrokes.clear()
                        textDialogOpen = false
                    }
                }
            }
        }
    }

    renameTarget?.let { ch ->
        var text by remember(ch) { mutableStateOf(ch.name) }
        Dialog(onDismissRequest = { renameTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("✎ Karakteri Yeniden Adlandır", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Accent2, unfocusedBorderColor = Surface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("İptal", modifier = Modifier.weight(1f)) { renameTarget = null }
                    Chip("Kaydet", color = Accent2, selected = true, modifier = Modifier.weight(1f)) {
                        ch.name = text.ifBlank { ch.name }
                        renameTarget = null
                    }
                }
            }
        }
    }

    deleteTarget?.let { ch ->
        Dialog(onDismissRequest = { deleteTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🗑 \"${ch.name}\" silinsin mi?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Karakter, tüm karelerdeki örnekleriyle birlikte silinir. Bu işlem geri alınamaz.", color = TextDim, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("İptal", modifier = Modifier.weight(1f)) { deleteTarget = null }
                    Chip("Sil", color = Danger, selected = true, modifier = Modifier.weight(1f)) {
                        frames.forEach { f -> f.instances.removeAll { it.character === ch } }
                        library.remove(ch)
                        deleteTarget = null
                    }
                }
            }
        }
    }

    layerSettings?.let { l ->
        Dialog(onDismissRequest = { layerSettings = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface1)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("⚙ Katman Ayarları", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = l.name,
                    onValueChange = { l.name = it },
                    label = { Text("Ad", color = TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Accent2, unfocusedBorderColor = Surface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Label("Opaklık %${(l.opacity * 100).toInt()}")
                AppSlider(l.opacity, { l.opacity = it }, 0f..1f, 240)
                Label("Karışım modu")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LayerBlend.entries.forEach { b ->
                        Chip(b.label, selected = l.blend == b, color = Accent2) { l.blend = b }
                    }
                }
                Chip(
                    if (l.locked) "🔒 Kilitli" else "🔓 Kilitsiz",
                    selected = l.locked, color = Danger
                ) { l.locked = !l.locked }
                Chip("Tamam", color = Accent2, selected = true, modifier = Modifier.fillMaxWidth()) {
                    layerSettings = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ================= ÜST: başlık + açılır/kapanır sekmeler =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Surface1)
                .padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Chip("🏠 Projeler", color = Surface2) {
                    saveProjectById(context, activeProjectMeta, frames, layers, library, fps)
                    persistExtras()
                    onBackToProjects()
                }

                RoundBtn("↶", enabled = !isPlaying && history.canUndo, size = 32) { doUndo() }
                RoundBtn("↷", enabled = !isPlaying && history.canRedo, size = 32) { doRedo() }

                Column(modifier = Modifier.weight(1f)) {
                    Text(activeProjectMeta.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    val subtitle = when {
                        statusText.isNotEmpty() -> statusText
                        saveBadge.isNotEmpty() -> saveBadge
                        else -> "${activeProjectMeta.template.label} · Kare ${currentIndex + 1}/${frames.size}"
                    }
                    Text(
                        subtitle,
                        color = when {
                            statusText.isEmpty() && saveBadge.isNotEmpty() -> Accent2
                            editorMode -> Color(0xFFCE93D8)
                            else -> TextDim
                        },
                        fontSize = 11.sp
                    )
                }

                Chip(
                    if (focusMode) "⛶ Odak: AÇIK" else "⛶ Odak",
                    selected = focusMode,
                    color = Accent2
                ) { focusMode = !focusMode }

                Chip(
                    if (timelineOpen) "🎞 Zaman ▾" else "🎞 Zaman ▸",
                    enabled = !focusMode,
                    color = Surface2
                ) { timelineOpen = !timelineOpen }

                Chip(
                    if (topPanelOpen) "🔼 Sekmeler" else "🔽 Sekmeler",
                    enabled = !focusMode,
                    color = Surface2
                ) { topPanelOpen = !topPanelOpen }

                if (abs(zoomScale - fitScale) > 0.001f || (panOffset - fitOffset).getDistance() > 1f) {
                    Chip("🔍 %${(zoomScale / fitScale * 100).toInt()}", color = Accent2) { fitToScene() }
                }

                Chip(
                    if (touchDrawingEnabled) "👆 Parmak: ÇİZ" else "👆 Parmak: KAPALI",
                    selected = touchDrawingEnabled,
                    color = if (touchDrawingEnabled) Accent else Surface2
                ) { touchDrawingEnabled = !touchDrawingEnabled }

                Chip("📤 Dışa Aktar", color = Accent2) { showExportDialog = true }

                Chip("💾 Kaydet") {
                    val ok = saveProjectById(context, activeProjectMeta, frames, layers, library, fps)
                    persistExtras()
                    statusText = if (ok) "Kaydedildi ✓" else "Kayıt hatası"
                }
            }

            if (topPanelOpen && !focusMode) {
                PanelRow {
                    Tab.entries.forEach { t ->
                        Chip(t.label, selected = tab == t, enabled = !isPlaying) { selectTab(t) }
                    }
                }

                when (tab) {
                    Tab.DRAW -> Column {
                        PanelRow {
                            listOf(Tool.FREE, Tool.BRUSH, Tool.ERASER, Tool.FILL, Tool.SELECT, Tool.EYEDROPPER, Tool.TEXT).forEach { t ->
                                Chip(t.label, selected = t == tool, enabled = !isPlaying) {
                                    tool = t
                                    if (tool != Tool.SELECT) selectedStrokes.clear()
                                }
                            }
                        }

                        PanelRow {
                            palette.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (c == penColor) 3.dp else 1.dp,
                                            color = if (c == penColor) Accent2 else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                penColor = c
                                                if (selectedStrokes.isNotEmpty()) {
                                                    val target = activeStrokes()
                                                    val before = target.toList()
                                                    selectedStrokes.forEachIndexed { idx, s ->
                                                        val i = target.indexOf(s)
                                                        if (i >= 0) {
                                                            val updated = s.copy(color = c)
                                                            target[i] = updated
                                                            selectedStrokes[idx] = updated
                                                        }
                                                    }
                                                    history.pushStrokes(target, before)
                                                }
                                            },
                                            onLongClick = {
                                                if (palette.size > 1) {
                                                    palette.remove(c)
                                                    PalettePrefs.saveColors(context, PalettePrefs.KEY_PALETTE, palette)
                                                }
                                            }
                                        )
                                )
                            }

                            Chip("🎨 Karıştır", color = Accent2) { showColorMixer = true }

                            if (tool == Tool.BRUSH) {
                                BrushType.entries.forEach { bt ->
                                    Chip(bt.label, selected = bt == brushType) { brushType = bt }
                                }
                            }

                            if (tool == Tool.ERASER) {
                                EraserMode.entries.forEach { em ->
                                    Chip(em.label, selected = em == eraserMode, color = Danger) { eraserMode = em }
                                }
                                Label("Boyut ${eraserWidth.toInt()}")
                                AppSlider(eraserWidth, { eraserWidth = it }, 8f..100f, 110)
                            } else {
                                Label("Kalınlık ${penWidth.toInt()}")
                                AppSlider(penWidth, { penWidth = it }, 2f..50f, 110)

                                Label("Opaklık %${(penAlpha * 100).toInt()}")
                                AppSlider(penAlpha, { penAlpha = it }, 0.1f..1f, 100)
                            }

                            if (selectedStrokes.isNotEmpty()) {
                                Chip("📋 Kopyala", color = Accent2) {
                                    val t = activeStrokes(); val before = t.toList()
                                    val copies = selectedStrokes.map { transformStroke(it, delta = Offset(20f, 20f)) }
                                    t.addAll(copies)
                                    history.pushStrokes(t, before)
                                    selectedStrokes.clear(); selectedStrokes.addAll(copies)
                                }
                                Chip("🗑 Seçileni Sil", color = Danger) {
                                    val t = activeStrokes(); val before = t.toList()
                                    t.removeAll(selectedStrokes)
                                    history.pushStrokes(t, before)
                                    selectedStrokes.clear()
                                }
                                Chip("✕ Temizle") { selectedStrokes.clear() }
                            } else {
                                Chip("Temizle", enabled = !isPlaying, color = Danger) {
                                    val t = activeStrokes(); val before = t.toList()
                                    t.clear(); history.pushStrokes(t, before)
                                }
                            }
                        }

                        PanelRow {
                            Chip(if (pressureEnabled) "✒ Basınç: AÇIK" else "✒ Basınç: KAPALI", selected = pressureEnabled, color = Accent2) {
                                pressureEnabled = !pressureEnabled
                            }
                            Label("Düzgünlük %${(smoothing * 100).toInt()}")
                            AppSlider(smoothing, { smoothing = it }, 0f..1f, 110)
                            Chip(symmetry.label, selected = symmetry != SymmetryMode.OFF, color = Accent) {
                                symmetry = SymmetryMode.entries[(symmetry.ordinal + 1) % SymmetryMode.entries.size]
                            }
                        }
                    }

                    Tab.SHAPE -> PanelRow {
                        listOf(Tool.LINE, Tool.CIRCLE, Tool.RECT, Tool.TRIANGLE).forEach { t ->
                            Chip(t.label, selected = t == tool, enabled = !isPlaying) { tool = t }
                        }
                        Chip(
                            if (autoCorrect) "Şekil Düzeltme: AÇIK" else "Şekil Düzeltme: KAPALI",
                            selected = autoCorrect,
                            color = Accent2
                        ) { autoCorrect = !autoCorrect }

                        Chip(
                            if (magnetSnap) "🧲 Yapışma: AÇIK" else "🧲 Yapışma: KAPALI",
                            selected = magnetSnap,
                            color = Accent
                        ) { magnetSnap = !magnetSnap }

                        Label("Kalınlık ${penWidth.toInt()}")
                        AppSlider(penWidth, { penWidth = it }, 2f..50f, 120)
                    }

                    Tab.SKEL -> PanelRow {
                        SkelTool.entries.forEach { t ->
                            Chip(t.label, selected = t == skelTool, color = Color(0xFFFB8C00)) { skelTool = t }
                        }
                        Chip("İskeleti Sil", color = Danger) {
                            val before = activeFrame.skeletonState()
                            activeFrame.joints.clear(); activeFrame.bones.clear(); activeFrame.rootJointId = -1
                            history.pushSkeleton(activeFrame, before)
                        }
                    }

                    Tab.TWEEN -> PanelRow {
                        Chip("Silik", selected = onionSkin, color = Accent2) { onionSkin = !onionSkin }
                        Label("Ara kare: ${tweenCount.toInt()}")
                        AppSlider(tweenCount, { tweenCount = it }, 1f..8f, 120, steps = 6)
                        Chip(
                            "✨ Ara Kare Üret",
                            enabled = !isPlaying && currentIndex < frames.size - 1,
                            color = Accent2
                        ) {
                            val list = makeInBetweens(
                                frames[currentIndex],
                                frames[currentIndex + 1],
                                tweenCount.toInt()
                            )
                            val src = frames[currentIndex].instances
                            list.forEach { f ->
                                f.instances.forEachIndexed { k, inst ->
                                    src.getOrNull(k)?.let { inst.layerId = it.layerId }
                                }
                            }
                            val at = currentIndex + 1
                            frames.addAll(at, list); currentIndex = at
                            history.push(
                                undo = { frames.removeAll(list); currentIndex = (at - 1).coerceIn(0, frames.size - 1) },
                                redo = { frames.addAll(at, list); currentIndex = at }
                            )
                        }
                    }

                    Tab.EDITOR -> {
                        PanelRow {
                            Chip(
                                "İskelet: " + if (skeletonMode) "AÇIK" else "KAPALI",
                                selected = skeletonMode,
                                color = Color(0xFFFB8C00)
                            ) { skeletonMode = !skeletonMode }
                            if (skeletonMode) {
                                SkelTool.entries.forEach { t ->
                                    Chip(t.label, selected = t == skelTool, color = Color(0xFFFB8C00)) { skelTool = t }
                                }
                            }
                            Chip("Bağlantı", selected = showBinding, color = Accent2) { showBinding = !showBinding }
                            Chip("📚 Kütüphaneye Kaydet", color = Accent2) {
                                val f = editorFrame
                                if (f.strokes.isNotEmpty()) {
                                    library.add(
                                        Character(
                                            name = "Karakter ${library.size + 1}",
                                            strokes = f.strokes.toList(),
                                            joints = f.joints.map { it.id to it.pos },
                                            bones = f.bones.toList(),
                                            rootId = f.rootJointId,
                                            binding = computeBinding(f.strokes, f.joints, f.bones)
                                        )
                                    )
                                    statusText = "Kütüphaneye eklendi ✓"
                                }
                            }
                        }
                    }

                    Tab.REF -> Column {
                        PanelRow {
                            Chip("➕ Görsel Ekle", color = Accent2) { imagePicker.launch("image/*") }
                            refs.forEach { r ->
                                Chip("🖼 ${r.id + 1}", selected = r.id == selectedRefId) { selectedRefId = r.id }
                            }
                            if (refs.isEmpty()) Label("Alt katmana yarı saydam görsel ekler (videoya girmez)")
                        }
                        val sel = refs.firstOrNull { it.id == selectedRefId }
                        if (sel != null) {
                            PanelRow {
                                Label("Opaklık %${(sel.opacity * 100).toInt()}")
                                AppSlider(sel.opacity, { sel.opacity = it }, 0.05f..1f, 110)
                                Label("Boyut %${(sel.scale * 100).toInt()}")
                                AppSlider(
                                    sel.scale,
                                    { ns ->
                                        val cx = sel.offset.x + sel.bitmap.width * sel.scale / 2f
                                        val cy = sel.offset.y + sel.bitmap.height * sel.scale / 2f
                                        sel.scale = ns
                                        sel.offset = Offset(cx - sel.bitmap.width * ns / 2f, cy - sel.bitmap.height * ns / 2f)
                                    },
                                    0.05f..4f, 120
                                )
                            }
                            PanelRow {
                                Chip(if (sel.visible) "👁 Görünür" else "🚫 Gizli", selected = sel.visible, color = Accent2) {
                                    sel.visible = !sel.visible
                                }
                                Chip(if (sel.locked) "🔒 Konum Kilitli" else "🔓 Sürükleyerek Taşı", selected = sel.locked, color = Danger) {
                                    sel.locked = !sel.locked
                                }
                                Chip("🗑 Sil", color = Danger) {
                                    refs.remove(sel)
                                    deleteRefFile(context, activeProjectMeta.id, sel)
                                    selectedRefId = refs.firstOrNull()?.id ?: -1
                                    persistExtras()
                                }
                            }
                        }
                    }

                    Tab.GUIDE -> Column {
                        PanelRow {
                            Chip(if (gridSnap) "▦ Izgara: AÇIK" else "▦ Izgara: KAPALI", selected = gridSnap, color = Accent2) {
                                gridSnap = !gridSnap
                            }
                            listOf(18f, 36f, 72f).forEach { s ->
                                Chip("${s.toInt()}", selected = gridStep == s) { gridStep = s }
                            }
                            Chip(if (guideSnap) "🧲 Kılavuza Yapış: AÇIK" else "🧲 Kılavuza Yapış: KAPALI", selected = guideSnap, color = Accent) {
                                guideSnap = !guideSnap
                            }
                        }
                        PanelRow {
                            Chip("➕ Yatay") { guides.add(Guide(true, activeProjectMeta.template.sceneH / 2f)) }
                            Chip("➕ Dikey") { guides.add(Guide(false, activeProjectMeta.template.sceneW / 2f)) }
                            Chip("✋ Kılavuz Taşı", selected = guideEdit, color = Accent) { guideEdit = !guideEdit }
                            Chip(if (showGuides) "👁 Göster" else "🚫 Gizli", selected = showGuides) { showGuides = !showGuides }
                            Chip("🗑 Kılavuzları Sil", color = Danger) { guides.clear() }
                        }
                        PanelRow {
                            Chip(if (rulerOn) "📏 Cetvel: AÇIK" else "📏 Cetvel: KAPALI", selected = rulerOn, color = Color(0xFFFFC107)) {
                                rulerOn = !rulerOn
                                if (rulerOn && rulerA == rulerB) {
                                    val t = activeProjectMeta.template
                                    rulerA = Offset(t.sceneW * 0.25f, t.sceneH / 2f)
                                    rulerB = Offset(t.sceneW * 0.75f, t.sceneH / 2f)
                                }
                            }
                            Label("Uçlardan çevir, ortasından taşı; yakından çizince düz akar")
                        }
                    }
                }
            }
        }

        // ================= ORTA: çizim alanı + sağ dock =================
        val template = activeProjectMeta.template

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ---------- Çalışma alanı (kâğıt bunun üstünde durur) ----------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B2B3B))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged {
                            canvasW = it.width.toFloat()
                            canvasH = it.height.toFloat()
                        }
                        .pointerInput(
                            penColor, penWidth, penAlpha, currentIndex, isPlaying, tool,
                            brushType, eraserMode, eraserWidth, autoCorrect, magnetSnap, skeletonMode,
                            editorMode, skelTool, moveCharMode, poseMode, activeLayerId,
                            touchDrawingEnabled, fitScale, pressureEnabled, smoothing, symmetry,
                            gridSnap, gridStep, guideSnap, guideEdit, rulerOn, rulerA, rulerB, tab, selectedRefId
                        ) {
                            if (isPlaying) return@pointerInput

                            awaitEachGesture {
                                val firstDown = awaitFirstDown(requireUnconsumed = false)

                                var event = currentEvent
                                var pressedList = event.changes.filter { it.pressed }

                                if (pressedList.size < 2) {
                                    val nextEvent = withTimeoutOrNull(45L) { awaitPointerEvent() }
                                    if (nextEvent != null) {
                                        event = nextEvent
                                        pressedList = event.changes.filter { it.pressed }
                                    }
                                }

                                // --- 1) 2 PARMAK GESTURE ---
                                if (pressedList.size >= 2) {
                                    var isMoved = false
                                    var prevCentroid: Offset? = null
                                    var prevDist: Float? = null

                                    while (true) {
                                        val currentPressed = event.changes.filter { it.pressed }
                                        if (currentPressed.size < 2) break

                                        val p1 = currentPressed[0].position
                                        val p2 = currentPressed[1].position
                                        val currCentroid = (p1 + p2) / 2f
                                        val currDist = dist(p1, p2)

                                        if (prevCentroid != null && prevDist != null && prevDist > 5f) {
                                            val rawZoomFactor = if (prevDist > 0f) currDist / prevDist else 1f
                                            val oldScale = zoomScale
                                            val newScale = (oldScale * rawZoomFactor).coerceIn(fitScale * 0.3f, fitScale * 8f)
                                            val effectiveZoomFactor = newScale / oldScale

                                            if (abs(effectiveZoomFactor - 1f) > 0.008f || (currCentroid - prevCentroid).getDistance() > 2f) {
                                                isMoved = true
                                            }

                                            panOffset = currCentroid - (prevCentroid - panOffset) * effectiveZoomFactor
                                            zoomScale = newScale
                                        }
                                        prevCentroid = currCentroid
                                        prevDist = currDist

                                        currentPressed.forEach { it.consume() }
                                        event = awaitPointerEvent()
                                    }

                                    if (!isMoved && !isPlaying) {
                                        doUndo()
                                    }
                                    return@awaitEachGesture
                                }

                                // --- 2) TEK PARMAK DOKUNMA ---
                                val isTouch = firstDown.type == PointerType.Touch

                                if (isTouch && !touchDrawingEnabled) {
                                    var prevPos = firstDown.position
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val pressed = ev.changes.filter { it.pressed }
                                        if (pressed.isEmpty()) break

                                        if (pressed.size >= 2) {
                                            var isMoved = false
                                            var prevCentroid: Offset? = null
                                            var prevDist: Float? = null
                                            var curEv = ev
                                            while (true) {
                                                val pList = curEv.changes.filter { it.pressed }
                                                if (pList.size < 2) break
                                                val p1 = pList[0].position
                                                val p2 = pList[1].position
                                                val currCentroid = (p1 + p2) / 2f
                                                val currDist = dist(p1, p2)

                                                if (prevCentroid != null && prevDist != null && prevDist > 5f) {
                                                    val rawZoomFactor = if (prevDist > 0f) currDist / prevDist else 1f
                                                    val oldScale = zoomScale
                                                    val newScale = (oldScale * rawZoomFactor).coerceIn(fitScale * 0.3f, fitScale * 8f)
                                                    val effectiveZoomFactor = newScale / oldScale

                                                    if (abs(effectiveZoomFactor - 1f) > 0.008f || (currCentroid - prevCentroid).getDistance() > 2f) {
                                                        isMoved = true
                                                    }

                                                    panOffset = currCentroid - (prevCentroid - panOffset) * effectiveZoomFactor
                                                    zoomScale = newScale
                                                }
                                                prevCentroid = currCentroid
                                                prevDist = currDist
                                                pList.forEach { it.consume() }
                                                curEv = awaitPointerEvent()
                                            }
                                            if (!isMoved && !isPlaying) {
                                                doUndo()
                                            }
                                            break
                                        }

                                        val change = pressed.firstOrNull { it.id == firstDown.id } ?: break
                                        if (change.changedToUp()) break
                                        change.consume()

                                        val delta = change.position - prevPos
                                        panOffset += delta
                                        prevPos = change.position
                                    }
                                    return@awaitEachGesture
                                }

                                // --- 3) STYLUS / PEN VEYA PARMAKLA ÇİZİM ---
                                val localDownPos = toLocal(firstDown.position)

                                if (tool == Tool.EYEDROPPER) {
                                    val target = activeStrokes()
                                    val picked = sampleColorAt(target, localDownPos)
                                    if (picked != null) {
                                        penColor = picked
                                        penAlpha = picked.alpha
                                        pushRecent(picked)
                                        statusText = "Renk Seçildi 👁"
                                    }
                                    return@awaitEachGesture
                                }

                                // 1) Cetvel tutamaçları
                                if (rulerOn) {
                                    val r = 34f / zoomScale
                                    val mid = (rulerA + rulerB) / 2f
                                    val which = when {
                                        dist(localDownPos, rulerA) < r -> 0
                                        dist(localDownPos, rulerB) < r -> 1
                                        dist(localDownPos, mid) < r -> 2
                                        else -> -1
                                    }
                                    if (which >= 0) {
                                        drag(firstDown.id) { change ->
                                            change.consume()
                                            val d = (change.position - change.previousPosition) / zoomScale
                                            when (which) {
                                                0 -> rulerA += d
                                                1 -> rulerB += d
                                                else -> { rulerA += d; rulerB += d }
                                            }
                                        }
                                        return@awaitEachGesture
                                    }
                                }

                                // 2) Kılavuz düzenleme
                                if (guideEdit) {
                                    fun gd(g: Guide) = if (g.horizontal) abs(g.pos - localDownPos.y) else abs(g.pos - localDownPos.x)
                                    val g = guides.minByOrNull { gd(it) }
                                    val grabbed = g != null && gd(g) < 30f / zoomScale
                                    drag(firstDown.id) { change ->
                                        change.consume()
                                        if (grabbed && g != null) {
                                            val lp = toLocal(change.position)
                                            g.pos = if (g.horizontal) lp.y else lp.x
                                        }
                                    }
                                    if (grabbed && g != null) {
                                        val lim = if (g.horizontal) activeProjectMeta.template.sceneH else activeProjectMeta.template.sceneW
                                        if (g.pos < 0f || g.pos > lim) guides.remove(g)
                                    }
                                    return@awaitEachGesture
                                }

                                // 3) Referans görseli taşıma
                                if (tab == Tab.REF && !editorMode) {
                                    val r = refs.firstOrNull { it.id == selectedRefId && !it.locked }
                                    drag(firstDown.id) { change ->
                                        change.consume()
                                        if (r != null) r.offset += (change.position - change.previousPosition) / zoomScale
                                    }
                                    return@awaitEachGesture
                                }

                                // 4) Metin aracı
                                if (tool == Tool.TEXT) {
                                    if (!editorMode && activeLayer.locked) {
                                        statusText = "🔒 Katman kilitli"
                                        return@awaitEachGesture
                                    }
                                    val t = activeStrokes()
                                    val hit = t.lastOrNull {
                                        it.text != null && getPointsBounds(it.points).inflate(12f / zoomScale).contains(localDownPos)
                                    }
                                    drag(firstDown.id) { it.consume() }
                                    textEditTarget = hit
                                    textEditPos = localDownPos
                                    if (hit != null) { textSize = hit.fontSize; textBold = hit.textBold }
                                    textDialogOpen = true
                                    return@awaitEachGesture
                                }

                                if (poseMode && !editorMode) {
                                    var target: CharInstance? = null
                                    var jointId: Int? = null
                                    for (inst in orderedInstances(frames[currentIndex]).reversed()) {
                                        val j = findPoseJoint(inst, localDownPos)
                                        if (j != null) {
                                            target = inst
                                            jointId = j
                                            break
                                        }
                                    }
                                    val poseBefore = target?.snapshot()
                                    drag(firstDown.id) { change ->
                                        change.consume()
                                        val inst = target
                                        val id = jointId
                                        if (inst != null && id != null) {
                                            val localDelta = (change.position - change.previousPosition) / zoomScale
                                            if (id == inst.character.rootId) {
                                                inst.offset += localDelta
                                            } else {
                                                poseDrag(inst, id, toLocal(change.position) - inst.offset)
                                            }
                                        }
                                    }
                                    target?.let { t -> poseBefore?.let { history.pushInstance(t, it) } }
                                    return@awaitEachGesture
                                }

                                if (moveCharMode && !editorMode) {
                                    val target = orderedInstances(frames[currentIndex]).lastOrNull { inst ->
                                        val q = localDownPos - inst.offset
                                        snapPoint(q, inst.character.strokes, 40f) != q
                                    }
                                    val moveBefore = target?.snapshot()
                                    drag(firstDown.id) { change ->
                                        change.consume()
                                        if (target != null) {
                                            target.offset += (change.position - change.previousPosition) / zoomScale
                                        }
                                    }
                                    target?.let { t -> moveBefore?.let { history.pushInstance(t, it) } }
                                    return@awaitEachGesture
                                }

                                if (skeletonMode) {
                                    val frame = activeFrame
                                    val skelBefore = frame.skeletonState()
                                    val startJoint = findJoint(frame, localDownPos)

                                    when (skelTool) {
                                        SkelTool.BONE -> {
                                            var endPos = localDownPos
                                            var moved = false
                                            if (startJoint != null) previewFrom = startJoint.pos

                                            drag(firstDown.id) { change ->
                                                change.consume()
                                                endPos = toLocal(change.position)
                                                if (dist(localDownPos, endPos) > 20f) moved = true
                                                if (startJoint != null) previewTo = endPos
                                            }

                                            if (startJoint == null && !moved) {
                                                frame.joints.add(Joint(frame.nextJointId++, localDownPos))
                                            } else if (startJoint != null && moved) {
                                                val endJoint = findJoint(frame, endPos, excludeId = startJoint.id)
                                                    ?: Joint(frame.nextJointId++, endPos).also { frame.joints.add(it) }
                                                val exists = frame.bones.any {
                                                    (it.a == startJoint.id && it.b == endJoint.id) ||
                                                            (it.a == endJoint.id && it.b == startJoint.id)
                                                }
                                                if (!exists) frame.bones.add(Bone(startJoint.id, endJoint.id))
                                            }
                                            previewFrom = null
                                            previewTo = null
                                        }

                                        SkelTool.MOVE -> {
                                            drag(firstDown.id) { change ->
                                                change.consume()
                                                if (startJoint != null) {
                                                    val localDelta = (change.position - change.previousPosition) / zoomScale
                                                    if (startJoint.id == frame.rootJointId) {
                                                        frame.joints.forEach { it.pos = it.pos + localDelta }
                                                    } else {
                                                        startJoint.pos = toLocal(change.position)
                                                    }
                                                }
                                            }
                                        }

                                        SkelTool.DELETE -> {
                                            drag(firstDown.id) { change -> change.consume() }
                                            if (startJoint != null) deleteJoint(frame, startJoint.id)
                                        }

                                        SkelTool.ROOT -> {
                                            drag(firstDown.id) { change -> change.consume() }
                                            if (startJoint != null) frame.rootJointId = startJoint.id
                                        }
                                    }
                                    history.pushSkeleton(frame, skelBefore)
                                    return@awaitEachGesture
                                }

                                if (!editorMode && activeLayer.locked) {
                                    statusText = "🔒 Katman kilitli"
                                    return@awaitEachGesture
                                }

                                val holdMs = 600L
                                val isShapeTool = tool == Tool.LINE || tool == Tool.CIRCLE || tool == Tool.RECT || tool == Tool.TRIANGLE
                                val isFreeTool = tool == Tool.FREE || tool == Tool.BRUSH
                                val useRuler = rulerOn && isFreeTool && distToRuler(localDownPos) < 45f / zoomScale
                                val startPos = when {
                                    useRuler -> projectOnRuler(localDownPos)
                                    isShapeTool -> snapScene(localDownPos, true)
                                    isFreeTool -> snapScene(localDownPos, false)
                                    else -> localDownPos
                                }
                                shapeStart = startPos
                                currentPoints.clear()
                                currentPoints.add(startPos)

                                val editTarget = activeStrokes()
                                val editBefore = editTarget.toList()
                                currentPressures.clear()
                                currentPressures.add(pressureFactor(firstDown))

                                var snapped = false
                                var lastMoveTime = System.currentTimeMillis()
                                var lastPos = startPos
                                var smoothPos = startPos

                                var erasing = isEraserButton(currentEvent) || tool == Tool.ERASER

                                if (tool == Tool.FILL) {
                                    val t = activeStrokes()
                                    val before = t.toList()
                                    val visible = if (editorMode) editorFrame.strokes.toList()
                                    else layers.filter { it.visible }.flatMap { frames[currentIndex].peekStrokes(it) }
                                    val region = floodFillRegion(
                                        visible, localDownPos,
                                        activeProjectMeta.template.sceneW, activeProjectMeta.template.sceneH,
                                        penColor, penAlpha
                                    )
                                    if (region != null) insertFillStroke(t, region)
                                    else fillStrokeAt(t, localDownPos, penColor)
                                    history.pushStrokes(t, before)
                                    return@awaitEachGesture
                                }

                                if (tool == Tool.SELECT) {
                                    if (selectedStrokes.isNotEmpty()) {
                                        val b = getStrokesBounds(selectedStrokes).inflate(10f)
                                        val hitR = 30f / zoomScale
                                        val center = b.center
                                        val rotHandle = Offset(center.x, b.top - 40f / zoomScale)
                                        val corners = listOf(b.topLeft, b.topRight, b.bottomRight, b.bottomLeft)
                                        val rotHit = dist(localDownPos, rotHandle) < hitR
                                        val cornerIdx = corners.indexOfFirst { dist(localDownPos, it) < hitR }

                                        if (rotHit || cornerIdx >= 0) {
                                            val handleTarget = activeStrokes()
                                            val handleBefore = handleTarget.toList()
                                            val originals = selectedStrokes.toList()
                                            val indices = originals.map { o -> handleTarget.indexOfFirst { it === o } }
                                            val pivot = if (rotHit) center else corners[(cornerIdx + 2) % 4]
                                            val startVec = localDownPos - pivot
                                            val startDist = hypot(startVec.x, startVec.y).coerceAtLeast(1f)
                                            val startAng = atan2(startVec.y, startVec.x)

                                            drag(firstDown.id) { change ->
                                                change.consume()
                                                val v = toLocal(change.position) - pivot
                                                val updated = originals.map { o ->
                                                    if (rotHit) transformStroke(o, pivot = pivot, rotationRad = atan2(v.y, v.x) - startAng)
                                                    else transformStroke(o, pivot = pivot, scale = (hypot(v.x, v.y) / startDist).coerceIn(0.05f, 20f))
                                                }
                                                updated.forEachIndexed { k, u ->
                                                    val i = indices[k]
                                                    if (i >= 0) handleTarget[i] = u
                                                    selectedStrokes[k] = u
                                                }
                                            }
                                            history.pushStrokes(handleTarget, handleBefore)
                                            return@awaitEachGesture
                                        }
                                    }

                                    val target = activeStrokes()
                                    val clickedStroke = target.lastOrNull { s ->
                                        snapPoint(localDownPos, listOf(s), 25f) != localDownPos ||
                                                (s.text != null && getPointsBounds(s.points).contains(localDownPos))
                                    }

                                    if (clickedStroke != null) {
                                        if (!selectedStrokes.contains(clickedStroke)) {
                                            selectedStrokes.clear()
                                            selectedStrokes.add(clickedStroke)
                                        }
                                        val moveBefore = target.toList()
                                        drag(firstDown.id) { change ->
                                            change.consume()
                                            val delta = (change.position - change.previousPosition) / zoomScale
                                            selectedStrokes.indices.forEach { idx ->
                                                val s = selectedStrokes[idx]
                                                val i = target.indexOf(s)
                                                if (i >= 0) {
                                                    val updated = transformStroke(s, delta = delta)
                                                    target[i] = updated
                                                    selectedStrokes[idx] = updated
                                                }
                                            }
                                        }
                                        history.pushStrokes(target, moveBefore)
                                    } else {
                                        var endPos = localDownPos
                                        drag(firstDown.id) { change ->
                                            change.consume()
                                            endPos = toLocal(change.position)
                                            val rect = Rect(
                                                minOf(localDownPos.x, endPos.x),
                                                minOf(localDownPos.y, endPos.y),
                                                maxOf(localDownPos.x, endPos.x),
                                                maxOf(localDownPos.y, endPos.y)
                                            )
                                            selectionBox = rect
                                        }
                                        selectionBox?.let { r ->
                                            val inBox = findStrokesInRect(target, r)
                                            selectedStrokes.clear()
                                            selectedStrokes.addAll(inBox)
                                        }
                                        selectionBox = null
                                    }
                                    return@awaitEachGesture
                                }

                                if (erasing) {
                                    currentPoints.clear()
                                    currentPressures.clear()
                                    eraserPos = localDownPos
                                    if (eraserMode == EraserMode.PARTIAL) {
                                        erasePartialAt(activeStrokes(), localDownPos, (eraserWidth / 2f) / zoomScale)
                                    } else {
                                        eraseNormalAt(activeStrokes(), localDownPos, (eraserWidth / 2f) / zoomScale)
                                    }
                                }

                                while (true) {
                                    val remaining =
                                        (holdMs - (System.currentTimeMillis() - lastMoveTime)).coerceAtLeast(1L)
                                    val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }

                                    if (event == null) {
                                        if (!snapped && !erasing && isFreeTool && autoCorrect) {
                                            val r = recognizeShape(currentPoints.toList())
                                            if (r != null) {
                                                currentPoints.clear()
                                                currentPoints.addAll(r)
                                                currentPressures.clear()
                                                snapped = true
                                            }
                                        }
                                        lastMoveTime = System.currentTimeMillis() + 100000L
                                        continue
                                    }

                                    val change = event.changes.firstOrNull { it.id == firstDown.id } ?: break
                                    if (change.changedToUp()) break
                                    change.consume()

                                    val localChangePos = toLocal(change.position)

                                    if (!erasing && isEraserButton(event)) {
                                        erasing = true
                                        currentPoints.clear()
                                        currentPressures.clear()
                                    }

                                    if (erasing) {
                                        eraserPos = localChangePos
                                        if (eraserMode == EraserMode.PARTIAL) {
                                            erasePartialAt(activeStrokes(), localChangePos, (eraserWidth / 2f) / zoomScale)
                                        } else {
                                            eraseNormalAt(activeStrokes(), localChangePos, (eraserWidth / 2f) / zoomScale)
                                        }
                                        continue
                                    }

                                    if (snapped) continue

                                    if (isFreeTool) {
                                        val targetPos = if (useRuler) projectOnRuler(localChangePos) else snapScene(localChangePos, false)
                                        val k = 1f - smoothing * 0.85f
                                        smoothPos = smoothPos + (targetPos - smoothPos) * k
                                        if (hypot(smoothPos.x - lastPos.x, smoothPos.y - lastPos.y) > 3f) {
                                            currentPressures.add(pressureFactor(change))
                                            currentPoints.add(smoothPos)
                                            lastPos = smoothPos
                                            lastMoveTime = System.currentTimeMillis()
                                        }
                                    } else {
                                        currentPressures.clear()
                                        currentPoints.clear()
                                        currentPoints.addAll(generateShape(tool, shapeStart, snapScene(localChangePos, true)))
                                    }
                                }

                                eraserPos = null
                                if (!erasing && currentPoints.size >= 2) {
                                    val freehand = isFreeTool
                                    var finalPoints = currentPoints.toList()
                                    var finalPressures: List<Float>? =
                                        if (freehand && currentPressures.size == finalPoints.size && currentPressures.any { it != 1f })
                                            currentPressures.toList() else null
                                    val target = activeStrokes()

                                    if (freehand && !snapped && smoothing > 0.05f) {
                                        val (sp, spr) = smoothStroke(finalPoints, finalPressures, if (smoothing > 0.6f) 2 else 1)
                                        finalPoints = sp
                                        finalPressures = spr
                                    }

                                    if (magnetSnap && finalPoints.size >= 2 && dist(finalPoints.first(), finalPoints.last()) > 20f) {
                                        val others = target.toList()
                                        val m = finalPoints.toMutableList()
                                        m[0] = snapPoint(m.first(), others, 15f)
                                        m[m.size - 1] = snapPoint(m.last(), others, 15f)
                                        finalPoints = m
                                    }

                                    val newStroke = StrokeData(
                                        points = finalPoints,
                                        color = penColor,
                                        width = penWidth,
                                        alpha = penAlpha,
                                        brushType = if (tool == Tool.BRUSH) brushType else BrushType.PEN,
                                        pressures = finalPressures
                                    )
                                    target.add(newStroke)
                                    target.addAll(
                                        symmetryCopies(newStroke, symmetry, activeProjectMeta.template.sceneW, activeProjectMeta.template.sceneH)
                                    )
                                }
                                currentPoints.clear()
                                currentPressures.clear()
                                history.pushStrokes(editTarget, editBefore)
                            }
                        }
                ) {
                    val sw = template.sceneW
                    val sh = template.sceneH

                    withTransform({
                        translate(panOffset.x, panOffset.y)
                        scale(zoomScale, zoomScale, pivot = Offset.Zero)
                    }) {
                        // kâğıt gölgesi
                        drawRect(Color(0x66000000), topLeft = Offset(6f / zoomScale, 8f / zoomScale), size = Size(sw, sh))

                        // kâğıdın içi: her şey sahne sınırına kırpılır (video ile birebir aynı kadraj)
                        clipRect(0f, 0f, sw, sh) {
                            drawCanvasTemplatePattern(template, sw, sh)
                            if (editorMode) drawRect(Color(0x22FFC107), size = Size(sw, sh))

                            // referans görseller (kâğıdın hemen üstünde, çizimlerin altında)
                            refs.forEach { r ->
                                if (r.visible) {
                                    withTransform({
                                        translate(r.offset.x, r.offset.y)
                                        scale(r.scale, r.scale, pivot = Offset.Zero)
                                    }) {
                                        drawImage(r.bitmap, alpha = r.opacity, filterQuality = FilterQuality.Medium)
                                    }
                                }
                            }

                            if (gridSnap && gridDots.isNotEmpty()) {
                                drawPoints(
                                    gridDots, PointMode.Points, Color(0xAA3B82F6),
                                    strokeWidth = 4f / zoomScale, cap = StrokeCap.Round
                                )
                            }

                            if (onionSkin && !isPlaying && !editorMode && currentIndex > 0) {
                                val prev = frames[currentIndex - 1]
                                layers.forEach { l ->
                                    if (l.visible && !l.shared) {
                                        prev.peekStrokes(l).forEach { s ->
                                            drawStrokeDataEnhanced(s, alphaMultiplier = onionAlpha)
                                        }
                                    }
                                }
                                orderedInstances(prev).forEach { drawInstance(it, onionAlpha) }
                            }

                            if (editorMode) {
                                val bind = if (showBinding)
                                    computeBinding(activeFrame.strokes, activeFrame.joints, activeFrame.bones)
                                else null
                                activeFrame.strokes.forEachIndexed { i, s ->
                                    val bi = bind?.getOrNull(i) ?: -1
                                    val c = if (bi >= 0) BindColors[bi % BindColors.size] else s.color
                                    drawStrokeDataEnhanced(s.copy(color = c))
                                }
                            } else {
                                drawFrameLayers(frames[currentIndex], layers, sw, sh)
                                if (poseMode && !isPlaying) {
                                    orderedInstances(frames[currentIndex]).forEach { drawInstanceSkeleton(it) }
                                }
                            }

                            if (currentPoints.size >= 2) {
                                val preview = StrokeData(
                                    points = currentPoints.toList(),
                                    color = penColor,
                                    width = penWidth,
                                    alpha = penAlpha,
                                    brushType = if (tool == Tool.BRUSH) brushType else BrushType.PEN,
                                    pressures = if (currentPressures.size == currentPoints.size && currentPressures.any { it != 1f })
                                        currentPressures.toList() else null
                                )
                                drawStrokeDataEnhanced(preview)
                                symmetryCopies(preview, symmetry, sw, sh).forEach { drawStrokeDataEnhanced(it) }
                            }

                            if (symmetry != SymmetryMode.OFF) {
                                val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                                val axis = Accent.copy(alpha = 0.7f)
                                if (symmetry != SymmetryMode.HORIZONTAL)
                                    drawLine(axis, Offset(sw / 2f, 0f), Offset(sw / 2f, sh), strokeWidth = 2f / zoomScale, pathEffect = dash)
                                if (symmetry != SymmetryMode.VERTICAL)
                                    drawLine(axis, Offset(0f, sh / 2f), Offset(sw, sh / 2f), strokeWidth = 2f / zoomScale, pathEffect = dash)
                            }
                        }

                        // kâğıt çerçevesi (kâğıtla birlikte hareket eder)
                        drawRect(
                            color = if (editorMode) Color(0xFFCE93D8) else Color(0xFF3B82F6),
                            topLeft = Offset.Zero,
                            size = Size(sw, sh),
                            style = Stroke(width = 3f / zoomScale)
                        )

                        eraserPos?.let {
                            val r = (eraserWidth / 2f) / zoomScale
                            drawCircle(Color(0x55FF5470), radius = r, center = it)
                            drawCircle(Danger, radius = r, center = it, style = Stroke(width = 2.5f / zoomScale))
                        }

                        if (selectedStrokes.isNotEmpty()) {
                            val bounds = getStrokesBounds(selectedStrokes).inflate(10f)
                            drawRect(
                                color = Accent2,
                                topLeft = bounds.topLeft,
                                size = bounds.size,
                                style = Stroke(width = 2.5f / zoomScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                            )
                            val hr = 11f / zoomScale
                            val ring = 2f / zoomScale
                            listOf(bounds.topLeft, bounds.topRight, bounds.bottomRight, bounds.bottomLeft).forEach { c ->
                                drawCircle(Color.White, hr + ring, c)
                                drawCircle(Accent2, hr, c)
                            }
                            val rot = Offset(bounds.center.x, bounds.top - 40f / zoomScale)
                            drawLine(Accent2, Offset(bounds.center.x, bounds.top), rot, strokeWidth = 2f / zoomScale)
                            drawCircle(Color.White, hr + ring, rot)
                            drawCircle(Accent, hr, rot)
                        }

                        selectionBox?.let { r ->
                            drawRect(
                                color = Accent,
                                topLeft = r.topLeft,
                                size = r.size,
                                style = Stroke(width = 2f / zoomScale)
                            )
                        }

                        if (tab == Tab.REF) {
                            refs.firstOrNull { it.id == selectedRefId }?.let { r ->
                                drawRect(
                                    Accent, topLeft = r.offset,
                                    size = Size(r.bitmap.width * r.scale, r.bitmap.height * r.scale),
                                    style = Stroke(2f / zoomScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                                )
                            }
                        }

                        if (showGuides) {
                            val gc = Color(0xCC00E5FF)
                            val dash = PathEffect.dashPathEffect(floatArrayOf(12f / zoomScale, 8f / zoomScale))
                            guides.forEach { g ->
                                if (g.horizontal) drawLine(gc, Offset(0f, g.pos), Offset(sw, g.pos), strokeWidth = 1.5f / zoomScale, pathEffect = dash)
                                else drawLine(gc, Offset(g.pos, 0f), Offset(g.pos, sh), strokeWidth = 1.5f / zoomScale, pathEffect = dash)
                            }
                        }

                        if (rulerOn) {
                            val dx = rulerB.x - rulerA.x
                            val dy = rulerB.y - rulerA.y
                            val len = hypot(dx, dy).coerceAtLeast(1f)
                            val ux = dx / len; val uy = dy / len
                            val nx = -uy; val ny = ux
                            val half = 26f
                            val band = Path().apply {
                                moveTo(rulerA.x + nx * half, rulerA.y + ny * half)
                                lineTo(rulerB.x + nx * half, rulerB.y + ny * half)
                                lineTo(rulerB.x - nx * half, rulerB.y - ny * half)
                                lineTo(rulerA.x - nx * half, rulerA.y - ny * half)
                                close()
                            }
                            drawPath(band, Color(0x55FFC107))
                            drawPath(band, Color(0xFFFFC107), style = Stroke(2f / zoomScale))
                            var d = 0f
                            var ti = 0
                            while (d <= len) {
                                val tl = if (ti % 5 == 0) 16f else 8f
                                val px = rulerA.x + ux * d
                                val py = rulerA.y + uy * d
                                drawLine(
                                    Color(0xFF8D6E00),
                                    Offset(px + nx * half, py + ny * half),
                                    Offset(px + nx * (half - tl), py + ny * (half - tl)),
                                    strokeWidth = 1.5f / zoomScale
                                )
                                d += 20f; ti++
                            }
                            val hr = 13f / zoomScale
                            listOf(rulerA, rulerB, (rulerA + rulerB) / 2f).forEachIndexed { k, c ->
                                drawCircle(Color.White, hr + 2f / zoomScale, c)
                                drawCircle(if (k == 2) Accent else Color(0xFFFFC107), hr, c)
                            }
                        }

                        if (skeletonMode && !isPlaying) {
                            drawSkeleton(activeFrame)
                            val f = previewFrom
                            val t = previewTo
                            if (f != null && t != null) {
                                drawLine(
                                    BoneColor.copy(alpha = 0.5f),
                                    f,
                                    t,
                                    strokeWidth = 8f / zoomScale,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                // ---------- Mini oynatma çubuğu (zaman çizelgesi gizliyken) ----------
                if ((focusMode || !timelineOpen) && !editorMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Surface1.copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoundBtn("⏮", enabled = !isPlaying && currentIndex > 0) { currentIndex-- }
                        RoundBtn(if (isPlaying) "⏸" else "▶", color = Accent2, size = 44) {
                            if (!isPlaying && currentIndex == frames.size - 1) currentIndex = 0
                            isPlaying = !isPlaying
                        }
                        RoundBtn("⏭", enabled = !isPlaying && currentIndex < frames.size - 1) { currentIndex++ }
                        Text(
                            "${currentIndex + 1}/${frames.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (focusMode) {
                            Chip("✕ Çık", color = Surface2) { focusMode = false }
                        }
                    }
                }
            }

            // ---------- Aç/kapa oku ----------
            if (!focusMode) Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(20.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface1)
                    .clickable { layerPanelOpen = !layerPanelOpen },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (layerPanelOpen) "▶" else "◀",
                    color = Accent2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ---------- Sağ dock (Kütüphane & Katmanlar) ----------
            if (layerPanelOpen && !focusMode) {
                Column(
                    modifier = Modifier
                        .width(190.dp)
                        .fillMaxHeight()
                        .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface1)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🧍 Kütüphane", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (library.isEmpty()) Label("Editör'den karakter ekle")
                            library.toList().forEach { ch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Surface2)
                                        .clickable(enabled = !isPlaying && !editorMode) {
                                            frames[currentIndex].instances.add(CharInstance(ch).also { it.layerId = activeLayer.id })
                                        }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CharacterPreview(ch, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                                    Text(ch.name, color = Color.White, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                    Text("✎", color = Color.White, fontSize = 14.sp,
                                        modifier = Modifier.clickable { renameTarget = ch }.padding(4.dp))
                                    Text("🗑", color = Danger, fontSize = 13.sp,
                                        modifier = Modifier.clickable { deleteTarget = ch }.padding(4.dp))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Chip(
                                "✋",
                                selected = moveCharMode,
                                enabled = !isPlaying && !editorMode,
                                color = Accent2,
                                modifier = Modifier.weight(1f)
                            ) {
                                moveCharMode = !moveCharMode
                                if (moveCharMode) {
                                    poseMode = false
                                    skeletonMode = false
                                }
                            }
                            Chip(
                                "🕺",
                                selected = poseMode,
                                enabled = !isPlaying && !editorMode,
                                color = Danger,
                                modifier = Modifier.weight(1f)
                            ) {
                                poseMode = !poseMode
                                if (poseMode) {
                                    moveCharMode = false
                                    skeletonMode = false
                                }
                            }
                            Chip(
                                "🗑",
                                enabled = !isPlaying && !editorMode && frames[currentIndex].instances.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) { frames[currentIndex].instances.removeLastOrNull() }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.55f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface1)
                            .verticalScroll(rememberScrollState())
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🗂 Katmanlar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val idx = layers.indexOfFirst { it.id == activeLayer.id }
                        layers.reversed().forEach { l ->
                            LayerRow(
                                l, l.id == activeLayer.id,
                                onSelect = { activeLayerId = l.id },
                                onSettings = { layerSettings = l }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            RoundBtn("+", enabled = !isPlaying, color = Accent2, size = 28) {
                                val newId = (layers.maxOfOrNull { it.id } ?: -1) + 1
                                layers.add(idx + 1, Layer(newId, "Katman $newId"))
                                activeLayerId = newId
                            }
                            RoundBtn("◈", enabled = !isPlaying, color = Accent2, size = 28) {
                                val newId = (layers.maxOfOrNull { it.id } ?: -1) + 1
                                layers.add(idx + 1, Layer(newId, "Ortak $newId", shared = true))
                                activeLayerId = newId
                            }
                            RoundBtn("▲", enabled = !isPlaying && idx < layers.size - 1, size = 28) {
                                layers.add(idx + 1, layers.removeAt(idx))
                            }
                            RoundBtn("▼", enabled = !isPlaying && idx > 0, size = 28) {
                                layers.add(idx - 1, layers.removeAt(idx))
                            }
                            RoundBtn("🗑", enabled = !isPlaying && layers.size > 1, color = Danger, size = 28) {
                                val removed = layers.removeAt(idx)
                                frames.forEach { f ->
                                    f.layerStrokes.remove(removed.id)
                                    f.instances.removeAll { it.layerId == removed.id }
                                }
                                activeLayerId = layers[minOf(idx, layers.size - 1)].id
                            }
                        }
                    }
                }
            }
        }

        // ================= ALT: ZAMAN ÇİZELGESİ (TIMELINE) =================
        if (timelineOpen && !focusMode && !editorMode) {
            TimelineView(
                frames = frames,
                currentIndex = currentIndex,
                layers = layers,
                activeLayerId = activeLayerId,
                canvasW = activeProjectMeta.template.sceneW,
                canvasH = activeProjectMeta.template.sceneH,
                isPlaying = isPlaying,
                fps = fps,
                isLooping = isLooping,
                onionSkin = onionSkin,
                onionAlpha = onionAlpha,
                audioFileName = audioFileName,
                isAudioMuted = isAudioMuted,
                template = activeProjectMeta.template,
                onSelectFrame = { currentIndex = it },
                onSelectLayer = { activeLayerId = it },
                onAddFrame = {
                    val newFrame = Frame()
                    frames[currentIndex].instances.forEach { newFrame.instances.add(it.copy()) }
                    val at = currentIndex + 1
                    frames.add(at, newFrame); currentIndex = at
                    history.push(
                        undo = { frames.remove(newFrame); currentIndex = (at - 1).coerceIn(0, frames.size - 1) },
                        redo = { frames.add(at, newFrame); currentIndex = at }
                    )
                },
                onDuplicateFrame = {
                    val dup = frames[currentIndex].copy()
                    val at = currentIndex + 1
                    frames.add(at, dup); currentIndex = at
                    history.push(
                        undo = { frames.remove(dup); currentIndex = (at - 1).coerceIn(0, frames.size - 1) },
                        redo = { frames.add(at, dup); currentIndex = at }
                    )
                },
                onDeleteFrame = {
                    if (frames.size > 1) {
                        val at = currentIndex
                        val removed = frames.removeAt(at)
                        currentIndex = at.coerceAtMost(frames.size - 1)
                        history.push(
                            undo = { frames.add(at, removed); currentIndex = at },
                            redo = { frames.remove(removed); currentIndex = at.coerceAtMost(frames.size - 1) }
                        )
                    }
                },
                onMoveFrame = { from, to ->
                    if (from in frames.indices && to in frames.indices && from != to) {
                        val f = frames.removeAt(from); frames.add(to, f); currentIndex = to
                        history.push(
                            undo = { frames.removeAt(to); frames.add(from, f); currentIndex = from },
                            redo = { frames.removeAt(from); frames.add(to, f); currentIndex = to }
                        )
                    }
                },
                onTogglePlay = {
                    if (!isPlaying && currentIndex == frames.size - 1) currentIndex = 0
                    isPlaying = !isPlaying
                },
                onToggleLoop = { isLooping = !isLooping },
                onFpsChange = { fps = it },
                onToggleOnionSkin = { onionSkin = !onionSkin },
                onOnionAlphaChange = { onionAlpha = it },
                onPickAudio = { audioPicker.launch("audio/*") },
                onToggleMuteAudio = { isAudioMuted = !isAudioMuted },
                onRemoveAudio = {
                    audioUri = null
                    audioFileName = null
                    statusText = "Ses kaldırıldı"
                },
                waveform = waveform,
                multiSelect = multiSelect,
                selectedFrames = pickedFrames,
                clipboardCount = frameClipboard.size,
                onToggleMultiSelect = {
                    multiSelect = !multiSelect
                    selectedFrames.clear()
                    if (multiSelect) frames.getOrNull(currentIndex)?.let { selectedFrames.add(it) }
                },
                onToggleFrameSelected = { i ->
                    frames.getOrNull(i)?.let { f -> if (!selectedFrames.remove(f)) selectedFrames.add(f) }
                },
                onSelectAllFrames = { selectedFrames.clear(); selectedFrames.addAll(frames) },
                onCopyFrames = {
                    if (pickedFrames.isNotEmpty()) {
                        frameClipboard.clear()
                        frameClipboard.addAll(pickedFrames.map { it.copy() })
                        statusText = "${pickedFrames.size} kare kopyalandı 📋"
                    }
                },
                onCutFrames = {
                    if (pickedFrames.isNotEmpty()) {
                        frameClipboard.clear()
                        frameClipboard.addAll(pickedFrames.map { it.copy() })
                        deleteFrames(pickedFrames)
                    }
                },
                onPasteFrames = { pasteFrames() },
                onDeleteSelectedFrames = { deleteFrames(pickedFrames) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// S Pen yan tuşu veya fare sağ tuşu basılı mı?
private fun isEraserButton(e: PointerEvent): Boolean {
    val stylus = e.changes.any { it.type == PointerType.Stylus }
    val eraserEnd = e.changes.any { it.type == PointerType.Eraser }
    return eraserEnd ||
            e.buttons.isSecondaryPressed ||
            e.buttons.isTertiaryPressed ||
            (stylus && e.buttons.isPrimaryPressed)
}