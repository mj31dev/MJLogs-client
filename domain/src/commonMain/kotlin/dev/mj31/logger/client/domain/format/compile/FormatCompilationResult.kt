package dev.mj31.logger.client.domain.format.compile

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

sealed interface FormatCompilationResult {

    data class Success(val spec: LogFormatSpec) : FormatCompilationResult

    /**
     * [field] tells the UI which input to mark; [FormatErrorField.NONE] is what makes a message fall
     * back to a general notice instead of being attached to the wrong field.
     */
    data class Failure(
        val message: String,
        val field: FormatErrorField = FormatErrorField.NONE,
    ) : FormatCompilationResult
}
