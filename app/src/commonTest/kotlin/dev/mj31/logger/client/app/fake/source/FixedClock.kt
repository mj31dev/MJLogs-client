package dev.mj31.logger.client.app.fake.source

import kotlin.time.Clock
import kotlin.time.Instant

/** [Clock] frozen at a single instant so that the derived reference date is deterministic. */
class FixedClock(
    private val instant: Instant,
) : Clock {

    override fun now(): Instant = instant
}
