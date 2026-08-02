package dev.mj31.logger.client.domain.format.spec

/** Named regex groups a [LogFormatSpec.linePattern] may expose. */
object LogFormatGroups {
    const val TIMESTAMP: String = "ts"
    const val LEVEL: String = "lvl"
    const val TAG: String = "tag"
    const val MESSAGE: String = "msg"
}
