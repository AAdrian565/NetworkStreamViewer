package com.adriant.networkstreamviewer.data.ndi

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import com.adriant.networkstreamviewer.domain.model.NdiAudioDiagnostics
import com.adriant.networkstreamviewer.domain.model.NdiAudioLevels
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class NdiAudioPlayer(
    context: Context,
    private val onStatusChanged: (NdiAudioStatus) -> Unit = {},
    private val onLevelsChanged: (NdiAudioLevels) -> Unit = {},
    private val onDiagnosticsChanged: (NdiAudioDiagnostics) -> Unit = {},
    private val nativeAudio: NativeAudioSource = NativeAudioSource.Native,
    private val clock: Clock = Clock { System.nanoTime() / NANOS_PER_MILLISECOND }
) {
    interface NativeAudioSource {
        fun fillAudioBuffer(buffer: ByteBuffer, sampleRate: Int, channelCount: Int, samplesPerChannel: Int): Long
        fun getAudioPerformance(): LongArray

        object Native : NativeAudioSource {
            override fun fillAudioBuffer(buffer: ByteBuffer, sampleRate: Int, channelCount: Int, samplesPerChannel: Int) =
                NdiNative.fillAudioBuffer(buffer, sampleRate, channelCount, samplesPerChannel)

            override fun getAudioPerformance(): LongArray = NdiNative.getAudioPerformance()
        }
    }

    fun interface Clock {
        fun nowMillis(): Long
    }

    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val lock = Object()
    private val meter = AudioLevelMeter(AudioLevelMeter.Clock { clock.nowMillis() })
    private var track: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null
    private var writer: Thread? = null
    private var generation = 0L
    private var running = false
    private var focusGranted = false
    private var permanentFocusLoss = false
    private var transientFocusPause = false
    private var duckMultiplier = 1f
    private var userVolume = 1f
    private var muted = false
    private var status = NdiAudioStatus.STARTING

    fun setVolume(volume: Float) {
        synchronized(lock) {
            userVolume = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 0f
            applyVolumeLocked()
        }
    }

    fun setMuted(muted: Boolean) {
        synchronized(lock) {
            this.muted = muted
            if (!muted && permanentFocusLoss) {
                permanentFocusLoss = false
                focusGranted = false
            }
            applyVolumeLocked()
            lock.notifyAll()
        }
    }

    fun retryAudioFocus() {
        synchronized(lock) {
            if (!running) return
            permanentFocusLoss = false
            focusGranted = false
            lock.notifyAll()
        }
    }

    fun start() {
        synchronized(lock) {
            if (running) return
            generation += 1
            running = true
            status = NdiAudioStatus.STARTING
            meter.reset()
            publishStatusLocked()
            publishLevels(NdiAudioLevels.FLOOR)
            writer = Thread({ writerLoop(generation) }, "NdiAudioWriter").also {
                it.start()
            }
        }
    }

    fun stop() {
        val thread: Thread?
        synchronized(lock) {
            if (!running && writer == null && track == null) return
            generation += 1
            running = false
            focusGranted = false
            transientFocusPause = false
            track?.let {
                try {
                    it.pause()
                    it.stop()
                    it.flush()
                } catch (_: Exception) {
                    // The track may already be dead; release remains deterministic.
                }
            }
            lock.notifyAll()
            thread = writer
        }
        if (thread != null && thread !== Thread.currentThread()) {
            try {
                thread.join()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        synchronized(lock) {
            releaseTrackLocked()
            abandonFocusLocked()
            writer = null
            meter.reset()
            status = NdiAudioStatus.STARTING
            publishStatusLocked()
            publishLevels(NdiAudioLevels.FLOOR)
            publishDiagnosticsLocked(0, 0, 0)
        }
    }

    private fun writerLoop(loopGeneration: Long) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val pullBytes = SAMPLES_PER_PULL * CHANNEL_COUNT * BYTES_PER_SAMPLE
        val buffer = ByteBuffer.allocateDirect(pullBytes).order(ByteOrder.nativeOrder())
        val startedAt = clock.nowMillis()
        var lastFrameCount = Long.MIN_VALUE
        var lastProgressAt = startedAt
        var lastStatsAt = startedAt
        var totalFrames = 0L
        var droppedFrames = 0L
        var underruns = 0

        try {
            val createdTrack = createTrack()
            synchronized(lock) {
                if (!running || generation != loopGeneration) return
                if (createdTrack == null) {
                    reportOutputFailureLocked()
                    return
                }
                track = createdTrack
            }

            synchronized(lock) {
                if (!requestFocusLocked()) {
                    publishStatusLocked(NdiAudioStatus.FOCUS_LOST)
                }
            }

            while (isRunning(loopGeneration)) {
                synchronized(lock) {
                    while (running && generation == loopGeneration && !focusGranted) {
                        lock.wait(100L)
                        if (!permanentFocusLoss) requestFocusLocked()
                    }
                    if (!running || generation != loopGeneration) break
                    track?.play()
                    applyVolumeLocked()
                }

                buffer.clear()
                val frameCount = try {
                    nativeAudio.fillAudioBuffer(buffer, SAMPLE_RATE, CHANNEL_COUNT, SAMPLES_PER_PULL)
                } catch (_: Exception) {
                    INVALID_CAPTURE
                }
                if (frameCount == INVALID_CAPTURE) {
                    synchronized(lock) { reportOutputFailureLocked() }
                    break
                }
                if (frameCount >= 0) {
                    if (lastFrameCount == Long.MIN_VALUE || frameCount > lastFrameCount) {
                        lastProgressAt = clock.nowMillis()
                        synchronized(lock) { publishStatusLocked(NdiAudioStatus.PLAYING) }
                    }
                    lastFrameCount = frameCount
                }
                if (clock.nowMillis() - lastProgressAt >= if (lastFrameCount == Long.MIN_VALUE || lastFrameCount == 0L) NO_SIGNAL_START_MS else NO_SIGNAL_STALL_MS) {
                    synchronized(lock) {
                        publishStatusLocked(NdiAudioStatus.NO_SIGNAL)
                        publishLevels(meter.reset())
                    }
                }

                buffer.position(0)
                buffer.limit(pullBytes)
                while (buffer.hasRemaining() && isRunning(loopGeneration)) {
                    val written = try {
                        track?.write(buffer, buffer.remaining(), AudioTrack.WRITE_BLOCKING) ?: ERROR_OUTPUT
                    } catch (_: Exception) {
                        ERROR_OUTPUT
                    }
                    if (written <= 0) {
                        synchronized(lock) { reportOutputFailureLocked() }
                        break
                    }
                }
                if (!isRunning(loopGeneration)) break

                buffer.position(0)
                buffer.limit(pullBytes)
                synchronized(lock) { publishLevels(meter.process(buffer)) }

                val now = clock.nowMillis()
                if (now - lastStatsAt >= STATS_INTERVAL_MS) {
                    val stats = try { nativeAudio.getAudioPerformance() } catch (_: Exception) { LongArray(0) }
                    if (stats.size >= 2) {
                        totalFrames = stats[0]
                        droppedFrames = stats[1]
                    }
                    synchronized(lock) {
                        underruns = track?.underrunCount ?: underruns
                        publishDiagnosticsLocked(totalFrames, droppedFrames, underruns)
                    }
                    lastStatsAt = now
                }
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (_: Exception) {
            synchronized(lock) { if (running && generation == loopGeneration) reportOutputFailureLocked() }
        } finally {
            synchronized(lock) {
                if (generation == loopGeneration) {
                    track?.pause()
                    track?.flush()
                    focusGranted = false
                    abandonFocusLocked()
                    releaseTrackLocked()
                    writer = null
                }
            }
        }
    }

    private fun createTrack(): AudioTrack? {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val minimum = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) return null
        val bufferSize = max(minimum, SAMPLES_PER_PULL * CHANNEL_COUNT * BYTES_PER_SAMPLE * 4)
        fun build(lowLatency: Boolean): AudioTrack? = try {
            val candidate = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize)
                .apply { if (lowLatency) setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY) }
                .build()
            if (candidate.state == AudioTrack.STATE_INITIALIZED) candidate else {
                candidate.release()
                null
            }
        } catch (_: Exception) {
            null
        }
        return build(true) ?: build(false)
    }

    private fun requestFocusLocked(): Boolean {
        if (permanentFocusLoss || focusGranted) return focusGranted
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener { change -> onFocusChanged(change) }
            .build()
            .also { focusRequest = it }
        focusGranted = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (focusGranted) {
            transientFocusPause = false
            applyVolumeLocked()
        }
        return focusGranted
    }

    private fun onFocusChanged(change: Int) {
        synchronized(lock) {
            if (!running) return
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    focusGranted = true
                    permanentFocusLoss = false
                    transientFocusPause = false
                    duckMultiplier = 1f
                    track?.flush()
                    applyVolumeLocked()
                    lock.notifyAll()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    focusGranted = false
                    transientFocusPause = true
                    track?.pause()
                    track?.flush()
                    publishStatusLocked(NdiAudioStatus.FOCUS_LOST)
                    lock.notifyAll()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    duckMultiplier = 0.2f
                    applyVolumeLocked()
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    focusGranted = false
                    permanentFocusLoss = true
                    track?.pause()
                    track?.flush()
                    publishStatusLocked(NdiAudioStatus.FOCUS_LOST)
                    lock.notifyAll()
                }
            }
        }
    }

    private fun applyVolumeLocked() {
        val gain = if (muted || !focusGranted) 0f else userVolume * duckMultiplier
        try { track?.setVolume(gain) } catch (_: Exception) { }
    }

    private fun abandonFocusLocked() {
        focusRequest?.let { try { audioManager.abandonAudioFocusRequest(it) } catch (_: Exception) { } }
        focusRequest = null
        duckMultiplier = 1f
    }

    private fun releaseTrackLocked() {
        track?.let { try { it.release() } catch (_: Exception) { } }
        track = null
    }

    private fun reportOutputFailureLocked() {
        if (!running) return
        publishStatusLocked(NdiAudioStatus.OUTPUT_FAILURE)
        running = false
        lock.notifyAll()
    }

    private fun isRunning(loopGeneration: Long): Boolean = synchronized(lock) {
        running && generation == loopGeneration
    }

    private fun publishStatusLocked(value: NdiAudioStatus = status) {
        status = value
        onStatusChanged(value)
        onDiagnosticsChanged(NdiAudioDiagnostics(status = value))
    }

    private fun publishLevels(value: NdiAudioLevels) {
        onLevelsChanged(value)
    }

    private fun publishDiagnosticsLocked(total: Long, dropped: Long, underruns: Int) {
        onDiagnosticsChanged(NdiAudioDiagnostics(status, SAMPLE_RATE, CHANNEL_COUNT, total, dropped, underruns))
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val CHANNEL_COUNT = 2
        const val SAMPLES_PER_PULL = 960
        const val BYTES_PER_SAMPLE = 2
        const val NO_SIGNAL_START_MS = 2_000L
        const val NO_SIGNAL_STALL_MS = 1_500L
        const val STATS_INTERVAL_MS = 1_000L
        const val INVALID_CAPTURE = -3L
        const val ERROR_OUTPUT = -1
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
