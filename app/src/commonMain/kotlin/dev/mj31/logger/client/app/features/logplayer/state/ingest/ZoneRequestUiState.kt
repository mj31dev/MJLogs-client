package dev.mj31.logger.client.app.features.logplayer.state.ingest

import dev.mj31.logger.client.domain.model.time.SourceZone

/**
 * A time zone being chosen.
 *
 * [subject] names what the zone is for — a file name, or nothing for the screen's clock. [current]
 * carries where the present zone came from, because "UTC, since nothing said otherwise" and "UTC,
 * as chosen" read the same and are answered differently: only the second can be handed back to
 * the file. [availableZoneIds] is every region the platform knows, sorted.
 */
data class ZoneRequestUiState(
    val target: ZoneTarget,
    val subject: String?,
    val current: SourceZone,
    val availableZoneIds: List<String>,
)
