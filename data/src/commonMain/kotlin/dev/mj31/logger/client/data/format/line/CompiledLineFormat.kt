package dev.mj31.logger.client.data.format.line

import dev.mj31.logger.client.domain.format.spec.LogFormatGroups
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.data.format.timestamp.TimestampPatternCompiler
import dev.mj31.logger.client.data.format.timestamp.CompiledTimestampPattern

/**
 * Executable form of a [LogFormatSpec]: the line regex plus the compiled timestamp pattern.
 *
 * Compilation is the expensive part of parsing, therefore instances are meant to be created once per
 * specification and shared between parsers.
 */
internal class CompiledLineFormat private constructor(
    val spec: LogFormatSpec,
    val lineRegex: Regex,
    val timestamp: CompiledTimestampPattern,
) {

    val hasLevelGroup: Boolean = declaresGroup(name = LogFormatGroups.LEVEL)
    val hasTagGroup: Boolean = declaresGroup(name = LogFormatGroups.TAG)
    val hasMessageGroup: Boolean = declaresGroup(name = LogFormatGroups.MESSAGE)

    /** Number of optional components the pattern captures; used to prefer richer formats on ties. */
    val capturedComponents: Int = listOf(hasLevelGroup, hasTagGroup).count { it }

    private fun declaresGroup(name: String): Boolean =
        spec.linePattern.contains(other = LineFormatCompiler.groupDeclaration(name = name))

    companion object {

        /** @throws IllegalArgumentException when either the line pattern or the timestamp pattern is invalid. */
        fun compile(spec: LogFormatSpec): CompiledLineFormat {
            val regex = runCatching { Regex(pattern = spec.linePattern, option = RegexOption.IGNORE_CASE) }
                .getOrElse { error ->
                    throw IllegalArgumentException("Log format '${spec.name}' has an invalid line pattern: ${error.message}")
                }
            val timestamp = runCatching { TimestampPatternCompiler.compile(pattern = spec.timestampPattern) }
                .getOrElse { error ->
                    throw IllegalArgumentException("Log format '${spec.name}' has an invalid timestamp pattern: ${error.message}")
                }
            return CompiledLineFormat(spec = spec, lineRegex = regex, timestamp = timestamp)
        }
    }
}
