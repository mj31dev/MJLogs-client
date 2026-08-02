package dev.mj31.logger.client.domain.player

import kotlinx.coroutines.flow.StateFlow

/**
 * A decoded video frame in BGRA (8 bits per channel) layout.
 *
 * Deliberately not a data class: [pixels] is a large mutable buffer and every emission must be
 * treated as a distinct value by [StateFlow].
 */
class VideoFrame(
    val width: Int,
    val height: Int,
    val pixels: ByteArray,
    val sequence: Long,
)
