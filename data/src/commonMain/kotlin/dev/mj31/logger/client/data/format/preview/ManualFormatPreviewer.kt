package dev.mj31.logger.client.data.format.preview

import dev.mj31.logger.client.data.format.line.ManualFormatCompiler
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.format.preview.LogFormatPreviewer
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

/**
 * Previews whichever shape the user is describing.
 *
 * Compiling happens once, here, so that a description that cannot be compiled is reported as the
 * failure it is — naming the field at fault — instead of each previewer having to discover it again.
 */
class ManualFormatPreviewer(
    private val compiler: LogFormatCompiler = ManualFormatCompiler(),
    private val regex: RegexLogFormatPreviewer = RegexLogFormatPreviewer(compiler = compiler),
    private val structured: StructuredLogFormatPreviewer = StructuredLogFormatPreviewer(),
) : LogFormatPreviewer {

    override fun preview(input: ManualFormatInput, sampleLines: List<String>): FormatPreview {
        if (sampleLines.isEmpty()) return FormatPreview.Empty
        val spec = when (val compiled = compiler.compile(input = input)) {
            is FormatCompilationResult.Failure -> return FormatPreview.Invalid(
                message = compiled.message,
                field = compiled.field,
            )

            is FormatCompilationResult.Success -> compiled.spec
        }
        return when (spec) {
            is LogFormatSpec.Regex -> regex.preview(input = input, sampleLines = sampleLines)
            is LogFormatSpec.Json, is LogFormatSpec.Delimited ->
                structured.preview(spec = spec, sampleLines = sampleLines)
        }
    }
}
