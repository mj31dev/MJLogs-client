package dev.mj31.logger.client.app.usecase.timeline

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.sync.SyncAnchor
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.log.LogEntry

class FindEntryAtVideoPositionUseCaseTest {

    private val findEntry = FindEntryAtVideoPositionUseCase()

    /** Video second 0 corresponds to [TestLogEntries.BASE]. */
    private val anchor = SyncAnchor(logTimestamp = TestLogEntries.BASE, videoPositionMillis = 0L)

    private val entries: List<LogEntry> = listOf(
        TestLogEntries.entryAt(offsetMillis = 0L, id = "e0"),
        TestLogEntries.entryAt(offsetMillis = 1_000L, id = "e1"),
        TestLogEntries.entryAt(offsetMillis = 2_000L, id = "e2"),
        TestLogEntries.entryAt(offsetMillis = 5_000L, id = "e3"),
    )

    @Test
    fun `returns the last entry not newer than the mapped instant`() {
        val entry = findEntry(entries = entries, anchor = anchor, videoPositionMillis = 1_500L)

        assertThat(entry?.id).isEqualTo("e1")
    }

    @Test
    fun `an exact timestamp hit selects that very entry`() {
        assertThat(findEntry(entries = entries, anchor = anchor, videoPositionMillis = 0L)?.id).isEqualTo("e0")
        assertThat(findEntry(entries = entries, anchor = anchor, videoPositionMillis = 2_000L)?.id).isEqualTo("e2")
        assertThat(findEntry(entries = entries, anchor = anchor, videoPositionMillis = 5_000L)?.id).isEqualTo("e3")
    }

    @Test
    fun `one millisecond before an entry still selects the previous one`() {
        val entry = findEntry(entries = entries, anchor = anchor, videoPositionMillis = 1_999L)

        assertThat(entry?.id).isEqualTo("e1")
    }

    @Test
    fun `returns null while the video plays before the very first record`() {
        val lateEntries = listOf(
            TestLogEntries.entryAt(offsetMillis = 10_000L, id = "late0"),
            TestLogEntries.entryAt(offsetMillis = 11_000L, id = "late1"),
        )

        assertThat(findEntry(entries = lateEntries, anchor = anchor, videoPositionMillis = 0L)).isNull()
        assertThat(findEntry(entries = lateEntries, anchor = anchor, videoPositionMillis = 9_999L)).isNull()
        assertThat(findEntry(entries = lateEntries, anchor = anchor, videoPositionMillis = 10_000L)?.id)
            .isEqualTo("late0")
    }

    @Test
    fun `returns the last entry once the video runs past the end of the log`() {
        val entry = findEntry(entries = entries, anchor = anchor, videoPositionMillis = 900_000L)

        assertThat(entry?.id).isEqualTo("e3")
    }

    @Test
    fun `returns null for an empty list of entries`() {
        assertThat(findEntry(entries = emptyList(), anchor = anchor, videoPositionMillis = 1_000L)).isNull()
    }

    @Test
    fun `a single entry is returned from its own timestamp onwards`() {
        val single = listOf(TestLogEntries.entryAt(offsetMillis = 1_000L, id = "only"))

        assertThat(findEntry(entries = single, anchor = anchor, videoPositionMillis = 999L)).isNull()
        assertThat(findEntry(entries = single, anchor = anchor, videoPositionMillis = 1_000L)?.id).isEqualTo("only")
        assertThat(findEntry(entries = single, anchor = anchor, videoPositionMillis = 50_000L)?.id).isEqualTo("only")
    }

    @Test
    fun `entries sharing a timestamp resolve to the last one of the group`() {
        val duplicates = listOf(
            TestLogEntries.entryAt(offsetMillis = 1_000L, id = "d0"),
            TestLogEntries.entryAt(offsetMillis = 1_000L, id = "d1"),
            TestLogEntries.entryAt(offsetMillis = 1_000L, id = "d2"),
            TestLogEntries.entryAt(offsetMillis = 3_000L, id = "d3"),
        )

        assertThat(findEntry(entries = duplicates, anchor = anchor, videoPositionMillis = 1_000L)?.id).isEqualTo("d2")
    }

    @Test
    fun `the binary search picks the right element in a large list`() {
        val large = TestLogEntries.sequence(count = 10_000, stepMillis = 1_000L)

        assertThat(findEntry(entries = large, anchor = anchor, videoPositionMillis = 0L)?.id).isEqualTo("src-1:1")
        assertThat(findEntry(entries = large, anchor = anchor, videoPositionMillis = 7_777_500L)?.id)
            .isEqualTo("src-1:7778")
        assertThat(findEntry(entries = large, anchor = anchor, videoPositionMillis = 9_999_000L)?.id)
            .isEqualTo("src-1:10000")
        assertThat(findEntry(entries = large, anchor = anchor, videoPositionMillis = 99_999_000L)?.id)
            .isEqualTo("src-1:10000")
    }

    @Test
    fun `the mapping honours the anchor offset`() {
        // The user pinned the entry at BASE + 5s to second 20 of the video, so the video starts 15s earlier.
        val shifted = SyncAnchor(logTimestamp = TestLogEntries.at(offsetMillis = 5_000L), videoPositionMillis = 20_000L)

        assertThat(findEntry(entries = entries, anchor = shifted, videoPositionMillis = 20_000L)?.id).isEqualTo("e3")
        assertThat(findEntry(entries = entries, anchor = shifted, videoPositionMillis = 15_000L)?.id).isEqualTo("e0")
        assertThat(findEntry(entries = entries, anchor = shifted, videoPositionMillis = 14_999L)).isNull()
    }
}
