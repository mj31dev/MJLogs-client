package dev.mj31.logger.client.app.features.sessions

/** Every action the session list can produce. */
sealed interface SessionsIntent {

    /**
     * Open this file.
     *
     * Handled outside this store: what a session replaces is the player's workspace, and the list
     * has no business reaching into it.
     */
    data class Open(val path: String) : SessionsIntent

    /** Ask for a session file that is not in the list. */
    data object RequestOpenFile : SessionsIntent

    /** Carry on with the workspace that was open when the application was last closed. */
    data object ContinueLast : SessionsIntent

    /** Start from nothing. Handled outside this store, for the same reason [Open] is. */
    data object StartNew : SessionsIntent

    /** Drop an entry from the list; the file itself is never touched. */
    data class Forget(val path: String) : SessionsIntent
}
