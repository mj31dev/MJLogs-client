package dev.mj31.logger.client.app.features.logplayer.state.format

import dev.mj31.logger.client.domain.format.compile.FormatErrorField

/** Why a format was rejected, and which input the user has to fix. */
data class FormatError(
    val message: String,
    val field: FormatErrorField,
)
