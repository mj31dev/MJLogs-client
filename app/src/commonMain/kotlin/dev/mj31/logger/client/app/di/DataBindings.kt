package dev.mj31.logger.client.app.di

import dev.mj31.logger.client.data.repository.InMemoryLogSessionRepository
import dev.mj31.logger.client.data.repository.InMemorySyncRepository
import dev.mj31.logger.client.data.repository.InMemoryVideoRepository
import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.detect.LogFormatDetector
import dev.mj31.logger.client.domain.format.preview.LogFormatPreviewer
import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.repository.LogSessionRepository
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.repository.VideoRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import me.tatarka.inject.annotations.Provides
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.data.format.preview.RegexLogFormatPreviewer
import dev.mj31.logger.client.data.format.line.TemplateLogFormatCompiler
import dev.mj31.logger.client.data.format.parse.RegexLogLineParserFactory
import dev.mj31.logger.client.data.format.detect.HeuristicLogFormatDetector

/**
 * Storage and the format engine of `:data`.
 *
 * The bindings live here instead of on the classes themselves, so `:domain` and `:data` never see
 * the injection framework: only this composition root knows it exists.
 */
interface DataBindings {

    @Provides
    fun clock(): Clock = Clock.System

    @Provides
    fun timeZone(): TimeZone = TimeZone.currentSystemDefault()

    @Provides
    fun logLineParserFactory(): LogLineParserFactory = RegexLogLineParserFactory()

    @Provides
    fun logFormatDetector(): LogFormatDetector = HeuristicLogFormatDetector()

    @Provides
    fun logFormatCompiler(): LogFormatCompiler = TemplateLogFormatCompiler()

    @Provides
    fun logFormatPreviewer(): LogFormatPreviewer = RegexLogFormatPreviewer()

    @AppScope
    @Provides
    fun logSessionRepository(): LogSessionRepository = InMemoryLogSessionRepository()

    @AppScope
    @Provides
    fun videoRepository(): VideoRepository = InMemoryVideoRepository()

    @AppScope
    @Provides
    fun syncRepository(): SyncRepository = InMemorySyncRepository()
}
