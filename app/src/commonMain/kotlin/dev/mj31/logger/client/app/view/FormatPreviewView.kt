package dev.mj31.logger.client.app.view

import dev.mj31.logger.client.app.theme.type.ContentType
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.app.theme.LocalLogLevelColors
import dev.mj31.logger.client.app.theme.Spacing
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.preview.PreviewLine
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.preview_matched_lines
import dev.mj31.logger.client.app.resources.preview_unavailable
import org.jetbrains.compose.resources.stringResource

private const val UNMATCHED_ALPHA = 0.55f
private const val PREVIEW_MAX_HEIGHT = 150

/** A colour swatch is a mark next to a word, not a control, so it is sized rather than spaced. */
private const val SWATCH_SIZE = 8

/**
 * Shows the sample lines the way the format currently typed would read them.
 *
 * Each component gets its own colour, and a line no rule matches is dimmed: that is precisely what
 * the importer would attach to the previous record instead of turning into a new one.
 *
 * The sample is drawn in every state, dimmed whole while the format does not compile, so the block
 * keeps the height it had. It sits above the inputs, and a preview that vanished between two
 * keystrokes would drag the box being typed in upwards with it.
 */
@Composable
fun FormatPreviewView(
    preview: FormatPreview,
    sampleLines: List<String>,
    modifier: Modifier = Modifier,
) {
    if (sampleLines.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        PreviewStatus(text = statusText(preview = preview), color = statusColor(preview = preview))
        Spacer(modifier = Modifier.height(height = Spacing.tight))
        Legend()
        Spacer(modifier = Modifier.height(height = Spacing.small))
        PreviewLines(lines = linesOf(preview = preview, sampleLines = sampleLines))
    }
}

/** What the format reads out of the sample, or why nothing can be read at all. */
@Composable
private fun statusText(preview: FormatPreview): String = when (preview) {
    // The message itself is attached to the input that caused it, right below the fields.
    is FormatPreview.Invalid, FormatPreview.Empty -> stringResource(resource = Res.string.preview_unavailable)

    is FormatPreview.Ready -> stringResource(
        resource = Res.string.preview_matched_lines,
        preview.matchedLines,
        preview.totalLines,
    )
}

private fun linesOf(preview: FormatPreview, sampleLines: List<String>): List<PreviewLine> =
    (preview as? FormatPreview.Ready)?.lines ?: sampleLines.map { text -> PreviewLine(text = text) }

@Composable
private fun statusColor(preview: FormatPreview): Color = when {
    preview !is FormatPreview.Ready -> MaterialTheme.colorScheme.onSurfaceVariant
    preview.matchedLines == 0 -> MaterialTheme.colorScheme.error
    preview.matchedLines < preview.totalLines -> LocalLogLevelColors.current.warn
    else -> LocalLogLevelColors.current.info
}

@Composable
private fun PreviewStatus(text: String, color: Color) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun PreviewLines(lines: List<PreviewLine>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = PREVIEW_MAX_HEIGHT.dp)
            .clip(shape = MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .padding(all = Spacing.small),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .horizontalScroll(state = rememberScrollState()),
        ) {
            val palette = previewPalette()
            lines.forEach { line ->
                Text(
                    text = annotate(line = line, palette = palette),
                    style = ContentType.record,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(space = Spacing.small)) {
        val palette = previewPalette()
        LogComponent.entries.forEach { component ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = Spacing.tight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(size = SWATCH_SIZE.dp)
                        .clip(shape = MaterialTheme.shapes.extraSmall)
                        .background(color = palette.colorOf(component = component)),
                )
                Text(
                    text = component.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Colour of every component plus the two neutral shades used around them. */
private data class PreviewPalette(
    val timestamp: Color,
    val level: Color,
    val tag: Color,
    val message: Color,
    val separator: Color,
    val unmatched: Color,
) {

    fun colorOf(component: LogComponent): Color = when (component) {
        LogComponent.TIMESTAMP -> timestamp
        LogComponent.LEVEL -> level
        LogComponent.TAG -> tag
        LogComponent.MESSAGE -> message
    }
}

@Composable
private fun previewPalette(): PreviewPalette = PreviewPalette(
    timestamp = MaterialTheme.colorScheme.primary,
    level = LocalLogLevelColors.current.warn,
    // The anchor accent is one colour for both schemes and all but disappears against white here.
    tag = MaterialTheme.colorScheme.secondary,
    message = MaterialTheme.colorScheme.onSurface,
    separator = MaterialTheme.colorScheme.onSurfaceVariant,
    unmatched = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = UNMATCHED_ALPHA),
)

private fun annotate(line: PreviewLine, palette: PreviewPalette): AnnotatedString = buildAnnotatedString {
    if (!line.isRecord) {
        withStyle(style = SpanStyle(color = palette.unmatched)) { append(text = line.text) }
        return@buildAnnotatedString
    }
    var index = 0
    line.spans.forEach { span ->
        if (span.startIndex < index) return@forEach
        appendSeparator(text = line.text.substring(startIndex = index, endIndex = span.startIndex), palette = palette)
        withStyle(style = SpanStyle(color = palette.colorOf(component = span.component), fontWeight = FontWeight.Medium)) {
            append(text = line.text.substring(startIndex = span.startIndex, endIndex = span.endIndex))
        }
        index = span.endIndex
    }
    appendSeparator(text = line.text.substring(startIndex = index), palette = palette)
}

private fun AnnotatedString.Builder.appendSeparator(text: String, palette: PreviewPalette) {
    if (text.isEmpty()) return
    withStyle(style = SpanStyle(color = palette.separator)) { append(text = text) }
}
