package dev.mj31.logger.client.app.features.sessions

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.repository.FakeWorkspaceRepository
import dev.mj31.logger.client.domain.model.workspace.RecentPackage
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsStoreTest {

    @Test
    fun `starts empty and follows what the store remembers`() = runTest {
        val repository = FakeWorkspaceRepository()
        val store = storeOf(repository = repository)
        assertThat(store.state.value.isEmpty).isTrue()

        repository.rememberPackage(entry = recent(path = "/tmp/case.mjclog"))

        assertThat(store.state.value.recent.map { it.path }).containsExactly("/tmp/case.mjclog")
    }

    @Test
    fun `forgetting an entry removes it from the list`() = runTest {
        val repository = FakeWorkspaceRepository()
        repository.rememberPackage(entry = recent(path = "/tmp/one.mjclog"))
        repository.rememberPackage(entry = recent(path = "/tmp/two.mjclog"))
        val store = storeOf(repository = repository)

        store.handleIntent(intent = SessionsIntent.Forget(path = "/tmp/one.mjclog"))

        assertThat(store.state.value.recent.map { it.path }).containsExactly("/tmp/two.mjclog")
    }

    /**
     * Opening replaces the player's whole workspace, which is the player's decision; the list only
     * surfaces the request. Handling it here would give one screen a say over another's contents.
     */
    @Test
    fun `opening is not something the list decides`() = runTest {
        val repository = FakeWorkspaceRepository()
        repository.rememberPackage(entry = recent(path = "/tmp/one.mjclog"))
        val store = storeOf(repository = repository)

        store.handleIntent(intent = SessionsIntent.Open(path = "/tmp/one.mjclog"))
        store.handleIntent(intent = SessionsIntent.RequestOpenFile)

        assertThat(store.state.value.recent.map { it.path }).containsExactly("/tmp/one.mjclog")
    }

    /** Unconfined so that a change in the repository is visible in the state on the next line. */
    private fun TestScope.storeOf(repository: FakeWorkspaceRepository): SessionsStore = SessionsStore(
        repository = repository,
        scope = CoroutineScope(
            context = backgroundScope.coroutineContext + UnconfinedTestDispatcher(scheduler = testScheduler),
        ),
    )

    private fun recent(path: String): RecentPackage = RecentPackage(
        path = path,
        name = path.substringAfterLast(delimiter = '/'),
        lastOpened = Instant.fromEpochMilliseconds(epochMilliseconds = 1_000L),
    )
}
