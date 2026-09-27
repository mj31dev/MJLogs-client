package dev.mj31.logger.client.data.format.structure

import dev.mj31.logger.client.data.format.timestamp.TimestampPatternCompiler
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import dev.mj31.logger.client.domain.format.spec.field.RecordFieldMap

/**
 * Turns a hand written description of a JSON or delimited log into a specification.
 *
 * Every rejection names the input that caused it, because the dialog marks that field: a message
 * that only said "invalid format" would leave the user to find which of six boxes is wrong.
 */
internal object StructuredFormatCompiler {

    /** Written by the user in front of a column position, as in `#2` for the third column. */
    private const val INDEX_PREFIX = "#"

    private const val TAB_ESCAPE = "\\t"

    fun compile(input: ManualFormatInput): FormatCompilationResult = when (input) {
        is ManualFormatInput.Json -> compileJson(input = input)
        is ManualFormatInput.Delimited -> compileDelimited(input = input)
        is ManualFormatInput.Template -> failure(
            message = "This description is not a structured format.",
            field = FormatErrorField.NONE,
        )
    }

    private fun compileJson(input: ManualFormatInput.Json): FormatCompilationResult {
        timestampError(pattern = input.timestampPattern)?.let { return it }
        if (input.timestampKey.isBlank()) return missingTimestampField(what = "key")

        return FormatCompilationResult.Success(
            spec = LogFormatSpec.Json(
                name = CUSTOM_JSON_NAME,
                fields = RecordFieldMap.of(
                    timestamp = ComponentLocator.Key(name = input.timestampKey.trim()),
                    level = keyOf(text = input.levelKey),
                    tag = keyOf(text = input.tagKey),
                    message = keyOf(text = input.messageKey),
                ),
                timestampPattern = input.timestampPattern,
                origin = FormatOrigin.USER_DEFINED,
            ),
        )
    }

    private fun compileDelimited(input: ManualFormatInput.Delimited): FormatCompilationResult {
        val delimiter = delimiterOf(text = input.delimiter)
        val timestamp = locatorOf(text = input.timestampField, hasHeader = input.hasHeader)
        rejection(input = input, delimiter = delimiter, timestamp = timestamp)?.let { return it }

        return FormatCompilationResult.Success(
            spec = LogFormatSpec.Delimited(
                name = CUSTOM_DELIMITED_NAME,
                delimiter = requireNotNull(delimiter),
                hasHeader = input.hasHeader,
                fields = RecordFieldMap.of(
                    timestamp = requireNotNull(timestamp),
                    level = locatorOf(text = input.levelField, hasHeader = input.hasHeader),
                    tag = locatorOf(text = input.tagField, hasHeader = input.hasHeader),
                    message = locatorOf(text = input.messageField, hasHeader = input.hasHeader),
                ),
                timestampPattern = input.timestampPattern,
                origin = FormatOrigin.USER_DEFINED,
            ),
        )
    }

    /**
     * The first thing wrong with a delimited description, or null when there is nothing.
     *
     * Every branch names the input at fault, because the dialog marks that field; a message saying
     * only "invalid format" would leave the user to work out which of six boxes it means.
     */
    private fun rejection(
        input: ManualFormatInput.Delimited,
        delimiter: Char?,
        timestamp: ComponentLocator?,
    ): FormatCompilationResult.Failure? = when {
        delimiter == null -> failure(
            message = "The separator must be a single character, or $TAB_ESCAPE for a tab.",
            field = FormatErrorField.DELIMITER,
        )

        input.timestampField.isBlank() -> missingTimestampField(what = "column")

        timestamp == null -> failure(
            message = "Without a header row a column has to be given by position, as in ${INDEX_PREFIX}0.",
            field = FormatErrorField.TIMESTAMP_FIELD,
        )

        else -> timestampError(pattern = input.timestampPattern)
    }

    private fun timestampError(pattern: String): FormatCompilationResult.Failure? =
        runCatching { TimestampPatternCompiler.compile(pattern = pattern) }
            .exceptionOrNull()
            ?.let { error ->
                failure(
                    message = error.message ?: "The timestamp pattern cannot be read.",
                    field = FormatErrorField.TIMESTAMP_PATTERN,
                )
            }

    private fun missingTimestampField(what: String): FormatCompilationResult.Failure = failure(
        message = "Name the $what holding the timestamp; the other components are optional.",
        field = FormatErrorField.TIMESTAMP_FIELD,
    )

    private fun keyOf(text: String): ComponentLocator? =
        text.trim().takeIf { it.isNotEmpty() }?.let { ComponentLocator.Key(name = it) }

    /** A position is always accepted; a name only means something when there is a header to match it. */
    private fun locatorOf(text: String, hasHeader: Boolean): ComponentLocator? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith(prefix = INDEX_PREFIX)) {
            return trimmed.removePrefix(prefix = INDEX_PREFIX).toIntOrNull()
                ?.takeIf { it >= 0 }
                ?.let { ComponentLocator.Index(position = it) }
        }
        return if (hasHeader) ComponentLocator.Key(name = trimmed) else null
    }

    private fun delimiterOf(text: String): Char? = when {
        text == TAB_ESCAPE -> '\t'
        text.length == 1 -> text.first()
        else -> null
    }

    private fun failure(message: String, field: FormatErrorField): FormatCompilationResult.Failure =
        FormatCompilationResult.Failure(message = message, field = field)

    private const val CUSTOM_JSON_NAME = "Custom JSON"
    private const val CUSTOM_DELIMITED_NAME = "Custom separated"
}
