package com.example.animator

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class AudioWaveform(val peaks: FloatArray, val peaksPerSecond: Int, val durationUs: Long) {
    val durationSec: Float get() = durationUs / 1_000_000f

    // [startSec, endSec] aralığındaki en yüksek genlik (0..1)
    fun maxIn(startSec: Float, endSec: Float): Float {
        if (peaks.isEmpty()) return 0f
        val a = (startSec * peaksPerSecond).toInt().coerceIn(0, peaks.size - 1)
        val b = maxOf(a, (endSec * peaksPerSecond).toInt()).coerceAtMost(peaks.size - 1)
        var m = 0f
        for (i in a..b) if (peaks[i] > m) m = peaks[i]
        return m
    }
}

fun computeWaveform(context: Context, uri: Uri, peaksPerSecond: Int = 50): AudioWaveform? {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    try {
        extractor.setDataSource(context, uri, null)
        val idx = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(idx)
        val format = extractor.getTrackFormat(idx)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

        val dec = MediaCodec.createDecoderByType(mime)
        decoder = dec
        dec.configure(format, null, null, 0)
        dec.start()

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        var peaks = FloatArray(2048)
        var maxBucket = -1
        var frameCounter = 0L
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        val deadline = System.currentTimeMillis() + 60_000

        while (!outputDone && System.currentTimeMillis() < deadline) {
            if (!inputDone) {
                val ii = dec.dequeueInputBuffer(5_000)
                if (ii >= 0) {
                    val buf = dec.getInputBuffer(ii)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        dec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        dec.queueInputBuffer(ii, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val oi = dec.dequeueOutputBuffer(info, 5_000)
            if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val f = dec.outputFormat
                sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            } else if (oi >= 0) {
                val out = dec.getOutputBuffer(oi)
                if (out != null && info.size > 0) {
                    out.position(info.offset)
                    out.limit(info.offset + info.size)
                    val sb = out.order(ByteOrder.nativeOrder()).asShortBuffer()
                    val n = sb.remaining()
                    var i = 0
                    while (i + channels <= n) {
                        var m = 0
                        for (c in 0 until channels) {
                            val v = abs(sb.get(i + c).toInt())
                            if (v > m) m = v
                        }
                        val bucket = (frameCounter * peaksPerSecond / sampleRate).toInt()
                        if (bucket >= peaks.size) peaks = peaks.copyOf(maxOf(peaks.size * 2, bucket + 1))
                        if (bucket > maxBucket) maxBucket = bucket
                        val nv = m / 32768f
                        if (nv > peaks[bucket]) peaks[bucket] = nv
                        frameCounter++
                        i += channels
                    }
                }
                dec.releaseOutputBuffer(oi, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            }
        }
        if (maxBucket < 0) return null
        var mx = 0.01f
        for (i in 0..maxBucket) if (peaks[i] > mx) mx = peaks[i]
        // sqrt: sessiz kısımlar da görünsün
        val arr = FloatArray(maxBucket + 1) { sqrt((peaks[it] / mx).coerceIn(0f, 1f)) }
        return AudioWaveform(arr, peaksPerSecond, frameCounter * 1_000_000L / sampleRate)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        try { decoder?.stop() } catch (_: Exception) {}
        try { decoder?.release() } catch (_: Exception) {}
        try { extractor.release() } catch (_: Exception) {}
    }
}

@Composable
fun WaveformStrip(
    wf: AudioWaveform,
    fps: Float,
    frameCount: Int,
    currentIndex: Int,
    enabled: Boolean,
    onSeekFrame: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val videoSec = frameCount / fps
    val span = maxOf(videoSec, wf.durationSec).coerceAtLeast(0.1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B1B27))
            .pointerInput(frameCount, fps, enabled, span) {
                if (!enabled) return@pointerInput
                detectTapGestures { p ->
                    val sec = (p.x / size.width).coerceIn(0f, 1f) * span
                    onSeekFrame((sec * fps).toInt().coerceIn(0, frameCount - 1))
                }
            }
            .pointerInput(frameCount, fps, enabled, span) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures { change, _ ->
                    val sec = (change.position.x / size.width).coerceIn(0f, 1f) * span
                    onSeekFrame((sec * fps).toInt().coerceIn(0, frameCount - 1))
                }
            }
    ) {
        val mid = size.height / 2f
        val step = 2.dp.toPx()
        val videoEndX = videoSec / span * size.width
        var x = 0f
        while (x < size.width) {
            val t = x / size.width * span
            val a = wf.maxIn(t, t + span * step / size.width)
            val h = (a * (mid - 2f)).coerceAtLeast(1f)
            drawLine(
                if (x <= videoEndX) Color(0xFF00D9A0) else Color(0xFF555566),
                Offset(x, mid - h), Offset(x, mid + h),
                strokeWidth = step * 0.7f
            )
            x += step
        }
        val px = (currentIndex / fps) / span * size.width
        drawLine(Color.White, Offset(px, 0f), Offset(px, size.height), strokeWidth = 2.dp.toPx())
    }
}