package dev.mj31.logger.client.domain.format.compile

/**
 * Manual format description entered by the user when detection fails.
 *
 * @param timestampPattern for example `yyyy-MM-dd HH:mm:ss.SSS`.
 * @param structureTemplate for example `{timestamp} {level} {tag}: {message}`; literal characters
 * are matched verbatim and runs of whitespace match any whitespace.
 */
data class ManualFormatInput(
    val timestampPattern: String,
    val structureTemplate: String,
    val utcOffsetMinutes: Int = 0,
)
