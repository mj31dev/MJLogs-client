package dev.mj31.logger.client.app.features.logplayer

import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.player.VideoStep

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

    /** Nudges the playhead by whole frames or whole seconds; negative [steps] move it back. */
    data class StepVideo(val step: VideoStep, val steps: Int) : LogPlayerIntent

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

    /** Finds the anchor without help: the container's creation time, then the clock on the screen. */
    data object SynchronizeAutomatically : LogPlayerIntent

    /**
     * Replaces an anchor good to a second with one good to a frame, by finding the moment the clock
     * on the screen changed minute.
     */
    data object RefineWithScreenClock : LogPlayerIntent

    /** Puts the screen into the mode where the user draws a rectangle around the clock. */
    data object RequestClockRegion : LogPlayerIntent

    /** The rectangle the user drew, in fractions of the frame. */
    data class SetClockRegion(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) : LogPlayerIntent

    data object CancelClockRegion : LogPlayerIntent

    data object CancelAutoSync : LogPlayerIntent

    /**
     * Everything that treats the workspace as a file rather than as something to look at.
     *
     * Grouped so that the store can hand the whole family to the one collaborator that knows about
     * persistence, instead of listing six more cases beside the playback ones.
     */
    sealed interface Workspace : LogPlayerIntent

    /** Empties the workspace; the session file it belonged to is written and let go, not abandoned. */
    data object StartNewSession : Workspace

    /** Reopens the workspace that was on screen when the application was last closed. */
    data object ContinueLastSession : Workspace

    /** Asks where to write a new session file; answered with [LogPlayerEffect.PickSessionSaveTarget]. */
    data object RequestSaveSession : Workspace

    data class SaveSession(val path: String) : Workspace

    /** Brings the package the workspace already belongs to up to date with what is on screen. */
    data object SaveSessionChanges : Workspace

    data object CancelSessionSave : Workspace

    /** Asks for a session file to open; answered with [LogPlayerEffect.PickSessionFile]. */
    data object RequestOpenSession : Workspace

    data class OpenSession(val path: String) : Workspace
}
