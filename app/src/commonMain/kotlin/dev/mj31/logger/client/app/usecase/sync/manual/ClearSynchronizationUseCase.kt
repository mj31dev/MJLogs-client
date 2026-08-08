package dev.mj31.logger.client.app.usecase.sync.manual

import dev.mj31.logger.client.domain.repository.SyncRepository

/** Detaches the two timelines again. */
class ClearSynchronizationUseCase(
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke() {
        syncRepository.clearAnchor()
    }
}
