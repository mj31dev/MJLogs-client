package dev.mj31.logger.client.data.format.structure

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.data.format.detect.StructureFirstLogFormatDetector
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import kotlin.test.Test

class StructuredFormatDetectionTest {

    private val detector = StructureFirstLogFormatDetector()

    @Test
    fun `a file of JSON objects is recognized and its keys are located`() {
        val spec = detect(
            """{"timestamp":"2024-01-15T10:23:45.123","level":"INFO","logger":"Net","message":"connected"}""",
            """{"timestamp":"2024-01-15T10:23:46.500","level":"WARN","logger":"Net","message":"slow"}""",
        ) as LogFormatSpec.Json

        assertThat(spec.fields[LogComponent.TIMESTAMP]).isEqualTo(ComponentLocator.Key(name = "timestamp"))
        assertThat(spec.fields[LogComponent.LEVEL]).isEqualTo(ComponentLocator.Key(name = "level"))
        assertThat(spec.fields[LogComponent.TAG]).isEqualTo(ComponentLocator.Key(name = "logger"))
        assertThat(spec.fields[LogComponent.MESSAGE]).isEqualTo(ComponentLocator.Key(name = "message"))
    }

    @Test
    fun `one JSON line inside a plain text log does not drag the file into the JSON branch`() {
        assertNotStructured(
            "2024-01-15 10:23:45.123 INFO Net: connected",
            """{"payload":1}""",
            "2024-01-15 10:23:47.000 INFO Net: done",
            "2024-01-15 10:23:48.000 WARN Net: retrying",
        )
    }

    @Test
    fun `a comma separated table is read as columns rather than as prose with commas`() {
        val spec = detect(
            "timestamp,level,logger,message",
            "2024-01-15 10:23:45.123,INFO,Net,connected",
            "2024-01-15 10:23:46.500,WARN,Net,slow",
        ) as LogFormatSpec.Delimited

        assertThat(spec.delimiter).isEqualTo(',')
        assertThat(spec.hasHeader).isTrue()
        assertThat(spec.fields[LogComponent.MESSAGE]).isEqualTo(ComponentLocator.Key(name = "message"))
    }

    @Test
    fun `a table without a header is addressed by position`() {
        val spec = detect(
            "2024-01-15 10:23:45.123,INFO,connected to the gateway",
            "2024-01-15 10:23:46.500,WARN,retrying",
        ) as LogFormatSpec.Delimited

        assertThat(spec.hasHeader).isFalse()
        assertThat(spec.fields[LogComponent.TIMESTAMP]).isEqualTo(ComponentLocator.Index(position = 0))
        assertThat(spec.fields[LogComponent.LEVEL]).isEqualTo(ComponentLocator.Index(position = 1))
        assertThat(spec.fields[LogComponent.MESSAGE]).isEqualTo(ComponentLocator.Index(position = 2))
    }

    @Test
    fun `the delimiter is inferred rather than assumed`() {
        val spec = detect(
            "2024-01-15 10:23:45.123\tINFO\tconnected, then idle",
            "2024-01-15 10:23:46.500\tWARN\tslow, still waiting",
        ) as LogFormatSpec.Delimited

        // The message holds commas; only the tab splits every line into the same number of columns.
        assertThat(spec.delimiter).isEqualTo('\t')
    }

    @Test
    fun `prose whose comma count varies stays plain text`() {
        assertNotStructured(
            "2024-01-15 10:23:45.123 INFO Net: connected, retried, then settled",
            "2024-01-15 10:23:46.500 INFO Net: idle",
            "2024-01-15 10:23:47.000 INFO Net: closed, done",
        )
    }

    @Test
    fun `JSON lines after a banner are still JSON lines`() {
        val spec = detect(
            "Log exported 2024-01-15",
            "device=Pixel 8",
            """{"timestamp":"2024-01-15T10:23:45.123","level":"INFO","message":"connected"}""",
            """{"timestamp":"2024-01-15T10:23:46.500","level":"WARN","message":"slow"}""",
            """{"timestamp":"2024-01-15T10:23:47.500","level":"INFO","message":"done"}""",
        )

        assertThat(spec).isInstanceOf(LogFormatSpec.Json::class.java)
    }

    @Test
    fun `a table after a banner keeps its header row`() {
        val spec = detect(
            "# exported by LogTool",
            "timestamp,level,logger,message",
            "2024-01-15 10:23:45.123,INFO,Net,connected",
            "2024-01-15 10:23:46.500,WARN,Net,slow",
            "2024-01-15 10:23:47.500,INFO,Net,done",
        ) as LogFormatSpec.Delimited

        assertThat(spec.hasHeader).isTrue()
        assertThat(spec.fields[LogComponent.TIMESTAMP]).isEqualTo(ComponentLocator.Key(name = "timestamp"))
    }

    private fun detect(vararg lines: String): LogFormatSpec {
        val result = detector.detect(sampleLines = lines.toList())
        return (result as FormatDetectionResult.Detected).spec
    }

    /**
     * The claim these cases make is that the structural probes let the sample through, not that the
     * catalogue behind them then recognizes it. Falling through to an undetermined result — which
     * opens the format dialog — is just as much a pass as being read as a regular expression.
     */
    private fun assertNotStructured(vararg lines: String) {
        val result = detector.detect(sampleLines = lines.toList())
        val spec = (result as? FormatDetectionResult.Detected)?.spec ?: return
        assertThat(spec).isInstanceOf(LogFormatSpec.Regex::class.java)
    }
}
