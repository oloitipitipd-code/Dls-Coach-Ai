package com.dlscoachai.app

import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Small, allocation-light frame metric helper kept Android-independent for JVM tests.
 * Returns -1 when the supplied image plane cannot be sampled safely.
 */
internal object CaptureFrameMetrics {
    fun averageBrightness(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int
    ): Int {
        if (width < 1 || height < 1 || rowStride < 1 || pixelStride < 3) return -1
        val pixels = buffer.duplicate()
        val steps = 24
        var total = 0L
        var count = 0
        for (row in 0 until steps) {
            val y = (row * height / steps).coerceAtMost(height - 1)
            for (column in 0 until steps) {
                val x = (column * width / steps).coerceAtMost(width - 1)
                val offset = y.toLong() * rowStride + x.toLong() * pixelStride
                if (offset >= 0 && offset + 2 < pixels.limit()) {
                    val index = offset.toInt()
                    val red = pixels.get(index).toInt() and 0xff
                    val green = pixels.get(index + 1).toInt() and 0xff
                    val blue = pixels.get(index + 2).toInt() and 0xff
                    total += (0.2126 * red + 0.7152 * green + 0.0722 * blue).roundToInt()
                    count++
                }
            }
        }
        return if (count == 0) -1 else (total / count).toInt()
    }
}
