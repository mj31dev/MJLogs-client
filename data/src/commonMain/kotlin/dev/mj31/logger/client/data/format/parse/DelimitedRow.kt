package dev.mj31.logger.client.data.format.parse

/**
 * Splits one physical line of a delimited file into its fields.
 *
 * Quoting follows the usual convention: a field may be wrapped in double quotes, inside which the
 * delimiter loses its meaning and a doubled quote stands for a literal one.
 *
 * A field whose quote is never closed means the record continues on the next physical line. This
 * pipeline reads a file line by line, so such a row cannot be assembled here and is reported as
 * malformed; the caller then treats the line as a continuation of the previous record, which keeps
 * the text visible rather than dropping it.
 */
internal object DelimitedRow {

    private const val QUOTE = '"'

    /**
     * Where each field sits in [line], so that a preview can point at it.
     *
     * The range covers the field as written, quotes included: it is used to highlight the original
     * text, not the value read out of it.
     */
    fun ranges(line: String, delimiter: Char): List<IntRange>? {
        if (split(line = line, delimiter = delimiter) == null) return null
        val ranges = mutableListOf<IntRange>()
        var start = 0
        var quoted = false
        line.forEachIndexed { index, character ->
            when {
                character == QUOTE -> quoted = !quoted
                !quoted && character == delimiter -> {
                    ranges += start until index
                    start = index + 1
                }
            }
        }
        ranges += start until line.length
        return ranges
    }

    /** Returns the fields of [line], or `null` when a quoted field is left open. */
    fun split(line: String, delimiter: Char): List<String>? {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0

        while (index < line.length) {
            val character = line[index]
            when {
                quoted && character == QUOTE && line.getOrNull(index = index + 1) == QUOTE -> {
                    field.append(QUOTE)
                    index++
                }

                character == QUOTE -> quoted = !quoted

                !quoted && character == delimiter -> {
                    fields += field.toString()
                    field.clear()
                }

                else -> field.append(character)
            }
            index++
        }

        if (quoted) return null
        fields += field.toString()
        return fields
    }
}
