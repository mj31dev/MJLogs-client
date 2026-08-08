package dev.mj31.logger.client.domain.sync

/**
 * How a [SyncAnchor] came to exist.
 *
 * The mapping between the two timelines is identical whichever way the anchor was produced, but the
 * screen has to be able to say where it came from and how much it can be trusted: an anchor a human
 * placed on a record is exact, one derived from container metadata is worth about a second.
 */
enum class SyncOrigin {

    /** The user pinned a selected log record to the playhead. */
    SELECTED_ENTRY,

    /** The user typed the wall clock time visible in the frame. */
    FRAME_TIME,

    /** Derived from the creation time written into the video container. */
    VIDEO_METADATA,

    /** Derived from the clock the recording itself shows, read at the moment it changed minute. */
    SCREEN_CLOCK,
    ;

    val isAutomatic: Boolean
        get() = this == VIDEO_METADATA || this == SCREEN_CLOCK
}
