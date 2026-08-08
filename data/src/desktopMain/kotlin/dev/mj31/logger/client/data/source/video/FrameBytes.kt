package dev.mj31.logger.client.data.source.video

import dev.mj31.logger.client.domain.player.VideoFrame
import java.nio.ByteBuffer
import org.bytedeco.javacv.Frame

/**
 * Copies a decoded plane into a packed BGRA array.
 *
 * The player keeps two buffers and alternates them, because it publishes a frame sixty times a
 * second into a flow the UI is still reading. A scan grabs a dozen frames in total and hands each to
 * a recognizer that is done with it before the next arrives, so it simply allocates.
 */
internal fun Frame.toVideoFrame(sequence: Long): VideoFrame? {
    val source = image?.firstOrNull() as? ByteBuffer ?: return null
    if (imageWidth <= 0 || imageHeight <= 0) return null

    val rowBytes = imageWidth * BYTES_PER_PIXEL
    val target = ByteArray(size = rowBytes * imageHeight)

    source.rewind()
    if (imageStride == rowBytes) {
        source.get(target, 0, minOf(a = target.size, b = source.remaining()))
    } else {
        repeat(times = imageHeight) { row ->
            source.position(row * imageStride)
            source.get(target, row * rowBytes, rowBytes)
        }
    }
    return VideoFrame(width = imageWidth, height = imageHeight, pixels = target, sequence = sequence)
}

private const val BYTES_PER_PIXEL = 4
