package dev.mj31.logger.client.app.view

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.player.VideoFrame
import kotlin.test.Test

class VideoFrameImageTest {

    @Test
    fun `a decoded frame becomes an image of the same size`() {
        val frame = VideoFrame(
            width = WIDTH,
            height = HEIGHT,
            pixels = ByteArray(size = WIDTH * HEIGHT * BYTES_PER_PIXEL) { 0x7F },
            sequence = 1L,
        )

        val image = frame.toImageBitmap()

        assertThat(image).isNotNull()
        assertThat(image?.width).isEqualTo(WIDTH)
        assertThat(image?.height).isEqualTo(HEIGHT)
    }

    @Test
    fun `an empty frame produces no image`() {
        val frame = VideoFrame(width = 0, height = 0, pixels = ByteArray(size = 0), sequence = 0L)

        assertThat(frame.toImageBitmap()).isNull()
    }

    private companion object {
        const val WIDTH = 64
        const val HEIGHT = 48
        const val BYTES_PER_PIXEL = 4
    }
}
