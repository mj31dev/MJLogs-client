package dev.mj31.logger.client.data.sync.screen

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.data.source.video.FFmpegVideoFrameScanner
import dev.mj31.logger.client.data.source.video.FFmpegVideoMetadataSource
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.sync.screen.ClockRegionPresets
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

/**
 * The one assertion the whole automatic synchronization rests on: that the clock a real device
 * recording shows can actually be read off it.
 *
 * Everything else in the feature is arithmetic over the numbers this produces, so it is worth
 * testing against a genuine screen recording rather than a drawing of one — real font, real
 * compression artefacts, real status bar.
 */
class TesseractScreenClockReaderTest {

    private val reader = TesseractScreenClockReader(dataDirectory = RepositoryFiles.tessdataDirectory)

    @AfterTest
    fun tearDown() = reader.release()

    @Test
    fun `the bundled model is found`() {
        assertThat(reader.isAvailable).isTrue()
    }

    @Test
    fun `the clock of a device recording is read from the preset region`() = runTest {
        val scan = FFmpegVideoFrameScanner(dispatcher = Dispatchers.IO).open(media = screencast())
        assertThat(scan).isNotNull()
        requireNotNull(scan)

        try {
            println("duration ${scan.durationMillis}")
            var found = 0
            for (position in 0L..scan.durationMillis step STEP_MILLIS) {
                val frame = scan.frameAt(positionMillis = position) ?: continue
                val readings = ClockRegionPresets.ordered.map { region -> reader.read(frame = frame, region = region) }
                println("  $position (${frame.width}x${frame.height}) -> $readings")
                if (readings.any { it != null }) found += 1
            }

            assertThat(found).isGreaterThan(0)
        } finally {
            scan.close()
        }
    }

    @Test
    fun `the recording declares when it was made`() = runTest {
        val metadata = FFmpegVideoMetadataSource(dispatcher = Dispatchers.IO).read(media = screencast())

        println("metadata = $metadata")
        assertThat(metadata).isNotNull()
    }

    private fun screencast(): VideoMedia {
        val file = RepositoryFiles.sample(name = SCREENCAST)
        return VideoMedia(path = file.absolutePath, name = SCREENCAST)
    }

    private companion object {
        const val SCREENCAST = "device-screencast.mov"
        const val STEP_MILLIS = 5_000L
    }
}
