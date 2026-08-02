package dev.mj31.logger.client.app.usecase.timeline

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.log.TestLogEntries
import dev.mj31.logger.client.domain.sync.SyncAnchor
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.time.TimeRange

class ResolveTimelineOverlapUseCaseTest {

    private val resolveOverlap = ResolveTimelineOverlapUseCase()

    private val logRange = TimeRange(
        start = TestLogEntries.at(offsetMillis = 10_000L),
        end = TestLogEntries.at(offsetMillis = 50_000L),
    )

    /** Anchor whose video starts exactly [offsetMillis] after the session origin. */
    private fun anchorStartingAt(offsetMillis: Long): SyncAnchor =
        SyncAnchor(logTimestamp = TestLogEntries.at(offsetMillis = offsetMillis), videoPositionMillis = 0L)

    @Test
    fun `a video covering the whole log yields the log range as overlap`() {
        val overlap = resolveOverlap(
            logRange = logRange,
            anchor = anchorStartingAt(offsetMillis = 0L),
            videoDurationMillis = 60_000L,
        )

        assertThat(overlap.hasOverlap).isTrue()
        assertThat(overlap.overlap).isEqualTo(logRange)
        assertThat(overlap.logRange).isEqualTo(logRange)
        assertThat(overlap.videoRange).isEqualTo(
            TimeRange(start = TestLogEntries.BASE, end = TestLogEntries.at(offsetMillis = 60_000L)),
        )
    }

    @Test
    fun `a video started after the log begins clips the overlap on the left`() {
        val overlap = resolveOverlap(
            logRange = logRange,
            anchor = anchorStartingAt(offsetMillis = 20_000L),
            videoDurationMillis = 60_000L,
        )

        assertThat(overlap.hasOverlap).isTrue()
        assertThat(overlap.overlap).isEqualTo(
            TimeRange(
                start = TestLogEntries.at(offsetMillis = 20_000L),
                end = TestLogEntries.at(offsetMillis = 50_000L),
            ),
        )
    }

    @Test
    fun `a video ending before the log ends clips the overlap on the right`() {
        val overlap = resolveOverlap(
            logRange = logRange,
            anchor = anchorStartingAt(offsetMillis = -30_000L),
            videoDurationMillis = 60_000L,
        )

        assertThat(overlap.hasOverlap).isTrue()
        assertThat(overlap.overlap).isEqualTo(
            TimeRange(
                start = TestLogEntries.at(offsetMillis = 10_000L),
                end = TestLogEntries.at(offsetMillis = 30_000L),
            ),
        )
    }

    @Test
    fun `a video shorter than the log is fully contained in the overlap`() {
        val overlap = resolveOverlap(
            logRange = logRange,
            anchor = anchorStartingAt(offsetMillis = 20_000L),
            videoDurationMillis = 10_000L,
        )

        assertThat(overlap.overlap).isEqualTo(
            TimeRange(
                start = TestLogEntries.at(offsetMillis = 20_000L),
                end = TestLogEntries.at(offsetMillis = 30_000L),
            ),
        )
    }

    @Test
    fun `timelines touching in a single instant still overlap`() {
        val overlap = resolveOverlap(
            logRange = logRange,
            anchor = anchorStartingAt(offsetMillis = 50_000L),
            videoDurationMillis = 60_000L,
        )

        assertThat(overlap.hasOverlap).isTrue()
        assertThat(overlap.overlap).isEqualTo(
            TimeRange(
                start = TestLogEntries.at(offsetMillis = 50_000L),
                end = TestLogEntries.at(offsetMillis = 50_000L),
            ),
        )
        assertThat(overlap.overlap?.durationMillis).isEqualTo(0L)
    }

    @Test
    fun `a video recorded after the log has no overlap`() {
        val overlap = resolveOverlap(
            logRange = logRange,
            anchor = anchorStartingAt(offsetMillis = 100_000L),
            videoDurationMillis = 60_000L,
        )

        assertThat(overlap.overlap).isNull()
        assertThat(overlap.hasOverlap).isFalse()
        assertThat(overlap.logRange).isEqualTo(logRange)
        assertThat(overlap.videoRange).isEqualTo(
            TimeRange(
                start = TestLogEntries.at(offsetMillis = 100_000L),
                end = TestLogEntries.at(offsetMillis = 160_000L),
            ),
        )
    }

    @Test
    fun `a video recorded before the log has no overlap`() {
        val overlap = resolveOverlap(
            logRange = logRange,
            anchor = anchorStartingAt(offsetMillis = -100_000L),
            videoDurationMillis = 60_000L,
        )

        assertThat(overlap.overlap).isNull()
        assertThat(overlap.hasOverlap).isFalse()
    }

    @Test
    fun `without an anchor there is no video range and no overlap`() {
        val overlap = resolveOverlap(logRange = logRange, anchor = null, videoDurationMillis = 60_000L)

        assertThat(overlap.videoRange).isNull()
        assertThat(overlap.overlap).isNull()
        assertThat(overlap.hasOverlap).isFalse()
        assertThat(overlap.logRange).isEqualTo(logRange)
    }

    @Test
    fun `without a log range there is no overlap but the video range is known`() {
        val overlap = resolveOverlap(
            logRange = null,
            anchor = anchorStartingAt(offsetMillis = 0L),
            videoDurationMillis = 60_000L,
        )

        assertThat(overlap.logRange).isNull()
        assertThat(overlap.overlap).isNull()
        assertThat(overlap.hasOverlap).isFalse()
        assertThat(overlap.videoRange).isEqualTo(
            TimeRange(start = TestLogEntries.BASE, end = TestLogEntries.at(offsetMillis = 60_000L)),
        )
    }

    @Test
    fun `an unknown video duration leaves the video range undefined`() {
        // A non positive duration means "not probed yet", consistently with
        // MapLogTimeToVideoPositionUseCase, which then leaves the upper bound open.
        val overlap = resolveOverlap(
            logRange = logRange,
            anchor = anchorStartingAt(offsetMillis = 10_000L),
            videoDurationMillis = 0L,
        )

        assertThat(overlap.videoRange).isNull()
        assertThat(overlap.overlap).isNull()
        assertThat(overlap.hasOverlap).isFalse()
    }
}
