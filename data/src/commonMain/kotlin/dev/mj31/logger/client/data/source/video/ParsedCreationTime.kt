package dev.mj31.logger.client.data.source.video

import kotlin.time.Instant

/**
 * What [CreationTimeParser] could establish about when a recording started.
 *
 * [offsetMinutes] is null whenever the container stated the moment in UTC and said nothing about
 * where the device was standing, which is the ordinary case.
 */
data class ParsedCreationTime(
    val instant: Instant,
    val offsetMinutes: Int?,
)
