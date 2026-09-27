package dev.mj31.logger.client.data.format.detect

import dev.mj31.logger.client.data.format.structure.DelimitedFormatProbe
import dev.mj31.logger.client.data.format.structure.JsonFormatProbe
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.detect.LogFormatDetector
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec

/**
 * Tries the structured shapes before the catalogue of regular expressions.
 *
 * Order matters rather than being a matter of taste. A row like `2024-01-15 10:23:45,INFO,Net,up`
 * also satisfies a regex candidate built around a comma, so a single ranking by matched lines would
 * sometimes read a table as prose. The structural probes answer a yes-or-no question first — is
 * every line an object, does one delimiter split every line the same way — and only a yes short
 * circuits; anything else costs one pass over the sample and falls through untouched.
 *
 * Strictness would also refuse any file that opens with a few lines about itself, since those are
 * neither objects nor rows. So when the whole sample fails, the probes are asked again with the
 * leading lines set aside, a line at a time up to [HeuristicLogFormatDetector.MAX_PREAMBLE_LINES];
 * the first offset at which a shape holds is where the preamble ends. The whole sample is always
 * tried first, so a file that was recognized before is recognized the same way now.
 */
class StructureFirstLogFormatDetector(
    private val fallback: LogFormatDetector = HeuristicLogFormatDetector(),
) : LogFormatDetector {

    override fun detect(sampleLines: List<String>): FormatDetectionResult {
        val probes = sampleLines.filter { it.isNotBlank() }.take(n = MAX_PROBES)
        if (probes.size < MIN_PROBES) return fallback.detect(sampleLines = sampleLines)

        val structured = (0..maxPreamble(probes = probes)).firstNotNullOfOrNull { preamble ->
            probe(probes = probes.drop(n = preamble))
        }
        return structured?.let { spec ->
            FormatDetectionResult.Detected(
                spec = spec,
                confidence = 1f,
                missingComponents = missingOf(spec = spec),
            )
        } ?: fallback.detect(sampleLines = sampleLines)
    }

    private fun probe(probes: List<String>): LogFormatSpec? =
        JsonFormatProbe.probe(probes = probes) ?: DelimitedFormatProbe.probe(probes = probes)

    /** Leaves at least [MIN_PROBES] lines for the shape to be judged on. */
    private fun maxPreamble(probes: List<String>): Int =
        minOf(HeuristicLogFormatDetector.MAX_PREAMBLE_LINES, probes.size - MIN_PROBES)

    /**
     * Reports what the record does not name, so the user is asked the same question a plain text log
     * raises. Knowing every key of an object is not the same as knowing that none of them is a level:
     * a logger that writes `sev` instead of `level` is exactly the case the confirmation exists for.
     */
    private fun missingOf(spec: LogFormatSpec): Set<LogComponent> {
        val fields = when (spec) {
            is LogFormatSpec.Json -> spec.fields
            is LogFormatSpec.Delimited -> spec.fields
            is LogFormatSpec.Regex -> return emptySet()
        }
        return setOf(LogComponent.LEVEL, LogComponent.TAG).filterTo(destination = mutableSetOf()) { fields[it] == null }
    }

    private companion object {
        const val MAX_PROBES = 200

        /** One line agreeing with a shape proves nothing; the strictness needs something to be strict about. */
        const val MIN_PROBES = 2
    }
}
