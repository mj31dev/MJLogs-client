package dev.mj31.logger.client.app.features.logplayer.state.ingest

/** What a chosen time zone applies to. */
sealed interface ZoneTarget {

    /** One file of the session, which is read again in the chosen zone. */
    data class Source(val sourceId: String) : ZoneTarget

    /** The clock on the screen, which a typed or recognized frame time is read in. */
    data object FrameTime : ZoneTarget
}
