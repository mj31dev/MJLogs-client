package dev.mj31.logger.client.domain.sync

import com.google.common.truth.Truth.assertThat
import kotlin.time.Instant
import kotlin.test.Test

class SyncStateTest {

    private val anchor = SyncAnchor(
        logTimestamp = Instant.parse("2024-05-01T10:00:30Z"),
        videoPositionMillis = 10_000L,
        origin = SyncOrigin.SELECTED_ENTRY,
        logEntryId = "entry-1",
    )

    @Test
    fun `the video start is the anchored record minus its position`() {
        assertThat(anchor.videoStartInstant).isEqualTo(Instant.parse("2024-05-01T10:00:20Z"))
    }

    @Test
    fun `an anchor at the very beginning starts the video at the record itself`() {
        val atStart = anchor.copy(videoPositionMillis = 0L)

        assertThat(atStart.videoStartInstant).isEqualTo(atStart.logTimestamp)
    }

    @Test
    fun `the unsynced state exposes no anchor`() {
        assertThat(SyncState.Unsynced.isSynced).isFalse()
        assertThat(SyncState.Unsynced.anchorOrNull).isNull()
    }

    @Test
    fun `the synced state exposes its anchor`() {
        val state: SyncState = SyncState.Synced(anchor = anchor)

        assertThat(state.isSynced).isTrue()
        assertThat(state.anchorOrNull).isEqualTo(anchor)
    }
}
