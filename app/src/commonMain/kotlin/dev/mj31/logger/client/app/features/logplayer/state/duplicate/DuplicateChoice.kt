package dev.mj31.logger.client.app.features.logplayer.state.duplicate

/** What to do with a file that repeats one already open. */
enum class DuplicateChoice {

    /** Join it onto the open file, keeping each shared record once. Offered only for an overlap. */
    MERGE,

    /** Open it as a file of its own, shared records and all. */
    ADD_SEPARATELY,

    /** Leave it out. */
    SKIP,
}
