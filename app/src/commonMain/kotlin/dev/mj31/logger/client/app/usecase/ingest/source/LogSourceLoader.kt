package dev.mj31.logger.client.app.usecase.ingest.source

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.source.IdGenerator
import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.source.TextFileDataSource
import dev.mj31.logger.client.domain.model.time.SourceZone
import kotlinx.datetime.LocalDate

/**
 * Reads a log file and materializes it as a [LogSource].
 *
 * Shared by the automatic and the manual import flows so both produce identical sources.
 *
 * The reference date is handed in rather than read off a clock here. This used to be "today", which
 * meant the same file produced different timestamps depending on when it was opened; deciding it is
 * a judgement about the file, and judgements belong to the use case that owns the import.
 */
class LogSourceLoader(
    private val dataSource: TextFileDataSource,
    private val assembler: LogSourceAssembler,
    private val idGenerator: IdGenerator,
) {

    suspend fun read(path: String): TextFileContent = dataSource.read(path = path)

    fun buildSource(
        content: TextFileContent,
        spec: LogFormatSpec,
        referenceDate: LocalDate,
        sourceId: String? = null,
        displayName: String? = null,
        zone: SourceZone = SourceZone.UTC,
    ): LogSource = assembler.assemble(
        descriptor = LogSourceDescriptor(
            id = sourceId ?: idGenerator.next(prefix = "src"),
            name = displayName ?: content.name,
            path = content.path,
        ),
        spec = spec,
        lines = content.lines,
        referenceDate = referenceDate,
        zone = zone,
    )
}
