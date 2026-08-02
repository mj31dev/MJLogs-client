package dev.mj31.logger.client.data.format.detect

import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.compile.LogFormatCompiler
import dev.mj31.logger.client.domain.format.detect.LogFormatDetector
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import kotlinx.datetime.LocalDate
import dev.mj31.logger.client.data.format.line.TemplateLogFormatCompiler
import dev.mj31.logger.client.data.format.parse.RegexLogLineParser
import dev.mj31.logger.client.data.format.line.CompiledLineFormat

/**
 * Recognizes a log layout by running every candidate of [BuiltInLogFormats] over a sample of lines.
 *
 * The confidence of a candidate is the ratio of sample lines it turns into a record; obvious
 * continuation lines (stack frames and indented text) are excluded from the denominator because no
 * format is expected to match them. Candidate regexes are compiled lazily and cached for the lifetime
 * of the detector, so repeated detections only pay the matching cost.
 */
class HeuristicLogFormatDetector(
    private val candidates: List<LogFormatSpec> = BuiltInLogFormats.candidates,
    private val referenceDate: LocalDate = DEFAULT_REFERENCE_DATE,
    private val guesser: LogFormatGuesser = LogFormatGuesser(),
    private val formatCompiler: LogFormatCompiler = TemplateLogFormatCompiler(),
) : LogFormatDetector {

    private val compiledCandidates: List<Lazy<CompiledLineFormat?>> = candidates.map { spec ->
        lazy { runCatching { CompiledLineFormat.compile(spec = spec) }.getOrNull() }
    }

    override fun detect(sampleLines: List<String>): FormatDetectionResult {
        val sample = sampleLines.filter { it.isNotBlank() }.take(n = MAX_SAMPLE_LINES)
        val probes = sample.filterNot { isContinuation(line = it) }
        if (probes.size < MIN_MATCHED_LINES) {
            return undetermined(sample = sample, reason = tooFewLinesReason(count = probes.size))
        }
        val best = bestCandidate(probes = probes)
        return when {
            best == null -> undetermined(sample = sample, reason = NO_CANDIDATE_REASON)
            best.confidence >= MIN_CONFIDENCE && best.matched >= MIN_MATCHED_LINES -> detected(
                score = best,
                sample = sample,
            )

            else -> undetermined(sample = sample, reason = lowConfidenceReason(score = best))
        }
    }

    private fun detected(score: CandidateScore, sample: List<String>): FormatDetectionResult.Detected {
        val missing = buildSet {
            if (!score.capturesLevel) add(element = LogComponent.LEVEL)
            if (!score.capturesTag) add(element = LogComponent.TAG)
        }
        return FormatDetectionResult.Detected(
            spec = score.spec,
            confidence = score.confidence,
            missingComponents = missing,
            // Only worth computing when the user will be asked anyway.
            suggestion = if (missing.isEmpty()) null else validatedSuggestion(sample = sample),
        )
    }

    private fun bestCandidate(probes: List<String>): CandidateScore? = compiledCandidates
        .asSequence()
        .mapNotNull { it.value }
        .map { format -> scoreOf(format = format, probes = probes) }
        .filter { it.matched > 0 }
        .reduceOrNull { best, score -> if (score.isBetterThan(other = best)) score else best }

    private fun scoreOf(format: CompiledLineFormat, probes: List<String>): CandidateScore {
        val matched = countMatches(format = format, probes = probes)
        return CandidateScore(
            spec = format.spec,
            matched = matched,
            confidence = matched.toFloat() / probes.size,
            capturedComponents = format.capturedComponents,
            capturesLevel = format.hasLevelGroup,
            capturesTag = format.hasTagGroup,
        )
    }

    private fun countMatches(format: CompiledLineFormat, probes: List<String>): Int {
        val parser = RegexLogLineParser(format = format, referenceDate = referenceDate)
        return probes.count { parser.parse(line = it) is ParsedLine.Record }
    }

    private fun undetermined(sample: List<String>, reason: String): FormatDetectionResult.Undetermined =
        FormatDetectionResult.Undetermined(
            sampleLines = sample.take(n = MAX_PREVIEW_LINES),
            reason = reason,
            suggestion = validatedSuggestion(sample = sample),
        )

    /**
     * Infers a description of these very lines and keeps it only if it actually parses them.
     *
     * Pre-filling the dialog with a broken guess would cost the user more time than the neutral
     * default, so an unverified suggestion is never proposed.
     */
    private fun validatedSuggestion(sample: List<String>): ManualFormatInput? {
        val guess = guesser.guess(sampleLines = sample) ?: return null
        val compiled = formatCompiler.compile(input = guess) as? FormatCompilationResult.Success ?: return null
        val format = runCatching { CompiledLineFormat.compile(spec = compiled.spec) }.getOrNull() ?: return null
        val probes = sample.filterNot { isContinuation(line = it) }
        if (probes.isEmpty()) return null
        val parser = RegexLogLineParser(format = format, referenceDate = referenceDate)
        val matched = probes.count { parser.parse(line = it) is ParsedLine.Record }
        return if (matched >= probes.size * MIN_CONFIDENCE) guess else null
    }

    private fun tooFewLinesReason(count: Int): String =
        "The sample contains only $count line(s) that could start a record; at least $MIN_MATCHED_LINES are required. " +
            "Please describe the timestamp pattern and the line structure manually."

    private fun lowConfidenceReason(score: CandidateScore): String =
        "No built-in log format matched enough lines: the best candidate '${score.spec.name}' reached " +
            "${percentOf(confidence = score.confidence)}% confidence (${score.matched} line(s)), " +
            "while ${percentOf(confidence = MIN_CONFIDENCE)}% is required. " +
            "Please describe the timestamp pattern and the line structure manually."

    private fun percentOf(confidence: Float): Int = (confidence * PERCENT_SCALE).toInt()

    private fun isContinuation(line: String): Boolean =
        line.firstOrNull()?.isWhitespace() == true || CONTINUATION_PATTERN.containsMatchIn(input = line)

    private data class CandidateScore(
        val spec: LogFormatSpec,
        val matched: Int,
        val confidence: Float,
        val capturedComponents: Int,
        val capturesLevel: Boolean = false,
        val capturesTag: Boolean = false,
    ) {

        /** Higher confidence wins; ties go to the candidate capturing more components, then to the earlier one. */
        fun isBetterThan(other: CandidateScore): Boolean = when {
            confidence != other.confidence -> confidence > other.confidence
            else -> capturedComponents > other.capturedComponents
        }
    }

    companion object {

        /** Date used to complete timestamps while probing; detection only cares about matching, not absolute time. */
        val DEFAULT_REFERENCE_DATE: LocalDate = LocalDate(year = 1970, monthNumber = 1, dayOfMonth = 1)

        const val MIN_CONFIDENCE: Float = 0.6f
        const val MIN_MATCHED_LINES: Int = 3
        const val MAX_SAMPLE_LINES: Int = 200
        const val MAX_PREVIEW_LINES: Int = 8

        private const val PERCENT_SCALE = 100
        private const val NO_CANDIDATE_REASON =
            "No built-in log format matched any line of the sample: the best confidence was 0%. " +
                "Please describe the timestamp pattern and the line structure manually."
        private val CONTINUATION_PATTERN = Regex(pattern = "^\\s*(at |Caused by:|\\.\\.\\.\\s|\\t)")
    }
}
