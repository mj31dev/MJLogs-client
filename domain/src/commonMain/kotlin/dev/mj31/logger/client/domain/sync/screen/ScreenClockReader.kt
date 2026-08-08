package dev.mj31.logger.client.domain.sync.screen

import dev.mj31.logger.client.domain.player.VideoFrame

/**
 * Reads the wall clock a frame displays.
 *
 * The exact same operation a human performs when they look at the corner of a screencast and type
 * what they see into the frame time field — which is why an anchor built from it is worth as much
 * as the one built by hand, and lands in the same [dev.mj31.logger.client.domain.sync.SyncAnchor].
 *
 * Implementations accept every spelling a device may use — `21:41`, `9:41 PM`, and the bare `2:39`
 * an iPhone shows with no meridiem anywhere beside it — and return `null` whenever the region holds
 * no time at all, which is the ordinary case while the recognizer is still looking for the right
 * place to read. What they never do is guess which half of the day a bare reading means: that is
 * reported through [ScreenClockTime] and settled against the logs.
 */
interface ScreenClockReader {

    val isAvailable: Boolean

    fun read(frame: VideoFrame, region: ClockRegion): ScreenClockTime?

    fun release()
}
