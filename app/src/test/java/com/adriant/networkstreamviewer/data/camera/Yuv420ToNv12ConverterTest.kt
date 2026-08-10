package com.adriant.networkstreamviewer.data.camera

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class Yuv420ToNv12ConverterTest {
    @Test
    fun copiesCroppedRowsWithPixelAndRowStrides() {
        val input =
            ByteBuffer.wrap(
                byteArrayOf(
                    10,
                    99,
                    50,
                    88,
                    60,
                    77,
                    70,
                    66,
                    20,
                    99,
                    30,
                    88,
                    40,
                    77,
                    50,
                    66,
                    60,
                    99,
                    70,
                    88,
                    80,
                    77,
                    90,
                    66,
                ),
            )
        val output = ByteArray(4)

        copyPlaneToOutput(
            input = input,
            inputOffset = 0,
            inputRowStride = 8,
            inputPixelStride = 2,
            cropLeft = 1,
            cropTop = 1,
            planeWidth = 2,
            planeHeight = 2,
            output = output,
            outputOffset = 0,
            outputPixelStride = 1,
        )

        assertArrayEquals(byteArrayOf(30, 40, 70, 80), output)
    }

    @Test
    fun interleavesChromaPlanesWithUnitInputPixelStride() {
        val output = ByteArray(8)

        copyPlaneToOutput(
            input = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)),
            inputOffset = 0,
            inputRowStride = 2,
            inputPixelStride = 1,
            cropLeft = 0,
            cropTop = 0,
            planeWidth = 2,
            planeHeight = 2,
            output = output,
            outputOffset = 0,
            outputPixelStride = 2,
        )
        copyPlaneToOutput(
            input = ByteBuffer.wrap(byteArrayOf(5, 6, 7, 8)),
            inputOffset = 0,
            inputRowStride = 2,
            inputPixelStride = 1,
            cropLeft = 0,
            cropTop = 0,
            planeWidth = 2,
            planeHeight = 2,
            output = output,
            outputOffset = 1,
            outputPixelStride = 2,
        )

        assertArrayEquals(byteArrayOf(1, 5, 2, 6, 3, 7, 4, 8), output)
    }
}
