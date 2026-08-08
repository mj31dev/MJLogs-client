package dev.mj31.logger.client.app.usecase.sync.auto

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.usecase.sync.auto.screen.FindMinuteChangeUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.LocateClockRegionUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.ReadClockUseCase
import dev.mj31.logger.client.data.source.video.FFmpegVideoFrameScanner
import dev.mj31.logger.client.data.sync.screen.TesseractScreenClockReader
import dev.mj31.logger.client.domain.source.video.VideoScan
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime

/**
 * The automatic synchronization measured against real screen recordings rather than against a
 * drawing of one.
 *
 * Every number the feature produces comes out of these two steps, and both of them are guesses about
 * a picture: which part of the frame holds a clock, and which frame that clock changed on. A test
 * that fed them a synthetic status bar would confirm the arithmetic and miss everything that
 * actually goes wrong — a recognizer that reads `01:09` off a clock showing `09:28`, a stretch of
 * frames during a screen transition where nothing is readable at all.
 */
class ScreenClockScanTest {

    private val reader = TesseractScreenClockReader(dataDirectory = SampleRecordings.tessdataDirectory)
    private val readClock = ReadClockUseCase(reader = reader)
    private val locateClockRegion = LocateClockRegionUseCase(readClock = readClock)
    private val findMinuteChange = FindMinuteChangeUseCase(readClock = readClock)
    private val scanner = FFmpegVideoFrameScanner(dispatcher = Dispatchers.IO)

    @AfterTest
    fun tearDown() = reader.release()

    @Test
    fun `the clock of a phone recording is located and its minute change pinned to a frame`() = runTest {
        withScan(name = SampleRecordings.WITH_CLOCK) { scan ->
            val region = locateClockRegion(
                scan = scan,
                spanMillis = scan.durationMillis,
            )
            assertThat(region).isNotNull()
            requireNotNull(region)

            val change = findMinuteChange(scan = scan, region = region)
            assertThat(change).isNotNull()
            requireNotNull(change)

            assertThat(change.reading.time.second).isEqualTo(0)
            assertThat(change.accuracyMillis).isAtMost(500L)
            assertThat(change.reading.positionMillis).isIn(0L..FindMinuteChangeUseCase.WINDOW_MILLIS)
        }
    }

    /**
     * The minute reported is the one the recording goes on showing, not a flicker the search settled
     * on: read the clock again a few seconds later and it says the same thing.
     *
     * The other half of the property — that the minute before the change is one lower — is asserted
     * against a scripted clock instead, in [MinuteChangeSearchTest]. It cannot be asserted here: the
     * change in this recording falls within half a second of the first frame, and there is no room
     * left in front of it to read.
     */
    @Test
    fun `the minute found is the one the recording keeps showing`() = runTest {
        withScan(name = SampleRecordings.WITH_CLOCK) { scan ->
            val region = requireNotNull(
                locateClockRegion(
                    scan = scan,
                    spanMillis = scan.durationMillis,
                ),
            )
            val change = requireNotNull(findMinuteChange(scan = scan, region = region))
            val after = readClock(
                scan = scan,
                region = region,
                positionMillis = change.reading.positionMillis + AFTER_MILLIS,
            )

            assertThat(after?.time).isEqualTo(change.reading.time)
        }
    }

    @Test
    fun `a recording between two ticks reports no minute change`() = runTest {
        withScan(name = SampleRecordings.WITHOUT_MINUTE_CHANGE) { scan ->
            val region = locateClockRegion(scan = scan, spanMillis = scan.durationMillis)
            assertThat(region).isNotNull()

            assertThat(findMinuteChange(scan = scan, region = requireNotNull(region))).isNull()
        }
    }

    /**
     * The clock is read correctly, not merely read.
     *
     * This recording was captured at a known moment, and a narrow crop aimed at where an iPhone
     * "should" keep its clock used to return `00:54` for it — every frame, consistently, because the
     * crop clipped the status bar's back-to-app chip and the time together and the recognizer made
     * one string of the two. Nothing downstream could have told that from an answer.
     */
    @Test
    fun `the clock of the short recording reads the time it was captured at`() = runTest {
        withScan(name = SampleRecordings.WITHOUT_MINUTE_CHANGE) { scan ->
            val region = requireNotNull(locateClockRegion(scan = scan, spanMillis = scan.durationMillis))

            val clock = readClock(scan = scan, region = region, positionMillis = scan.durationMillis / 2)

            assertThat(clock?.time).isEqualTo(SHORT_RECORDING_CLOCK)
        }
    }

    @Test
    fun `a recording without a status bar yields no region to read`() = runTest {
        withScan(name = SampleRecordings.WITHOUT_CLOCK) { scan ->
            assertThat(locateClockRegion(scan = scan, spanMillis = scan.durationMillis)).isNull()
        }
    }

    private suspend fun withScan(name: String, block: suspend (VideoScan) -> Unit) {
        val scan = requireNotNull(scanner.open(media = SampleRecordings.media(name = name)))
        try {
            block(scan)
        } finally {
            scan.close()
        }
    }

    private companion object {
        const val AFTER_MILLIS = 3_000L

        /** The short sample was captured at 08:44, between two ticks of the minute. */
        val SHORT_RECORDING_CLOCK = LocalTime(hour = 8, minute = 44)
    }
}
