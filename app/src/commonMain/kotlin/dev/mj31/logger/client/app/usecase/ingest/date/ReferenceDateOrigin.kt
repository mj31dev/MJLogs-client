package dev.mj31.logger.client.app.usecase.ingest.date

/** Where the day a dateless record belongs to was taken from, most trustworthy first. */
enum class ReferenceDateOrigin {

    /**
     * A date the file states about itself before its first record: `Log started 2024-08-01 22:14`.
     *
     * Trusted over everything else, because it is the only source that dates the *start* of the log
     * rather than its name or its last write.
     */
    HEADER,

    /** A date written into the file name, as log rotation leaves behind: `app.log.2024-08-01`. */
    FILE_NAME,

    /** The moment the file — or the archive entry — was last written. */
    MODIFICATION_TIME,

    /** The user said so. */
    STATED,
}
