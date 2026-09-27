package dev.mj31.logger.client.app.features.logplayer.state.duplicate

/** Why a file about to join the session is put to the user first. */
enum class DuplicateKind {

    /** Another path holding the very records of a file already open. */
    COPY,

    /** The same log caught at another moment: some records in both, some in one only. */
    OVERLAP,
}
