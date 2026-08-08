package dev.mj31.logger.client.data.sync.screen

import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.sync.screen.ClockRegion

/**
 * Cuts the clock out of a frame and hands it over in the shape a recognizer expects.
 *
 * Three things stand between a decoded frame and a readable line of text. The digits of a status bar
 * are around twenty pixels tall even on a full resolution recording, which is below what recognition
 * is reliable at, so the crop is enlarged. A status bar is usually light text on a dark background,
 * the inverse of what recognizers are trained on, so a predominantly dark crop is inverted. And the
 * colour carries nothing at all, so it is dropped first.
 *
 * Kept apart from the recognizer itself because all of this is arithmetic over a byte array, and
 * arithmetic can be tested without loading a native library.
 */
class ClockImagePreparer(private val scale: Int = DEFAULT_SCALE) {

    fun prepare(frame: VideoFrame, region: ClockRegion): ClockImage? {
        if (!region.isValid || frame.width <= 0 || frame.height <= 0) return null
        val left = region.leftPixels(width = frame.width)
        val top = region.topPixels(height = frame.height)
        val width = region.widthPixels(width = frame.width)
        val height = region.heightPixels(height = frame.height)
        if (width <= 0 || height <= 0) return null

        val grey = greyscale(frame = frame, left = left, top = top, width = width, height = height)
        if (isMostlyDark(pixels = grey)) invert(pixels = grey)
        return enlarge(pixels = grey, width = width, height = height)
    }

    /** BGRA in, one byte out, using the usual luma weights expressed in thousandths. */
    private fun greyscale(frame: VideoFrame, left: Int, top: Int, width: Int, height: Int): ByteArray {
        val target = ByteArray(size = width * height)
        val stride = frame.width * BYTES_PER_PIXEL
        for (row in 0 until height) {
            var source = (top + row) * stride + left * BYTES_PER_PIXEL
            var index = row * width
            repeat(times = width) {
                val blue = frame.pixels[source].toInt() and BYTE_MASK
                val green = frame.pixels[source + 1].toInt() and BYTE_MASK
                val red = frame.pixels[source + 2].toInt() and BYTE_MASK
                val luma = (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT) / WEIGHT_TOTAL
                target[index] = luma.toByte()
                source += BYTES_PER_PIXEL
                index += 1
            }
        }
        return target
    }

    private fun isMostlyDark(pixels: ByteArray): Boolean {
        if (pixels.isEmpty()) return false
        var total = 0L
        pixels.forEach { pixel -> total += (pixel.toInt() and BYTE_MASK) }
        return total / pixels.size < MID_GREY
    }

    private fun invert(pixels: ByteArray) {
        for (index in pixels.indices) {
            pixels[index] = (BYTE_MASK - (pixels[index].toInt() and BYTE_MASK)).toByte()
        }
    }

    /** Nearest neighbour: the goal is to give the recognizer more pixels, not a smoother picture. */
    private fun enlarge(pixels: ByteArray, width: Int, height: Int): ClockImage {
        val factor = scale.coerceAtLeast(minimumValue = 1)
        if (factor == 1) return ClockImage(pixels = pixels, width = width, height = height)

        val scaledWidth = width * factor
        val scaledHeight = height * factor
        val target = ByteArray(size = scaledWidth * scaledHeight)
        for (row in 0 until scaledHeight) {
            val sourceRow = (row / factor) * width
            val targetRow = row * scaledWidth
            for (column in 0 until scaledWidth) {
                target[targetRow + column] = pixels[sourceRow + column / factor]
            }
        }
        return ClockImage(pixels = target, width = scaledWidth, height = scaledHeight)
    }

    companion object {
        const val DEFAULT_SCALE = 3

        private const val BYTES_PER_PIXEL = 4
        private const val BYTE_MASK = 0xFF
        private const val MID_GREY = 128
        private const val RED_WEIGHT = 299
        private const val GREEN_WEIGHT = 587
        private const val BLUE_WEIGHT = 114
        private const val WEIGHT_TOTAL = 1_000
    }
}
