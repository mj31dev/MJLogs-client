package dev.mj31.logger.client.app.features.logplayer

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.LogPlayerRobot
import dev.mj31.logger.client.app.fake.repository.FakeSessionPackageStore
import dev.mj31.logger.client.app.fake.repository.FakeWorkspaceRepository
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/** What the player remembers between runs, and what it does with a session file. */
class LogPlayerStoreWorkspaceTest {

    /**
     * The stored workspace is offered by the start screen, not forced onto the player.
     *
     * Reopening it unasked was the earlier design and it is the wrong default for a tool opened to
     * look at something new as often as to carry on — so the player stays empty until told.
     */
    @Test
    fun `does not reopen the last workspace on its own`() = runTest {
        val stored = FakeWorkspaceRepository(stored = storedWorkspace())

        val robot = LogPlayerRobot.create(testScope = this, workspaceRepository = stored)

        assertThat(robot.state.sources).isEmpty()
    }

    @Test
    fun `reopens the workspace that was open last when asked to continue`() = runTest {
        val stored = FakeWorkspaceRepository(stored = storedWorkspace())
        val robot = LogPlayerRobot.create(testScope = this, workspaceRepository = stored)

        robot.dispatch(intent = LogPlayerIntent.ContinueLastSession)

        assertThat(robot.state.sources.map { it.name }).containsExactly(LogPlayerFixtures.firstFile.name)
        assertThat(robot.state.filter.levels).containsExactly(LogLevel.ERROR)
        assertThat(robot.state.followVideo).isFalse()
    }

    /** Restoring has to put the screencast back into the decoder, not only the logs into the list. */
    @Test
    fun `continuing reopens the screencast at the frame it was left on`() = runTest {
        val media = VideoMedia(path = "/recordings/run.mp4", name = "run.mp4")
        val stored = FakeWorkspaceRepository(
            stored = storedWorkspace().copy(video = media, videoPositionMillis = 4_000L),
        )
        val robot = LogPlayerRobot.create(testScope = this, workspaceRepository = stored)

        robot.dispatch(intent = LogPlayerIntent.ContinueLastSession)

        assertThat(robot.player.openedMedia).containsExactly(media)
        assertThat(robot.player.seekPositions).containsExactly(4_000L)
    }

