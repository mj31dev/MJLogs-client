package dev.mj31.logger.client.domain.format.preview

import dev.mj31.logger.client.domain.format.compile.ManualFormatInput

/**
 * Applies a format that the user is still typing to a sample of lines.
 *
 * It is what turns the format dialog into a live editor: every keystroke shows which fragment of
 * each line becomes the timestamp, the level, the tag and the body.
 */
interface LogFormatPreviewer {
    fun preview(input: ManualFormatInput, sampleLines: List<String>): FormatPreview
}
