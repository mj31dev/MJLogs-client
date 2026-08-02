package dev.mj31.logger.client.domain.format.spec

/** Tokens understood inside [LogFormatSpec.timestampPattern]. */
object TimestampPatternTokens {
    const val YEAR_FOUR: String = "yyyy"
    const val YEAR_TWO: String = "yy"
    const val MONTH_NAME: String = "MMM"
    const val MONTH: String = "MM"
    const val DAY: String = "dd"
    const val HOUR: String = "HH"
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
        MINUTE,
        SECOND,
    )
}
