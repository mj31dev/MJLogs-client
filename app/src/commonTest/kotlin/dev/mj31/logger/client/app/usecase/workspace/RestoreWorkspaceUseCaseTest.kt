package dev.mj31.logger.client.app.usecase.workspace

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParser
import dev.mj31.logger.client.app.fake.format.ScriptedLogLineParserFactory
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.app.fake.source.FakeTextFileDataSource
import dev.mj31.logger.client.app.fake.source.FixedClock
import dev.mj31.logger.client.app.fake.source.FixedIdGenerator
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceAssembler
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.data.repository.InMemoryLogSessionRepository
import dev.mj31.logger.client.data.repository.InMemorySyncRepository
import dev.mj31.logger.client.data.repository.InMemoryVideoRepository
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.source.TextFileContent
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone

/** Reopening a stored workspace: what comes back, and what is said about what does not. */
class RestoreWorkspaceUseCaseTest {

    private val files = FakeTextFileDataSource()
    private val sessionRepository = InMemoryLogSessionRepository()
    private val videoRepository = InMemoryVideoRepository()
    private val syncRepository = InMemorySyncRepository()

    private val useCase = RestoreWorkspaceUseCase(
        loader = LogSourceLoader(
            dataSource = files,
            assembler = LogSourceAssembler(parserFactory = ScriptedLogLineParserFactory()),
            idGenerator = FixedIdGenerator(),
            clock = FixedClock(instant = TestLogEntries.at(offsetMillis = 0L)),
            timeZone = TimeZone.UTC,
        ),
        sessionRepository = sessionRepository,
        videoRepository = videoRepository,
        syncRepository = syncRepository,
    )

    @Test
    fun `reads every referenced file back into the session`() = runTest {
        register(path = "/logs/a.txt", messages = listOf("First", "Second"))
        register(path = "/logs/b.txt", messages = listOf("Third"))

        val result = useCase(snapshot = snapshot(paths = listOf("/logs/a.txt", "/logs/b.txt")))

        assertThat(result.restoredSourceCount).isEqualTo(2)
        assertThat(result.hasMissingFiles).isFalse()
        assertThat(sessionRepository.sources.value.sumOf { it.entryCount }).isEqualTo(3)
    }

    /**
     * A stored filter names the sources it was written against, so a regenerated identifier would
     * quietly widen the filter to everything.
     */
    @Test
    fun `keeps the identifiers the sources were stored with`() = runTest {
        register(path = "/logs/a.txt", messages = listOf("First"))

        useCase(snapshot = snapshot(paths = listOf("/logs/a.txt")))

        assertThat(sessionRepository.sources.value.single().id).isEqualTo("stored-/logs/a.txt")
    }

    @Test
    fun `names the files that are no longer there and opens the rest`() = runTest {
        register(path = "/logs/a.txt", messages = listOf("First"))
        files.registerFailure(path = "/logs/gone.txt", error = IllegalStateException("no such file"))

        val result = useCase(snapshot = snapshot(paths = listOf("/logs/a.txt", "/logs/gone.txt")))

        assertThat(result.missingFileNames).containsExactly("gone.txt")
        assertThat(result.restoredSourceCount).isEqualTo(1)
        assertThat(sessionRepository.sources.value).hasSize(1)
    }

    @Test
    fun `brings the screencast and the anchor back with it`() = runTest {
        register(path = "/logs/a.txt", messages = listOf("First"))
        val anchor = SyncAnchor(
            logTimestamp = TestLogEntries.at(offsetMillis = 5_000L),
            videoPositionMillis = 2_000L,
            origin = SyncOrigin.SCREEN_CLOCK,
        )

        useCase(snapshot = snapshot(paths = listOf("/logs/a.txt")).copy(anchor = anchor))

        assertThat(videoRepository.media.value).isEqualTo(VideoMedia(path = "/v/clip.mp4", name = "clip.mp4"))
        assertThat(syncRepository.syncState.value.anchorOrNull).isEqualTo(anchor)
    }

    @Test
    fun `a workspace without an anchor leaves the timelines apart`() = runTest {
        register(path = "/logs/a.txt", messages = listOf("First"))
        syncRepository.setAnchor(
            anchor = SyncAnchor(
                logTimestamp = TestLogEntries.at(offsetMillis = 0L),
                videoPositionMillis = 0L,
                origin = SyncOrigin.SELECTED_ENTRY,
            ),
        )

        useCase(snapshot = snapshot(paths = listOf("/logs/a.txt")))

        assertThat(syncRepository.syncState.value.isSynced).isFalse()
    }

    @Test
    fun `replaces whatever was open rather than adding to it`() = runTest {
        register(path = "/logs/a.txt", messages = listOf("First"))
        register(path = "/logs/b.txt", messages = listOf("Second"))
        useCase(snapshot = snapshot(paths = listOf("/logs/a.txt", "/logs/b.txt")))

        useCase(snapshot = snapshot(paths = listOf("/logs/a.txt")))

        assertThat(sessionRepository.sources.value.map { it.path }).containsExactly("/logs/a.txt")
    }

    private fun register(path: String, messages: List<String>) {
        files.register(
            content = TextFileContent(
                path = path,
                name = path.substringAfterLast(delimiter = '/'),
                lines = messages.mapIndexed { index, message ->
                    ScriptedLogLineParser.recordLine(
                        timestamp = TestLogEntries.at(offsetMillis = index * 1_000L),
                        level = LogLevel.INFO,
                        tag = "Network",
                        message = message,
                    )
                },
            ),
        )
    }

    private fun snapshot(paths: List<String>): WorkspaceSnapshot = WorkspaceSnapshot(
        logSources = paths.map { path ->
            LogSourceRef(
                id = "stored-$path",
                name = path.substringAfterLast(delimiter = '/'),
                path = path,
                format = TestLogEntries.SPEC,
            )
        },
        video = VideoMedia(path = "/v/clip.mp4", name = "clip.mp4"),
    )
}
