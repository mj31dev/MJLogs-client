package dev.mj31.logger.client.data.format.line

/** Characters that carry a special meaning inside a regular expression. */
private const val REGEX_SPECIAL_CHARACTERS = "\\^\$.|?*+()[]{}"

/** Regex fragment matching a run of whitespace; literal spaces in patterns are intentionally lenient. */
internal const val WHITESPACE_RUN = "\\s+"

/** Escapes [char] so that it is matched verbatim by a regular expression. */
internal fun escapeRegexLiteral(char: Char): String = if (char in REGEX_SPECIAL_CHARACTERS) "\\$char" else char.toString()

/**
 * Appends the literal starting at [index] of [source] to [target] and returns the index after it.
 *
 * A run of whitespace is collapsed into [WHITESPACE_RUN] so that generated patterns tolerate column
 * aligned log files.
 */
internal fun appendLiteral(source: String, index: Int, target: StringBuilder): Int {
    val char = source[index]
    if (!char.isWhitespace()) {
        target.append(escapeRegexLiteral(char = char))
        return index + 1
    }
    var end = index
    while (end < source.length && source[end].isWhitespace()) {
        end++
    }
    target.append(WHITESPACE_RUN)
    return end
}
