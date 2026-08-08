package dev.mj31.logger.client.app.usecase.sync.auto

import dev.mj31.logger.client.domain.sync.SyncAnchor

/**
 * How an attempt at synchronizing without the user ended.
 *
 * Only [Synchronized] changes anything. Every other case is a statement about what the recording did
 * not reveal, and each is answered differently on screen — which is the point of distinguishing
 * them: "the clock is somewhere I did not look" and "the clock and the logs cannot be reconciled at
 * any real offset" are opposite problems with opposite remedies.
 */
sealed interface AutoSyncOutcome {

    data class Synchronized(val anchor: SyncAnchor) : AutoSyncOutcome

    /** No region of the frame holds a clock that behaves like one; the user is asked to point. */
    data object ClockNotFound : AutoSyncOutcome

    /** A clock was found and never changed minute, so it locates nothing precisely. */
    data object NoMinuteChange : AutoSyncOutcome

    /** There is nothing to correlate: the workspace is missing a screencast or a log file. */
    data object NothingToCorrelate : AutoSyncOutcome

    /** The build carries no recognition model, so the picture cannot be read at all. */
    data object RecognizerMissing : AutoSyncOutcome

    /** The screencast could not be opened a second time for inspection. */
    data object VideoUnreadable : AutoSyncOutcome
}
