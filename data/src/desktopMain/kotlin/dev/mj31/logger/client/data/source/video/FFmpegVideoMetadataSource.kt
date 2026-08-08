package dev.mj31.logger.client.data.source.video

import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.media.VideoMetadata
import dev.mj31.logger.client.domain.source.video.VideoMetadataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber

/**
 * Reads what a container says about itself through the bundled FFmpeg libraries.
 *
 * The file has to be opened to be asked, but nothing is decoded: this costs a header read, which is
 * why it is the first thing an automatic synchronization tries.
 */
class FFmpegVideoMetadataSource(
    private val dispatcher: CoroutineDispatcher,
    private val parser: CreationTimeParser = CreationTimeParser(),
) : VideoMetadataSource {

    override suspend fun read(media: VideoMedia): VideoMetadata? = withContext(context = dispatcher) {
        val grabber = FFmpegFrameGrabber(media.path)
        avutil.av_log_set_level(avutil.AV_LOG_ERROR)
        try {
            runCatching { grabber.start() }.getOrElse { return@withContext null }
            metadataOf(grabber = grabber)
        } finally {
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
    }

    /** Stream level entries come first so that a format level one of the same name wins. */
    private fun metadataOf(grabber: FFmpegFrameGrabber): VideoMetadata {
        val declared = grabber.videoMetadata.orEmpty() + grabber.metadata.orEmpty()
        val created = parser.parse(metadata = declared)
        return VideoMetadata(
            creationTime = created?.instant,
            creationOffsetMinutes = created?.offsetMinutes,
            durationMillis = grabber.lengthInTime.coerceAtLeast(minimumValue = 0L) / MICROS_PER_MILLI,
            width = grabber.imageWidth,
            height = grabber.imageHeight,
        )
    }

    private companion object {
        const val MICROS_PER_MILLI = 1_000L
    }
}
