package dev.mj31.logger.client.app.usecase.timeline

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import dev.mj31.logger.client.app.fake.repository.FakeSyncRepository
import dev.mj31.logger.client.app.usecase.sync.SynchronizeTimelinesUseCase
import dev.mj31.logger.client.app.usecase.sync.ClearSynchronizationUseCase

class SyncUseCasesTest {

    private val entry = TestLogEntries.entry(
        id = "src-1:42",
        lineNumber = 42,
        timestamp = TestLogEntries.at(offsetMillis = 90_000L),
    )

    @Test
    fun `both timelines are independent until the user synchronizes them`() = runTest {
        val repository = FakeSyncRepository()

        assertThat(repository.syncState.value).isEqualTo(SyncState.Unsynced)
        assertThat(repository.syncState.value.isSynced).isFalse()
        assertThat(repository.syncState.value.anchorOrNull).isNull()
    }

    @Test
    fun `synchronizing pins the selected record to the current video position`() = runTest {
        val repository = FakeSyncRepository()
        val synchronize = SynchronizeTimelinesUseCase(syncRepository = repository)

        val anchor = synchronize(entry = entry, videoPositionMillis = 30_000L)

        assertThat(anchor.logEntryId).isEqualTo("src-1:42")
        assertThat(anchor.logTimestamp).isEqualTo(entry.timestamp)
        assertThat(anchor.videoPositionMillis).isEqualTo(30_000L)
        assertThat(anchor.videoStartInstant).isEqualTo(TestLogEntries.at(offsetMillis = 60_000L))
    }

    @Test
    fun `synchronizing flips the repository state to synced with the produced anchor`() = runTest {
        val repository = FakeSyncRepository()
        val synchronize = SynchronizeTimelinesUseCase(syncRepository = repository)

        val anchor = synchronize(entry = entry, videoPositionMillis = 30_000L)

        assertThat(repository.setAnchorCallCount).isEqualTo(1)
        assertThat(repository.syncState.value).isEqualTo(SyncState.Synced(anchor = anchor))
        assertThat(repository.syncState.value.isSynced).isTrue()
        assertThat(repository.syncState.value.anchorOrNull).isEqualTo(anchor)
    }

    @Test
    fun `synchronizing at video position zero starts the video at the record timestamp`() = runTest {
        val repository = FakeSyncRepository()
        val synchronize = SynchronizeTimelinesUseCase(syncRepository = repository)

        val anchor = synchronize(entry = entry, videoPositionMillis = 0L)

        assertThat(anchor.videoStartInstant).isEqualTo(entry.timestamp)
    }

    @Test
    fun `synchronizing again replaces the previous anchor`() = runTest {
        val repository = FakeSyncRepository()
        val synchronize = SynchronizeTimelinesUseCase(syncRepository = repository)

        synchronize(entry = entry, videoPositionMillis = 30_000L)
        val second = synchronize(
            entry = TestLogEntries.entry(id = "src-1:43", timestamp = TestLogEntries.at(offsetMillis = 120_000L)),
            videoPositionMillis = 5_000L,
        )

        assertThat(repository.setAnchorCallCount).isEqualTo(2)
        assertThat(repository.syncState.value.anchorOrNull).isEqualTo(second)
        assertThat(second.videoStartInstant).isEqualTo(TestLogEntries.at(offsetMillis = 115_000L))
    }

    @Test
    fun `clearing detaches the two timelines again`() = runTest {
        val anchor = SyncAnchor(
            logTimestamp = entry.timestamp,
            videoPositionMillis = 30_000L,
            logEntryId = entry.id,
        )
        val repository = FakeSyncRepository(initialState = SyncState.Synced(anchor = anchor))
        val clear = ClearSynchronizationUseCase(syncRepository = repository)

        clear()

        assertThat(repository.clearAnchorCallCount).isEqualTo(1)
        assertThat(repository.syncState.value).isEqualTo(SyncState.Unsynced)
        assertThat(repository.syncState.value.anchorOrNull).isNull()
    }

    @Test
    fun `clearing an already unsynced state is a no-op`() = runTest {
        val repository = FakeSyncRepository()
        val clear = ClearSynchronizationUseCase(syncRepository = repository)

        clear()

        assertThat(repository.syncState.value).isEqualTo(SyncState.Unsynced)
    }

    @Test
    fun `an anchor without a log entry id is still a valid mapping`() {
        val anchor = SyncAnchor(logTimestamp = TestLogEntries.BASE, videoPositionMillis = 1_500L)

        assertThat(anchor.logEntryId).isNull()
        assertThat(anchor.videoStartInstant).isEqualTo(TestLogEntries.at(offsetMillis = -1_500L))
    }
}
