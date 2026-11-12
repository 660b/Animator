package com.example.animator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.io.File
import java.nio.ByteBuffer

// --- DAMLALIK (EYEDROPPER) RENK SEÇİM MANTIĞI ---

fun sampleColorAt(strokes: List<StrokeData>, p: Offset): Color? {
    for (i in strokes.indices.reversed()) {
        val s = strokes[i]
        if (s.text != null) {
            if (getPointsBounds(s.points).contains(p)) return s.color
            continue
        }
        if (snapPoint(p, listOf(s), s.width + 15f) != p) {
            return s.color
        }
        if (s.isFilled && s.fillColor != null) {
            val bounds = getPointsBounds(s.points)
            if (bounds.inflate(20f).contains(p)) {
                return s.fillColor
            }
        }
    }
    return null
}

// --- BİTMAP FRAME RENDERER (COMPOSE DRAW SCOPE İLE BİREBİR) ---

fun renderFrameToBitmap(
    frame: Frame,
    layers: List<Layer>,
    width: Int,
    height: Int,
    template: CanvasTemplate
): Bitmap {
    val image = ImageBitmap(width, height)
    val composeCanvas = androidx.compose.ui.graphics.Canvas(image)
    CanvasDrawScope().draw(
        Density(1f), LayoutDirection.Ltr, composeCanvas,
        Size(width.toFloat(), height.toFloat())
    ) {
        drawCanvasTemplatePattern(template, size.width, size.height)
        drawFrameLayers(frame, layers, size.width, size.height)
    }
    return image.asAndroidBitmap()
}

// --- DIŞA MP4 VİDEO AKTARMA (MEDIASTORE MOVIES GALLERY + SES DESTEĞİ) ---

fun exportProjectAsVideo(
    context: Context,
    meta: ProjectMeta,
    frames: List<Frame>,
    layers: List<Layer>,
    fps: Float,
    audioUri: Uri?,
    onProgress: (Float) -> Unit,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit
) {
    // Arayüz thread'inde anlık kopya al
    val frameList = frames.toList()
    val layerList = layers.toList()
    val template = meta.template
    val w = template.sceneW.toInt() / 2 * 2
    val h = template.sceneH.toInt() / 2 * 2
    val projectName = meta.name

    Thread {
        var audioFile: File? = null
        var tempMp4: File? = null
        try {
            onProgress(0.02f)
            val videoDurationUs = (frameList.size * 1_000_000f / fps).toLong()
            var audioNote = ""

            if (audioUri != null) {
                val f = File(context.cacheDir, "export_audio_${System.currentTimeMillis()}.m4a")
                val ok = try { transcodeAudioToAac(context, audioUri, videoDurationUs, f) } catch (e: Exception) { false }
                if (ok) audioFile = f else { f.delete(); audioNote = " (ses eklenemedi)" }
            }
            onProgress(0.1f)

            val videoName = "Animator_${projectName.replace(" ", "_")}_${System.currentTimeMillis()}.mp4"
            val tmp = File(context.cacheDir, videoName)
            tempMp4 = tmp

            encodeFramesToMp4(
                outputFile = tmp,
                frameCount = frameList.size,
                fps = fps,
                width = w,
                height = h,
                audioFile = audioFile,
                renderFrame = { i -> renderFrameToBitmap(frameList[i], layerList, w, h, template) },
                onProgress = { p -> onProgress(0.1f + 0.85f * p) }
            )

            saveMp4ToGallery(context, tmp, videoName)
            onProgress(1f)
            onComplete(
                "Galeride 'Movies/Animator' klasörüne MP4 olarak aktarıldı! 🎥✓" +
                        if (audioFile != null) " (sesli 🎵)" else audioNote
            )
        } catch (e: Exception) {
            e.printStackTrace()
            onError("Video aktarma hatası: ${e.message}")
        } finally {
            tempMp4?.delete()
            audioFile?.delete()
        }
    }.start()
}

