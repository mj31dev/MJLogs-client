package dev.mj31.logger.client.data.format.timestamp

import dev.mj31.logger.client.domain.format.spec.TimestampPatternTokens

/**
 * Infers a timestamp pattern from the raw text of a timestamp, without relying on the catalogue of
 * known layouts.
 *
 * The inference works on the *shape* of the text: runs of digits separated by punctuation. It is
 * what lets the application propose `dd.MM.yyyy_HH.mm.ss` for `01.08.2026_10.23.45`, a layout no
 * built-in candidate covers.
 */
internal object TimestampShapeInference {

    /** Smallest and largest number of digit groups a timestamp is expected to have. */
    const val MIN_GROUPS: Int = 3
    const val MAX_GROUPS: Int = 7

    /**
     * Digit runs joined by punctuation, e.g. `2024-01-15 10:23:45.123` or `01.08.2026_10.23.45`.
     *
     * One expression per group count: reading a shorter run is how the inference recovers when the
     * greedy reading swallows an unrelated number that follows the timestamp.
     */
    private val numericRuns: Map<Int, Regex> = (MIN_GROUPS..MAX_GROUPS).associateWith { groups ->
        Regex(pattern = """\d{1,4}(?:(?:[-/.:,_T] ?|[ T])\d{1,6}){2,${groups - 1}}""")
    }
    private val epochMillis = Regex(pattern = """\b\d{13}\b""")
    private val epochSeconds = Regex(pattern = """\b\d{10}\b""")
    private val trailingOffset = Regex(pattern = """^(Z|[+-]\d{2}:?\d{2})""")

    /** Text of the timestamp candidate found in [line], together with its position. */
    data class Region(val text: String, val startIndex: Int, val endIndex: Int, val hasOffset: Boolean)

    fun findRegion(line: String, maxGroups: Int = MAX_GROUPS): Region? {
        val match = numericRuns[maxGroups]?.find(input = line)
            ?: epochMillis.find(input = line)
            ?: epochSeconds.find(input = line)
            ?: return null
        val end = match.range.last + 1
        val offset = trailingOffset.find(input = line.substring(startIndex = end))?.value
        return Region(
            // The offset is kept out of the text: only the digit runs take part in the shape analysis.
            text = match.value,
            startIndex = match.range.first,
            endIndex = end + (offset?.length ?: 0),
            hasOffset = offset != null,
        )
    }

    /**
     * Returns the pattern shared by [regions], or `null` when they do not agree on a single shape.
     *
     * Several samples are needed to tell `dd.MM` from `MM.dd`: a value above twelve can only be a day.
     */
    fun infer(regions: List<Region>): String? {
        if (regions.isEmpty()) return null
        val shapes = regions.map { split(region = it) }
        val reference = shapes.first()
        if (shapes.any { it.separators != reference.separators || it.groups.size != reference.groups.size }) return null
        val tokens = tokensOf(shapes = shapes) ?: return null
        return buildPattern(tokens = tokens, separators = reference.separators, hasOffset = reference.hasOffset)
    }

    private fun split(region: Region): Shape {
        val groups = mutableListOf<String>()
        val separators = mutableListOf<String>()
        val digits = StringBuilder()
        val separator = StringBuilder()
        region.text.forEach { char ->
            if (char.isDigit()) {
                if (separator.isNotEmpty()) {
                    separators += separator.toString()
                    separator.clear()
                }
                digits.append(char)
            } else {
                if (digits.isNotEmpty()) {
                    groups += digits.toString()
                    digits.clear()
                }
                separator.append(char)
            }
        }
        if (digits.isNotEmpty()) groups += digits.toString()
        return Shape(groups = groups, separators = separators, hasOffset = region.hasOffset)
    }

    private fun buildPattern(tokens: List<String>, separators: List<String>, hasOffset: Boolean): String {
        val body = buildString {
            tokens.forEachIndexed { index, token ->
                append(token)
                separators.getOrNull(index = index)?.let { append(it) }
            }
        }
        return if (hasOffset) body + TimestampPatternTokens.OFFSET else body
    }

