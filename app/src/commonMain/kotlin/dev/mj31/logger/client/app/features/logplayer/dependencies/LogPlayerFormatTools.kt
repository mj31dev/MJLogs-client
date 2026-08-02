package dev.mj31.logger.client.app.features.logplayer.dependencies

import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.preview.LogFormatPreviewer

/** Format tooling the dialog needs: compiling what the user typed and previewing how it reads. */
data class LogPlayerFormatTools(
    val compiler: LogFormatCompiler,
    val previewer: LogFormatPreviewer,
)
