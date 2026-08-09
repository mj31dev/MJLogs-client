package dev.mj31.logger.client.app.features.sessions

import dev.mj31.logger.client.domain.model.workspace.RecentPackage

/**
 * What the start screen offers: where you were, where you have been, and a clean sheet.
 *
 * The recent list is deliberately not a list of every session that exists. A package is an ordinary
 * file that can live anywhere and be handed to anyone, so the only ones that can be listed are the
 * ones this installation has been shown — everything else is reached through the file picker.
 */
data class SessionsState(
    val lastSession: LastSessionUi? = null,
    val recent: List<RecentPackage> = emptyList(),
) {

    val isEmpty: Boolean
        get() = lastSession == null && recent.isEmpty()
}

/**
 * The workspace that was on screen when the application was last closed.
 *
 * It is offered rather than reopened. Restoring it without being asked used to be the plan, and it
 * is the wrong default for a tool opened to look at something new as often as to carry on: the list
 * puts continuing one click away and starting fresh equally close.
 *
 * It need not be a saved file at all — a workspace of loose logs is the common case — which is why
 * it is described by what it holds rather than by a path.
 */
data class LastSessionUi(
    val label: String,
    val logCount: Int,
    val hasVideo: Boolean,
)
