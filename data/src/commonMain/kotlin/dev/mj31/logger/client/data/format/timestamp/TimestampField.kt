package dev.mj31.logger.client.data.format.timestamp

/**
 * Calendar component a timestamp pattern is able to capture.
 *
 * [groupName] is the name of the regex group exposed by the compiled fragment. Group names are kept
 * strictly alphanumeric because the JVM regex engine rejects anything else.
 */
enum class TimestampField(val groupName: String) {
    YEAR("tsYear"),
    YEAR_SHORT("tsYear"),
    MONTH_NAME("tsMonthName"),
    MONTH("tsMonth"),
    DAY("tsDay"),
    HOUR("tsHour"),
    MINUTE("tsMinute"),
    SECOND("tsSecond"),
    FRACTION("tsFraction"),
    OFFSET("tsOffset"),
    EPOCH_MILLIS("tsEpochMillis"),
    EPOCH_SECONDS("tsEpochSeconds"),
    ;

    companion object {

        /** Fields that pin a timestamp to a calendar day; their absence enables midnight rollover. */
        val dateFields: Set<TimestampField> = setOf(YEAR, YEAR_SHORT, MONTH, MONTH_NAME, DAY)
    }
}
