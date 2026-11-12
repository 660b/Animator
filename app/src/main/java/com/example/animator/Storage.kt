package com.example.animator

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LoadedProject(
    val meta: ProjectMeta,
    val frames: List<Frame>,
    val layers: List<Layer>,
    val library: List<Character>,
    val fps: Float
)

private const val LEGACY_LAYER_ID = 1

private fun projectMetaToJson(m: ProjectMeta) = JSONObject().apply {
    put("id", m.id)
    put("name", m.name)
    put("template", m.template.name)
    put("createdTime", m.createdTime)
    put("updatedTime", m.updatedTime)
    put("frameCount", m.frameCount)
    put("fps", m.fps.toDouble())
}

private fun jsonToProjectMeta(o: JSONObject): ProjectMeta {
    val id = o.optString("id", "proj_default")
    val name = o.optString("name", "Proje 1")
    val tName = o.optString("template", CanvasTemplate.BLANK_LANDSCAPE.name)
    val template = try { CanvasTemplate.valueOf(tName) } catch (e: Exception) { CanvasTemplate.BLANK_LANDSCAPE }
    val createdTime = o.optLong("createdTime", System.currentTimeMillis())
    val updatedTime = o.optLong("updatedTime", System.currentTimeMillis())
    val frameCount = o.optInt("frameCount", 1)
    val fps = o.optDouble("fps", 12.0).toFloat()
    return ProjectMeta(
        id = id,
        name = name,
        template = template,
        createdTime = createdTime,
        updatedTime = updatedTime,
        frameCount = frameCount,
        fps = fps
    )
}

private fun strokeToJson(s: StrokeData) = JSONObject().apply {
    put("c", s.color.toArgb())
    put("w", s.width.toDouble())
    put("a", s.alpha.toDouble())
    put("f", s.isFilled)
    if (s.fillColor != null) put("fc", s.fillColor.toArgb())
    put("bt", s.brushType.name)
    s.pressures?.let { pr -> put("pr", JSONArray().apply { pr.forEach { put(it.toDouble()) } }) }
    if (s.fillOnly) put("fo", true)
    if (s.text != null) {
        put("tx", s.text)
        put("fs", s.fontSize.toDouble())
        put("tw", s.textW.toDouble())
        put("tb", s.textBold)
    }
    val arr = JSONArray()
    s.points.forEach {
        arr.put(it.x.toDouble())
        arr.put(it.y.toDouble())
    }
    put("p", arr)
}

private fun jsonToStroke(o: JSONObject): StrokeData {
    val arr = o.getJSONArray("p")
    val pts = ArrayList<Offset>()
    var i = 0
    while (i + 1 < arr.length()) {
        pts.add(Offset(arr.getDouble(i).toFloat(), arr.getDouble(i + 1).toFloat()))
        i += 2
    }
    val color = Color(o.getInt("c"))
    val width = o.getDouble("w").toFloat()
    val alpha = o.optDouble("a", 1.0).toFloat()
    val isFilled = o.optBoolean("f", false)
    val fillColor = if (o.has("fc")) Color(o.getInt("fc")) else null
    val brushTypeStr = o.optString("bt", BrushType.PEN.name)
    val brushType = try { BrushType.valueOf(brushTypeStr) } catch (e: Exception) { BrushType.PEN }
    val prArr = o.optJSONArray("pr")
    val pressures = if (prArr != null && prArr.length() == pts.size)
        List(prArr.length()) { prArr.getDouble(it).toFloat() } else null
    return StrokeData(
        points = pts,
        color = color,
        width = width,
        alpha = alpha,
        isFilled = isFilled,
        fillColor = fillColor,
        brushType = brushType,
        pressures = pressures,
        fillOnly = o.optBoolean("fo", false),
        text = if (o.has("tx")) o.getString("tx") else null,
        fontSize = o.optDouble("fs", 48.0).toFloat(),
        textW = o.optDouble("tw", 0.0).toFloat(),
        textBold = o.optBoolean("tb", false)
    )
}

private fun strokesToJson(list: List<StrokeData>) = JSONArray().apply {
    list.forEach { put(strokeToJson(it)) }
}

private fun jsonToStrokes(arr: JSONArray): List<StrokeData> =
    (0 until arr.length()).map { jsonToStroke(arr.getJSONObject(it)) }

