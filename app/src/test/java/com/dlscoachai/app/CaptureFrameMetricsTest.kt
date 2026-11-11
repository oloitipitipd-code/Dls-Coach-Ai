package com.dlscoachai.app

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureFrameMetricsTest {
    @Test
    fun samplesOpaqueRedRgbaPixels() {
        val bytes = ByteArray(4) { index -> if (index == 0) 0xff.toByte() else 0 }
        val result = CaptureFrameMetrics.averageBrightness(ByteBuffer.wrap(bytes), 1, 1, 4, 4)
        assertEquals(54, result)
    }

    @Test
    fun respectsRowStridePadding() {
        // Two RGBA pixels per row, followed by four padding bytes.
        val bytes = ByteArray(24)
        for (row in 0..1) {
            val offset = row * 12
            bytes[offset] = 0xff.toByte()
            bytes[offset + 1] = 0xff.toByte()
            bytes[offset + 2] = 0xff.toByte()
            bytes[offset + 3] = 0xff.toByte()
            bytes[offset + 4] = 0xff.toByte()
            bytes[offset + 5] = 0xff.toByte()
            bytes[offset + 6] = 0xff.toByte()
            bytes[offset + 7] = 0xff.toByte()
        }
        val result = CaptureFrameMetrics.averageBrightness(ByteBuffer.wrap(bytes), 2, 2, 12, 4)
        assertEquals(255, result)
    }

    @Test
    fun rejectsInvalidDimensionsAndPixelStride() {
        val buffer = ByteBuffer.wrap(ByteArray(16))
        assertEquals(-1, CaptureFrameMetrics.averageBrightness(buffer, 0, 1, 4, 4))
        assertEquals(-1, CaptureFrameMetrics.averageBrightness(buffer, 1, 1, 4, 2))
    }

    @Test
    fun rejectsBufferTooShortToReadAnyRgbPixel() {
        val result = CaptureFrameMetrics.averageBrightness(ByteBuffer.wrap(byteArrayOf(1, 2)), 1, 1, 4, 4)
        assertEquals(-1, result)
    }
}
