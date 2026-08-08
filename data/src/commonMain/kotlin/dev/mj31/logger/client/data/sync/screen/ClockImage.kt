package dev.mj31.logger.client.data.sync.screen

/**
 * The clock area of a frame, cut out and prepared for recognition: one grey byte per pixel, dark
 * text on a light background, enlarged.
 *
 * Not a data class for the same reason [dev.mj31.logger.client.domain.player.VideoFrame] is not one:
 * [pixels] is a buffer, and comparing two of them by value would be meaningless work.
 */
class ClockImage(
    val pixels: ByteArray,
    val width: Int,
    val height: Int,
)