private fun encodeFramesToMp4(
    outputFile: File,
    frameCount: Int,
    fps: Float,
    width: Int,
    height: Int,
    audioFile: File?,
    renderFrame: (Int) -> Bitmap,
    onProgress: (Float) -> Unit
) {
    val mime = "video/avc"
    val format = MediaFormat.createVideoFormat(mime, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps.toInt().coerceAtLeast(1))
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }
    val encoder = MediaCodec.createEncoderByType(mime)
    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    val inputSurface = encoder.createInputSurface()
    encoder.start()

    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var videoTrack = -1
    var audioTrack = -1
    var audioExtractor: MediaExtractor? = null
    var muxerStarted = false
    var eos = false
    var written = 0
    val frameDurationUs = (1_000_000f / fps).toLong()
    val info = MediaCodec.BufferInfo()

    fun startMuxer() {
        if (muxerStarted) return
        videoTrack = muxer.addTrack(encoder.outputFormat)
        if (audioFile != null) {
            val ex = MediaExtractor()
            ex.setDataSource(audioFile.absolutePath)
            val idx = (0 until ex.trackCount).firstOrNull {
                ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            if (idx != null) {
                ex.selectTrack(idx)
                audioTrack = muxer.addTrack(ex.getTrackFormat(idx))
                audioExtractor = ex
            } else {
                ex.release()
            }
        }
        muxer.start()
        muxerStarted = true
    }

    fun drain(timeoutUs: Long) {
        while (true) {
            val i = encoder.dequeueOutputBuffer(info, timeoutUs)
            if (i == MediaCodec.INFO_TRY_AGAIN_LATER) return
            if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                startMuxer()
            } else if (i >= 0) {
                val data = encoder.getOutputBuffer(i)
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                if (data != null && muxerStarted && info.size > 0) {
                    info.presentationTimeUs = written * frameDurationUs
                    written++
                    data.position(info.offset)
                    data.limit(info.offset + info.size)
                    muxer.writeSampleData(videoTrack, data, info)
                }
                val end = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                encoder.releaseOutputBuffer(i, false)
                if (end) { eos = true; return }
            }
        }
    }

    try {
        for (i in 0 until frameCount) {
            val bmp = renderFrame(i)
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                inputSurface.lockHardwareCanvas()
            } else {
                inputSurface.lockCanvas(null)
            }
            canvas.drawBitmap(bmp, 0f, 0f, null)
            inputSurface.unlockCanvasAndPost(canvas)
            bmp.recycle()
            drain(10_000L)          // çıkış kuyruğu dolmasın
            onProgress((i + 1) / frameCount.toFloat())
        }

        encoder.signalEndOfInputStream()
        val deadline = System.currentTimeMillis() + 15_000
        while (!eos && System.currentTimeMillis() < deadline) drain(10_000L)

        // Ses örneklerini videonun süresi kadar kopyala
        val ex = audioExtractor
        if (muxerStarted && ex != null && audioTrack >= 0) {
            val buf = ByteBuffer.allocate(256 * 1024)
            val ai = MediaCodec.BufferInfo()
            val limitUs = written * frameDurationUs
            while (true) {
                val size = ex.readSampleData(buf, 0)
                if (size < 0) break
                val t = ex.sampleTime
                if (t > limitUs) break
                val flags = if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                    MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                ai.set(0, size, t, flags)
                muxer.writeSampleData(audioTrack, buf, ai)
                ex.advance()
            }
        }
    } finally {
        try { audioExtractor?.release() } catch (_: Exception) {}
        try { encoder.stop() } catch (_: Exception) {}
        try { encoder.release() } catch (_: Exception) {}
        try { inputSurface.release() } catch (_: Exception) {}
        try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
        try { muxer.release() } catch (_: Exception) {}
    }
}

private fun saveMp4ToGallery(context: Context, mp4File: File, videoName: String): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, videoName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Animator")
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let { u ->
            context.contentResolver.openOutputStream(u)?.use { out ->
                mp4File.inputStream().use { input -> input.copyTo(out) }
            }
        }
        uri
    } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Animator")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, videoName)
        mp4File.copyTo(destFile, overwrite = true)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DATA, destFile.absolutePath)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        }
        context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }
}