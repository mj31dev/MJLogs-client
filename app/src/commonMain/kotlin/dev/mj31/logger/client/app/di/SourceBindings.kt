package dev.mj31.logger.client.app.di

import dev.mj31.logger.client.app.usecase.ingest.date.ResolveReferenceDateUseCase
import dev.mj31.logger.client.app.usecase.ingest.duplicate.DetectDuplicateUseCase
import dev.mj31.logger.client.app.usecase.ingest.duplicate.MergeSourcePartsUseCase
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.app.usecase.ingest.source.RebuildSourceUseCase
import dev.mj31.logger.client.app.usecase.ingest.zone.ChangeSourceZoneUseCase
import dev.mj31.logger.client.domain.repository.LogSessionRepository
import dev.mj31.logger.client.domain.source.archive.LogFileExpander
import me.tatarka.inject.annotations.Provides

/**
 * Use cases over a source already in the session: reading it again, merging another file into it,
 * telling whether a new file repeats it. Apart from [UseCaseBindings], which brings files in.
 */
interface SourceBindings {

    @Provides
    fun mergeSourceParts(): MergeSourcePartsUseCase = MergeSourcePartsUseCase()

    @Provides
    fun detectDuplicate(): DetectDuplicateUseCase = DetectDuplicateUseCase()

    @Provides
    fun rebuildSource(
        loader: LogSourceLoader,
        expander: LogFileExpander,
        resolveReferenceDate: ResolveReferenceDateUseCase,
        mergeParts: MergeSourcePartsUseCase,
    ): RebuildSourceUseCase = RebuildSourceUseCase(
        loader = loader,
        expander = expander,
        resolveReferenceDate = resolveReferenceDate,
        mergeParts = mergeParts,
    )

    @Provides
    fun changeSourceZone(
        rebuildSource: RebuildSourceUseCase,
        sessionRepository: LogSessionRepository,
        dispatcher: DefaultDispatcher,
    ): ChangeSourceZoneUseCase = ChangeSourceZoneUseCase(
        rebuildSource = rebuildSource,
        sessionRepository = sessionRepository,
        dispatcher = dispatcher,
    )
}
