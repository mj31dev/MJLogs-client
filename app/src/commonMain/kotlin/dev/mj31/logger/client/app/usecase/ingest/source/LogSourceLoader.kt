package dev.mj31.logger.client.app.usecase.ingest.source

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.source.IdGenerator
import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.source.TextFileDataSource
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import dev.mj31.logger.client.domain.model.log.LogSource

/**
 * Reads a log file and materializes it as a [LogSource].
 *
 * Shared by the automatic and the manual import flows so both produce identical sources.
 */
class LogSourceLoader(
    private val dataSource: TextFileDataSource,
    private val assembler: LogSourceAssembler,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    suspend fun read(path: String): TextFileContent = dataSource.read(path = path)

    fun buildSource(content: TextFileContent, spec: LogFormatSpec, sourceId: String? = null): LogSource =
        assembler.assemble(
            descriptor = LogSourceDescriptor(
                id = sourceId ?: idGenerator.next(prefix = "src"),
                name = content.name,
                path = content.path,
            ),
            spec = spec,
            lines = content.lines,
            referenceDate = clock.todayIn(timeZone = timeZone),
        )
}
