package dev.mj31.logger.client.app

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.data.source.LocalTextFileDataSource
import dev.mj31.logger.client.data.source.UuidIdGenerator
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.test.Test
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceAssembler
import dev.mj31.logger.client.app.usecase.ingest.LogImportResult
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileWithFormatUseCase
import dev.mj31.logger.client.app.usecase.ingest.ImportLogFileUseCase
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.data.format.line.TemplateLogFormatCompiler
import dev.mj31.logger.client.data.format.parse.RegexLogLineParserFactory
import dev.mj31.logger.client.data.format.detect.HeuristicLogFormatDetector

/**
 * End-to-end check of the import pipeline against the demo files shipped in `samples/`.
 *
 * It is the closest thing to running the application: real file reading, real detection, real
 * parsing and a real merge across three different log layouts.
 */
class SampleLogFilesIntegrationTest {

    private val samplesDirectory = File("../samples")

    private val loader = LogSourceLoader(
        dataSource = LocalTextFileDataSource(dispatcher = Dispatchers.Unconfined),
        assembler = LogSourceAssembler(parserFactory = RegexLogLineParserFactory()),
        idGenerator = UuidIdGenerator(),
        clock = Clock.System,
        timeZone = TimeZone.UTC,
    )

    private val importFile = ImportLogFileUseCase(
        loader = loader,
        detector = HeuristicLogFormatDetector(),
        dispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `every shipped sample with a standard layout is detected and parsed`() = runTest {
        val results = STANDARD_SAMPLES.map { name -> name to importFile(path = sample(name = name).path) }

        results.forEach { (name, result) ->
            assertThat(result).isInstanceOf(LogImportResult.Success::class.java)
            val success = result as LogImportResult.Success
            assertThat(success.confidence).isAtLeast(0.6f)
            assertThat(success.source.entryCount).isGreaterThan(0)
            assertThat(success.source.name).isEqualTo(name)
        }
    }

    @Test
    fun `the three samples merge into one chronological session`() = runTest {
        val sources = STANDARD_SAMPLES
            .map { name -> importFile(path = sample(name = name).path) }
            .filterIsInstance<LogImportResult.Success>()
            .map { it.source }

        val session = MergeLogSourcesUseCase()(sources = sources)

        assertThat(session.sources).hasSize(STANDARD_SAMPLES.size)
        assertThat(session.entries).hasSize(sources.sumOf { it.entryCount })
        assertThat(session.entries.map { it.timestamp }).isInOrder()
        assertThat(session.entries.map { it.sourceId }.distinct()).hasSize(STANDARD_SAMPLES.size)
        assertThat(session.timeRange).isNotNull()
    }

    @Test
    fun `stack traces are kept with the record they belong to`() = runTest {
        val result = importFile(path = sample(name = "network.txt").path) as LogImportResult.Success

        val withStackTrace = result.source.entries.filter { it.message.contains(other = "SocketTimeoutException") }
        assertThat(withStackTrace).isNotEmpty()
        assertThat(withStackTrace.all { it.level == LogLevel.ERROR }).isTrue()
        assertThat(withStackTrace.first().message.lines().size).isAtLeast(3)
    }

    @Test
    fun `levels and tags are recovered from every layout`() = runTest {
        val logcat = (importFile(path = sample(name = "device-ui.txt").path) as LogImportResult.Success).source
        val pipes = (importFile(path = sample(name = "backend-service.txt").path) as LogImportResult.Success).source

        assertThat(logcat.entries.map { it.tag }.distinct()).contains("Renderer")
        assertThat(logcat.entries.map { it.level }.distinct()).containsAtLeast(LogLevel.DEBUG, LogLevel.WARN)
        assertThat(pipes.entries.map { it.tag }.distinct()).contains("CacheStore")
        assertThat(pipes.entries.map { it.level }.distinct()).contains(LogLevel.FATAL)
    }

    @Test
    fun `the deliberately exotic sample falls back to the manual format flow`() = runTest {
        val detection = importFile(path = sample(name = "analytics-custom.txt").path)

        assertThat(detection).isInstanceOf(LogImportResult.FormatRequired::class.java)
        val request = detection as LogImportResult.FormatRequired
        assertThat(request.sampleLines).isNotEmpty()

        // The dialog pre-fills exactly this, so applying it unchanged is the expected user gesture.
        val suggestion = requireNotNull(request.suggestion)
        assertThat(suggestion.timestampPattern).isEqualTo("dd.MM.yyyy_HH.mm.ss")
        assertThat(suggestion.structureTemplate).isEqualTo("<{any}>~{timestamp}~{tag}~{message}")

        val compiled = TemplateLogFormatCompiler().compile(input = suggestion)
        assertThat(compiled).isInstanceOf(FormatCompilationResult.Success::class.java)

        val imported = ImportLogFileWithFormatUseCase(loader = loader, dispatcher = Dispatchers.Unconfined)(
            path = request.path,
            spec = (compiled as FormatCompilationResult.Success).spec,
        )

        val source = (imported as LogImportResult.Success).source
        assertThat(source.entryCount).isEqualTo(SAMPLE_ANALYTICS_LINE_COUNT)
        assertThat(source.entries.first().message).contains("event dispatched")
        // The leading counter was consumed by {any}, so the tag is the real component name.
        assertThat(source.entries.map { it.tag }.distinct()).containsExactly("ANALYTICS")
    }

    private fun sample(name: String): File {
        val file = File(samplesDirectory, name)
        check(file.isFile) { "Missing sample file ${file.absolutePath}" }
        return file
    }

    private companion object {
        val STANDARD_SAMPLES = listOf("network.txt", "device-ui.txt", "backend-service.txt")
        const val SAMPLE_ANALYTICS_LINE_COUNT = 40
    }
}