    @Test
    fun `a first run opens on nothing rather than on an empty stored workspace`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        assertThat(robot.state.sources).isEmpty()
        assertThat(robot.workspaceRepository.stored).isNull()
    }

    /** Starting fresh empties the workspace and lets the screencast go with it. */
    @Test
    fun `starting a new session clears the workspace`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.dispatch(intent = LogPlayerIntent.StartNewSession)

        assertThat(robot.state.sources).isEmpty()
        assertThat(robot.state.filter).isEqualTo(LogFilter())
        assertThat(robot.player.closeCallCount).isEqualTo(1)
    }

    @Test
    fun `importing a file is remembered without being asked`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.importBothLogFiles()

        assertThat(robot.workspaceRepository.stored?.logSources?.map { it.path })
            .containsExactly(LogPlayerFixtures.FIRST_PATH, LogPlayerFixtures.SECOND_PATH)
    }

    @Test
    fun `changing the filter is remembered`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importBothLogFiles()

        robot.dispatch(intent = LogPlayerIntent.UpdateFilter(filter = LogFilter(query = "timeout")))

        assertThat(robot.workspaceRepository.stored?.filter?.query).isEqualTo("timeout")
    }

    @Test
    fun `asking to save opens the panel`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.dispatch(intent = LogPlayerIntent.RequestSaveSession)

        assertThat(robot.effects).contains(LogPlayerEffect.PickSessionSaveTarget)
    }

    @Test
    fun `saving binds the workspace to the file it was written to`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.firstFile.path))

        robot.dispatch(
            intent = LogPlayerIntent.SaveSession(path = "/tmp/case"),
        )

        assertThat(robot.state.workspace.packagePath).isEqualTo("/tmp/case.mjclog")
        assertThat(robot.state.workspace.packageName).isEqualTo("case")
        assertThat(robot.state.workspace.hasUnsavedChanges).isFalse()
    }

    @Test
    fun `the progress bar is gone once the write has finished`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.dispatch(
            intent = LogPlayerIntent.SaveSession(path = "/tmp/case"),
        )

        assertThat(robot.state.workspace.isSaving).isFalse()
    }

    /**
     * The file is rewritten only when asked, so between the change and the write it is behind what
     * is on screen — and the window says so.
     */
    @Test
    fun `a change after saving marks the file as behind`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.dispatch(intent = LogPlayerIntent.SaveSession(path = "/tmp/case"))

        robot.dispatch(intent = LogPlayerIntent.UpdateFilter(filter = LogFilter(query = "later")))

        assertThat(robot.state.workspace.hasUnsavedChanges).isTrue()
    }

    @Test
    fun `writing the pending changes clears the marker`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.dispatch(intent = LogPlayerIntent.SaveSession(path = "/tmp/case"))
        robot.dispatch(intent = LogPlayerIntent.UpdateFilter(filter = LogFilter(query = "later")))

        robot.dispatch(intent = LogPlayerIntent.SaveSessionChanges)

        assertThat(robot.state.workspace.hasUnsavedChanges).isFalse()
        assertThat(robot.packageStore.read(path = "/tmp/case.mjclog").snapshot.filter.query).isEqualTo("later")
    }

    /**
     * A session file is written once and then left alone, so what it holds is what the workspace
     * looked like at that moment — which is exactly what reopening it has to bring back.
     */
    @Test
    fun `opening a session replaces what was on screen`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))
        robot.dispatch(intent = LogPlayerIntent.SaveSession(path = "/tmp/one"))
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.SECOND_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.SECOND_PATH))
        robot.dispatch(intent = LogPlayerIntent.SaveSession(path = "/tmp/two"))

        robot.dispatch(intent = LogPlayerIntent.OpenSession(path = "/tmp/one.mjclog"))

        assertThat(robot.state.sources.map { it.name }).containsExactly(LogPlayerFixtures.firstFile.name)
        assertThat(robot.state.workspace.packageName).isEqualTo("one")
    }

    /**
     * Closing the workspace is the last of the three moments the file is brought up to date.
     *
     * Without it, everything done since the last explicit save would be in the application store and
     * missing from the file the user thinks holds their session.
     */
    @Test
    fun `closing the workspace writes what was pending into the file`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))
        robot.dispatch(intent = LogPlayerIntent.SaveSession(path = "/tmp/one"))
        robot.dispatch(intent = LogPlayerIntent.UpdateFilter(filter = LogFilter(query = "pending")))

        robot.store.closeWorkspace()
        robot.settle()

        assertThat(robot.packageStore.read(path = "/tmp/one.mjclog").snapshot.filter.query).isEqualTo("pending")
        assertThat(robot.packageStore.releasedPaths).contains("/tmp/one.mjclog")
    }

    /**
     * The file is not a live mirror: what is imported after a save stays out of it until the next one.
     *
     * This is the whole point of dropping the second format — "saved" now means one thing, and the
     * marker in the window is what tells the user the file and the screen have drifted apart.
     */
    @Test
    fun `what is imported after a save stays out of the file until it is written again`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)
        robot.detector.enqueueDetected(spec = LogPlayerFixtures.FIRST_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.FIRST_PATH))
        robot.dispatch(intent = LogPlayerIntent.SaveSession(path = "/tmp/case"))

        robot.detector.enqueueDetected(spec = LogPlayerFixtures.SECOND_SPEC)
        robot.importLogFiles(paths = listOf(LogPlayerFixtures.SECOND_PATH))

        assertThat(robot.packageStore.read(path = "/tmp/case.mjclog").snapshot.logSources.map { it.path })
            .containsExactly(LogPlayerFixtures.FIRST_PATH)
        assertThat(robot.state.workspace.hasUnsavedChanges).isTrue()
    }

    @Test
    fun `asking to open a session opens the picker`() = runTest {
        val robot = LogPlayerRobot.create(testScope = this)

        robot.dispatch(intent = LogPlayerIntent.RequestOpenSession)

        assertThat(robot.effects).contains(LogPlayerEffect.PickSessionFile)
    }

    private fun storedWorkspace(): WorkspaceSnapshot = WorkspaceSnapshot(
        logSources = listOf(
            LogSourceRef(
                id = "restored-1",
                name = LogPlayerFixtures.firstFile.name,
                path = LogPlayerFixtures.firstFile.path,
                format = LogPlayerFixtures.MANUAL_SPEC,
            ),
        ),
        filter = LogFilter(levels = setOf(LogLevel.ERROR)),
        followVideo = false,
    )
}
