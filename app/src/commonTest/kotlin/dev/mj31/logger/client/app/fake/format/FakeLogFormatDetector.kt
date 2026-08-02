package dev.mj31.logger.client.app.fake.format

import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.detect.LogFormatDetector
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput

/**
 * [LogFormatDetector] answering from a scripted queue, one result per imported file, and falling
 * back to [result] once the queue runs dry.
 *
 * Tests never depend on the real detection heuristics, so a change of heuristic cannot silently
 * change what the layers above are asserted to do.
 */
class FakeLogFormatDetector(
    private val result: FormatDetectionResult? = null,
) : LogFormatDetector {

    private val queuedResults = ArrayDeque<FormatDetectionResult>()
    private val mutableSamples = mutableListOf<List<String>>()

    /** Sample passed to each [detect] call, in call order. */
    val samples: List<List<String>>
        get() = mutableSamples.toList()

    val lastSampleLines: List<String>?
        get() = mutableSamples.lastOrNull()

    val detectCallCount: Int
        get() = mutableSamples.size

    fun enqueueDetected(spec: LogFormatSpec, confidence: Float = 1f) {
        queuedResults += FormatDetectionResult.Detected(spec = spec, confidence = confidence)
    }

    /** Recognized, yet unable to locate the level and the tag; the user has to confirm. */
    fun enqueueDetectedButIncomplete(spec: LogFormatSpec, confidence: Float = 0.9f) {
        queuedResults += FormatDetectionResult.Detected(
            spec = spec,
            confidence = confidence,
            missingComponents = setOf(LogComponent.LEVEL, LogComponent.TAG),
        )
    }

    fun enqueueUndetermined(
        sampleLines: List<String>,
        reason: String,
        suggestion: ManualFormatInput? = null,
    ) {
        queuedResults += FormatDetectionResult.Undetermined(
            sampleLines = sampleLines,
            reason = reason,
            suggestion = suggestion,
        )
    }

    override fun detect(sampleLines: List<String>): FormatDetectionResult {
        mutableSamples += sampleLines
        return queuedResults.removeFirstOrNull()
            ?: result
            ?: FormatDetectionResult.Undetermined(sampleLines = sampleLines, reason = NO_SCRIPTED_RESULT)
    }

    companion object {
        const val NO_SCRIPTED_RESULT: String = "No detection result was scripted"
    }
}
