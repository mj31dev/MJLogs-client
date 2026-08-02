package dev.mj31.logger.client.data.player

import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.player.PlaybackState
import dev.mj31.logger.client.domain.player.PlaybackStatus
import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.player.VideoPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import java.nio.ByteBuffer

/**
 * [VideoPlayer] backed by the FFmpeg libraries bundled with the application.
 *
 * Nothing has to be installed on the machine, and the codec coverage is FFmpeg's own, HEVC included.
 * Frames are decoded straight into BGRA and handed to Compose, so no windowing interop is involved.
 *
 * The grabber is not thread safe, therefore every call into it is confined to a single decoding
 * thread, and playback is a job on that same thread which the transport controls cancel and restart.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FFmpegVideoPlayer(
    dispatcher: CoroutineDispatcher,
    private val maxFrameSide: Int = DEFAULT_MAX_FRAME_SIDE,
) : VideoPlayer {

    private val decoder = dispatcher.limitedParallelism(parallelism = 1)
    private val scope = CoroutineScope(context = SupervisorJob() + decoder)

    private val playbackState = MutableStateFlow(value = PlaybackState.IDLE)
    private val frameState = MutableStateFlow<VideoFrame?>(value = null)

    private var grabber: FFmpegFrameGrabber? = null
    private var playbackJob: Job? = null
    private val buffers = arrayOfNulls<ByteArray>(2)
    private var bufferIndex = 0
    private var sequence = 0L

    override val state: StateFlow<PlaybackState> = playbackState.asStateFlow()

    override val frames: StateFlow<VideoFrame?> = frameState.asStateFlow()

    override fun open(media: VideoMedia) {
        scope.launch {
            stopPlayback()
            closeGrabber()
            frameState.value = null

            val opened = runCatching { openGrabber(path = media.path) }.getOrElse { error ->
                playbackState.value = PlaybackState(
                    status = PlaybackStatus.ERROR,
                    errorMessage = "Unable to open ${media.name}: ${error.message.orEmpty()}",
                )
                return@launch
            }
            grabber = opened
            playbackState.value = PlaybackState(
                status = PlaybackStatus.PAUSED,
                positionMillis = 0L,
                durationMillis = opened.lengthInTime.coerceAtLeast(minimumValue = 0L) / MICROS_PER_MILLI,
            )
            // Both timelines stay independent until the user synchronizes them, so the screencast
            // waits on its first frame rather than starting to play.
            publishNextFrame(grabber = opened)
        }
    }

    override fun play() {
        scope.launch {
            val current = grabber ?: return@launch
            if (playbackState.value.status == PlaybackStatus.ENDED) {
                seekGrabber(grabber = current, positionMillis = 0L)
            }
            startPlayback(grabber = current)
        }
    }

    override fun pause() {
        scope.launch {
            stopPlayback()
            if (playbackState.value.hasMedia) {
                playbackState.value = playbackState.value.copy(status = PlaybackStatus.PAUSED)
            }
        }
    }

    override fun seekTo(positionMillis: Long) {
        scope.launch {
            val current = grabber ?: return@launch
            val wasPlaying = playbackState.value.isPlaying
            stopPlayback()
            seekGrabber(grabber = current, positionMillis = positionMillis)
            publishNextFrame(grabber = current)
            if (wasPlaying) startPlayback(grabber = current)
        }
    }

    override fun release() {
        runBlocking {
            runCatching { stopPlayback() }
            runCatching { closeGrabber() }
        }
        scope.cancel()
    }

    private fun openGrabber(path: String): FFmpegFrameGrabber = FFmpegFrameGrabber(path).apply {
        // FFmpeg dumps the whole stream layout on stderr otherwise, which is noise in an application.
        avutil.av_log_set_level(avutil.AV_LOG_ERROR)
        pixelFormat = avutil.AV_PIX_FMT_BGRA
        start()
        // A phone screencast is tall rather than wide, so the longest side is what has to be capped.
        val longestSide = maxOf(a = imageWidth, b = imageHeight)
        if (longestSide > maxFrameSide) {
            val scale = maxFrameSide.toDouble() / longestSide
            imageWidth = even(value = (imageWidth * scale).toInt())
            imageHeight = even(value = (imageHeight * scale).toInt())
            restart()
        }
    }

    private fun startPlayback(grabber: FFmpegFrameGrabber) {
        playbackState.value = playbackState.value.copy(status = PlaybackStatus.PLAYING)
        playbackJob = scope.launch {
            val wallStartNanos = System.nanoTime()
            val startTimestamp = grabber.timestamp
            while (isActive) {
                val frame = runCatching { grabber.grabImage() }.getOrNull() ?: break
                val elapsedMicros = grabber.timestamp - startTimestamp
                val waitMillis = (wallStartNanos + elapsedMicros * NANOS_PER_MICRO - System.nanoTime()) /
                    NANOS_PER_MILLI
                if (waitMillis > 0) delay(timeMillis = waitMillis)
                publish(frame = frame, positionMicros = grabber.timestamp)
            }
            if (isActive) {
                playbackState.value = playbackState.value.copy(status = PlaybackStatus.ENDED)
            }
        }
    }

    private suspend fun stopPlayback() {
        playbackJob?.cancelAndJoin()
        playbackJob = null
    }

    private fun seekGrabber(grabber: FFmpegFrameGrabber, positionMillis: Long) {
        val target = positionMillis.coerceAtLeast(minimumValue = 0L) * MICROS_PER_MILLI
        runCatching { grabber.setVideoTimestamp(target) }
        playbackState.value = playbackState.value.copy(
            positionMillis = grabber.timestamp / MICROS_PER_MILLI,
            status = PlaybackStatus.PAUSED,
        )
    }

    private fun publishNextFrame(grabber: FFmpegFrameGrabber) {
        val frame = runCatching { grabber.grabImage() }.getOrNull() ?: return
        publish(frame = frame, positionMicros = grabber.timestamp)
    }

    private fun publish(frame: Frame, positionMicros: Long) {
        toVideoFrame(frame = frame)?.let { frameState.value = it }
        playbackState.value = playbackState.value.copy(
            positionMillis = positionMicros.coerceAtLeast(minimumValue = 0L) / MICROS_PER_MILLI,
        )
    }

    /**
     * Copies the decoded plane into a packed BGRA array.
     *
     * Two arrays are alternated so the render path allocates nothing while the UI is still allowed
     * to read the frame published before.
     */
    private fun toVideoFrame(frame: Frame): VideoFrame? {
        val source = frame.image?.firstOrNull() as? ByteBuffer ?: return null
        val width = frame.imageWidth
        val height = frame.imageHeight
        if (width <= 0 || height <= 0) return null

        val rowBytes = width * BYTES_PER_PIXEL
        val size = rowBytes * height
        bufferIndex = (bufferIndex + 1) % buffers.size
        val target = buffers[bufferIndex]?.takeIf { it.size == size } ?: ByteArray(size = size).also {
            buffers[bufferIndex] = it
        }

        source.rewind()
        if (frame.imageStride == rowBytes) {
            source.get(target, 0, minOf(a = size, b = source.remaining()))
        } else {
            repeat(times = height) { row ->
                source.position(row * frame.imageStride)
                source.get(target, row * rowBytes, rowBytes)
            }
        }
        sequence += 1
        return VideoFrame(width = width, height = height, pixels = target, sequence = sequence)
    }

    private fun closeGrabber() {
        grabber?.let { runCatching { it.stop() }; runCatching { it.release() } }
        grabber = null
    }

    private fun even(value: Int): Int = (value - value % 2).coerceAtLeast(minimumValue = 2)

    private companion object {
        const val DEFAULT_MAX_FRAME_SIDE = 1280
        const val BYTES_PER_PIXEL = 4
        const val MICROS_PER_MILLI = 1_000L
        const val NANOS_PER_MICRO = 1_000L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
