package dev.mj31.logger.client.domain.format.detect

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.LogComponent

/** Outcome of automatic format recognition over a sample of log lines. */
sealed interface FormatDetectionResult {

    /**
     * A candidate format matched enough sample lines. [confidence] is the matched line ratio in `0..1`.
     *
     * [missingComponents] lists what the winning candidate does not capture: a log without a level
     * column parses fine, but the user should get the chance to say whether that is really the case.
     */
    data class Detected(
        val spec: LogFormatSpec,
        val confidence: Float,
        val missingComponents: Set<LogComponent> = emptySet(),
        val suggestion: ManualFormatInput? = null,
    ) : FormatDetectionResult

    /**
     * No candidate reached the confidence threshold: the UI must ask the user for the timestamp
     * pattern and the line structure, showing [sampleLines].
     *
     * [suggestion] is the most plausible description inferred from those very lines; it is meant to
     * pre-fill the dialog so the user usually only has to confirm it. It is `null` when even a guess
     * could not be produced.
     */
    data class Undetermined(
        val sampleLines: List<String>,
        val reason: String,
        val suggestion: ManualFormatInput? = null,
    ) : FormatDetectionResult
}
