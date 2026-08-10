package com.adriant.networkstreamviewer.data.camera

import android.graphics.ImageFormat
import android.media.Image
import java.nio.ByteBuffer

internal data class Nv12Frame(
    val data: ByteArray,
    val width: Int,
    val height: Int,
)

internal class Yuv420ToNv12Converter {
    private var output = ByteArray(0)

    fun convert(image: Image): Nv12Frame {
        require(image.format == ImageFormat.YUV_420_888 && image.planes.size == 3)
        val crop = image.cropRect
        require(
            crop.left >= 0 &&
                crop.top >= 0 &&
                crop.right <= image.width &&
                crop.bottom <= image.height &&
                crop.width() > 0 &&
                crop.height() > 0 &&
                crop.width() % 2 == 0 &&
                crop.height() % 2 == 0 &&
                crop.left % 2 == 0 &&
                crop.top % 2 == 0,
        )

        val width = crop.width()
        val height = crop.height()
        val requiredSize = width * height * 3 / 2
        if (output.size != requiredSize) output = ByteArray(requiredSize)

        copyPlane(
            plane = image.planes[0],
            cropLeft = crop.left,
            cropTop = crop.top,
            planeWidth = width,
            planeHeight = height,
            outputOffset = 0,
            outputPixelStride = 1,
        )
        copyPlane(
            plane = image.planes[1],
            cropLeft = crop.left / 2,
            cropTop = crop.top / 2,
            planeWidth = width / 2,
            planeHeight = height / 2,
            outputOffset = width * height,
            outputPixelStride = 2,
        )
        copyPlane(
            plane = image.planes[2],
            cropLeft = crop.left / 2,
            cropTop = crop.top / 2,
            planeWidth = width / 2,
            planeHeight = height / 2,
            outputOffset = width * height + 1,
            outputPixelStride = 2,
        )
        return Nv12Frame(data = output, width = width, height = height)
    }

    private fun copyPlane(
        plane: Image.Plane,
        cropLeft: Int,
        cropTop: Int,
        planeWidth: Int,
        planeHeight: Int,
        outputOffset: Int,
        outputPixelStride: Int,
    ) {
        copyPlaneToOutput(
            input = plane.buffer,
            inputOffset = plane.buffer.position(),
            inputRowStride = plane.rowStride,
            inputPixelStride = plane.pixelStride,
            cropLeft = cropLeft,
            cropTop = cropTop,
            planeWidth = planeWidth,
            planeHeight = planeHeight,
            output = output,
            outputOffset = outputOffset,
            outputPixelStride = outputPixelStride,
        )
    }
}

internal fun copyPlaneToOutput(
    input: ByteBuffer,
    inputOffset: Int,
    inputRowStride: Int,
    inputPixelStride: Int,
    cropLeft: Int,
    cropTop: Int,
    planeWidth: Int,
    planeHeight: Int,
    output: ByteArray,
    outputOffset: Int,
    outputPixelStride: Int,
) {
    require(inputRowStride > 0 && inputPixelStride > 0)
    require(cropLeft >= 0 && cropTop >= 0 && planeWidth > 0 && planeHeight > 0)
    require(outputPixelStride > 0)

    for (row in 0 until planeHeight) {
        for (column in 0 until planeWidth) {
            val inputIndex =
                inputOffset +
                    (cropTop + row) * inputRowStride +
                    (cropLeft + column) * inputPixelStride
            val outputIndex =
                outputOffset +
                    (row * planeWidth + column) * outputPixelStride
            output[outputIndex] = input.get(inputIndex)
        }
    }
}
