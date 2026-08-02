package dev.mj31.logger.client.data.format.timestamp

import dev.mj31.logger.client.domain.format.spec.TimestampPatternTokens
import dev.mj31.logger.client.data.format.line.appendLiteral

/**
 * Turns a textual timestamp pattern into a reusable [CompiledTimestampPattern].
 *
 * Recognized tokens are declared in [TimestampPatternTokens] and are consumed longest first, so that
 * `yyyy` never degrades into two `yy` tokens. Everything else is treated as a literal.
 */
object TimestampPatternCompiler {

    private val definitions: Map<String, TokenDefinition> = mapOf(
        TimestampPatternTokens.EPOCH_MILLIS to TokenDefinition(field = TimestampField.EPOCH_MILLIS, regex = "\\d{13}"),
        TimestampPatternTokens.EPOCH_SECONDS to TokenDefinition(field = TimestampField.EPOCH_SECONDS, regex = "\\d{10}"),
        TimestampPatternTokens.MICRO to TokenDefinition(field = TimestampField.FRACTION, regex = "\\d{6}"),
        TimestampPatternTokens.YEAR_FOUR to TokenDefinition(field = TimestampField.YEAR, regex = "\\d{4}"),
        TimestampPatternTokens.MONTH_NAME to TokenDefinition(field = TimestampField.MONTH_NAME, regex = "[A-Za-z]{3}"),
        TimestampPatternTokens.MILLI to TokenDefinition(field = TimestampField.FRACTION, regex = "\\d{3}"),
        TimestampPatternTokens.OFFSET to TokenDefinition(field = TimestampField.OFFSET, regex = "Z|[+-]\\d{2}:?\\d{2}"),
        TimestampPatternTokens.YEAR_TWO to TokenDefinition(field = TimestampField.YEAR_SHORT, regex = "\\d{2}"),
        TimestampPatternTokens.MONTH to TokenDefinition(field = TimestampField.MONTH, regex = "\\d{2}"),
        TimestampPatternTokens.DAY to TokenDefinition(field = TimestampField.DAY, regex = "\\d{2}"),
        TimestampPatternTokens.HOUR to TokenDefinition(field = TimestampField.HOUR, regex = "\\d{2}"),
        TimestampPatternTokens.MINUTE to TokenDefinition(field = TimestampField.MINUTE, regex = "\\d{2}"),
        TimestampPatternTokens.SECOND to TokenDefinition(field = TimestampField.SECOND, regex = "\\d{2}"),
    )

    /**
     * Compiles [pattern] into an executable fragment.
     *
     * @throws IllegalArgumentException when [pattern] is blank, repeats a token or contains no known token.
     */
    fun compile(pattern: String): CompiledTimestampPattern {
        require(pattern.isNotBlank()) { "Timestamp pattern must not be blank." }
        val builder = StringBuilder()
        val fields = linkedSetOf<TimestampField>()
        val usedGroups = mutableSetOf<String>()
        var index = 0
        while (index < pattern.length) {
            val token = TimestampPatternTokens.ordered.firstOrNull { pattern.startsWith(prefix = it, startIndex = index) }
            if (token == null) {
                index = appendLiteral(source = pattern, index = index, target = builder)
                continue
            }
            val definition = definitions.getValue(key = token)
            require(usedGroups.add(definition.field.groupName)) {
                "Timestamp pattern '$pattern' uses the token '$token' more than once."
            }
            builder.append(group(definition = definition))
            fields += definition.field
            index += token.length
        }
        require(fields.isNotEmpty()) { unusableMessage(pattern = pattern) }
        val source = builder.toString()
        require(runCatching { Regex(pattern = source) }.isSuccess) {
            "Timestamp pattern '$pattern' produced an invalid regular expression."
        }
        return CompiledTimestampPattern(pattern = pattern, regexSource = source, fields = fields)
    }

    private fun group(definition: TokenDefinition): String = "(?<${definition.field.groupName}>${definition.regex})"

    private fun unusableMessage(pattern: String): String {
        val known = TimestampPatternTokens.ordered.joinToString(separator = ", ")
        return "Timestamp pattern '$pattern' contains no known token; supported tokens are: $known."
    }

    private data class TokenDefinition(val field: TimestampField, val regex: String)
}
