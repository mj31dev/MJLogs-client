package dev.mj31.logger.client.data.source.video

import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.source.video.VideoScan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FFmpegFrameGrabber

/**
 * One video held open for inspection.
 *
 * Frames come back at their recorded size: a status bar clock is only a few dozen pixels tall to
 * begin with, and the scaling the player applies to keep redraws cheap would take it below what a
 * recognizer can read.
 *
 * The grabber is not thread safe, so every call is confined to [decoder], a dispatcher of width one
 * that belongs to this scan alone.
 */
class FFmpegVideoScan(
    private val grabber: FFmpegFrameGrabber,
    private val decoder: CoroutineDispatcher,
    override val durationMillis: Long,
) : VideoScan {

    private var sequence = 0L

    override suspend fun frameAt(positionMillis: Long): VideoFrame? = withContext(context = decoder) {
        val target = positionMillis.coerceIn(range = 0L..durationMillis.coerceAtLeast(minimumValue = 0L))
        runCatching {
            grabber.setVideoTimestamp(target * MICROS_PER_MILLI)
            grabber.grabImage()
        }.getOrNull()?.let { frame ->
            sequence += 1
            frame.toVideoFrame(sequence = sequence)
        }
    }

    override suspend fun close() {
        withContext(context = decoder) {
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
    }

    private companion object {
        const val MICROS_PER_MILLI = 1_000L
    }
}
