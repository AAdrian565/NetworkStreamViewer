package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiAudioLevels
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/** Computes display-ready stereo levels without changing the buffer consumed by AudioTrack. */
class AudioLevelMeter(
    private val clock: Clock = Clock { System.nanoTime() / NANOS_PER_MILLISECOND }
) {
    fun interface Clock {
        fun nowMillis(): Long
    }

    private var lastUpdateMillis = Long.MIN_VALUE
    private var lastProcessMillis = Long.MIN_VALUE
    private var leftPeak = FLOOR_DBFS
    private var rightPeak = FLOOR_DBFS
    private var leftRms = FLOOR_DBFS
    private var rightRms = FLOOR_DBFS
    private var leftPeakHoldUntil = 0L
    private var rightPeakHoldUntil = 0L
    private var leftClipUntil = 0L
    private var rightClipUntil = 0L
    private var published = NdiAudioLevels.FLOOR

    fun process(buffer: ByteBuffer, nowMillis: Long = clock.nowMillis()): NdiAudioLevels {
        if (lastProcessMillis != Long.MIN_VALUE && nowMillis - lastProcessMillis < PUBLISH_INTERVAL_MS) {
            return published
        }
        lastProcessMillis = nowMillis

        val samples = buffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer()
        val sampleCount = samples.remaining() / CHANNELS
        if (sampleCount <= 0) return published

        var leftPeakLinear = 0f
        var rightPeakLinear = 0f
        var leftSum = 0.0
        var rightSum = 0.0
        var leftClipped = false
        var rightClipped = false
        repeat(sampleCount) {
            val left = samples.get().toInt()
            val right = samples.get().toInt()
            val leftLinear = abs(left / 32768f)
            val rightLinear = abs(right / 32768f)
            leftPeakLinear = max(leftPeakLinear, leftLinear)
            rightPeakLinear = max(rightPeakLinear, rightLinear)
            leftSum += leftLinear * leftLinear
            rightSum += rightLinear * rightLinear
            leftClipped = leftClipped || left == Short.MIN_VALUE.toInt() || left == Short.MAX_VALUE.toInt()
            rightClipped = rightClipped || right == Short.MIN_VALUE.toInt() || right == Short.MAX_VALUE.toInt()
        }

        val leftPeakDb = toDbfs(leftPeakLinear)
        val rightPeakDb = toDbfs(rightPeakLinear)
        val leftRmsDb = toDbfs(sqrt(leftSum / sampleCount).toFloat())
        val rightRmsDb = toDbfs(sqrt(rightSum / sampleCount).toFloat())
        val elapsed = if (lastUpdateMillis == Long.MIN_VALUE) 0L else (nowMillis - lastUpdateMillis).coerceAtLeast(0L)
        leftRms = ballistics(leftRms, leftRmsDb, elapsed)
        rightRms = ballistics(rightRms, rightRmsDb, elapsed)
        leftPeak = peakBallistics(leftPeak, leftPeakDb, nowMillis, true)
        rightPeak = peakBallistics(rightPeak, rightPeakDb, nowMillis, false)
        if (leftClipped) leftClipUntil = nowMillis + CLIP_HOLD_MS
        if (rightClipped) rightClipUntil = nowMillis + CLIP_HOLD_MS
        lastUpdateMillis = nowMillis

        published = NdiAudioLevels(
            leftPeakDbfs = leftPeak,
            rightPeakDbfs = rightPeak,
            leftRmsDbfs = leftRms,
            rightRmsDbfs = rightRms,
            leftClipped = nowMillis < leftClipUntil,
            rightClipped = nowMillis < rightClipUntil
        )
        return published
    }

    fun reset(): NdiAudioLevels {
        lastUpdateMillis = Long.MIN_VALUE
        lastProcessMillis = Long.MIN_VALUE
        leftPeak = FLOOR_DBFS
        rightPeak = FLOOR_DBFS
        leftRms = FLOOR_DBFS
        rightRms = FLOOR_DBFS
        leftPeakHoldUntil = 0L
        rightPeakHoldUntil = 0L
        leftClipUntil = 0L
        rightClipUntil = 0L
        published = NdiAudioLevels.FLOOR
        return published
    }

    private fun peakBallistics(previous: Float, current: Float, now: Long, left: Boolean): Float {
        if (current >= previous) {
            if (left) leftPeakHoldUntil = now + PEAK_HOLD_MS else rightPeakHoldUntil = now + PEAK_HOLD_MS
            return current
        }
        val holdUntil = if (left) leftPeakHoldUntil else rightPeakHoldUntil
        if (now < holdUntil) return previous
        val elapsedSeconds = (now - holdUntil).coerceAtLeast(0L) / 1000f
        return max(FLOOR_DBFS, previous - elapsedSeconds * PEAK_FALL_DB_PER_SECOND)
    }

    private fun ballistics(previous: Float, current: Float, elapsedMillis: Long): Float {
        if (current >= previous || elapsedMillis <= 0L) return current
        val alpha = 1f - exp(-elapsedMillis / RMS_RELEASE_MS.toFloat())
        return previous + (current - previous) * alpha
    }

    private fun toDbfs(linear: Float): Float = (20f * ln(max(linear, MIN_LINEAR)) / LN_10)
        .coerceIn(FLOOR_DBFS, 0f)

    private companion object {
        const val CHANNELS = 2
        const val FLOOR_DBFS = -60f
        const val MIN_LINEAR = 0.001f
        const val LN_10 = 2.3025851f
        const val PUBLISH_INTERVAL_MS = 50L
        const val RMS_RELEASE_MS = 300L
        const val PEAK_HOLD_MS = 750L
        const val PEAK_FALL_DB_PER_SECOND = 20f
        const val CLIP_HOLD_MS = 1_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
