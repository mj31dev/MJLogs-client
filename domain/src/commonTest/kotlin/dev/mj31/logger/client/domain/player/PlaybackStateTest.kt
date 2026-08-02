package dev.mj31.logger.client.domain.player

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class PlaybackStateTest {

    @Test
    fun `only the playing status counts as playing`() {
        PlaybackStatus.entries.forEach { status ->
            val state = PlaybackState(status = status)

            assertThat(state.isPlaying).isEqualTo(status == PlaybackStatus.PLAYING)
        }
    }

    @Test
    fun `media is considered loaded unless the player is idle or broken`() {
        val withMedia = listOf(PlaybackStatus.READY, PlaybackStatus.PLAYING, PlaybackStatus.PAUSED, PlaybackStatus.ENDED)
        val withoutMedia = listOf(PlaybackStatus.IDLE, PlaybackStatus.ERROR)

        assertThat(withMedia.all { PlaybackState(status = it).hasMedia }).isTrue()
        assertThat(withoutMedia.none { PlaybackState(status = it).hasMedia }).isTrue()
    }

    @Test
    fun `the idle constant carries no position, duration or error`() {
        assertThat(PlaybackState.IDLE.status).isEqualTo(PlaybackStatus.IDLE)
        assertThat(PlaybackState.IDLE.positionMillis).isEqualTo(0L)
        assertThat(PlaybackState.IDLE.durationMillis).isEqualTo(0L)
        assertThat(PlaybackState.IDLE.errorMessage).isNull()
    }

    @Test
    fun `a frame keeps the buffer it was created with`() {
        val pixels = ByteArray(size = 16)

        val frame = VideoFrame(width = 2, height = 2, pixels = pixels, sequence = 7L)

        assertThat(frame.pixels).isSameInstanceAs(pixels)
        assertThat(frame.sequence).isEqualTo(7L)
    }
}
