package dev.mj31.logger.client.app.usecase.ingest

import dev.mj31.logger.client.app.usecase.ingest.date.ResolveStartDayUseCase
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.format.FakeLogFormatDetector
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParser
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParserFactory
import dev.mj31.logger.client.app.usecase.ingest.date.ResolveReferenceDateUseCase
import dev.mj31.logger.client.app.fake.source.FakeLogFileExpander
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.app.fake.source.FakeTextFileDataSource
import dev.mj31.logger.client.app.fake.source.FixedClock
import dev.mj31.logger.client.app.fake.source.FixedIdGenerator
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceAssembler
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.source.TextFileDataSource
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ImportLogFileWithFormatUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private fun content(lines: List<String>): TextFileContent =
        TextFileContent(path = PATH, name = FILE_NAME, lines = lines)

    private fun loader(
        dataSource: TextFileDataSource,
        parserFactory: LogLineParserFactory = ScriptedLogLineParserFactory(),
    ): LogSourceLoader = LogSourceLoader(
        dataSource = dataSource,
        assembler = LogSourceAssembler(parserFactory = parserFactory),
        idGenerator = FixedIdGenerator(ids = listOf("src-1")),
    )

    @Test
    fun `a manual format that matches produces a source with full confidence`() = runTest {
        val lines = listOf(
            ScriptedLogLineParser.recordLine(timestamp = TestLogEntries.BASE, message = "Start"),
            "\tstack frame",
        )
        val useCase = ImportLogFileWithFormatUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = content(lines = lines))),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH, spec = TestLogEntries.SPEC)

        assertThat(result is LogImportResult.Success).isTrue()
        val success = result as LogImportResult.Success
        assertThat(success.confidence).isEqualTo(1f)
        assertThat(success.source.entries.single().message).isEqualTo("Start\n\tstack frame")
    }

    @Test
    fun `a manual format matching no line fails`() = runTest {
        val useCase = ImportLogFileWithFormatUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.of(content = content(lines = listOf("header", "footer"))),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH, spec = TestLogEntries.SPEC)

        assertThat(result).isEqualTo(
            LogImportResult.Failure(path = PATH, message = "No line matched the provided format"),
        )
    }

    @Test
    fun `an uncompilable format is reported as a failure`() = runTest {
        val useCase = ImportLogFileWithFormatUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.of(content = content(lines = listOf("anything"))),
                parserFactory = ScriptedLogLineParserFactory(createFailureMessage = "Invalid timestamp pattern"),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH, spec = TestLogEntries.SPEC)

        assertThat(result).isEqualTo(LogImportResult.Failure(path = PATH, message = "Invalid timestamp pattern"))
    }

    @Test
    fun `a read error is reported as a failure`() = runTest {
        val useCase = ImportLogFileWithFormatUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.failing(path = PATH, error = IllegalStateException("Disk error")),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH, spec = TestLogEntries.SPEC)

        assertThat(result).isEqualTo(LogImportResult.Failure(path = PATH, message = "Disk error"))
    }

    @Test
    fun `a file of an unsupported type is rejected before it is read`() = runTest {
        val dataSource = FakeTextFileDataSource()
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = dataSource),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = "/media/screencast.mp4").single()

        val refusal = result as LogImportResult.UnsupportedType
        assertThat(refusal.fileName).isEqualTo("screencast.mp4")
        assertThat(refusal.message).contains(".txt")
        assertThat(dataSource.requestedPaths).isEmpty()
    }

    @Test
    fun `a manual format cannot smuggle in an unsupported type either`() = runTest {
        val dataSource = FakeTextFileDataSource()
        val useCase = ImportLogFileWithFormatUseCase(
            loader = loader(dataSource = dataSource),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = "/media/screencast.mp4", spec = TestLogEntries.SPEC)

        assertThat(result).isInstanceOf(LogImportResult.UnsupportedType::class.java)
        assertThat(dataSource.requestedPaths).isEmpty()
    }

    @Test
    fun `a log extension is accepted whatever its case`() = runTest {
        val upperCase = TextFileContent(
            path = "/logs/App.LOG",
            name = "App.LOG",
            lines = listOf(
                ScriptedLogLineParser.recordLine(
                    timestamp = TestLogEntries.at(offsetMillis = 0L),
                    level = LogLevel.INFO,
                    tag = "Network",
                    message = "connected",
                ),
            ),
        )
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = upperCase)),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = "/logs/App.LOG").single()

        assertThat(result).isInstanceOf(LogImportResult.Success::class.java)
    }

    private companion object {
        const val PATH = "/logs/app.txt"
        const val FILE_NAME = "app.txt"
    }
}
