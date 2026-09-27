package dev.mj31.logger.client.data.format.line

import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.model.time.SourceZone

import dev.mj31.logger.client.data.format.structure.StructuredFormatCompiler
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput

/**
 * Compiles whichever shape the user chose to describe.
 *
 * The three descriptions have nothing in common beyond the timestamp, so each has its own compiler
 * and this only picks between them. It exists so that the dialog can switch shapes without the rest
 * of the application learning that there is more than one.
 *
 * The zone is the one input all three share, so it is checked here rather than three times: a
 * blank one leaves the choice to the file, anything else has to name a zone that exists.
 */
class ManualFormatCompiler(
    private val template: TemplateLogFormatCompiler = TemplateLogFormatCompiler(),
) : LogFormatCompiler {

    override fun compile(input: ManualFormatInput): FormatCompilationResult {
        val zoneId = if (input.zoneId.isBlank()) {
            null
        } else {
            SourceZone.idOf(text = input.zoneId) ?: return FormatCompilationResult.Failure(
                message = "'${input.zoneId.trim()}' is not a time zone. Use a name such as Europe/Berlin " +
                    "or an offset such as UTC+03:00, or leave it empty.",
                field = FormatErrorField.ZONE,
            )
        }
        val compiled = when (input) {
            is ManualFormatInput.Template -> template.compile(input = input)
            is ManualFormatInput.Json -> StructuredFormatCompiler.compile(input = input)
            is ManualFormatInput.Delimited -> StructuredFormatCompiler.compile(input = input)
        }
        return when (compiled) {
            is FormatCompilationResult.Success -> compiled.copy(spec = compiled.spec.withZoneId(zoneId = zoneId))
            is FormatCompilationResult.Failure -> compiled
        }
    }
}
