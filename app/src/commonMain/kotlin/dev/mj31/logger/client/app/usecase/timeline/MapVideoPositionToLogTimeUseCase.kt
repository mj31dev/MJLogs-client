package dev.mj31.logger.client.app.usecase.timeline

import dev.mj31.logger.client.domain.sync.SyncAnchor
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** Converts a video position into the wall clock instant it corresponds to. */
class MapVideoPositionToLogTimeUseCase {

    operator fun invoke(anchor: SyncAnchor, videoPositionMillis: Long): Instant =
        anchor.videoStartInstant + videoPositionMillis.milliseconds
}
