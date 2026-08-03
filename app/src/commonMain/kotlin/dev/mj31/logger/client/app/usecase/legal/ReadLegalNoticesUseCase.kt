package dev.mj31.logger.client.app.usecase.legal

import dev.mj31.logger.client.domain.model.legal.LegalNotice
import dev.mj31.logger.client.domain.repository.LegalNoticeRepository

/** Hands the licence texts that shipped with this build to the window that displays them. */
class ReadLegalNoticesUseCase(
    private val repository: LegalNoticeRepository,
) {

    suspend operator fun invoke(): List<LegalNotice> = repository.read()
}
