package dev.mj31.logger.client.domain.player

/**
 * The two sizes of nudge a playhead is moved by when it is not being dragged.
 *
 * A second is how a person describes where they want to be; a frame is the smallest move there is,
 * and the one that matters when the question is which of two frames a log line belongs to — the
 * whole point of putting a recording beside a log is to answer that, and a slider cannot.
 */
enum class VideoStep {
    FRAME,
    SECOND,
}
