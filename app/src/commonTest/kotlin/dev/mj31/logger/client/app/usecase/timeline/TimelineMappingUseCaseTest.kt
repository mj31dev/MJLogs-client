package dev.mj31.logger.client.app.usecase.timeline

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class TimelineMappingUseCaseTest {

    private val mapToLogTime = MapVideoPositionToLogTimeUseCase()
    private val mapToVideoPosition = MapLogTimeToVideoPositionUseCase()

    /** The user pinned the record at BASE + 30s to second 30 of a two minute screencast. */
    private val anchor = SyncAnchor(
        logTimestamp = TestLogEntries.at(offsetMillis = 30_000L),
        videoPositionMillis = 30_000L,
        origin = SyncOrigin.SELECTED_ENTRY,
        logEntryId = "src-1:7",
    )
    private val videoDurationMillis = 120_000L

    @Test
    fun `the anchor exposes the wall clock instant the recording started at`() {
        assertThat(anchor.videoStartInstant).isEqualTo(TestLogEntries.BASE)
    }

    @Test
    fun `a video position maps to the instant offset from the video start`() {
        assertThat(mapToLogTime(anchor = anchor, videoPositionMillis = 0L)).isEqualTo(TestLogEntries.BASE)
        assertThat(mapToLogTime(anchor = anchor, videoPositionMillis = 30_000L))
            .isEqualTo(anchor.logTimestamp)
        assertThat(mapToLogTime(anchor = anchor, videoPositionMillis = 90_000L))
            .isEqualTo(TestLogEntries.at(offsetMillis = 90_000L))
    }

    @Test
    fun `mapping a position to a time and back is the identity inside the video`() {
        listOf(0L, 1L, 15_000L, 30_000L, 119_999L, videoDurationMillis).forEach { position ->
            val instant = mapToLogTime(anchor = anchor, videoPositionMillis = position)
            val roundTrip = mapToVideoPosition(
                anchor = anchor,
                timestamp = instant,
                videoDurationMillis = videoDurationMillis,
            )

            assertThat(roundTrip).isEqualTo(position)
        }
    }

    @Test
    fun `mapping a time to a position and back is the identity inside the video`() {
        listOf(0L, 5_000L, 30_000L, videoDurationMillis).forEach { offset ->
            val timestamp = TestLogEntries.at(offsetMillis = offset)
            val position = mapToVideoPosition(
                anchor = anchor,
                timestamp = timestamp,
                videoDurationMillis = videoDurationMillis,
            )

            assertThat(position).isNotNull()
            assertThat(mapToLogTime(anchor = anchor, videoPositionMillis = position ?: 0L)).isEqualTo(timestamp)
        }
    }

    @Test
    fun `a record older than the recording has no video position`() {
        val beforeStart = anchor.videoStartInstant - 1.milliseconds

        val position = mapToVideoPosition(
            anchor = anchor,
            timestamp = beforeStart,
            videoDurationMillis = videoDurationMillis,
        )

        assertThat(position).isNull()
    }

    @Test
    fun `a record newer than the end of the recording has no video position`() {
        val afterEnd = anchor.videoStartInstant + (videoDurationMillis + 1L).milliseconds

        val position = mapToVideoPosition(
            anchor = anchor,
            timestamp = afterEnd,
            videoDurationMillis = videoDurationMillis,
        )

        assertThat(position).isNull()
    }

    @Test
    fun `the last frame of the recording is still mappable`() {
        val lastFrame = anchor.videoStartInstant + videoDurationMillis.milliseconds

        val position = mapToVideoPosition(
            anchor = anchor,
            timestamp = lastFrame,
            videoDurationMillis = videoDurationMillis,
        )

        assertThat(position).isEqualTo(videoDurationMillis)
    }

    @Test
    fun `an unknown duration leaves the upper bound open`() {
        val farInTheFuture = TestLogEntries.at(offsetMillis = 10_000_000L)

        val position = mapToVideoPosition(anchor = anchor, timestamp = farInTheFuture, videoDurationMillis = 0L)

        assertThat(position).isEqualTo(10_000_000L)
    }

    @Test
    fun `an unknown duration still rejects records before the video start`() {
        val beforeStart = anchor.videoStartInstant - 1.milliseconds

        val position = mapToVideoPosition(anchor = anchor, timestamp = beforeStart, videoDurationMillis = 0L)

        assertThat(position).isNull()
    }

    @Test
    fun `an anchor at position zero maps the log timestamp onto the video start`() {
        val zeroAnchor = SyncAnchor(
            logTimestamp = TestLogEntries.BASE,
            videoPositionMillis = 0L,
            origin = SyncOrigin.SELECTED_ENTRY,
        )

        assertThat(zeroAnchor.videoStartInstant).isEqualTo(TestLogEntries.BASE)
        assertThat(mapToLogTime(anchor = zeroAnchor, videoPositionMillis = 2_000L))
            .isEqualTo(TestLogEntries.at(offsetMillis = 2_000L))
    }
}
