package com.example.animator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class CanvasPattern {
    BLANK, GRID, LINE
}

enum class CanvasTemplate(
    val label: String,
    val isPortrait: Boolean,
    val pattern: CanvasPattern
) {
    BLANK_LANDSCAPE("🖼 Yatay Düz", false, CanvasPattern.BLANK),
    BLANK_PORTRAIT("📱 Dikey Düz", true, CanvasPattern.BLANK),
    GRID_PORTRAIT("🏁 Dikey Kareli", true, CanvasPattern.GRID),
    LINE_PORTRAIT("📝 Dikey Çizgili", true, CanvasPattern.LINE),
    GRID_LANDSCAPE("📊 Yatay Kareli", false, CanvasPattern.GRID),
    LINE_LANDSCAPE("📄 Yatay Çizgili", false, CanvasPattern.LINE);

    // Sabit sahne boyutu (16'nın katı: video encoder için güvenli)
    val sceneW: Float get() = if (isPortrait) 1136f else 1600f
    val sceneH: Float get() = if (isPortrait) 1600f else 1136f
}

data class ProjectMeta(
    val id: String,
    var name: String,
    val template: CanvasTemplate = CanvasTemplate.BLANK_LANDSCAPE,
    val createdTime: Long = System.currentTimeMillis(),
    var updatedTime: Long = System.currentTimeMillis(),
    var frameCount: Int = 1,
    var fps: Float = 12f
)

enum class BrushType(val label: String) {
    PEN("🖊 Kalem"),
    MARKER("🖍 İşaretleyici"),
    SOFT("🖌 Yumuşak")
}

enum class EraserMode(val label: String) {
    NORMAL("✂ Komple Çizgi"),
    PARTIAL("🧩 Parça Parça")
}

data class StrokeData(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val alpha: Float = 1f,
    val isFilled: Boolean = false,
    val fillColor: Color? = null,
    val brushType: BrushType = BrushType.PEN,
    val pressures: List<Float>? = null,
    val fillOnly: Boolean = false,
    val text: String? = null,
    val fontSize: Float = 48f,
    val textW: Float = 0f,
    val textBold: Boolean = false
)

// Eklem: konumu sonra sürükleyeceğimiz için değiştirilebilir
class Joint(val id: Int, pos: Offset) {
    var pos by mutableStateOf(pos)
}

// Kemik: iki eklemi id ile bağlar
data class Bone(val a: Int, val b: Int)

// Katman. shared = true ise çizgileri tüm karelerde ortaktır (eski "arka plan" gibi)
class Layer(val id: Int, name: String, val shared: Boolean = false) {
    var name by mutableStateOf(name)
    var visible by mutableStateOf(true)
    var opacity by mutableFloatStateOf(1f)
    var locked by mutableStateOf(false)
    var blend by mutableStateOf(LayerBlend.NORMAL)
    val sharedStrokes = mutableStateListOf<StrokeData>()
}

class Frame {
    // Sadece karakter editöründe kullanılır
    val strokes = mutableStateListOf<StrokeData>()

    // Katman id -> bu karedeki çizgiler (ortak olmayan katmanlar için)
    val layerStrokes = mutableStateMapOf<Int, SnapshotStateList<StrokeData>>()

    val joints = mutableStateListOf<Joint>()
    val bones = mutableStateListOf<Bone>()
    val instances = mutableStateListOf<CharInstance>()
    var nextJointId = 0
    var rootJointId by mutableIntStateOf(-1)

    // Yazmak için: yoksa oluşturur
    fun strokesOf(layer: Layer): SnapshotStateList<StrokeData> =
        if (layer.shared) layer.sharedStrokes
        else layerStrokes.getOrPut(layer.id) { mutableStateListOf() }

    // Sadece okumak/çizmek için: oluşturmaz
    fun peekStrokes(layer: Layer): List<StrokeData> =
        if (layer.shared) layer.sharedStrokes
        else layerStrokes[layer.id] ?: emptyList()

    fun copy(): Frame {
        val f = Frame()
        layerStrokes.forEach { (id, list) ->
            f.layerStrokes[id] = mutableStateListOf<StrokeData>().also { it.addAll(list.map { s -> s.copy() }) }
        }
        f.strokes.addAll(strokes.map { it.copy() })
        f.joints.addAll(joints.map { Joint(it.id, it.pos) })
        f.bones.addAll(bones)
        f.instances.addAll(instances.map { it.copy() })
        f.nextJointId = nextJointId
        f.rootJointId = rootJointId
        return f
    }
}

enum class Tool(val label: String) {
    FREE("✏ Serbest"),
    BRUSH("🖌 Fırça"),
    ERASER("🧹 Silgi"),
    FILL("🪣 Doldurma"),
    SELECT("↖ Seçim"),
    EYEDROPPER("👁 Damlalık"),
    LINE("╱ Çizgi"),
    CIRCLE("◯ Daire"),
    RECT("▭ Kare"),
    TRIANGLE("△ Üçgen"),
    TEXT("🔤 Metin")
}

// Kütüphanedeki karakter şablonu
class Character(
    name: String,
    val strokes: List<StrokeData>,
    val joints: List<Pair<Int, Offset>>,
    val bones: List<Bone>,
    val rootId: Int = -1,
    val binding: List<Int> = emptyList()
) {
    var name by mutableStateOf(name)
}

class CharInstance(
    val character: Character,
    offset: Offset = Offset.Zero,
    var layerId: Int = 0
) {
    var offset by mutableStateOf(offset)
    val pose = mutableStateMapOf<Int, Offset>()

    fun copy(): CharInstance {
        val c = CharInstance(character, offset, layerId)
        c.pose.putAll(pose)
        return c
    }
}

enum class SkelTool(val label: String) {
    BONE("🦴 Kemik"),
    MOVE("✋ Taşı"),
    DELETE("🗑 Sil"),
    ROOT("⭐ Kök")
}