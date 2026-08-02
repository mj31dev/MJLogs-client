package dev.mj31.logger.client.data.format.detect

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertIs
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.data.format.parse.RegexLogLineParserFactory

class HeuristicLogFormatDetectorTest {

    private val detector = HeuristicLogFormatDetector()
    private val factory = RegexLogLineParserFactory()

    @Test
    fun `detects an ISO-8601 format with level and tag`() {
        val lines = listOf(
            "2024-01-15T10:23:45.123+03:00 INFO [Network] Connected to server",
            "2024-01-15T10:23:46.001+03:00 DEBUG [Cache] Cache hit for key user-1",
            "2024-01-15T10:23:47.500+03:00 WARN [Network] Retrying request",
            "2024-01-15T10:23:48.900+03:00 ERROR [Network] Request failed",
        )

        val spec = detected(lines = lines, minimumConfidence = 1.0f)
        val record = firstRecord(spec = spec, line = lines[0])

        assertThat(record.level).isEqualTo(LogLevel.INFO)
        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("Connected to server")
    }

    @Test
    fun `detects a plain date time format with a bracketed tag`() {
        val lines = listOf(
            "2024-01-15 10:23:45.123 INFO  [MainActivity] Application started",
            "2024-01-15 10:23:45.456 DEBUG [MainActivity] Restoring saved state",
            "2024-01-15 10:23:46.001 WARN  [SyncWorker] Sync postponed",
            "2024-01-15 10:23:47.220 ERROR [SyncWorker] Sync failed",
        )

        val spec = detected(lines = lines, minimumConfidence = 1.0f)
        val record = firstRecord(spec = spec, line = lines[0])

        assertThat(record.level).isEqualTo(LogLevel.INFO)
        assertThat(record.tag).isEqualTo("MainActivity")
        assertThat(record.message).isEqualTo("Application started")
    }

    @Test
    fun `detects the Android logcat brief format`() {
        val lines = listOf(
            "01-15 10:23:45.123 D/Network( 1234): Connected to server",
            "01-15 10:23:45.456 I/MainActivity( 1234): Application started",
            "01-15 10:23:46.001 W/SyncWorker( 1234): Sync postponed",
            "01-15 10:23:47.220 E/SyncWorker( 1234): Sync failed",
        )

        val spec = detected(lines = lines, minimumConfidence = 1.0f)
        val record = firstRecord(spec = spec, line = lines[0])

        assertThat(record.level).isEqualTo(LogLevel.DEBUG)
        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("Connected to server")
    }

    @Test
    fun `detects the Android logcat thread time format`() {
        val lines = listOf(
            "01-15 10:23:45.123  1234  1300 D Network: Connected to server",
            "01-15 10:23:45.456  1234  1234 I MainActivity: Application started",
            "01-15 10:23:46.001  1234  1300 W SyncWorker: Sync postponed",
            "01-15 10:23:47.220  1234  1300 E SyncWorker: Sync failed",
        )

        val spec = detected(lines = lines, minimumConfidence = 1.0f)
        val record = firstRecord(spec = spec, line = lines[0])

        assertThat(record.level).isEqualTo(LogLevel.DEBUG)
        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("Connected to server")
    }

    @Test
    fun `detects a pipe separated format`() {
        val lines = listOf(
            "2024-01-15 10:23:45.123 | INFO | Network | Connected to server",
            "2024-01-15 10:23:45.456 | DEBUG | Cache | Cache hit",
            "2024-01-15 10:23:46.001 | WARN | Network | Retrying request",
            "2024-01-15 10:23:47.220 | ERROR | Network | Request failed",
        )

        val spec = detected(lines = lines, minimumConfidence = 1.0f)
        val record = firstRecord(spec = spec, line = lines[0])

        assertThat(record.level).isEqualTo(LogLevel.INFO)
        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("Connected to server")
    }

    @Test
    fun `detects a bracketed time only format`() {
        val lines = listOf(
            "[10:23:45] Application started",
            "[10:23:46] Connected to server",
            "[10:23:47] Cache warmed up",
            "[10:23:48] Ready",
        )

        val spec = detected(lines = lines, minimumConfidence = 1.0f)
        val record = firstRecord(spec = spec, line = lines[0])

        assertThat(record.message).isEqualTo("Application started")
    }

