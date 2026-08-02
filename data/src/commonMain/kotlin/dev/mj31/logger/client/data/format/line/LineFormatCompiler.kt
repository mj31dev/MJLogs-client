package dev.mj31.logger.client.data.format.line

import dev.mj31.logger.client.domain.format.spec.LogFormatGroups
import dev.mj31.logger.client.domain.model.log.LogLevel

/**
 * Assembles full line regular expressions out of a structure template and a timestamp fragment.
 *
 * A structure template is a regular expression containing [TIMESTAMP_PLACEHOLDER] where the timestamp
 * has to appear and [LEVEL_PLACEHOLDER] where the severity token alternation has to appear.
 */
internal object LineFormatCompiler {

    const val TIMESTAMP_PLACEHOLDER: String = "%TS%"
    const val LEVEL_PLACEHOLDER: String = "%LEVEL%"

    /** Regex fragment for a tag captured from a user defined template. */
    const val TAG_FRAGMENT: String = "[^\\s\\]:]{1,60}"

    /** Regex fragment for a discarded, varying part of a line such as a counter or a thread id. */
    const val ANY_FRAGMENT: String = "(?:.*?)"

    /** Alternation of every known severity token, longest first so that `WARNING` wins over `W`. */
    val levelAlternation: String = LogLevel.knownTokens
        .sortedWith(comparator = compareByDescending<String> { it.length }.thenBy { it })
        .joinToString(separator = "|")

    /** Substitutes the placeholders of [template] with the concrete timestamp and level fragments. */
    fun buildLinePattern(template: String, timestampRegex: String): String = template
        .replace(oldValue = TIMESTAMP_PLACEHOLDER, newValue = timestampGroup(timestampRegex = timestampRegex))
        .replace(oldValue = LEVEL_PLACEHOLDER, newValue = levelAlternation)

    /** Wraps [timestampRegex] into the outer `ts` group expected by every [LogFormatGroups] consumer. */
    fun timestampGroup(timestampRegex: String): String = "(?<${LogFormatGroups.TIMESTAMP}>$timestampRegex)"

    /** Named group declaration, e.g. `(?<lvl>` used to test whether a pattern captures a component. */
    fun groupDeclaration(name: String): String = "(?<$name>"
}
