package dev.mj31.logger.client.domain.format.spec

/** Placeholders accepted in the user provided structure template. */
object LogFormatPlaceholders {
    const val TIMESTAMP: String = "{timestamp}"
    const val LEVEL: String = "{level}"
    const val TAG: String = "{tag}"
    const val MESSAGE: String = "{message}"

    /**
     * Matches and discards a varying fragment, for example a counter or a thread id that carries no
     * information for the viewer. Unlike the capturing placeholders it may appear several times.
     */
    const val ANY: String = "{any}"

    val all: List<String> = listOf(TIMESTAMP, LEVEL, TAG, MESSAGE, ANY)

    /** Placeholders that fill a field of the record and may therefore appear only once. */
    val capturing: List<String> = listOf(TIMESTAMP, LEVEL, TAG, MESSAGE)
}
