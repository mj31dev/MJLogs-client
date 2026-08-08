package dev.mj31.logger.client.app.features.logplayer.dependencies

import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileUseCase
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.usecase.session.FilterLogEntriesUseCase
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.ClearSynchronizationUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapLogTimeToVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.SynchronizeTimelinesUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.ParseFrameTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.SynchronizeAtTimestampUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.ComposeFrameTimeUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.AutoSynchronizeUseCase
import dev.mj31.logger.client.app.usecase.playback.StepVideoPositionUseCase

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
    val autoSynchronize: AutoSynchronizeUseCase,
    val stepVideoPosition: StepVideoPositionUseCase,
    val clearSynchronization: ClearSynchronizationUseCase,
    val mapVideoPositionToLogTime: MapVideoPositionToLogTimeUseCase,
    val mapLogTimeToVideoPosition: MapLogTimeToVideoPositionUseCase,
)
