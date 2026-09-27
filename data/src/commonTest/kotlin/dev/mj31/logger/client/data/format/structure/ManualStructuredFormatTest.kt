package dev.mj31.logger.client.data.format.structure

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.data.format.line.ManualFormatCompiler
import dev.mj31.logger.client.data.format.preview.ManualFormatPreviewer
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import kotlin.test.Test

/** What the format dialog produces when the user describes a JSON or a delimited log by hand. */
class ManualStructuredFormatTest {

    private val compiler = ManualFormatCompiler()
    private val previewer = ManualFormatPreviewer()

    @Test
    fun `keys typed for a JSON log become the places its components are read from`() {
        val spec = compile(
            input = ManualFormatInput.Json(
                timestampPattern = "yyyy-MM-ddTHH:mm:ss.SSS",
                timestampKey = "@timestamp",
                levelKey = "log.level",
                messageKey = "message",
            ),
        ) as LogFormatSpec.Json

        assertThat(spec.fields[LogComponent.TIMESTAMP]).isEqualTo(ComponentLocator.Key(name = "@timestamp"))
        assertThat(spec.fields[LogComponent.LEVEL]).isEqualTo(ComponentLocator.Key(name = "log.level"))
        // A blank box means the component is absent, which is an ordinary thing for a log to be.
        assertThat(spec.fields[LogComponent.TAG]).isNull()
    }

    @Test
    fun `a JSON description with no timestamp key is rejected at that box`() {
        val failure = compileFailure(
            input = ManualFormatInput.Json(timestampPattern = "HH:mm:ss", timestampKey = "  "),
        )

        assertThat(failure.field).isEqualTo(FormatErrorField.TIMESTAMP_FIELD)
    }

    @Test
    fun `a tab is written as an escape because it cannot be typed into a box`() {
        val spec = compile(
            input = delimited(delimiter = "\\t", timestampField = "#0"),
        ) as LogFormatSpec.Delimited

        assertThat(spec.delimiter).isEqualTo('\t')
    }

    @Test
    fun `a separator that is not one character is rejected at the separator box`() {
        val failure = compileFailure(input = delimited(delimiter = ", ", timestampField = "#0"))

        assertThat(failure.field).isEqualTo(FormatErrorField.DELIMITER)
    }

    @Test
    fun `a column named without a header row has nothing to name`() {
        val failure = compileFailure(
            input = delimited(delimiter = ",", timestampField = "timestamp", hasHeader = false),
        )

        assertThat(failure.field).isEqualTo(FormatErrorField.TIMESTAMP_FIELD)
    }

    @Test
    fun `a broken timestamp pattern is reported against the pattern, not the columns`() {
        val failure = compileFailure(input = delimited(delimiter = ",", timestampField = "#0", pattern = "nonsense"))

        assertThat(failure.field).isEqualTo(FormatErrorField.TIMESTAMP_PATTERN)
    }

    @Test
    fun `the preview says which lines read and points at the column each component comes from`() {
        val preview = previewer.preview(
            input = delimited(delimiter = ",", timestampField = "#0", levelField = "#1", messageField = "#2"),
            sampleLines = listOf("2024-01-15 10:23:45.123,INFO,connected", "  at Socket.read"),
        ) as FormatPreview.Ready

        assertThat(preview.lines.map { it.isRecord }).containsExactly(true, false).inOrder()
        val message = preview.lines.first().spans.single { it.component == LogComponent.MESSAGE }
        val line = preview.lines.first().text
        assertThat(line.substring(startIndex = message.startIndex, endIndex = message.endIndex))
            .isEqualTo("connected")
    }

    @Test
    fun `the preview points at the value of a JSON key and not at the key`() {
        val preview = previewer.preview(
            input = ManualFormatInput.Json(
                timestampPattern = "yyyy-MM-ddTHH:mm:ss.SSS",
                timestampKey = "timestamp",
                messageKey = "message",
            ),
            sampleLines = listOf("""{"timestamp":"2024-01-15T10:23:45.123","message":"connected"}"""),
        ) as FormatPreview.Ready

        val line = preview.lines.single()
        val message = line.spans.single { it.component == LogComponent.MESSAGE }
        assertThat(line.text.substring(startIndex = message.startIndex, endIndex = message.endIndex))
            .isEqualTo("connected")
    }

    @Test
    fun `a description that cannot be compiled is reported as such instead of previewing nothing`() {
        val preview = previewer.preview(
            input = delimited(delimiter = ", ", timestampField = "#0"),
            sampleLines = listOf("a,b,c"),
        )

        assertThat((preview as FormatPreview.Invalid).field).isEqualTo(FormatErrorField.DELIMITER)
    }

    private fun delimited(
        delimiter: String,
        timestampField: String,
        levelField: String = "",
        messageField: String = "",
        hasHeader: Boolean = false,
        pattern: String = "yyyy-MM-dd HH:mm:ss.SSS",
    ): ManualFormatInput.Delimited = ManualFormatInput.Delimited(
        timestampPattern = pattern,
        delimiter = delimiter,
        hasHeader = hasHeader,
        timestampField = timestampField,
        levelField = levelField,
        messageField = messageField,
    )

    private fun compile(input: ManualFormatInput): LogFormatSpec =
        (compiler.compile(input = input) as FormatCompilationResult.Success).spec

    private fun compileFailure(input: ManualFormatInput): FormatCompilationResult.Failure =
        compiler.compile(input = input) as FormatCompilationResult.Failure
}
