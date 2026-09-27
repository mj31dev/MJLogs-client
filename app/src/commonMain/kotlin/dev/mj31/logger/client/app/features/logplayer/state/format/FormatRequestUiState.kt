package dev.mj31.logger.client.app.features.logplayer.state.format

import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.model.log.LogSource

/**
 * Data shown by the dialog that asks the user to describe an unrecognized log format.
 *
 * [draft] is what is currently typed, held whole rather than as loose strings: a log can be described
 * as a line layout, as a JSON object or as a table, and those three have almost no fields in common.
 * Its variant is also what the dialog shows — switching shape *is* replacing the draft.
 *
 * [suggestion] is the layout inferred from [sampleLines]; the dialog opens on it so that confirming
 * is usually enough.
 */
data class FormatRequestUiState(
    val path: String,
    val fileName: String,
    val sampleLines: List<String>,
    val reason: String,
    val draft: ManualFormatInput = FormatDefaults.template,
    val preview: FormatPreview = FormatPreview.Empty,
    val suggestion: ManualFormatInput.Template? = null,
    val error: FormatError? = null,
    /** Set when the file already parsed and only needs a confirmation that nothing is missing. */
    val detectedSource: LogSource? = null,
) {

    val isConfirmation: Boolean
        get() = detectedSource != null

    /** Which set of inputs the dialog shows, and which of them the buttons switch between. */
    val kind: FormatKind
        get() = FormatKind.of(input = draft)

    /**
     * Error to show right now: the one reported by the last import attempt, or, while the user is
     * typing, the one the live preview reports.
     */
    val activeError: FormatError?
        get() = error ?: (preview as? FormatPreview.Invalid)?.let {
            FormatError(message = it.message, field = it.field)
        }

    fun errorFor(field: FormatErrorField): String? = activeError?.takeIf { it.field == field }?.message

    val timestampPatternError: String?
        get() = errorFor(field = FormatErrorField.TIMESTAMP_PATTERN)

    val structureTemplateError: String?
        get() = errorFor(field = FormatErrorField.STRUCTURE_TEMPLATE)

    /** Fallback for a failure that belongs to no input, shown as a notice instead of a field error. */
    val generalError: String?
        get() = errorFor(field = FormatErrorField.NONE)

    /**
     * Applying a format that reads nothing would only add an empty source to the session, so the
     * preview — not the state of the boxes — decides. It is the same parser the import would use.
     */
    val canApply: Boolean
        get() = (preview as? FormatPreview.Ready)?.matchedLines?.let { it > 0 } == true
}