    @Test
    fun `detects an epoch millis format`() {
        val lines = listOf(
            "1705314225123 INFO Network Connected to server",
            "1705314226001 DEBUG Cache Cache hit",
            "1705314227500 WARN Network Retrying request",
            "1705314228900 ERROR Network Request failed",
        )

        val spec = detected(lines = lines, minimumConfidence = 1.0f)
        val record = firstRecord(spec = spec, line = lines[0])

        assertThat(spec.timestampPattern).isEqualTo("epochMillis")
        assertThat(record.level).isEqualTo(LogLevel.INFO)
        assertThat(record.tag).isEqualTo("Network")
        assertThat(record.message).isEqualTo("Connected to server")
    }

    @Test
    fun `ignores stack trace lines while detecting`() {
        val lines = listOf(
            "2024-01-15 10:23:45.123 INFO  [MainActivity] Application started",
            "2024-01-15 10:23:45.456 ERROR [SyncWorker] Sync failed",
            "java.lang.IllegalStateException: broken",
            "\tat com.example.SyncWorker.run(SyncWorker.kt:42)",
            "    at com.example.Runner.execute(Runner.kt:11)",
            "Caused by: java.io.IOException: closed",
            "2024-01-15 10:23:46.001 WARN  [SyncWorker] Retrying",
            "2024-01-15 10:23:47.220 INFO  [SyncWorker] Recovered",
        )

        val result = detector.detect(sampleLines = lines)

        val detected = assertIs<FormatDetectionResult.Detected>(result)
        assertThat(detected.confidence).isGreaterThan(0.7f)
    }

    @Test
    fun `returns undetermined for free form text`() {
        val lines = listOf(
            "Lorem ipsum dolor sit amet",
            "consectetur adipiscing elit",
            "sed do eiusmod tempor incididunt",
            "ut labore et dolore magna aliqua",
        )

        val result = detector.detect(sampleLines = lines)

        val undetermined = assertIs<FormatDetectionResult.Undetermined>(result)
        assertThat(undetermined.sampleLines).hasSize(4)
        assertThat(undetermined.reason).contains("confidence")
    }

    @Test
    fun `returns undetermined when too few lines could start a record`() {
        val result = detector.detect(sampleLines = listOf("2024-01-15 10:23:45.123 INFO [A] one", "   indented"))

        val undetermined = assertIs<FormatDetectionResult.Undetermined>(result)
        assertThat(undetermined.reason).contains("at least")
    }

    @Test
    fun `limits the preview to eight lines`() {
        val lines = List(size = 20) { index -> "free form line number $index" }

        val undetermined = assertIs<FormatDetectionResult.Undetermined>(detector.detect(sampleLines = lines))

        assertThat(undetermined.sampleLines).hasSize(HeuristicLogFormatDetector.MAX_PREVIEW_LINES)
    }

    @Test
    fun `scores a partially matching sample below one`() {
        val lines = listOf(
            "2024-01-15 10:23:45.123 INFO  [MainActivity] Application started",
            "2024-01-15 10:23:45.456 DEBUG [MainActivity] Restoring state",
            "2024-01-15 10:23:46.001 WARN  [SyncWorker] Sync postponed",
            "totally unrelated line",
        )

        val detected = assertIs<FormatDetectionResult.Detected>(detector.detect(sampleLines = lines))

        assertThat(detected.confidence).isWithin(TOLERANCE).of(0.75f)
    }

    @Test
    fun `handles a large sample quickly`() {
        val lines = List(size = 500) { index ->
            "2024-01-15 10:23:45.${(index % 1000).toString().padStart(length = 3, padChar = '0')} INFO [Bench] line $index"
        }

        val detected = assertIs<FormatDetectionResult.Detected>(detector.detect(sampleLines = lines))

        assertThat(detected.confidence).isWithin(TOLERANCE).of(1.0f)
    }

    private fun detected(lines: List<String>, minimumConfidence: Float): LogFormatSpec {
        val result = detector.detect(sampleLines = lines)
        val detected = assertIs<FormatDetectionResult.Detected>(result)
        assertThat(detected.confidence).isAtLeast(minimumConfidence)
        return detected.spec
    }

    private fun firstRecord(spec: LogFormatSpec, line: String): ParsedLine.Record {
        val parser = factory.create(spec = spec, referenceDate = REFERENCE_DATE)
        return assertIs<ParsedLine.Record>(parser.parse(line = line))
    }

