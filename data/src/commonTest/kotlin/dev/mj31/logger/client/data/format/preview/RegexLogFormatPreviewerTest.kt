package dev.mj31.logger.client.data.format.preview

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.format.preview.HighlightedSpan
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import kotlin.test.Test
import kotlin.test.assertIs
import dev.mj31.logger.client.domain.model.log.LogLevel

class RegexLogFormatPreviewerTest {

    private val previewer = RegexLogFormatPreviewer()

    @Test
    fun `reports which fragment of a line feeds which component`() {
        val line = "2024-01-15 10:23:45.123 WARN [CacheStore]: evicted 15 entries"

        val preview = ready(
            input = ManualFormatInput(
                timestampPattern = "yyyy-MM-dd HH:mm:ss.SSS",
                structureTemplate = "{timestamp} {level} [{tag}]: {message}",
            ),
            lines = listOf(line),
        )

        val previewLine = preview.lines.single()
        assertThat(previewLine.isRecord).isTrue()
        assertThat(previewLine.level).isEqualTo(LogLevel.WARN)
        assertThat(fragments(line = line, spans = previewLine.spans)).containsExactly(
            LogComponent.TIMESTAMP to "2024-01-15 10:23:45.123",
            LogComponent.LEVEL to "WARN",
            LogComponent.TAG to "CacheStore",
            LogComponent.MESSAGE to "evicted 15 entries",
        ).inOrder()
    }

    @Test
    fun `a template without level and tag only highlights what it captures`() {
        val line = "10:23:45 booting the exporter"

        val preview = ready(
            input = ManualFormatInput(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {message}"),
            lines = listOf(line),
        )

        val previewLine = preview.lines.single()
        assertThat(previewLine.level).isNull()
        assertThat(fragments(line = line, spans = previewLine.spans)).containsExactly(
            LogComponent.TIMESTAMP to "10:23:45",
            LogComponent.MESSAGE to "booting the exporter",
        ).inOrder()
    }

    @Test
    fun `lines the format does not match are reported as continuations`() {
        val preview = ready(
            input = ManualFormatInput(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {message}"),
            lines = listOf("10:23:45 upload failed", "    at Http2Stream.takeHeaders(Http2Stream.kt:143)"),
        )

        assertThat(preview.lines.map { it.isRecord }).containsExactly(true, false).inOrder()
        assertThat(preview.matchedLines).isEqualTo(1)
        assertThat(preview.totalLines).isEqualTo(2)
        assertThat(preview.lines.last().spans).isEmpty()
    }

    @Test
    fun `an uncompilable format is reported instead of throwing`() {
        val preview = previewer.preview(
            input = ManualFormatInput(timestampPattern = "???", structureTemplate = "{timestamp} {message}"),
            sampleLines = listOf("10:23:45 boot"),
        )

        val invalid = assertIs<FormatPreview.Invalid>(preview)
        assertThat(invalid.message).isNotEmpty()
    }

    @Test
    fun `an empty sample yields an empty preview`() {
        val preview = previewer.preview(
            input = ManualFormatInput(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {message}"),
            sampleLines = emptyList(),
        )

        assertThat(preview).isEqualTo(FormatPreview.Empty)
    }

    private fun ready(input: ManualFormatInput, lines: List<String>): FormatPreview.Ready =
        assertIs<FormatPreview.Ready>(previewer.preview(input = input, sampleLines = lines))

    private fun fragments(line: String, spans: List<HighlightedSpan>): List<Pair<LogComponent, String>> =
        spans.map { span ->
            span.component to line.substring(startIndex = span.startIndex, endIndex = span.endIndex)
        }
}
