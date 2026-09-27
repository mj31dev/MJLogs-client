package dev.mj31.logger.client.app.usecase.sync

import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.domain.model.time.ZoneOrigin

/**
 * Decides which zone a time read off the screen is in.
 *
 * A screencast shows the clock of the device it was recorded on, and that device is the one whose
 * log the recording accompanies — so the screen is read in the zone of the logs. When the session
 * holds files from several zones, the record the user selected says which device they are looking
 * at; with nothing selected, the first file stands for the session. The user can still say
 * otherwise, and when they do, nothing overrides them.
 */
class ResolveSyncZoneUseCase {

    operator fun invoke(sources: List<LogSource>, selectedSourceId: String?, chosenZoneId: String?): SourceZone {
        chosenZoneId?.let { chosen -> return SourceZone(id = chosen, origin = ZoneOrigin.CHOSEN) }
        return sources.firstOrNull { it.id == selectedSourceId }?.zone
            ?: sources.firstOrNull()?.zone
            ?: SourceZone.UTC
    }
}
