package com.example.animator

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File

// Herhangi bir ses dosyasını (mp3, aac, ogg, wav...) AAC/M4A'ya çevirir; maxDurationUs'ten sonrasını keser.
fun transcodeAudioToAac(context: Context, uri: Uri, maxDurationUs: Long, out: File): Boolean {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var encoder: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    try {
        extractor.setDataSource(context, uri, null)
        val trackIdx = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return false
        extractor.selectTrack(trackIdx)
        val inFormat = extractor.getTrackFormat(trackIdx)
        val mime = inFormat.getString(MediaFormat.KEY_MIME) ?: return false
        val sampleRate = inFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = inFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 2)

        val dec = MediaCodec.createDecoderByType(mime)
        decoder = dec
        dec.configure(inFormat, null, null, 0)
        dec.start()

        val encFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
        }
        val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder = enc
        enc.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        enc.start()

        val mux = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer = mux
        var track = -1
        val encInfo = MediaCodec.BufferInfo()
        val decInfo = MediaCodec.BufferInfo()
        var encoderDone = false
        var extractorDone = false
        var decoderDone = false
        var pcmChannels = channels
        val deadline = System.currentTimeMillis() + 60_000

        fun drainEncoder() {
            while (true) {
                val i = enc.dequeueOutputBuffer(encInfo, 0)
                if (i == MediaCodec.INFO_TRY_AGAIN_LATER) return
                if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = mux.addTrack(enc.outputFormat)
                    mux.start()
                    muxerStarted = true
                } else if (i >= 0) {
                    val data = enc.getOutputBuffer(i)
                    if (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) encInfo.size = 0
                    if (data != null && encInfo.size > 0 && muxerStarted) {
                        data.position(encInfo.offset)
                        data.limit(encInfo.offset + encInfo.size)
                        mux.writeSampleData(track, data, encInfo)
                    }
                    val end = encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    enc.releaseOutputBuffer(i, false)
                    if (end) { encoderDone = true; return }
                }
            }
        }

        fun queueEncoderEos(ptsUs: Long) {
            while (System.currentTimeMillis() < deadline) {
                val ei = enc.dequeueInputBuffer(10_000)
                if (ei >= 0) {
                    enc.queueInputBuffer(ei, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    return
                }
                drainEncoder()
            }
        }

        while (!encoderDone && System.currentTimeMillis() < deadline) {
            if (!extractorDone) {
                val ii = dec.dequeueInputBuffer(0)
                if (ii >= 0) {
                    val buf = dec.getInputBuffer(ii)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        dec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        extractorDone = true
                    } else {
                        dec.queueInputBuffer(ii, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            if (!decoderDone) {
                val oi = dec.dequeueOutputBuffer(decInfo, 0)
                if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    pcmChannels = dec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else if (oi >= 0) {
                    val pcm = dec.getOutputBuffer(oi)
                    val isEos = decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    val pastEnd = decInfo.presentationTimeUs >= maxDurationUs
                    if (pcm != null && decInfo.size > 0 && !pastEnd) {
                        pcm.position(decInfo.offset)
                        pcm.limit(decInfo.offset + decInfo.size)
                        val bytesPerFrame = 2 * pcmChannels
                        var consumed = 0
                        while (pcm.hasRemaining() && System.currentTimeMillis() < deadline) {
                            val ei = enc.dequeueInputBuffer(10_000)
                            if (ei < 0) { drainEncoder(); continue }
                            val ib = enc.getInputBuffer(ei)!!
                            ib.clear()
                            var n = minOf(ib.remaining(), pcm.remaining())
                            n -= n % bytesPerFrame
                            if (n <= 0) n = minOf(ib.remaining(), pcm.remaining())
                            val oldLimit = pcm.limit()
                            pcm.limit(pcm.position() + n)
                            ib.put(pcm)
                            pcm.limit(oldLimit)
                            val pts = decInfo.presentationTimeUs + consumed / bytesPerFrame * 1_000_000L / sampleRate
                            enc.queueInputBuffer(ei, 0, n, pts, 0)
                            consumed += n
                        }
                    }
                    dec.releaseOutputBuffer(oi, false)
                    if (isEos || pastEnd) {
                        decoderDone = true
                        queueEncoderEos(decInfo.presentationTimeUs)
                    }
                }
            }

            drainEncoder()
            if (extractorDone && decoderDone) Thread.sleep(5)
        }
        return muxerStarted && encoderDone
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    } finally {
        try { decoder?.stop() } catch (_: Exception) {}
        try { decoder?.release() } catch (_: Exception) {}
        try { encoder?.stop() } catch (_: Exception) {}
        try { encoder?.release() } catch (_: Exception) {}
        try { if (muxerStarted) muxer?.stop() } catch (_: Exception) {}
        try { muxer?.release() } catch (_: Exception) {}
        try { extractor.release() } catch (_: Exception) {}
    }
}