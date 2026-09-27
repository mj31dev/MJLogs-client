package dev.mj31.logger.client.data.format.preview

import dev.mj31.logger.client.data.format.parse.DelimitedRow
import dev.mj31.logger.client.data.format.parse.DispatchingLogLineParserFactory
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.format.preview.HighlightedSpan
import dev.mj31.logger.client.domain.format.preview.PreviewLine
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import dev.mj31.logger.client.domain.format.spec.field.RecordFieldMap
import kotlinx.datetime.LocalDate

/**
 * Shows what a structured description reads out of the sample lines.
 *
 * Whether a line parses is answered by the parser the import would use, so the preview cannot claim
 * a line reads when it does not. Where each component *sits* is worked out separately, because a
 * parser reports values and a preview has to point at the original text.
 *
 * A delimited row is located exactly. A JSON key is located by searching the raw line for it, which
 * is a best effort: a nested path, or a key that also occurs inside a string value, simply yields no
 * highlight. Nothing depends on the highlight being complete — it is a reading aid over text the
 * user can already see.
 */
class StructuredLogFormatPreviewer(
    private val parserFactory: DispatchingLogLineParserFactory = DispatchingLogLineParserFactory(),
) {

    fun preview(spec: LogFormatSpec, sampleLines: List<String>): FormatPreview {
        val parser = runCatching { parserFactory.create(spec = spec, referenceDate = REFERENCE_DATE) }
            .getOrElse { return FormatPreview.Empty }
        val fields = fieldsOf(spec = spec) ?: return FormatPreview.Empty

        return FormatPreview.Ready(
            lines = sampleLines.map { line ->
                when (val parsed = parser.parse(line = line)) {
                    is ParsedLine.Continuation, is ParsedLine.ColumnHeader -> PreviewLine(text = line)
                    is ParsedLine.Record -> PreviewLine(
                        text = line,
                        spans = spansOf(spec = spec, fields = fields, line = line),
                        isRecord = true,
                        level = parsed.level,
                    )
                }
            },
        )
    }

    private fun fieldsOf(spec: LogFormatSpec): RecordFieldMap? = when (spec) {
        is LogFormatSpec.Json -> spec.fields
        is LogFormatSpec.Delimited -> spec.fields
        is LogFormatSpec.Regex -> null
    }

    private fun spansOf(spec: LogFormatSpec, fields: RecordFieldMap, line: String): List<HighlightedSpan> =
        LogComponent.entries
            .mapNotNull { component ->
                fields[component]?.let { locator -> spanOf(spec = spec, component = component, locator = locator, line = line) }
            }
            .sortedBy { it.startIndex }

    private fun spanOf(
        spec: LogFormatSpec,
        component: LogComponent,
        locator: ComponentLocator,
        line: String,
    ): HighlightedSpan? {
        val range = when (spec) {
            is LogFormatSpec.Delimited -> columnRange(spec = spec, locator = locator, line = line)
            is LogFormatSpec.Json -> keyRange(locator = locator, line = line)
            is LogFormatSpec.Regex -> null
        } ?: return null
        if (range.isEmpty()) return null
        return HighlightedSpan(component = component, startIndex = range.first, endIndex = range.last + 1)
    }

    private fun columnRange(spec: LogFormatSpec.Delimited, locator: ComponentLocator, line: String): IntRange? {
        val ranges = DelimitedRow.ranges(line = line, delimiter = spec.delimiter) ?: return null
        val position = when (locator) {
            is ComponentLocator.Index -> locator.position
            // A header name is only resolvable against the header row, which a single previewed line
            // is not; the position is left unknown rather than guessed.
            is ComponentLocator.Key -> return null
        }
        return ranges.getOrNull(index = position)
    }

    private fun keyRange(locator: ComponentLocator, line: String): IntRange? {
        val name = (locator as? ComponentLocator.Key)?.name ?: return null
        if (name.contains(char = '.')) return null
        val keyIndex = line.indexOf(string = "\"$name\"")
        if (keyIndex < 0) return null
        val colon = line.indexOf(char = ':', startIndex = keyIndex + name.length + 2)
        if (colon < 0) return null
        return valueRange(line = line, from = colon + 1)
    }

    /** The value token following a colon: a quoted string, or everything up to the next separator. */
    private fun valueRange(line: String, from: Int): IntRange? {
        val start = (from until line.length).firstOrNull { !line[it].isWhitespace() } ?: return null
        if (line[start] == '"') {
            val end = (start + 1 until line.length).firstOrNull { line[it] == '"' && line[it - 1] != '\\' }
                ?: return null
            return (start + 1) until end
        }
        val end = (start until line.length).firstOrNull { line[it] == ',' || line[it] == '}' } ?: line.length
        return start until end
    }

    private companion object {

        /** Previewing only asks whether a line reads, never when it happened. */
        val REFERENCE_DATE: LocalDate = LocalDate(year = 1970, monthNumber = 1, dayOfMonth = 1)
    }
}