    @Test
    fun `reads a bracketed level with no tag`() {
        val lines = listOf(
            "2024-01-15 10:23:45.053 [Info] > exporter started, version 2.4.0",
            "2024-01-15 10:23:45.054 [Info] > writing to /var/log/exporter",
            "2024-01-15 10:23:46.101 [Debug] > flush finished in 48 ms",
            "2024-01-15 10:23:47.500 [Info] > 12 batches queued",
        )

        val spec = assertIs<FormatDetectionResult.Detected>(detector.detect(sampleLines = lines)).spec
        val records = lines.map { line -> firstRecord(spec = spec, line = line) }

        assertThat(records.map { it.level })
            .containsExactly(LogLevel.INFO, LogLevel.INFO, LogLevel.DEBUG, LogLevel.INFO)
            .inOrder()
        assertThat(records.first().message).isEqualTo("exporter started, version 2.4.0")
    }

    @Test
    fun `reads a bracketed level followed by a bracketed origin`() {
        val lines = listOf(
            "2024-01-15 10:23:45.261 [Debug] [UploadWorker.kt] enqueue() > batch of 12 queued",
            "2024-01-15 10:23:45.900 [Info] [CacheStore.kt] warmUp() > 40 entries restored",
            "2024-01-15 10:23:46.100 [Debug] [UploadWorker.kt] enqueue() > retrying once",
        )

        val spec = assertIs<FormatDetectionResult.Detected>(detector.detect(sampleLines = lines)).spec
        val record = firstRecord(spec = spec, line = lines.first())

        assertThat(record.level).isEqualTo(LogLevel.DEBUG)
        assertThat(record.tag).isEqualTo("UploadWorker.kt")
        assertThat(record.message).startsWith("enqueue()")
    }

    @Test
    fun `reads a timestamp whose milliseconds are separated by a colon`() {
        val lines = listOf(
            "2024/01/15 18:50:07:267  connecting to the sync endpoint",
            "2024/01/15 18:50:07:273  handshake completed",
            "2024/01/15 18:50:07:451  first page received",
        )

        val spec = assertIs<FormatDetectionResult.Detected>(detector.detect(sampleLines = lines)).spec
        val record = firstRecord(spec = spec, line = lines.first())

        assertThat(spec.timestampPattern).isEqualTo("yyyy/MM/dd HH:mm:ss:SSS")
        assertThat(record.message).isEqualTo("connecting to the sync endpoint")
    }

    @Test
    fun `a payload dumped under a record does not break its detection`() {
        val lines = listOf(
            "2024/01/15 18:50:07:267  request headers:",
            "{",
            "    \"content-type\" = \"application/json\";",
            "}",
            "2024/01/15 18:50:07:451  response received",
            "2024/01/15 18:50:07:900  done",
        )

        val result = assertIs<FormatDetectionResult.Detected>(detector.detect(sampleLines = lines))

        assertThat(result.spec.timestampPattern).isEqualTo("yyyy/MM/dd HH:mm:ss:SSS")
    }

    private companion object {
        const val TOLERANCE = 0.001f
        val REFERENCE_DATE = LocalDate(year = 2024, monthNumber = 1, dayOfMonth = 15)
    }
    @Test
    fun `an unrecognized layout comes with a verified suggestion`() {
        val lines = listOf(
            "<0000>~01.08.2026_10.23.45~ANALYTICS~event dispatched (0)",
            "<0001>~01.08.2026_10.23.46~ANALYTICS~event dispatched (1)",
            "<0002>~01.08.2026_10.23.47~ANALYTICS~event dispatched (2)",
            "<0003>~01.08.2026_10.23.48~ANALYTICS~event dispatched (3)",
        )

        val result = assertIs<FormatDetectionResult.Undetermined>(detector.detect(sampleLines = lines))

        val suggestion = requireNotNull(result.suggestion)
        assertThat(suggestion.timestampPattern).isEqualTo("dd.MM.yyyy_HH.mm.ss")
        assertThat(suggestion.structureTemplate).isEqualTo("<{any}>~{timestamp}~{tag}~{message}")
    }

    @Test
    fun `free text without any timestamp gets no suggestion`() {
        val result = assertIs<FormatDetectionResult.Undetermined>(
            detector.detect(
                sampleLines = listOf("starting exporter", "everything is fine", "shutting down"),
            ),
        )

        assertThat(result.suggestion).isNull()
    }

}
