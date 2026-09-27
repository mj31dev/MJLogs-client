package dev.mj31.logger.client.data.format.structure

import dev.mj31.logger.client.data.format.parse.DelimitedRow
import dev.mj31.logger.client.data.format.timestamp.TimestampShapeInference
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import dev.mj31.logger.client.domain.format.spec.field.RecordFieldMap
import dev.mj31.logger.client.domain.model.log.LogLevel

/**
 * Decides whether a sample is a delimited table, which delimiter it uses and where its components
 * sit.
 *
 * The delimiter is not asked for because it is inferable: only one of the candidates splits every
 * line into the same number of columns. That same test is what keeps a plain text log out of this
 * branch — prose does not have a constant number of commas.
 */
internal object DelimitedFormatProbe {

    /** Fewer than this many columns is far more likely to be prose containing a comma than a table. */
    private const val MIN_COLUMNS = 3

    private val candidateDelimiters = listOf(',', '\t', ';')

    fun probe(probes: List<String>): LogFormatSpec.Delimited? =
        candidateDelimiters.firstNotNullOfOrNull { delimiter -> probeWith(probes = probes, delimiter = delimiter) }

    private fun probeWith(probes: List<String>, delimiter: Char): LogFormatSpec.Delimited? {
        val table = probes.map { line -> DelimitedRow.split(line = line, delimiter = delimiter) ?: return null }
        if (table.isEmpty()) return null
        val width = table.first().size
        if (width < MIN_COLUMNS || table.any { it.size != width }) return null

        val header = table.first().map { it.trim() }.takeIf { headerLooksLikeNames(cells = it) }
        val rows = if (header == null) table else table.drop(n = 1)
        if (rows.isEmpty()) return null

        val (timestampIndex, timestampPattern) = timestampOf(rows = rows, width = width, header = header)
            ?: return null

        return LogFormatSpec.Delimited(
            name = nameOf(delimiter = delimiter),
            delimiter = delimiter,
            hasHeader = header != null,
            fields = RecordFieldMap.of(
                timestamp = locatorOf(index = timestampIndex, header = header),
                level = levelLocator(rows = rows, width = width, header = header, timestampIndex = timestampIndex),
                tag = namedLocator(component = LogComponent.TAG, header = header),
                message = messageLocator(width = width, header = header, timestampIndex = timestampIndex),
            ),
            timestampPattern = timestampPattern,
            origin = FormatOrigin.DETECTED,
        )
    }

    /** A header row names its columns, so none of its cells parses as a time and one is a known name. */
    private fun headerLooksLikeNames(cells: List<String>): Boolean {
        if (cells.any { TimestampShapeInference.findRegion(line = it) != null }) return false
        return LogComponent.entries.any { component ->
            FieldSynonyms.match(component = component, names = cells) != null
        }
    }

    private fun timestampOf(rows: List<List<String>>, width: Int, header: List<String>?): Pair<Int, String>? {
        val named = header?.let { names ->
            FieldSynonyms.match(component = LogComponent.TIMESTAMP, names = names)
                ?.let { name -> names.indexOfFirst { it.equals(other = name, ignoreCase = true) } }
        }
        val ordered = listOfNotNull(named) + (0 until width).filterNot { it == named }
        return ordered.firstNotNullOfOrNull { index ->
            patternOf(rows = rows, index = index)?.let { pattern -> index to pattern }
        }
    }

    private fun patternOf(rows: List<List<String>>, index: Int): String? {
        val regions = rows.mapNotNull { row ->
            row.getOrNull(index = index)?.trim()?.let { TimestampShapeInference.findRegion(line = it) }
        }
        if (regions.size != rows.size) return null
        return TimestampShapeInference.infer(regions = regions)
    }

    /** Without a header the level is found by content: a column whose every value is a known token. */
    private fun levelLocator(
        rows: List<List<String>>,
        width: Int,
        header: List<String>?,
        timestampIndex: Int,
    ): ComponentLocator? {
        namedLocator(component = LogComponent.LEVEL, header = header)?.let { return it }
        if (header != null) return null
        val index = (0 until width).firstOrNull { candidate ->
            candidate != timestampIndex && rows.all { row ->
                row.getOrNull(index = candidate)?.trim()?.let { LogLevel.fromToken(token = it) } != null
            }
        }
        return index?.let { ComponentLocator.Index(position = it) }
    }

    /** Without a header the message is taken to be the last column, which is where loggers put it. */
    private fun messageLocator(width: Int, header: List<String>?, timestampIndex: Int): ComponentLocator? {
        namedLocator(component = LogComponent.MESSAGE, header = header)?.let { return it }
        if (header != null) return null
        val last = width - 1
        return if (last == timestampIndex) null else ComponentLocator.Index(position = last)
    }

    private fun namedLocator(component: LogComponent, header: List<String>?): ComponentLocator? {
        val names = header ?: return null
        return FieldSynonyms.match(component = component, names = names)?.let { ComponentLocator.Key(name = it) }
    }

    private fun locatorOf(index: Int, header: List<String>?): ComponentLocator =
        header?.getOrNull(index = index)?.let { ComponentLocator.Key(name = it) }
            ?: ComponentLocator.Index(position = index)

    private fun nameOf(delimiter: Char): String = when (delimiter) {
        '\t' -> "Tab separated"
        ';' -> "Semicolon separated"
        else -> "Comma separated"
    }
}
