package dev.mj31.logger.client.domain.format.compile

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

/** Turns a [ManualFormatInput] into an executable [LogFormatSpec]. */
interface LogFormatCompiler {
    fun compile(input: ManualFormatInput): FormatCompilationResult
}
