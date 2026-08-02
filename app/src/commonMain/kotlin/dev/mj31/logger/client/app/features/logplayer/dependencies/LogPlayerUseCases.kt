package dev.mj31.logger.client.app.features.logplayer.dependencies

import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileUseCase
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.usecase.session.FilterLogEntriesUseCase
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.app.usecase.sync.ClearSynchronizationUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapLogTimeToVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.SynchronizeTimelinesUseCase
import dev.mj31.logger.client.app.usecase.sync.ParseFrameTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.SynchronizeAtTimestampUseCase
import dev.mj31.logger.client.app.usecase.sync.ComposeFrameTimeUseCase

/** Use cases the screen depends on, bundled to keep the constructor readable. */
data class LogPlayerUseCases(
    val mergeLogSources: MergeLogSourcesUseCase,
    val importLogFile: ImportLogFileUseCase,
    val importLogFileWithFormat: ImportLogFileWithFormatUseCase,
    val filterLogEntries: FilterLogEntriesUseCase,
    val synchronizeTimelines: SynchronizeTimelinesUseCase,
    val synchronizeAtTimestamp: SynchronizeAtTimestampUseCase,
    val parseFrameTime: ParseFrameTimeUseCase,
    val composeFrameTime: ComposeFrameTimeUseCase,
    val clearSynchronization: ClearSynchronizationUseCase,
    val mapVideoPositionToLogTime: MapVideoPositionToLogTimeUseCase,
    val mapLogTimeToVideoPosition: MapLogTimeToVideoPositionUseCase,
)
