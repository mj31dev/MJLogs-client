package dev.mj31.logger.client.app.fake.video

import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.sync.screen.ClockRegion
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReader
import dev.mj31.logger.client.domain.sync.screen.ScreenClockTime

/**
 * The recognizer a build without a bundled model has: present, and unable to read anything.
 *
 * It is the default in tests for the same reason it is the honest default in the application — the
 * screen has to keep working when the picture cannot be read, and that path deserves to be the one
 * most tests exercise by accident.
 */
class SilentClockReader(override val isAvailable: Boolean = false) : ScreenClockReader {

    override fun read(frame: VideoFrame, region: ClockRegion): ScreenClockTime? = null

    override fun release() = Unit
}
