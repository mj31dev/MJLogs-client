package dev.mj31.logger.client.app.usecase.sync

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.repository.FakeSyncRepository
import dev.mj31.logger.client.domain.sync.SyncState
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test

class SynchronizeAtTimestampUseCaseTest {

    private val repository = FakeSyncRepository()
    private val useCase = SynchronizeAtTimestampUseCase(syncRepository = repository)

    @Test
    fun `the typed instant is pinned to the current video position`() = runTest {
        val anchor = useCase(timestamp = TIMESTAMP, videoPositionMillis = 10_000L)

        assertThat(anchor.logTimestamp).isEqualTo(TIMESTAMP)
        assertThat(anchor.videoPositionMillis).isEqualTo(10_000L)
        assertThat(repository.syncState.value).isEqualTo(SyncState.Synced(anchor = anchor))
    }

    @Test
    fun `no record is claimed for an anchor the user typed`() = runTest {
        val anchor = useCase(timestamp = TIMESTAMP, videoPositionMillis = 0L)

        assertThat(anchor.logEntryId).isNull()
    }

    @Test
    fun `synchronizing again replaces the previous anchor`() = runTest {
        useCase(timestamp = TIMESTAMP, videoPositionMillis = 1_000L)
        val second = useCase(timestamp = TIMESTAMP, videoPositionMillis = 5_000L)

        assertThat(repository.syncState.value).isEqualTo(SyncState.Synced(anchor = second))
        assertThat(repository.setAnchorCallCount).isEqualTo(2)
    }

    private companion object {
        val TIMESTAMP: Instant = Instant.parse("2024-05-01T10:00:20Z")
    }
}
