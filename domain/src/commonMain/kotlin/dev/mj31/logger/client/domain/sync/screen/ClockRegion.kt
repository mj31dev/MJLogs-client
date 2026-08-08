package dev.mj31.logger.client.domain.sync.screen

/**
 * Where on the frame the clock is, in fractions of the frame rather than in pixels.
 *
 * A region is measured once and reused across frames that the decoder may hand over at a different
 * size than the player shows, so pixels would be wrong the moment either end rescales. All four
 * edges are fractions of the frame, `0` being its top left corner.
 */
data class ClockRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {

    val isValid: Boolean
        get() = left in UNIT && top in UNIT && right in UNIT && bottom in UNIT && left < right && top < bottom

    fun leftPixels(width: Int): Int = (left * width).toInt().coerceIn(range = 0..width)

    fun topPixels(height: Int): Int = (top * height).toInt().coerceIn(range = 0..height)

    /** At least one pixel wide, so that a region rounded away on a small frame is still readable. */
    fun widthPixels(width: Int): Int =
        ((right - left) * width).toInt().coerceIn(range = 1..(width - leftPixels(width = width)).coerceAtLeast(1))

    fun heightPixels(height: Int): Int =
        ((bottom - top) * height).toInt().coerceIn(range = 1..(height - topPixels(height = height)).coerceAtLeast(1))

    private companion object {
        val UNIT = 0f..1f
    }
}
