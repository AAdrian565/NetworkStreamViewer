package com.adriant.networkstreamviewer.data.camera

import android.graphics.ImageFormat
import android.media.Image

internal class Yuv420ToNv12Converter {
    private var output = ByteArray(0)

    fun convert(image: Image): ByteArray {
        require(image.format == ImageFormat.YUV_420_888 && image.planes.size == 3)
        val width = image.width
        val height = image.height
        val requiredSize = width * height * 3 / 2
        if (output.size != requiredSize) output = ByteArray(requiredSize)

        copyPlane(image.planes[0], width, height, 0, 1)
        copyPlane(image.planes[1], width / 2, height / 2, width * height, 2)
        copyPlane(image.planes[2], width / 2, height / 2, width * height + 1, 2)
        return output
    }

    private fun copyPlane(
        plane: Image.Plane,
        planeWidth: Int,
        planeHeight: Int,
        outputOffset: Int,
        outputPixelStride: Int
    ) {
        val input = plane.buffer
        val inputOffset = input.position()
        for (row in 0 until planeHeight) {
            for (column in 0 until planeWidth) {
                val inputIndex = inputOffset + row * plane.rowStride + column * plane.pixelStride
                val outputIndex = outputOffset + (row * planeWidth + column) * outputPixelStride
                output[outputIndex] = input.get(inputIndex)
            }
        }
    }
}