private fun layerToJson(l: Layer) = JSONObject().apply {
    put("id", l.id)
    put("name", l.name)
    put("visible", l.visible)
    put("shared", l.shared)
    put("opacity", l.opacity.toDouble())
    put("locked", l.locked)
    put("blend", l.blend.name)
    put("strokes", strokesToJson(l.sharedStrokes))
}

private fun jsonToLayer(o: JSONObject): Layer {
    val l = Layer(o.getInt("id"), o.getString("name"), o.optBoolean("shared", false))
    l.visible = o.optBoolean("visible", true)
    l.opacity = o.optDouble("opacity", 1.0).toFloat()
    l.locked = o.optBoolean("locked", false)
    l.blend = try { LayerBlend.valueOf(o.optString("blend", "NORMAL")) } catch (e: Exception) { LayerBlend.NORMAL }
    o.optJSONArray("strokes")?.let { l.sharedStrokes.addAll(jsonToStrokes(it)) }
    return l
}

private fun characterToJson(c: Character) = JSONObject().apply {
    put("name", c.name)
    put("strokes", strokesToJson(c.strokes))
    put("joints", JSONArray().apply {
        c.joints.forEach { (id, p) ->
            put(JSONArray().put(id).put(p.x.toDouble()).put(p.y.toDouble()))
        }
    })
    put("bones", JSONArray().apply {
        c.bones.forEach { put(JSONArray().put(it.a).put(it.b)) }
    })
    put("root", c.rootId)
    put("binding", JSONArray().apply { c.binding.forEach { put(it) } })
}

private fun jsonToCharacter(o: JSONObject): Character {
    val ja = o.getJSONArray("joints")
    val joints = (0 until ja.length()).map {
        val j = ja.getJSONArray(it)
        j.getInt(0) to Offset(j.getDouble(1).toFloat(), j.getDouble(2).toFloat())
    }
    val ba = o.getJSONArray("bones")
    val bones = (0 until ba.length()).map {
        val b = ba.getJSONArray(it)
        Bone(b.getInt(0), b.getInt(1))
    }
    val bi = o.getJSONArray("binding")
    val binding = (0 until bi.length()).map { bi.getInt(it) }
    return Character(
        name = o.getString("name"),
        strokes = jsonToStrokes(o.getJSONArray("strokes")),
        joints = joints,
        bones = bones,
        rootId = o.getInt("root"),
        binding = binding
    )
}

private fun frameToJson(f: Frame, library: List<Character>) = JSONObject().apply {
    put("ls", JSONObject().apply {
        f.layerStrokes.forEach { (id, list) -> put(id.toString(), strokesToJson(list)) }
    })
    put("instances", JSONArray().apply {
        f.instances.forEach { inst ->
            val idx = library.indexOfFirst { it === inst.character }
            if (idx >= 0) {
                put(JSONObject().apply {
                    put("ch", idx)
                    put("ly", inst.layerId)
                    put("ox", inst.offset.x.toDouble())
                    put("oy", inst.offset.y.toDouble())
                    put("pose", JSONArray().apply {
                        inst.pose.forEach { (id, p) ->
                            put(JSONArray().put(id).put(p.x.toDouble()).put(p.y.toDouble()))
                        }
                    })
                })
            }
        }
    })
}

private fun jsonToFrame(o: JSONObject, library: List<Character>): Frame {
    val f = Frame()
    val ls = o.optJSONObject("ls")
    if (ls != null) {
        ls.keys().forEach { k ->
            f.layerStrokes[k.toInt()] =
                mutableStateListOf<StrokeData>().also { it.addAll(jsonToStrokes(ls.getJSONArray(k))) }
        }
    } else if (o.has("strokes")) {
        f.layerStrokes[LEGACY_LAYER_ID] =
            mutableStateListOf<StrokeData>().also { it.addAll(jsonToStrokes(o.getJSONArray("strokes"))) }
    }
    val ia = o.getJSONArray("instances")
    for (k in 0 until ia.length()) {
        val io = ia.getJSONObject(k)
        val ch = library.getOrNull(io.getInt("ch")) ?: continue
        val inst = CharInstance(
            ch,
            Offset(io.getDouble("ox").toFloat(), io.getDouble("oy").toFloat())
        )
        inst.layerId = io.optInt("ly", LEGACY_LAYER_ID)
        val pa = io.getJSONArray("pose")
        for (j in 0 until pa.length()) {
            val p = pa.getJSONArray(j)
            inst.pose[p.getInt(0)] = Offset(p.getDouble(1).toFloat(), p.getDouble(2).toFloat())
        }
        f.instances.add(inst)
    }
    return f
}

