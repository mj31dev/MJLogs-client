package dev.mj31.logger.client.app.features.logplayer.state.format

import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.model.log.LogSource

/**
 * Data shown by the dialog that asks the user to describe an unrecognized log format.
 *
 * [suggestion] is the layout inferred from [sampleLines]; the dialog pre-fills its inputs with it so
 * that confirming is usually enough.
 */
data class FormatRequestUiState(
    val path: String,
    val fileName: String,
    val sampleLines: List<String>,
    val reason: String,
    val timestampPattern: String = FormatDefaults.TIMESTAMP_PATTERN,
    val structureTemplate: String = FormatDefaults.STRUCTURE_TEMPLATE,
    val preview: FormatPreview = FormatPreview.Empty,
    val suggestion: ManualFormatInput? = null,
    val error: FormatError? = null,
    /** Set when the file already parsed and only needs a confirmation that nothing is missing. */
    val detectedSource: LogSource? = null,
) {

    val isConfirmation: Boolean
        get() = detectedSource != null

    /**
     * Error to show right now: the one reported by the last import attempt, or, while the user is
     * typing, the one the live preview reports.
     */
    val activeError: FormatError?
        get() = error ?: (preview as? FormatPreview.Invalid)?.let {
            FormatError(message = it.message, field = it.field)
        }

    val timestampPatternError: String?
        get() = messageFor(field = FormatErrorField.TIMESTAMP_PATTERN)

    val structureTemplateError: String?
        get() = messageFor(field = FormatErrorField.STRUCTURE_TEMPLATE)

    /** Fallback for a failure that belongs to no input, shown as a notice instead of a field error. */
    val generalError: String?
        get() = messageFor(field = FormatErrorField.NONE)

    private fun messageFor(field: FormatErrorField): String? = activeError?.takeIf { it.field == field }?.message

    /** The draft currently typed by the user, ready to be compiled. */
    val draft: ManualFormatInput
        get() = ManualFormatInput(timestampPattern = timestampPattern, structureTemplate = structureTemplate)

    /** Applying a format that reads nothing would only add an empty source to the session. */
    val canApply: Boolean
        get() = timestampPattern.isNotBlank() &&
            structureTemplate.isNotBlank() &&
            (preview as? FormatPreview.Ready)?.matchedLines?.let { it > 0 } == true
}
