package dev.mj31.logger.client.data.source.video

import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.source.video.VideoFrameScanner
import dev.mj31.logger.client.domain.source.video.VideoScan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber

/**
 * Opens a second decoder over a file the player may already be showing.
 *
 * FFmpeg is perfectly willing to have the same file open twice, and the alternative — borrowing the
 * player's grabber — would mean seeking the picture out from under whoever is watching it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FFmpegVideoFrameScanner(
    private val dispatcher: CoroutineDispatcher,
) : VideoFrameScanner {

    override suspend fun open(media: VideoMedia): VideoScan? {
        val decoder = dispatcher.limitedParallelism(parallelism = 1)
        return withContext(context = decoder) {
            val grabber = FFmpegFrameGrabber(media.path)
            avutil.av_log_set_level(avutil.AV_LOG_ERROR)
            grabber.pixelFormat = avutil.AV_PIX_FMT_BGRA
            runCatching { grabber.start() }.getOrElse {
                runCatching { grabber.release() }
                return@withContext null
            }
            FFmpegVideoScan(
                grabber = grabber,
                decoder = decoder,
                durationMillis = grabber.lengthInTime.coerceAtLeast(minimumValue = 0L) / MICROS_PER_MILLI,
            )
        }
    }

    private companion object {
        const val MICROS_PER_MILLI = 1_000L
    }
}
