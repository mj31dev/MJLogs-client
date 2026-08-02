package dev.mj31.logger.client.data.player

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.player.PlaybackStatus
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.media.VideoMedia

class UnavailableVideoPlayerTest {

    private val player = UnavailableVideoPlayer(reason = REASON)

    @Test
    fun `reports why playback is unavailable from the start`() {
        assertThat(player.state.value.status).isEqualTo(PlaybackStatus.ERROR)
        assertThat(player.state.value.errorMessage).isEqualTo(REASON)
        assertThat(player.state.value.hasMedia).isFalse()
    }

    @Test
    fun `opening a media keeps reporting the same reason`() {
        player.open(media = VideoMedia(path = "/media/a.mp4", name = "a.mp4"))

        assertThat(player.state.value.status).isEqualTo(PlaybackStatus.ERROR)
        assertThat(player.state.value.errorMessage).isEqualTo(REASON)
    }

    @Test
    fun `transport commands are silently ignored instead of crashing`() {
        player.play()
        player.pause()
        player.seekTo(positionMillis = 1_000L)
        player.release()

        assertThat(player.state.value.status).isEqualTo(PlaybackStatus.ERROR)
        assertThat(player.frames.value).isNull()
    }

    private companion object {
        const val REASON = "libVLC was not found."
    }
}
