package dev.mj31.logger.client.domain.repository

import dev.mj31.logger.client.domain.model.legal.LegalNotice

/** Reads the licence texts the distribution carries. */
interface LegalNoticeRepository {

    /**
     * Every notice that shipped, the summary first.
     *
     * Empty when the application runs from a raw class path rather than from an installed build,
     * because then no notice was ever copied next to it.
     */
    suspend fun read(): List<LegalNotice>
}
