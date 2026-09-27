package dev.mj31.logger.client.app.usecase.ingest

import dev.mj31.logger.client.domain.model.time.ZoneOrigin
import dev.mj31.logger.client.domain.model.time.SourceZone
import dev.mj31.logger.client.app.usecase.ingest.date.ResolveStartDayUseCase
import dev.mj31.logger.client.domain.source.archive.ExpandedLogFile
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
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.detect.FormatDetectionResult
import dev.mj31.logger.client.domain.format.parse.LogLineParserFactory
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.source.TextFileDataSource
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ImportLogFileUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `a format without a level asks for confirmation instead of importing silently`() = runTest {
        val lines = listOf(
            recordLine(offsetMillis = 0L, message = "started"),
            recordLine(offsetMillis = 1_000L, message = "stopped"),
        )
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = content(lines = lines))),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(
                    spec = TestLogEntries.SPEC,
                    confidence = 0.9f,
                    missingComponents = setOf(LogComponent.LEVEL, LogComponent.TAG),
                ),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH).single()

        val confirmation = result as LogImportResult.NeedsConfirmation
        assertThat(confirmation.missing).containsExactly(LogComponent.LEVEL, LogComponent.TAG)
        assertThat(confirmation.reason).contains("level")
        // The file is already parsed: confirming must not cost a second read.
        assertThat(confirmation.source.entryCount).isEqualTo(lines.size)
        assertThat(confirmation.sampleLines).isNotEmpty()
    }

    @Test
    fun `a format that captures everything is imported straight away`() = runTest {
        val lines = listOf(recordLine(offsetMillis = 0L, message = "started"))
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = content(lines = lines))),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        assertThat(useCase(path = PATH).single()).isInstanceOf(LogImportResult.Success::class.java)
    }

    private fun content(lines: List<String>): TextFileContent =
        TextFileContent(path = PATH, name = FILE_NAME, lines = lines)

    private fun loader(
        dataSource: TextFileDataSource,
        parserFactory: LogLineParserFactory = ScriptedLogLineParserFactory(),
        idGenerator: FixedIdGenerator = FixedIdGenerator(ids = listOf("src-1")),
    ): LogSourceLoader = LogSourceLoader(
        dataSource = dataSource,
        assembler = LogSourceAssembler(parserFactory = parserFactory),
        idGenerator = idGenerator,
    )

    private fun recordLine(offsetMillis: Long, message: String): String =
        ScriptedLogLineParser.recordLine(
            timestamp = TestLogEntries.at(offsetMillis = offsetMillis),
            level = LogLevel.INFO,
            tag = "Network",
            message = message,
        )

    @Test
    fun `a detected format produces a source with the detection confidence`() = runTest {
        val lines = listOf(
            recordLine(offsetMillis = 0L, message = "Start"),
            recordLine(offsetMillis = 1_000L, message = "Stop"),
        )
        val detector = FakeLogFormatDetector(
            result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 0.75f),
        )
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = content(lines = lines))),
            detector = detector,
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH).single()

        assertThat(result is LogImportResult.Success).isTrue()
        val success = result as LogImportResult.Success
        assertThat(success.confidence).isEqualTo(0.75f)
        assertThat(success.source.id).isEqualTo("src-1")
        assertThat(success.source.name).isEqualTo(FILE_NAME)
        assertThat(success.source.path).isEqualTo(PATH)
        assertThat(success.source.format).isEqualTo(TestLogEntries.SPEC)
        assertThat(success.source.entries.map { it.message }).containsExactly("Start", "Stop").inOrder()
    }

    @Test
    fun `the parser is created with the day the file was written, not the day it is opened`() = runTest {
        val factory = ScriptedLogLineParserFactory()
        val detector = FakeLogFormatDetector(
            result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
        )
        val idGenerator = FixedIdGenerator(ids = listOf("src-1"))
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.of(
                    content = content(lines = listOf(recordLine(offsetMillis = 0L, message = "Start"))),
                ),
                parserFactory = factory,
                idGenerator = idGenerator,
            ),
            detector = detector,
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        useCase(path = PATH).single()

        // The modification time of the file decides the day, so the same file always parses the same
        // way. It used to be whatever day the import happened to run on.
        assertThat(factory.lastReferenceDate).isEqualTo(LocalDate(year = 2024, monthNumber = 1, dayOfMonth = 15))
        assertThat(idGenerator.requestedPrefixes).containsExactly("src")
    }

    @Test
    fun `a date written into the file name outranks the modification time`() = runTest {
        val factory = ScriptedLogLineParserFactory()
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.of(
                    content = TextFileContent(
                        path = "/logs/app.log.2023-11-04",
                        name = "app.log.2023-11-04",
                        lines = listOf(recordLine(offsetMillis = 0L, message = "Start")),
                    ),
                ),
                parserFactory = factory,
            ),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        useCase(path = "/logs/app.log.2023-11-04").single()

        assertThat(factory.lastReferenceDate).isEqualTo(LocalDate(year = 2023, monthNumber = 11, dayOfMonth = 4))
    }

    @Test
    fun `an archive is imported as one source per entry it holds`() = runTest {
        val first = TextFileContent(path = "/tmp/x/a.log", name = "a.log", lines = listOf(recordLine(offsetMillis = 0L, message = "A")))
        val second = TextFileContent(path = "/tmp/x/b.log", name = "b.log", lines = listOf(recordLine(offsetMillis = 0L, message = "B")))
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource().apply {
                    register(content = first)
                    register(content = second)
                },
                idGenerator = FixedIdGenerator(ids = listOf("src-1", "src-2")),
            ),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(
                entries = mapOf(
                    "/logs/bundle.zip" to listOf(
                        ExpandedLogFile(
                            path = first.path,
                            displayName = first.name,
                            modifiedAt = FakeLogFileExpander.DEFAULT_MODIFICATION,
                        ),
                        ExpandedLogFile(
                            path = second.path,
                            displayName = second.name,
                            modifiedAt = FakeLogFileExpander.DEFAULT_MODIFICATION,
                        ),
                    ),
                ),
            ),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val results = useCase(path = "/logs/bundle.zip")

        assertThat(results).hasSize(2)
        assertThat(results.filterIsInstance<LogImportResult.Success>().map { it.source.name })
            .containsExactly("a.log", "b.log")
    }

    @Test
    fun `only non blank lines are handed to the detector`() = runTest {
        val detector = FakeLogFormatDetector(
            result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
        )
        val lines = listOf("", recordLine(offsetMillis = 0L, message = "Start"), "   ")
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = content(lines = lines))),
            detector = detector,
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        useCase(path = PATH).single()

        assertThat(detector.detectCallCount).isEqualTo(1)
        assertThat(detector.lastSampleLines).containsExactly(recordLine(offsetMillis = 0L, message = "Start"))
    }

    @Test
    fun `an undetermined detection asks the user for a format and forwards the sample`() = runTest {
        val sample = listOf("weird line 1", "weird line 2")
        val detector = FakeLogFormatDetector(
            result = FormatDetectionResult.Undetermined(sampleLines = sample, reason = "No candidate matched"),
        )
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = content(lines = sample))),
            detector = detector,
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH).single()

        assertThat(result).isEqualTo(
            LogImportResult.FormatRequired(
                path = PATH,
                fileName = FILE_NAME,
                sampleLines = sample,
                reason = "No candidate matched",
            ),
        )
    }

    @Test
    fun `a detected format producing no record asks the user for a format`() = runTest {
        val lines = List(size = 10) { index -> "unparseable line $index" }
        val detector = FakeLogFormatDetector(
            result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 0.9f),
        )
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = content(lines = lines))),
            detector = detector,
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH).single()

        assertThat(result).isEqualTo(
            LogImportResult.FormatRequired(
                path = PATH,
                fileName = FILE_NAME,
                sampleLines = lines.take(n = 8),
                reason = "Detected format produced no records",
            ),
        )
    }

    @Test
    fun `an empty file fails before detection runs`() = runTest {
        val detector = FakeLogFormatDetector(
            result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
        )
        val useCase = ImportLogFileUseCase(
            loader = loader(dataSource = FakeTextFileDataSource.of(content = content(lines = listOf("", "   ")))),
            detector = detector,
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH).single()

        assertThat(result).isEqualTo(LogImportResult.Failure(path = PATH, message = "File contains no log lines"))
        assertThat(detector.detectCallCount).isEqualTo(0)
    }

    @Test
    fun `a read error is reported as a failure carrying the error message`() = runTest {
        val detector = FakeLogFormatDetector(
            result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
        )
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.failing(
                    path = PATH,
                    error = IllegalStateException("Permission denied"),
                ),
            ),
            detector = detector,
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH).single()

        assertThat(result).isEqualTo(LogImportResult.Failure(path = PATH, message = "Permission denied"))
    }

    @Test
    fun `a read error without a message falls back to a generic failure text`() = runTest {
        val detector = FakeLogFormatDetector(
            result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
        )
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.failing(path = PATH, error = IllegalStateException()),
            ),
            detector = detector,
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val result = useCase(path = PATH).single()

        assertThat(result).isEqualTo(LogImportResult.Failure(path = PATH, message = "Unable to read file"))
    }

    @Test
    fun `a date stated in the preamble outranks the file name and the modification time`() = runTest {
        val factory = ScriptedLogLineParserFactory()
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.of(
                    content = TextFileContent(
                        path = "/logs/app.log.2023-11-04",
                        name = "app.log.2023-11-04",
                        lines = listOf(
                            "Device: Pixel 8",
                            "Log started 2022-02-03 10:00",
                            recordLine(offsetMillis = 0L, message = "Start"),
                        ),
                    ),
                ),
                parserFactory = factory,
                idGenerator = FixedIdGenerator(ids = listOf("src-1", "src-2")),
            ),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val success = useCase(path = "/logs/app.log.2023-11-04").single() as LogImportResult.Success

        assertThat(factory.lastReferenceDate).isEqualTo(LocalDate(year = 2022, monthNumber = 2, dayOfMonth = 3))
        assertThat(success.source.referenceDate).isEqualTo(LocalDate(year = 2022, monthNumber = 2, dayOfMonth = 3))
        assertThat(success.source.preamble).containsExactly("Device: Pixel 8", "Log started 2022-02-03 10:00").inOrder()
    }

    @Test
    fun `a file that dates itself in its preamble imports without a dated name or a modification time`() = runTest {
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.of(
                    content = content(lines = listOf("started 01.08.2026", recordLine(offsetMillis = 0L, message = "Start"))),
                ),
                idGenerator = FixedIdGenerator(ids = listOf("src-1", "src-2")),
            ),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(modifiedAt = null),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val success = useCase(path = PATH).single() as LogImportResult.Success

        assertThat(success.source.referenceDate).isEqualTo(LocalDate(year = 2026, monthNumber = 8, dayOfMonth = 1))
    }

    @Test
    fun `a zone named in the preamble is the zone the file is read in`() = runTest {
        val factory = ScriptedLogLineParserFactory()
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.of(
                    content = content(lines = listOf("TZ: Europe/Berlin", recordLine(offsetMillis = 0L, message = "Start"))),
                ),
                parserFactory = factory,
            ),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val success = useCase(path = PATH).single() as LogImportResult.Success

        assertThat(success.source.zone).isEqualTo(SourceZone(id = "Europe/Berlin", origin = ZoneOrigin.HEADER))
        assertThat(factory.createdSpecs.last().zoneId).isEqualTo("Europe/Berlin")
        assertThat(success.source.format).isEqualTo(TestLogEntries.SPEC)
    }

    @Test
    fun `a file with no date anywhere is still refused`() = runTest {
        val useCase = ImportLogFileUseCase(
            loader = loader(
                dataSource = FakeTextFileDataSource.of(
                    content = content(lines = listOf(recordLine(offsetMillis = 0L, message = "Start"))),
                ),
            ),
            detector = FakeLogFormatDetector(
                result = FormatDetectionResult.Detected(spec = TestLogEntries.SPEC, confidence = 1f),
            ),
            expander = FakeLogFileExpander(modifiedAt = null),
            resolveReferenceDate = ResolveReferenceDateUseCase(timeZone = TimeZone.UTC),
            resolveStartDay = ResolveStartDayUseCase(timeZone = TimeZone.UTC),
            dispatcher = dispatcher,
        )

        val failure = useCase(path = PATH).single() as LogImportResult.Failure

        assertThat(failure.message).contains("could not be determined")
    }

    private companion object {
        const val PATH = "/logs/app.txt"
        const val FILE_NAME = "app.txt"
    }
}
