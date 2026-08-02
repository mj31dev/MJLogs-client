package dev.mj31.logger.client.app.fake.format

import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput

/** [LogFormatCompiler] returning a scripted result while recording what the dialog submitted. */
class FakeLogFormatCompiler(
    var result: FormatCompilationResult,
) : LogFormatCompiler {

    private val mutableInputs = mutableListOf<ManualFormatInput>()

    val inputs: List<ManualFormatInput>
        get() = mutableInputs.toList()

    override fun compile(input: ManualFormatInput): FormatCompilationResult {
        mutableInputs += input
        return result
    }
}
