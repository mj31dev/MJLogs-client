package dev.mj31.logger.client.data.format.detect

import dev.mj31.logger.client.domain.format.spec.LogFormatPlaceholders
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.data.format.timestamp.TimestampShapeInference

/**
 * Produces the most plausible [ManualFormatInput] for lines no built-in candidate recognizes.
 *
 * The guess is what the format dialog pre-fills, so the user usually only has to confirm it. It is
 * built by locating the timestamp, then describing what surrounds it: parts that are identical in
 * every sample line become literals, parts that vary become placeholders.
 */
class LogFormatGuesser {

    fun guess(sampleLines: List<String>): ManualFormatInput? {
        val lines = sampleLines.filter { it.isNotBlank() }.take(n = MAX_SAMPLE_LINES)
        if (lines.size < MIN_SAMPLE_LINES) return null
        // Read the longest plausible timestamp first and fall back to shorter ones: a greedy read can
        // swallow an unrelated number that happens to follow the timestamp.
        return (TimestampShapeInference.MAX_GROUPS downTo TimestampShapeInference.MIN_GROUPS)
            .firstNotNullOfOrNull { maxGroups -> attempt(lines = lines, maxGroups = maxGroups) }
    }

    private fun attempt(lines: List<String>, maxGroups: Int): ManualFormatInput? {
        val segments = lines.mapNotNull { line -> segmentOf(line = line, maxGroups = maxGroups) }
        if (segments.size < MIN_SAMPLE_LINES || segments.size < lines.size * MIN_COVERAGE) return null

        val timestampPattern = TimestampShapeInference.infer(regions = segments.map { it.region }) ?: return null
        return ManualFormatInput(
            timestampPattern = timestampPattern,
            structureTemplate = prefixTemplate(prefixes = segments.map { it.prefix }) +
                LogFormatPlaceholders.TIMESTAMP +
                suffixTemplate(suffixes = segments.map { it.suffix }),
        )
    }

    private fun segmentOf(line: String, maxGroups: Int): Segment? {
        val region = TimestampShapeInference.findRegion(line = line, maxGroups = maxGroups) ?: return null
        return Segment(
            region = region,
            prefix = line.substring(startIndex = 0, endIndex = region.startIndex),
            suffix = line.substring(startIndex = region.endIndex),
        )
    }

    /** Everything before the timestamp: the varying middle, if any, becomes [LogFormatPlaceholders.ANY]. */
    private fun prefixTemplate(prefixes: List<String>): String {
        val first = prefixes.first()
        if (prefixes.all { it == first }) return literal(text = first)

        // A varying field usually shares leading characters with its neighbours ("<0000>" and
        // "<0001>" agree on "<000"), so the literal is backed off to the last delimiter.
        val head = commonPrefix(values = prefixes).dropLastWhile { it.isLetterOrDigit() }
        val tail = commonSuffix(values = prefixes, reserved = head.length).dropWhile { it.isLetterOrDigit() }
        return literal(text = head) + LogFormatPlaceholders.ANY + literal(text = tail)
    }

    /**
     * Everything after the timestamp, described as an optional level, an optional tag and the body.
     *
     * A component is only proposed when the samples agree on it, so a wrong guess degrades into
     * "everything is the message" rather than into a template that drops information.
     */
    private fun suffixTemplate(suffixes: List<String>): String {
        val builder = StringBuilder()
        var rest = suffixes
        builder.append(literal(text = consumeSeparator(parts = rest).also { rest = drop(parts = rest, count = it.length) }))

        val levelTokens = rest.map { token(text = it) }
        if (looksLikeLevel(tokens = levelTokens)) {
            rest = dropEach(parts = rest, tokens = levelTokens)
            builder.append(LogFormatPlaceholders.LEVEL)
            builder.append(literal(text = consumeSeparator(parts = rest).also { rest = drop(parts = rest, count = it.length) }))
        }

        val tagTokens = rest.map { token(text = it) }
        val tagSeparator = separatorAfterTag(parts = rest, tokens = tagTokens)
        if (tagSeparator != null) {
            builder.append(LogFormatPlaceholders.TAG)
            builder.append(literal(text = tagSeparator))
        }

        builder.append(LogFormatPlaceholders.MESSAGE)
        return builder.toString()
    }

    /** Longest run of punctuation shared by every part, e.g. `~`, ` ` or ` | `. */
    private fun consumeSeparator(parts: List<String>): String =
        commonPrefix(values = parts).takeWhile { !it.isLetterOrDigit() }

    private fun token(text: String): String = text.takeWhile { it.isLetterOrDigit() || it in TOKEN_EXTRA_CHARS }

    private fun looksLikeLevel(tokens: List<String>): Boolean {
        if (tokens.any { it.isEmpty() }) return false
        val recognized = tokens.count { LogLevel.fromToken(token = it) != null }
        return recognized >= tokens.size * MIN_COMPONENT_AGREEMENT
    }

    /**
     * Returns the literal separator that follows the tag, or `null` when the samples do not agree
     * that there is a tag at all.
     */
    private fun separatorAfterTag(parts: List<String>, tokens: List<String>): String? {
        if (tokens.any { it.isEmpty() || it.length > MAX_TAG_LENGTH }) return null
        val remainders = parts.mapIndexed { index, part -> part.drop(n = tokens[index].length) }
        val separator = consumeSeparator(parts = remainders)
        val identical = tokens.all { it == tokens.first() }
        return if (separator.isNotEmpty() || identical) separator else null
    }

    private fun drop(parts: List<String>, count: Int): List<String> = parts.map { it.drop(n = count) }

    private fun dropEach(parts: List<String>, tokens: List<String>): List<String> =
        parts.mapIndexed { index, part -> part.drop(n = tokens[index].length) }

    private fun commonPrefix(values: List<String>): String =
        values.reduce { left, right -> left.commonPrefixWith(other = right) }

    private fun commonSuffix(values: List<String>, reserved: Int): String {
        val suffix = values.reduce { left, right -> left.commonSuffixWith(other = right) }
        val room = values.minOf { it.length } - reserved
        return if (suffix.length > room) suffix.takeLast(n = room.coerceAtLeast(minimumValue = 0)) else suffix
    }

    /** Braces would be read back as placeholders, so a literal has to escape them. */
    private fun literal(text: String): String = text
        .replace(oldValue = "{", newValue = "{{")
        .replace(oldValue = "}", newValue = "}}")

    private data class Segment(
        val region: TimestampShapeInference.Region,
        val prefix: String,
        val suffix: String,
    )

    private companion object {
        const val MAX_SAMPLE_LINES = 40
        const val MIN_SAMPLE_LINES = 2
        const val MIN_COVERAGE = 0.6
        const val MIN_COMPONENT_AGREEMENT = 0.7
        const val MAX_TAG_LENGTH = 60
        const val TOKEN_EXTRA_CHARS = "_.-"
    }
}
