package dev.mj31.logger.client.app.di

import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileUseCase
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceAssembler
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.app.usecase.legal.ReadLegalNoticesUseCase
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.domain.format.detect.LogFormatDetector
import dev.mj31.logger.client.domain.repository.LegalNoticeRepository
import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.source.IdGenerator
import dev.mj31.logger.client.domain.source.TextFileDataSource
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import me.tatarka.inject.annotations.Provides

/**
 * Use cases: application logic built on the `:domain` ports, stateless, so only the collaborators
 * they need are shared.
 */
interface UseCaseBindings {

    @Provides
    fun mergeLogSources(): MergeLogSourcesUseCase = MergeLogSourcesUseCase()

    @Provides
    fun logSourceAssembler(parserFactory: LogLineParserFactory): LogSourceAssembler =
        LogSourceAssembler(parserFactory = parserFactory)

    @Provides
    fun logSourceLoader(
        dataSource: TextFileDataSource,
        assembler: LogSourceAssembler,
        idGenerator: IdGenerator,
        clock: Clock,
        timeZone: TimeZone,
    ): LogSourceLoader = LogSourceLoader(
        dataSource = dataSource,
        assembler = assembler,
        idGenerator = idGenerator,
        clock = clock,
        timeZone = timeZone,
    )

    @Provides
    fun importLogFile(
        loader: LogSourceLoader,
        detector: LogFormatDetector,
        dispatcher: DefaultDispatcher,
    ): ImportLogFileUseCase = ImportLogFileUseCase(
        loader = loader,
        detector = detector,
        dispatcher = dispatcher,
    )

    @Provides
    fun readLegalNotices(repository: LegalNoticeRepository): ReadLegalNoticesUseCase =
        ReadLegalNoticesUseCase(repository = repository)

    @Provides
    fun importLogFileWithFormat(
        loader: LogSourceLoader,
        dispatcher: DefaultDispatcher,
    ): ImportLogFileWithFormatUseCase = ImportLogFileWithFormatUseCase(loader = loader, dispatcher = dispatcher)
}
