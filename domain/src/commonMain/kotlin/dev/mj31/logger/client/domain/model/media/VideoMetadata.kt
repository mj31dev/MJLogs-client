package dev.mj31.logger.client.domain.model.media

import kotlinx.datetime.Instant

/**
 * What the container itself says about a screencast, read without playing it.
 *
 * [creationTime] is the moment the recording started, as written by the recorder. It is the cheapest
 * possible basis for an automatic synchronization, and also the least trustworthy one: phones
 * regularly write local time into a field defined as UTC, and some recorders leave it at the epoch.
 * Everything downstream therefore treats it as a candidate to be checked, never as a fact.
 *
 * [creationOffsetMinutes] is the rarer and far more valuable half of it. Some recorders — Apple's
 * among them — additionally write the creation moment with the offset the device was standing in.
 * When that is present, the time zone the screen clock belongs to is not a guess at all.
 */
data class VideoMetadata(
    val creationTime: Instant?,
    val creationOffsetMinutes: Int?,
    val durationMillis: Long,
    val width: Int,
    val height: Int,
)
