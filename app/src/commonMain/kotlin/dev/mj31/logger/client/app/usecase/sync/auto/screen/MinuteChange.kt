package dev.mj31.logger.client.app.usecase.sync.auto.screen

import dev.mj31.logger.client.domain.sync.screen.ScreenClockReading

/**
 * The moment a clock on screen turned over, and how tightly that moment could be pinned down.
 *
 * [accuracyMillis] is the width of the bracket the search finished with, not an estimate: the change
 * happened somewhere inside it. It normally comes out at a couple of frames, and stays honest when
 * an unreadable stretch stopped the bisection early — an anchor good to a second is still worth
 * having, as long as it says so.
 */
data class MinuteChange(
    val reading: ScreenClockReading,
    val accuracyMillis: Long,
)
