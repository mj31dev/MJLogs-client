package dev.mj31.logger.client.app.usecase.playback

import dev.mj31.logger.client.domain.player.PlaybackState
import dev.mj31.logger.client.domain.player.VideoStep
import kotlin.math.roundToLong

/**
 * Where the playhead lands when it is nudged by whole frames or whole seconds.
 *
 * The size of a frame is the file's own business and changes with it — thirty a second on one
 * recording, sixty on the next — so it is read from the playback state rather than assumed. A file
 * that declines to say falls back to [ASSUMED_FRAME_MILLIS], which is a common enough rate that the
 * button still moves the picture by roughly one frame instead of doing nothing at all.
 *
 * The result is clamped to the recording. Stepping past either end is not an error worth reporting —
 * the playhead simply stops where the recording does, which is what a person expects from a control
 * they are holding down.
 */
class StepVideoPositionUseCase {

    operator fun invoke(playback: PlaybackState, step: VideoStep, steps: Int): Long {
        val distance = when (step) {
            VideoStep.FRAME -> frameMillis(frameRateFps = playback.frameRateFps)
            VideoStep.SECOND -> MILLIS_PER_SECOND
        }
        val target = playback.positionMillis + distance * steps
        val last = playback.durationMillis.coerceAtLeast(minimumValue = 0L)
        return target.coerceIn(minimumValue = 0L, maximumValue = last)
    }

    private fun frameMillis(frameRateFps: Double): Long =
        if (frameRateFps > 0.0) {
            (MILLIS_PER_SECOND / frameRateFps).roundToLong().coerceAtLeast(minimumValue = 1L)
        } else {
            ASSUMED_FRAME_MILLIS
        }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L

        /** Thirty frames a second, the rate a phone records at unless told otherwise. */
        const val ASSUMED_FRAME_MILLIS = 33L
    }
}
