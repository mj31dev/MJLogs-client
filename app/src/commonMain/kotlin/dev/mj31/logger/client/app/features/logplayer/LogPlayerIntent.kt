package dev.mj31.logger.client.app.features.logplayer

import dev.mj31.logger.client.domain.model.log.LogFilter

/**
 * Every user action the screen can produce.
 *
 * The view never calls a behaviour method directly: it emits an intent, [LogPlayerStore.handleIntent]
 * is the single place where intents turn into state, and the resulting state flows back to the view.
 */
sealed interface LogPlayerIntent {

    /** Asks for the native screencast picker; answered with [LogPlayerEffect.PickVideoFile]. */
    data object RequestVideoImport : LogPlayerIntent

    /** Asks for the native log picker; answered with [LogPlayerEffect.PickLogFiles]. */
    data object RequestLogImport : LogPlayerIntent

    data class ImportVideo(val path: String) : LogPlayerIntent

    data class ImportLogFiles(val paths: List<String>) : LogPlayerIntent

    /** The user edited the format under construction; the preview follows every keystroke. */
    data class UpdateFormatDraft(
        val timestampPattern: String,
        val structureTemplate: String,
    ) : LogPlayerIntent

    /** Imports the pending file with the format currently drafted in the dialog. */
    data object SubmitManualFormat : LogPlayerIntent

    /** Keeps the file as the detector read it, components the user confirmed are simply absent. */
    data object AcceptDetectedFormat : LogPlayerIntent

    data object DismissFormatRequest : LogPlayerIntent

    data class UpdateFilter(val filter: LogFilter) : LogPlayerIntent

    data class SetTimeWindow(val windowMillis: Long?) : LogPlayerIntent

    data class SelectEntry(val entryId: String?) : LogPlayerIntent

    data object TogglePlayback : LogPlayerIntent

    data class Seek(val positionMillis: Long) : LogPlayerIntent

    /** Pins the selected record to the current playhead. */
    data object Synchronize : LogPlayerIntent

    /** The user is typing the wall clock time visible in the current frame. */
    data class UpdateFrameTime(val text: String) : LogPlayerIntent

    /** The user picked a date and a time with the mouse instead of typing them. */
    data class PickFrameTime(
        val dateMillis: Long,
        val hour: Int,
        val minute: Int,
    ) : LogPlayerIntent

    /** Pins the typed frame time to the current playhead, no log record involved. */
    data object SynchronizeAtFrameTime : LogPlayerIntent

    data object ClearSynchronization : LogPlayerIntent

    data class SetFollowVideo(val enabled: Boolean) : LogPlayerIntent
}
