package dev.mj31.logger.client.domain.format.spec

/** Tokens understood inside [LogFormatSpec.timestampPattern]. */
object TimestampPatternTokens {
    const val YEAR_FOUR: String = "yyyy"
    const val YEAR_TWO: String = "yy"
    const val MONTH_NAME: String = "MMM"
    const val MONTH: String = "MM"
    const val DAY: String = "dd"
    const val HOUR: String = "HH"

    /** Hour on a twelve hour clock; without [MERIDIEM] beside it the reading is ambiguous. */
    const val HOUR_12: String = "hh"

    /** The AM or PM marker, in any of the spellings `AM`, `am`, `A.M.`, `p.m.`. */
    const val MERIDIEM: String = "a"
    const val MINUTE: String = "mm"
    const val SECOND: String = "ss"
    const val MILLI: String = "SSS"
    const val MICRO: String = "SSSSSS"
    const val OFFSET: String = "XXX"
    const val EPOCH_MILLIS: String = "epochMillis"
    const val EPOCH_SECONDS: String = "epochSeconds"

    /** Ordered by length so that a greedy tokenizer always consumes the longest token first. */
    val ordered: List<String> = listOf(
        EPOCH_MILLIS,
        EPOCH_SECONDS,
        MICRO,
        YEAR_FOUR,
        MONTH_NAME,
        MILLI,
        OFFSET,
        YEAR_TWO,
        MONTH,
        DAY,
        HOUR,
        HOUR_12,
        MINUTE,
        SECOND,
        MERIDIEM,
    )

    /**
     * True when [pattern] reads a twelve hour clock but nothing tells morning from afternoon.
     *
     * Such a timestamp genuinely denotes two moments twelve hours apart, and no amount of context in
     * the file resolves it — which is why this is one of the two cases where the user is asked rather
     * than guessed at.
     */
    fun isHourAmbiguous(pattern: String): Boolean =
        pattern.contains(other = HOUR_12) && !pattern.contains(other = MERIDIEM)

    /**
     * True when [pattern] places a record on a calendar day by itself.
     *
     * A pattern that does not needs a day supplied from outside, and is therefore the only kind that
     * can be put on the wrong one. An epoch reading carries everything and needs nothing.
     */
    fun carriesDate(pattern: String): Boolean =
        pattern.contains(other = EPOCH_MILLIS) ||
            pattern.contains(other = EPOCH_SECONDS) ||
            listOf(YEAR_FOUR, YEAR_TWO, MONTH_NAME, MONTH, DAY).any { pattern.contains(other = it) }
}
