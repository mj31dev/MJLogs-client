package dev.mj31.logger.client.data.workspace

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/** The durable store, exercised against a real SQLite file rather than a stand-in for one. */
class RoomWorkspaceRepositoryTest {

    private val directory = Files.createTempDirectory("mjlogs-workspace").toFile()
    private val database = MjLogsDatabaseFactory.open(
        path = directory.resolve("test.db").absolutePath,
        dispatcher = Dispatchers.IO,
    )
    private val repository = RoomWorkspaceRepository(database = database, dispatcher = Dispatchers.IO)

    @AfterTest
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    @Test
    fun `returns nothing before anything was ever stored`() = runTest {
        assertThat(repository.loadLastWorkspace()).isNull()
    }

    @Test
    fun `stores and returns a workspace unchanged`() = runTest {
        repository.saveLastWorkspace(snapshot = SNAPSHOT)

        assertThat(repository.loadLastWorkspace()).isEqualTo(SNAPSHOT)
    }

    @Test
    fun `keeps the order of the log sources`() = runTest {
        val sources = listOf(sourceRef(id = "c"), sourceRef(id = "a"), sourceRef(id = "b"))
        repository.saveLastWorkspace(snapshot = SNAPSHOT.copy(logSources = sources))

        assertThat(repository.loadLastWorkspace()?.logSources?.map { it.id })
            .containsExactly("c", "a", "b")
            .inOrder()
    }

    @Test
    fun `replaces the previous workspace instead of accumulating sources`() = runTest {
        repository.saveLastWorkspace(snapshot = SNAPSHOT)
        repository.saveLastWorkspace(snapshot = SNAPSHOT.copy(logSources = listOf(sourceRef(id = "only"))))

        assertThat(repository.loadLastWorkspace()?.logSources?.map { it.id }).containsExactly("only")
    }

    @Test
    fun `writes the playhead without touching the rest`() = runTest {
        repository.saveLastWorkspace(snapshot = SNAPSHOT)

        repository.updatePlaybackPosition(positionMillis = 987_654L)

        val stored = repository.loadLastWorkspace()
        assertThat(stored?.videoPositionMillis).isEqualTo(987_654L)
        assertThat(stored?.copy(videoPositionMillis = SNAPSHOT.videoPositionMillis)).isEqualTo(SNAPSHOT)
    }

    @Test
    fun `lists the packages most recently opened first`() = runTest {
        repository.rememberPackage(entry = recent(path = "/tmp/old.mjclog", millis = 1_000L))
        repository.rememberPackage(entry = recent(path = "/tmp/new.mjclog", millis = 9_000L))

        assertThat(repository.recentPackages.first().map { it.path })
            .containsExactly("/tmp/new.mjclog", "/tmp/old.mjclog")
            .inOrder()
    }

    @Test
    fun `opening the same package again updates it instead of adding a row`() = runTest {
        repository.rememberPackage(entry = recent(path = "/tmp/one.mjclog", millis = 1_000L))
        repository.rememberPackage(entry = recent(path = "/tmp/one.mjclog", millis = 5_000L))

        val listed = repository.recentPackages.first()
        assertThat(listed).hasSize(1)
        assertThat(listed.single().lastOpened.toEpochMilliseconds()).isEqualTo(5_000L)
    }

    @Test
    fun `forgetting a package drops it from the list`() = runTest {
        repository.rememberPackage(entry = recent(path = "/tmp/one.mjclog", millis = 1_000L))

        repository.forgetPackage(path = "/tmp/one.mjclog")

        assertThat(repository.recentPackages.first()).isEmpty()
    }

    private fun recent(path: String, millis: Long): RecentPackage = RecentPackage(
        path = path,
        name = path.substringAfterLast(delimiter = '/'),
        lastOpened = Instant.fromEpochMilliseconds(epochMilliseconds = millis),
    )

    private companion object {

        fun sourceRef(id: String): LogSourceRef = LogSourceRef(
            id = id,
            name = "$id.txt",
            path = "/logs/$id.txt",
            format = LogFormatSpec(
                name = "detected",
                linePattern = "^(?<ts>.*)$",
                timestampPattern = "yyyy-MM-dd HH:mm:ss",
                fallbackLevel = LogLevel.WARN,
                utcOffsetMinutes = 120,
                origin = FormatOrigin.USER_DEFINED,
            ),
        )

        val SNAPSHOT = WorkspaceSnapshot(
            logSources = listOf(sourceRef(id = "first"), sourceRef(id = "second")),
            video = VideoMedia(path = "/videos/clip.mp4", name = "clip.mp4"),
            anchor = SyncAnchor(
                logTimestamp = Instant.fromEpochMilliseconds(epochMilliseconds = 1_700_000_000_000L),
                videoPositionMillis = 42_000L,
                origin = SyncOrigin.SCREEN_CLOCK,
                logEntryId = "entry-7",
                accuracyMillis = 33L,
            ),
            filter = LogFilter(
                query = "timeout",
                levels = setOf(LogLevel.ERROR, LogLevel.WARN),
                sourceIds = setOf("first"),
            ),
            timeWindowMillis = 5_000L,
            followVideo = false,
            videoPositionMillis = 12_345L,
            packagePath = "/tmp/investigation.mjclog",
        )
    }
}