    private fun tokensOf(shapes: List<Shape>): List<String>? {
        val groups = shapes.first().groups
        if (groups.size == 1) return epochTokenOf(digits = groups.single())?.let { listOf(it) }

        val yearIndex = groups.indexOfFirst { it.length == YEAR_DIGITS }
        return when {
            yearIndex in 0 until DATE_GROUPS && groups.size >= DATE_GROUPS ->
                dateTokens(shapes = shapes, yearIndex = yearIndex)?.let { date ->
                    timeTokens(groups = groups.drop(n = DATE_GROUPS))?.let { time -> date + time }
                }

            yearIndex < 0 && groups.size >= DATE_GROUPS + 2 ->
                listOf(TimestampPatternTokens.MONTH, TimestampPatternTokens.DAY) +
                    (timeTokens(groups = groups.drop(n = 2)) ?: return null)

            yearIndex < 0 -> timeTokens(groups = groups)

            else -> null
        }
    }

    private fun epochTokenOf(digits: String): String? = when (digits.length) {
        EPOCH_MILLIS_DIGITS -> TimestampPatternTokens.EPOCH_MILLIS
        EPOCH_SECONDS_DIGITS -> TimestampPatternTokens.EPOCH_SECONDS
        else -> null
    }

    /** Orders the three date components; the year position and the observed values decide. */
    private fun dateTokens(shapes: List<Shape>, yearIndex: Int): List<String>? {
        val day = TimestampPatternTokens.DAY
        val month = TimestampPatternTokens.MONTH
        val year = TimestampPatternTokens.YEAR_FOUR
        return when (yearIndex) {
            0 -> listOf(year, month, day)
            2 -> if (isDayFirst(shapes = shapes)) listOf(day, month, year) else listOf(month, day, year)
            else -> null
        }
    }

    private fun isDayFirst(shapes: List<Shape>): Boolean {
        val firstValues = shapes.mapNotNull { it.groups[0].toIntOrNull() }
        val secondValues = shapes.mapNotNull { it.groups[1].toIntOrNull() }
        return when {
            firstValues.any { it > MAX_MONTH } -> true
            secondValues.any { it > MAX_MONTH } -> false
            // Both readings stay possible: fall back to the convention of the separator in use.
            else -> shapes.first().separators.firstOrNull() != "/"
        }
    }

    private fun timeTokens(groups: List<String>): List<String>? {
        val hourMinuteSecond = listOf(
            TimestampPatternTokens.HOUR,
            TimestampPatternTokens.MINUTE,
            TimestampPatternTokens.SECOND,
        )
        return when (groups.size) {
            0 -> emptyList()
            HOUR_MINUTE_GROUPS -> hourMinuteSecond.dropLast(n = 1)
            TIME_GROUPS -> hourMinuteSecond
            TIME_GROUPS + 1 -> fractionTokenOf(digits = groups.last())?.let { hourMinuteSecond + it }
            else -> null
        }
    }

    private fun fractionTokenOf(digits: String): String? = when (digits.length) {
        MILLI_DIGITS -> TimestampPatternTokens.MILLI
        MICRO_DIGITS -> TimestampPatternTokens.MICRO
        else -> null
    }

    private data class Shape(
        val groups: List<String>,
        val separators: List<String>,
        val hasOffset: Boolean,
    )

    private const val YEAR_DIGITS = 4
    private const val DATE_GROUPS = 3
    private const val TIME_GROUPS = 3
    private const val HOUR_MINUTE_GROUPS = 2
    private const val MAX_MONTH = 12
    private const val MILLI_DIGITS = 3
    private const val MICRO_DIGITS = 6
    private const val EPOCH_MILLIS_DIGITS = 13
    private const val EPOCH_SECONDS_DIGITS = 10
}
