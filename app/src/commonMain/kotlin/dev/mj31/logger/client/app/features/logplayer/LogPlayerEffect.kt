package dev.mj31.logger.client.app.features.logplayer

import dev.mj31.logger.client.app.view.text.UiText

/**
 * One-shot events that must not be part of the state.
 *
 * They are consumed exactly once: replaying them after a recomposition or a window resize would
 * show a stale notification or reopen a file dialog.
 */
sealed interface LogPlayerEffect {

    /**
     * Transient notification, rendered as a dismissible bar by the screen.
     *
     * [isError] only drives the styling: a failure has to be distinguishable from a confirmation.
     */
    data class ShowMessage(
        val text: UiText,
        val isError: Boolean = false,
    ) : LogPlayerEffect

    /** The platform layer has to open its native screencast picker. */
    data object PickVideoFile : LogPlayerEffect

    /** The platform layer has to open its native log file picker. */
    data object PickLogFiles : LogPlayerEffect

    /** The platform layer has to ask where a session file of [kind] should be written. */
    data object PickSessionSaveTarget : LogPlayerEffect

    /** The platform layer has to open its native picker for saved session files. */
    data object PickSessionFile : LogPlayerEffect
}
