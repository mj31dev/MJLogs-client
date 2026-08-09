package dev.mj31.logger.client.app.usecase.workspace

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.repository.FakeSessionPackageStore
import dev.mj31.logger.client.app.fake.repository.FakeWorkspaceRepository
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.session.SessionFile
import kotlin.test.Test
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

/**
 * The rule that decides how eagerly a change reaches the file it came from.
 *
 * The application store takes everything immediately; the session file carries copies of the logs
 * and the screencast, so rewriting it costs a copy of every byte and happens only when asked.
 */
class PersistWorkspaceUseCaseTest {

    private val repository = FakeWorkspaceRepository()
    private val packageStore = FakeSessionPackageStore()
    private val useCase = PersistWorkspaceUseCase(
        workspaceRepository = repository,
        packageStore = packageStore,
    )

    @Test
    fun `a workspace with no file behind it is still stored`() = runTest {
        useCase(snapshot = WorkspaceSnapshot(filter = LogFilter(query = "boom")))

        assertThat(repository.stored?.filter?.query).isEqualTo("boom")
    }

    /**
     * The file used to follow every change when it held only references, and that is what was
     * dropped: writing on every keystroke is only affordable for a file that carries nothing.
     */
    @Test
    fun `the file is left behind until it is explicitly written`() = runTest {
        val path = save()

        useCase(snapshot = snapshot(path = path).copy(filter = LogFilter(query = "changed")))

        assertThat(packageStore.read(path = path).snapshot.filter.query).isEmpty()
        assertThat(repository.stored?.filter?.query).isEqualTo("changed")
    }

    @Test
    fun `flushing brings the file up to date`() = runTest {
        val path = save()
        val changed = snapshot(path = path).copy(filter = LogFilter(query = "changed"))

        val flushed = useCase.flushToPackage(snapshot = changed)

        assertThat(flushed).isTrue()
        assertThat(packageStore.read(path = path).snapshot.filter.query).isEqualTo("changed")
    }

    @Test
    fun `flushing a workspace that belongs to no file reports that there was nothing to do`() = runTest {
        assertThat(useCase.flushToPackage(snapshot = WorkspaceSnapshot())).isFalse()
    }

    @Test
    fun `the playhead is written on its own`() = runTest {
        useCase(snapshot = WorkspaceSnapshot())

        useCase.updatePlaybackPosition(positionMillis = 4_200L)

        assertThat(repository.stored?.videoPositionMillis).isEqualTo(4_200L)
        assertThat(repository.positionWriteCount).isEqualTo(1)
    }

    private suspend fun save(): String {
        val path = "/tmp/session" + SessionFile.EXTENSION
        packageStore.write(targetPath = path, snapshot = snapshot(path = path)).toList()
        return path
    }

    private fun snapshot(path: String): WorkspaceSnapshot = WorkspaceSnapshot(packagePath = path)
}
