package dev.mj31.logger.client.data.format.detect

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatGroups
import kotlin.test.Test
import dev.mj31.logger.client.data.format.timestamp.TimestampPatternCompiler
import dev.mj31.logger.client.data.format.line.LineFormatCompiler
import dev.mj31.logger.client.data.format.line.CompiledLineFormat

/** The catalogue is generated, so the invariants every candidate must hold are checked in bulk. */
class BuiltInLogFormatsTest {

    private val candidates = BuiltInLogFormats.candidates

    @Test
    fun `the catalogue is the cross product of the declared variants`() {
        assertThat(candidates).hasSize(BuiltInLogFormats.timestampVariants.size * BuiltInLogFormats.structureVariants.size)
    }

    @Test
    fun `every candidate compiles into an executable format`() {
        val broken = candidates.filter { runCatching { CompiledLineFormat.compile(spec = it) }.isFailure }

        assertThat(broken.map { it.name }).isEmpty()
    }

    @Test
    fun `every candidate captures a timestamp`() {
        val withoutTimestamp = candidates.filterNot {
            it.linePattern.contains(other = LineFormatCompiler.groupDeclaration(name = LogFormatGroups.TIMESTAMP))
        }

        assertThat(withoutTimestamp.map { it.name }).isEmpty()
    }

    @Test
    fun `every candidate is anchored so it cannot match in the middle of a line`() {
        val unanchored = candidates.filterNot { it.linePattern.startsWith(prefix = "^") }

        assertThat(unanchored.map { it.name }).isEmpty()
    }

    @Test
    fun `every candidate is named and marked as detected`() {
        assertThat(candidates.all { it.name.isNotBlank() }).isTrue()
        assertThat(candidates.all { it.origin == FormatOrigin.DETECTED }).isTrue()
        assertThat(candidates.map { it.name }.toSet()).hasSize(candidates.size)
    }

    @Test
    fun `richer structures come before the permissive ones`() {
        val timestampOnly = BuiltInLogFormats.structureVariants.indexOfFirst {
            !it.template.contains(other = LogFormatGroups.LEVEL) && !it.template.contains(other = LogFormatGroups.TAG)
        }
        val richest = BuiltInLogFormats.structureVariants.indexOfFirst {
            it.template.contains(other = LogFormatGroups.LEVEL) && it.template.contains(other = LogFormatGroups.TAG)
        }

        assertThat(richest).isLessThan(timestampOnly)
    }

    @Test
    fun `every timestamp variant is a compilable pattern`() {
        val broken = BuiltInLogFormats.timestampVariants.filter {
            runCatching { TimestampPatternCompiler.compile(pattern = it.pattern) }.isFailure
        }

        assertThat(broken.map { it.label }).isEmpty()
    }
}
