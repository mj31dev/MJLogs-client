package dev.mj31.logger.client.data.format.line

import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.spec.LogFormatGroups
import dev.mj31.logger.client.domain.format.spec.LogFormatPlaceholders
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.data.format.timestamp.TimestampPatternCompiler

/**
 * Compiles a user supplied structure template such as `{timestamp} {level} {tag}: {message}` into an
 * executable [LogFormatSpec].
 *
 * Literal characters of the template are matched verbatim, a run of whitespace matches any whitespace
 * and each placeholder becomes the corresponding named group of [LogFormatGroups].
 *
 * [LogFormatPlaceholders.ANY] is the exception: it captures nothing, only consuming a varying
 * fragment such as a counter, and may therefore appear several times.
 */
class TemplateLogFormatCompiler : LogFormatCompiler {

    override fun compile(input: ManualFormatInput): FormatCompilationResult {
        val timestamp = runCatching { TimestampPatternCompiler.compile(pattern = input.timestampPattern) }
            .getOrElse { error ->
                return failure(
                    message = error.message ?: DEFAULT_TIMESTAMP_ERROR,
                    field = FormatErrorField.TIMESTAMP_PATTERN,
                )
            }
        return compileStructure(input = input, timestampRegex = timestamp.regexSource)
    }

    private fun compileStructure(input: ManualFormatInput, timestampRegex: String): FormatCompilationResult {
        val body = when (val translation = translate(template = input.structureTemplate)) {
            is Translation.Failure -> return failure(
                message = translation.message,
                field = FormatErrorField.STRUCTURE_TEMPLATE,
            )
            is Translation.Success -> translation
        }
        if (LogFormatPlaceholders.TIMESTAMP !in body.placeholders) {
            return failure(
                message = "The structure template must contain the ${LogFormatPlaceholders.TIMESTAMP} placeholder.",
                field = FormatErrorField.STRUCTURE_TEMPLATE,
            )
        }
        val linePattern = LineFormatCompiler.buildLinePattern(
            template = anchored(body = body.pattern),
            timestampRegex = timestampRegex,
        )
        return specOf(input = input, linePattern = linePattern)
    }

    private fun specOf(input: ManualFormatInput, linePattern: String): FormatCompilationResult {
        if (runCatching { Regex(pattern = linePattern, option = RegexOption.IGNORE_CASE) }.isFailure) {
            return failure(
                message = "The structure template '${input.structureTemplate}' does not describe a usable line layout.",
                field = FormatErrorField.STRUCTURE_TEMPLATE,
            )
        }
        return FormatCompilationResult.Success(
            spec = LogFormatSpec(
                name = CUSTOM_FORMAT_NAME,
                linePattern = linePattern,
                timestampPattern = input.timestampPattern,
                utcOffsetMinutes = input.utcOffsetMinutes,
                origin = FormatOrigin.USER_DEFINED,
            ),
        )
    }

    private fun translate(template: String): Translation {
        val builder = StringBuilder()
        val placeholders = mutableSetOf<String>()
        var index = 0
        while (index < template.length) {
            when (val token = tokenAt(template = template, index = index)) {
                is Token.Brace -> {
                    builder.append(escapeRegexLiteral(char = token.char))
                    index += ESCAPED_BRACE_LENGTH
                }

                is Token.Placeholder -> {
                    if (!placeholders.add(token.name) && token.name in LogFormatPlaceholders.capturing) {
                        return Translation.Failure(message = "The placeholder ${token.name} is used more than once.")
                    }
                    builder.append(fragmentOf(placeholder = token.name))
                    index += token.name.length
                }

                is Token.Unknown -> return Translation.Failure(message = unknownMessage(unknown = token.text))

                Token.Literal -> index = appendLiteral(source = template, index = index, target = builder)
            }
        }
        return Translation.Success(pattern = builder.toString(), placeholders = placeholders)
    }

    private fun tokenAt(template: String, index: Int): Token {
        escapedBraceAt(template = template, index = index)?.let { brace -> return Token.Brace(char = brace) }
        LogFormatPlaceholders.all
            .firstOrNull { template.startsWith(prefix = it, startIndex = index) }
            ?.let { placeholder -> return Token.Placeholder(name = placeholder) }
        return unknownPlaceholderAt(template = template, index = index)
            ?.let { unknown -> Token.Unknown(text = unknown) }
            ?: Token.Literal
    }

    /** `{{` and `}}` stand for a literal brace, so that layouts embedding JSON can be described. */
    private fun escapedBraceAt(template: String, index: Int): Char? = when {
        template.startsWith(prefix = "{{", startIndex = index) -> '{'
        template.startsWith(prefix = "}}", startIndex = index) -> '}'
        else -> null
    }

    private fun unknownPlaceholderAt(template: String, index: Int): String? {
        if (template[index] != '{') return null
        val end = template.indexOf(char = '}', startIndex = index)
        return if (end < 0) null else template.substring(startIndex = index, endIndex = end + 1)
    }

    private fun unknownMessage(unknown: String): String {
        val supported = LogFormatPlaceholders.all.joinToString(separator = ", ")
        return "Unknown placeholder $unknown; supported placeholders are: $supported. " +
            "Write {{ and }} to match a literal brace."
    }

    private fun fragmentOf(placeholder: String): String = when (placeholder) {
        LogFormatPlaceholders.TIMESTAMP -> LineFormatCompiler.TIMESTAMP_PLACEHOLDER
        LogFormatPlaceholders.LEVEL -> "(?<${LogFormatGroups.LEVEL}>${LineFormatCompiler.LEVEL_PLACEHOLDER})"
        LogFormatPlaceholders.TAG -> "(?<${LogFormatGroups.TAG}>${LineFormatCompiler.TAG_FRAGMENT})"
        // Lazy and non capturing: it only has to give the surrounding literals a chance to anchor.
        LogFormatPlaceholders.ANY -> LineFormatCompiler.ANY_FRAGMENT
        else -> "(?<${LogFormatGroups.MESSAGE}>.*)"
    }

    private fun anchored(body: String): String {
        val trailing = if (body.endsWith(suffix = MESSAGE_FRAGMENT)) "\$" else ""
        return "^\\s*$body$trailing"
    }

    private fun failure(message: String, field: FormatErrorField): FormatCompilationResult.Failure =
        FormatCompilationResult.Failure(message = message, field = field)

    /** What the template reader found at one position. */
    private sealed interface Token {
        data class Brace(val char: Char) : Token
        data class Placeholder(val name: String) : Token
        data class Unknown(val text: String) : Token
        data object Literal : Token
    }

    private sealed interface Translation {
        data class Success(val pattern: String, val placeholders: Set<String>) : Translation
        data class Failure(val message: String) : Translation
    }

    private companion object {
        const val ESCAPED_BRACE_LENGTH = 2
        const val CUSTOM_FORMAT_NAME = "Custom format"
        const val DEFAULT_TIMESTAMP_ERROR = "The timestamp pattern cannot be compiled."
        val MESSAGE_FRAGMENT = "(?<${LogFormatGroups.MESSAGE}>.*)"
    }
}
