package dev.mj31.logger.client.domain.format.detect

/** Recognizes the layout of a log file from a sample of its lines. */
interface LogFormatDetector {
    fun detect(sampleLines: List<String>): FormatDetectionResult
}
