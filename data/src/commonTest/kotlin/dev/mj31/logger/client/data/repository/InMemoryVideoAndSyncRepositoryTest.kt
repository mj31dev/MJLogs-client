package dev.mj31.logger.client.data.repository

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncState
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.media.VideoMedia

class InMemoryVideoAndSyncRepositoryTest {

    @Test
    fun `the workspace starts without a screencast`() = runTest {
        assertThat(InMemoryVideoRepository().media.value).isNull()
    }

    @Test
    fun `a screencast can be set and replaced`() = runTest {
        val repository = InMemoryVideoRepository()
        val first = VideoMedia(path = "/media/a.mp4", name = "a.mp4")
        val second = VideoMedia(path = "/media/b.mp4", name = "b.mp4")

        repository.setMedia(media = first)
        assertThat(repository.media.value).isEqualTo(first)

        repository.setMedia(media = second)
        assertThat(repository.media.value).isEqualTo(second)
    }

    @Test
    fun `a screencast can be cleared`() = runTest {
        val repository = InMemoryVideoRepository()
        repository.setMedia(media = VideoMedia(path = "/media/a.mp4", name = "a.mp4"))

        repository.setMedia(media = null)

        assertThat(repository.media.value).isNull()
    }

    @Test
    fun `the timelines start unsynchronized`() = runTest {
        assertThat(InMemorySyncRepository().syncState.value).isEqualTo(SyncState.Unsynced)
    }

    @Test
    fun `an anchor can be set, replaced and cleared`() = runTest {
        val repository = InMemorySyncRepository()
        val anchor = SyncAnchor(logTimestamp = INSTANT, videoPositionMillis = 1_000L, logEntryId = "e1")

        repository.setAnchor(anchor = anchor)
        assertThat(repository.syncState.value.anchorOrNull).isEqualTo(anchor)

        val moved = anchor.copy(videoPositionMillis = 2_000L)
        repository.setAnchor(anchor = moved)
        assertThat(repository.syncState.value.anchorOrNull).isEqualTo(moved)

        repository.clearAnchor()
        assertThat(repository.syncState.value).isEqualTo(SyncState.Unsynced)
    }

    private companion object {
        val INSTANT: Instant = Instant.parse("2024-05-01T10:00:00Z")
    }
}
