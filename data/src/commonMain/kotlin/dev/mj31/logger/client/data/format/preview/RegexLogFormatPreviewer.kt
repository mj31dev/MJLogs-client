package dev.mj31.logger.client.data.format.preview

import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.format.preview.HighlightedSpan
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.spec.LogFormatGroups
import dev.mj31.logger.client.domain.format.preview.LogFormatPreviewer
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.preview.PreviewLine
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.data.format.line.TemplateLogFormatCompiler
import dev.mj31.logger.client.data.format.line.CompiledLineFormat

/**
 * Runs the format being edited over the sample lines and reports which fragment feeds which
 * component, using the very same regex the import would use.
 */
class RegexLogFormatPreviewer(
    private val compiler: LogFormatCompiler = TemplateLogFormatCompiler(),
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
        val format = runCatching { CompiledLineFormat.compile(spec = spec) }
            .getOrElse { error ->
                return FormatPreview.Invalid(
                    message = error.message ?: DEFAULT_ERROR,
                    field = FormatErrorField.STRUCTURE_TEMPLATE,
                )
            }

        return FormatPreview.Ready(lines = sampleLines.map { line -> previewOf(format = format, line = line) })
    }

    private fun previewOf(format: CompiledLineFormat, line: String): PreviewLine {
        val match = format.lineRegex.find(input = line) ?: return PreviewLine(text = line)
        val spans = componentsOf(format = format).mapNotNull { (component, group) ->
            match.groups[group]?.takeIf { it.range.first <= it.range.last }?.let { matched ->
                HighlightedSpan(
                    component = component,
                    startIndex = matched.range.first,
                    endIndex = matched.range.last + 1,
                )
            }
        }.sortedBy { it.startIndex }

        return PreviewLine(
            text = line,
            spans = spans,
            isRecord = true,
            level = levelOf(format = format, match = match),
        )
    }

    /**
     * Only the groups the pattern actually declares may be queried: asking a regex for an undefined
     * group name is an error, not an empty result.
     */
    private fun componentsOf(format: CompiledLineFormat): List<Pair<LogComponent, String>> = buildList {
        add(element = LogComponent.TIMESTAMP to LogFormatGroups.TIMESTAMP)
        if (format.hasLevelGroup) add(element = LogComponent.LEVEL to LogFormatGroups.LEVEL)
        if (format.hasTagGroup) add(element = LogComponent.TAG to LogFormatGroups.TAG)
        if (format.hasMessageGroup) add(element = LogComponent.MESSAGE to LogFormatGroups.MESSAGE)
    }

    private fun levelOf(format: CompiledLineFormat, match: MatchResult): LogLevel? {
        if (!format.hasLevelGroup) return null
        return match.groups[LogFormatGroups.LEVEL]?.value?.let { LogLevel.fromToken(token = it) }
    }

    private companion object {
        const val DEFAULT_ERROR = "The format cannot be compiled."
    }
}
