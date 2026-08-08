package dev.mj31.logger.client.data.player

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.player.PlaybackStatus
import dev.mj31.logger.client.domain.player.VideoFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.test.Test

/**
 * Exercises the real decoder against the clip shipped in `samples/`.
 *
 * Nothing has to be installed for this to run: the FFmpeg libraries travel with the build.
 */
class FFmpegVideoPlayerTest {

    private val clip = File("../samples/sample-clip.mp4")

    @Test
    fun `opening a clip yields a decoded frame and a duration`() = withPlayer { player ->
        player.open(media = VideoMedia(path = clip.absolutePath, name = clip.name))

        val frame = awaitFrame(player = player)

        assertThat(frame).isNotNull()
        assertThat(frame?.width).isGreaterThan(0)
        assertThat(frame?.height).isGreaterThan(0)
        assertThat(frame?.pixels?.size).isEqualTo((frame?.width ?: 0) * (frame?.height ?: 0) * BYTES_PER_PIXEL)
        assertThat(player.state.value.status).isNotEqualTo(PlaybackStatus.ERROR)
        assertThat(player.state.value.durationMillis).isGreaterThan(0L)
    }

    @Test
    fun `a frame carries a colour, not an empty buffer`() = withPlayer { player ->
        player.open(media = VideoMedia(path = clip.absolutePath, name = clip.name))

        val frame = requireNotNull(awaitFrame(player = player))

        assertThat(frame.pixels.any { it != 0.toByte() }).isTrue()
    }

    @Test
    fun `playing advances the position and pausing stops it`() = withPlayer { player ->
        player.open(media = VideoMedia(path = clip.absolutePath, name = clip.name))
        awaitFrame(player = player)

        player.play()
        val advanced = await(player = player) { it.positionMillis > 0L && it.isPlaying }
        assertThat(advanced).isTrue()

        player.pause()
        val paused = await(player = player) { it.status == PlaybackStatus.PAUSED }
        assertThat(paused).isTrue()

        val stopped = player.state.value.positionMillis
        runBlocking { delay(timeMillis = SETTLE_MILLIS) }
        assertThat(player.state.value.positionMillis).isEqualTo(stopped)
    }

    @Test
    fun `seeking moves the position and produces the frame at that point`() = withPlayer { player ->
        player.open(media = VideoMedia(path = clip.absolutePath, name = clip.name))
        val first = requireNotNull(awaitFrame(player = player))

        player.seekTo(positionMillis = SEEK_TARGET_MILLIS)

        val moved = await(player = player) { it.positionMillis >= SEEK_TARGET_MILLIS - SEEK_TOLERANCE_MILLIS }
        assertThat(moved).isTrue()

        // The position and the picture are published in that order, so waiting on the position and
        // then asserting on the frame is a race the decoder wins about one run in twenty.
        val redrawn = await(player = player) { (player.frames.value?.sequence ?: 0L) > first.sequence }
        assertThat(redrawn).isTrue()
    }

    @Test
    fun `an unreadable file is reported instead of hanging`() = withPlayer { player ->
        player.open(media = VideoMedia(path = "/nowhere/missing.mp4", name = "missing.mp4"))

        val failed = await(player = player) { it.status == PlaybackStatus.ERROR }

        assertThat(failed).isTrue()
        assertThat(player.state.value.errorMessage).contains("missing.mp4")
    }

    private fun withPlayer(block: (FFmpegVideoPlayer) -> Unit) {
        check(clip.isFile) { "Missing sample clip ${clip.absolutePath}" }
        val player = FFmpegVideoPlayer(dispatcher = Dispatchers.IO)
        try {
            block(player)
        } finally {
            player.release()
        }
    }

    private fun awaitFrame(player: FFmpegVideoPlayer): VideoFrame? = runBlocking {
        withTimeoutOrNull(timeMillis = TIMEOUT_MILLIS) {
            while (player.frames.value == null) delay(timeMillis = POLL_MILLIS)
            player.frames.value
        }
    }

    private fun await(player: FFmpegVideoPlayer, condition: (dev.mj31.logger.client.domain.player.PlaybackState) -> Boolean): Boolean =
        runBlocking {
            withTimeoutOrNull(timeMillis = TIMEOUT_MILLIS) {
                while (!condition(player.state.value)) delay(timeMillis = POLL_MILLIS)
                true
            } ?: false
        }

    private companion object {
        const val TIMEOUT_MILLIS = 20_000L
        const val POLL_MILLIS = 25L
        const val SETTLE_MILLIS = 300L
        const val SEEK_TARGET_MILLIS = 3_000L
        const val SEEK_TOLERANCE_MILLIS = 1_500L
        const val BYTES_PER_PIXEL = 4
    }
}
