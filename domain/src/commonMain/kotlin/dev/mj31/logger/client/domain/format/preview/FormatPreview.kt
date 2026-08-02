package dev.mj31.logger.client.domain.format.preview

import dev.mj31.logger.client.domain.format.compile.FormatErrorField

/** Outcome of applying a format under construction to a sample. */
sealed interface FormatPreview {

    data object Empty : FormatPreview

    data class Ready(val lines: List<PreviewLine>) : FormatPreview {

        val matchedLines: Int
            get() = lines.count { it.isRecord }

        val totalLines: Int
            get() = lines.size
    }

    /** The format does not compile at all; [message] is the same text the import would report. */
    data class Invalid(
        val message: String,
        val field: FormatErrorField = FormatErrorField.NONE,
    ) : FormatPreview
}
