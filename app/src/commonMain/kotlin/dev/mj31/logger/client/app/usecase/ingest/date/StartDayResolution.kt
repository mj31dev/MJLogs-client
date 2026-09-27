package dev.mj31.logger.client.app.usecase.ingest.date

import kotlinx.datetime.LocalDate

/** What checking a file's placement against its modification time concluded. */
sealed interface StartDayResolution {

    /** One day fits, or the question does not arise; the file is read on [day]. */
    data class Settled(val day: LocalDate) : StartDayResolution

    /**
     * Two days fit equally well and the file cannot say which.
     *
     * [candidates] holds exactly two, the assumed day first. Only the user can settle it, and until
     * they do the records would be off by a whole day — which is worse than asking.
     */
    data class Ambiguous(val candidates: List<LocalDate>) : StartDayResolution
}