private fun getProjectsDir(context: Context): File {
    val dir = File(context.filesDir, "projects")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

fun listProjects(context: Context): List<ProjectMeta> {
    val dir = getProjectsDir(context)
    val files = dir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
    
    // Legacy kayıt kontrolü
    val legacy = File(context.filesDir, "project.json")
    if (legacy.exists() && files.isEmpty()) {
        try {
            val legacyContent = legacy.readText()
            val meta = ProjectMeta("proj_default", "Eski Proje", CanvasTemplate.BLANK_LANDSCAPE)
            val newFile = File(dir, "${meta.id}.json")
            val root = JSONObject(legacyContent)
            root.put("meta", projectMetaToJson(meta))
            newFile.writeText(root.toString())
            legacy.delete()
            return listOf(meta)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val result = mutableListOf<ProjectMeta>()
    for (f in files) {
        try {
            val root = JSONObject(f.readText())
            if (root.has("meta")) {
                result.add(jsonToProjectMeta(root.getJSONObject("meta")))
            } else {
                val meta = ProjectMeta(f.nameWithoutExtension, f.nameWithoutExtension, CanvasTemplate.BLANK_LANDSCAPE)
                result.add(meta)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return result.sortedByDescending { it.updatedTime }
}

fun saveProjectById(
    context: Context,
    meta: ProjectMeta,
    frames: List<Frame>,
    layers: List<Layer>,
    library: List<Character>,
    fps: Float
): Boolean = try {
    meta.updatedTime = System.currentTimeMillis()
    meta.frameCount = frames.size
    meta.fps = fps

    val root = JSONObject().apply {
        put("meta", projectMetaToJson(meta))
        put("fps", fps.toDouble())
        put("library", JSONArray().apply { library.forEach { put(characterToJson(it)) } })
        put("layers", JSONArray().apply { layers.forEach { put(layerToJson(it)) } })
        put("frames", JSONArray().apply { frames.forEach { put(frameToJson(it, library)) } })
    }
    val dir = getProjectsDir(context)
    val file = File(dir, "${meta.id}.json")
    val tmp = File(dir, "${meta.id}.tmp")
    tmp.writeText(root.toString())
    tmp.renameTo(file)
    true
} catch (e: Exception) {
    e.printStackTrace()
    false
}

fun loadProjectById(context: Context, id: String): LoadedProject? = try {
    val file = File(getProjectsDir(context), "$id.json")
    if (!file.exists()) null else {
        val root = JSONObject(file.readText())
        val meta = if (root.has("meta")) jsonToProjectMeta(root.getJSONObject("meta"))
                   else ProjectMeta(id, id, CanvasTemplate.BLANK_LANDSCAPE)

        val la = root.getJSONArray("library")
        val library = (0 until la.length()).map { jsonToCharacter(la.getJSONObject(it)) }
        val fa = root.getJSONArray("frames")
        val frames = (0 until fa.length()).map { jsonToFrame(fa.getJSONObject(it), library) }

        val layers: List<Layer> = if (root.has("layers")) {
            val arr = root.getJSONArray("layers")
            (0 until arr.length()).map { jsonToLayer(arr.getJSONObject(it)) }
        } else {
            val bg = Layer(0, "Arka Plan", shared = true)
            root.optJSONArray("background")?.let { bg.sharedStrokes.addAll(jsonToStrokes(it)) }
            listOf(bg, Layer(LEGACY_LAYER_ID, "Katman 1"))
        }

        LoadedProject(
            meta = meta,
            frames = frames,
            layers = layers,
            library = library,
            fps = root.optDouble("fps", 12.0).toFloat()
        )
    }
} catch (e: Exception) {
    e.printStackTrace()
    null
}

fun deleteProjectById(context: Context, id: String): Boolean = try {
    deleteExtras(context, id)
    val file = File(getProjectsDir(context), "$id.json")
    if (file.exists()) file.delete() else true
} catch (e: Exception) {
    false
}