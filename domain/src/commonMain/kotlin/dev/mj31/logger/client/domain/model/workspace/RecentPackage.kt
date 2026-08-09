package dev.mj31.logger.client.domain.model.workspace

import kotlin.time.Instant

/**
 * A saved session file the application has opened before.
 *
 * Only files that were actually opened are listed: a session file is an ordinary file that can live
 * anywhere, so the application cannot know about the ones it was never handed.
 */
data class RecentPackage(
    val path: String,
    val name: String,
    val lastOpened: Instant,
)
