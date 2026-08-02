package dev.mj31.logger.client.data.player

import dev.mj31.logger.client.domain.player.VideoPlayer
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Chooses a playback backend for the current machine.
 *
 * FFmpeg travels with the application, so this normally always succeeds; the fallback only covers a
 * platform whose native bundle was not packaged, and it keeps the log side of the workspace working.
 */
object DesktopVideoPlayerProvider {

    fun create(dispatcher: CoroutineDispatcher): VideoPlayer =
        runCatching { FFmpegVideoPlayer(dispatcher = dispatcher) as VideoPlayer }
            .getOrElse { error ->
                UnavailableVideoPlayer(reason = "$DECODER_MISSING (${error.message.orEmpty()})")
            }

    private const val DECODER_MISSING =
        "No video decoder is available for this platform; log analysis stays available."
}
