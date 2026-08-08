package dev.mj31.logger.client.app.usecase.playback

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.player.PlaybackState
import dev.mj31.logger.client.domain.player.PlaybackStatus
import dev.mj31.logger.client.domain.player.VideoStep
import kotlin.test.Test

/**
 * Where a nudge of the playhead lands.
 *
 * The size of a frame is the recording's own, so the same button moves a sixty frame recording half
 * as far as a thirty frame one — which is the point: the step has to be one frame of *this* file, or
 * pressing it may not change the picture at all.
 */
class StepVideoPositionUseCaseTest {

    private val step = StepVideoPositionUseCase()

    @Test
    fun `a second is a second whatever the recording`() {
        assertThat(step(playback = at(positionMillis = 5_000L), step = VideoStep.SECOND, steps = 1))
            .isEqualTo(6_000L)
        assertThat(step(playback = at(positionMillis = 5_000L), step = VideoStep.SECOND, steps = -1))
            .isEqualTo(4_000L)
    }

    @Test
    fun `a frame is as long as this recording says it is`() {
        assertThat(step(playback = at(positionMillis = 5_000L, fps = 30.0), step = VideoStep.FRAME, steps = 1))
            .isEqualTo(5_033L)
        assertThat(step(playback = at(positionMillis = 5_000L, fps = 60.0), step = VideoStep.FRAME, steps = 1))
            .isEqualTo(5_017L)
        assertThat(step(playback = at(positionMillis = 5_000L, fps = 25.0), step = VideoStep.FRAME, steps = -1))
            .isEqualTo(4_960L)
    }

    /** A file that declines to state its rate still moves the picture rather than doing nothing. */
    @Test
    fun `a recording that states no rate is stepped by a plausible frame`() {
        assertThat(step(playback = at(positionMillis = 5_000L, fps = 0.0), step = VideoStep.FRAME, steps = 1))
            .isEqualTo(5_033L)
    }

    @Test
    fun `stepping past the start stops at the start`() {
        assertThat(step(playback = at(positionMillis = 200L), step = VideoStep.SECOND, steps = -1)).isEqualTo(0L)
    }

    @Test
    fun `stepping past the end stops at the end`() {
        assertThat(step(playback = at(positionMillis = 59_500L), step = VideoStep.SECOND, steps = 1))
            .isEqualTo(DURATION)
    }

    @Test
    fun `several steps at once are several steps`() {
        assertThat(step(playback = at(positionMillis = 5_000L, fps = 30.0), step = VideoStep.FRAME, steps = 3))
            .isEqualTo(5_099L)
    }

    private fun at(positionMillis: Long, fps: Double = 30.0) = PlaybackState(
        status = PlaybackStatus.PAUSED,
        positionMillis = positionMillis,
        durationMillis = DURATION,
        frameRateFps = fps,
    )

    private companion object {
        const val DURATION = 60_000L
    }
}
