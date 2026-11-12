package com.example.animator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class RefImage(val id: Int, val fileName: String, val bitmap: ImageBitmap) {
    var offset by mutableStateOf(Offset.Zero)
    var scale by mutableFloatStateOf(1f)
    var opacity by mutableFloatStateOf(0.4f)
    var visible by mutableStateOf(true)
    var locked by mutableStateOf(false)
}

class Guide(val horizontal: Boolean, pos: Float) {
    var pos by mutableFloatStateOf(pos)
}

private fun extrasDirPath(context: Context, projectId: String) =
    File(context.filesDir, "projects/${projectId}_extra")

private fun extrasDir(context: Context, projectId: String): File =
    extrasDirPath(context, projectId).also { if (!it.exists()) it.mkdirs() }

fun importRefImage(
    context: Context, projectId: String, uri: Uri,
    sceneW: Float, sceneH: Float, id: Int
): RefImage? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val png = bmp.hasAlpha()
        val name = "ref_$id.${if (png) "png" else "jpg"}"
        File(extrasDir(context, projectId), name).outputStream().use { out ->
            bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 92, out)
        }
        val r = RefImage(id, name, bmp.asImageBitmap())
        val s = minOf(sceneW / bmp.width, sceneH / bmp.height) * 0.9f
        r.scale = s
        r.offset = Offset((sceneW - bmp.width * s) / 2f, (sceneH - bmp.height * s) / 2f)
        r
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun deleteRefFile(context: Context, projectId: String, ref: RefImage) {
    try { File(extrasDirPath(context, projectId), ref.fileName).delete() } catch (_: Exception) {}
}

fun saveExtras(context: Context, projectId: String, refs: List<RefImage>, guides: List<Guide>) {
    try {
        if (refs.isEmpty() && guides.isEmpty() && !extrasDirPath(context, projectId).exists()) return
        val root = JSONObject()
        root.put("refs", JSONArray().apply {
            refs.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("file", r.fileName)
                    put("ox", r.offset.x.toDouble())
                    put("oy", r.offset.y.toDouble())
                    put("scale", r.scale.toDouble())
                    put("opacity", r.opacity.toDouble())
                    put("visible", r.visible)
                    put("locked", r.locked)
                })
            }
        })
        root.put("guides", JSONArray().apply {
            guides.forEach { g ->
                put(JSONObject().apply {
                    put("h", g.horizontal)
                    put("pos", g.pos.toDouble())
                })
            }
        })
        val dir = extrasDir(context, projectId)
        val tmp = File(dir, "extras.tmp")
        tmp.writeText(root.toString())
        tmp.renameTo(File(dir, "extras.json"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadExtras(context: Context, projectId: String): Pair<List<RefImage>, List<Guide>> {
    val refs = ArrayList<RefImage>()
    val guides = ArrayList<Guide>()
    try {
        val dir = extrasDirPath(context, projectId)
        val f = File(dir, "extras.json")
        if (!f.exists()) return refs to guides
        val root = JSONObject(f.readText())
        root.optJSONArray("refs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val bmp = BitmapFactory.decodeFile(File(dir, o.getString("file")).absolutePath) ?: continue
                val r = RefImage(o.getInt("id"), o.getString("file"), bmp.asImageBitmap())
                r.offset = Offset(o.optDouble("ox", 0.0).toFloat(), o.optDouble("oy", 0.0).toFloat())
                r.scale = o.optDouble("scale", 1.0).toFloat()
                r.opacity = o.optDouble("opacity", 0.4).toFloat()
                r.visible = o.optBoolean("visible", true)
                r.locked = o.optBoolean("locked", false)
                refs.add(r)
            }
        }
        root.optJSONArray("guides")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                guides.add(Guide(o.getBoolean("h"), o.getDouble("pos").toFloat()))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return refs to guides
}

fun deleteExtras(context: Context, projectId: String) {
    try { extrasDirPath(context, projectId).deleteRecursively() } catch (_: Exception) {}
}