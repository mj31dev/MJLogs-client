package dev.mj31.logger.client.app.features.logplayer

import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerLocalState
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_auto_sync_unavailable
import dev.mj31.logger.client.app.resources.message_auto_synchronized_clock
import dev.mj31.logger.client.app.resources.message_auto_synchronized_metadata
import dev.mj31.logger.client.app.resources.message_clock_not_found
import dev.mj31.logger.client.app.resources.message_no_minute_change
import dev.mj31.logger.client.app.resources.message_recognizer_missing
import dev.mj31.logger.client.app.resources.message_video_unreadable
import dev.mj31.logger.client.app.usecase.sync.auto.AutoSyncOutcome
import dev.mj31.logger.client.app.usecase.sync.auto.AutoSynchronizeUseCase
import dev.mj31.logger.client.app.view.format.formatLogDateTime
import dev.mj31.logger.client.app.view.text.UiText
import org.jetbrains.compose.resources.StringResource
import dev.mj31.logger.client.domain.model.log.LogSession
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import dev.mj31.logger.client.domain.sync.screen.ClockRegion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The part of the cycle that looks for an anchor instead of being told one.
 *
 * It lives beside [LogPlayerStore] rather than inside it because it is the only thing on this screen
 * that runs for seconds, can be cancelled halfway, and ends in half a dozen different ways. The
 * store still owns the cycle — every one of these is reached from `handleIntent` and every result
 * lands in the same state and the same effects.
 *
 * Nothing here disables anything. A scan holds its own decoder, so the recording plays throughout,
 * and an anchor the user places by hand mid-scan simply wins: it is the more recent answer.
 */
class AutoSyncHandler(
    private val local: MutableStateFlow<LogPlayerLocalState>,
    private val autoSynchronize: AutoSynchronizeUseCase,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val emit: (LogPlayerEffect) -> Unit,
    private val seekTo: (Long) -> Unit,
) {

    private var job: Job? = null

    val isRunning: Boolean
        get() = job?.isActive == true

    /** The whole cascade: the container's creation time first, then the clock on the screen. */
    fun automatic(media: VideoMedia, session: LogSession) = launchScan(session = session) {
        autoSynchronize.automatic(media = media, session = session)
    }

    /** Trades an anchor good to a second for one good to a frame; never runs unasked. */
    fun refine(media: VideoMedia, session: LogSession, region: ClockRegion?) = launchScan(session = session) {
        autoSynchronize.refine(media = media, session = session, region = region)
    }

    /** A rectangle the user drew: kept for this screencast, and read from immediately. */
    fun applyRegion(media: VideoMedia, session: LogSession, drawn: LogPlayerIntent.SetClockRegion) {
        val region = ClockRegion(
            left = drawn.left,
            top = drawn.top,
            right = drawn.right,
            bottom = drawn.bottom,
        )
        local.update { current ->
            current.copy(
                isSelectingClockRegion = false,
                clockRegion = region.takeIf { it.isValid } ?: current.clockRegion,
            )
        }
        if (region.isValid) refine(media = media, session = session, region = region)
    }

    fun cancel() {
        job?.cancel()
        job = null
        local.update { it.copy(isScanningClock = false) }
    }

    /** A screencast the user replaced takes every conclusion drawn about it with it. */
    fun forget() {
        cancel()
        local.update {
            it.copy(
                clockRegion = null,
                isSelectingClockRegion = false,
            )
        }
    }

    /** One scan at a time, on the thread the recognizer and the scanning decoder both belong to. */
    private fun launchScan(session: LogSession, block: suspend () -> AutoSyncOutcome) {
        if (session.isEmpty || isRunning) return
        local.update { it.copy(isScanningClock = true) }
        job = scope.launch {
            val outcome = withContext(context = dispatcher) { block() }
            local.update { it.copy(isScanningClock = false) }
            report(outcome = outcome)
        }
    }

    private fun report(outcome: AutoSyncOutcome) {
        when (outcome) {
            is AutoSyncOutcome.Synchronized -> {
                show(anchor = outcome.anchor)
                emit(LogPlayerEffect.ShowMessage(text = synchronizedMessage(anchor = outcome.anchor)))
            }

            AutoSyncOutcome.ClockNotFound -> {
                local.update { it.copy(isSelectingClockRegion = true) }
                emit(LogPlayerEffect.ShowMessage(text = UiText.Resource(resource = Res.string.message_clock_not_found)))
            }

            AutoSyncOutcome.NoMinuteChange -> emit(
                LogPlayerEffect.ShowMessage(text = UiText.Resource(resource = Res.string.message_no_minute_change)),
            )

            AutoSyncOutcome.NothingToCorrelate -> failed(resource = Res.string.message_auto_sync_unavailable)
            AutoSyncOutcome.RecognizerMissing -> failed(resource = Res.string.message_recognizer_missing)
            AutoSyncOutcome.VideoUnreadable -> failed(resource = Res.string.message_video_unreadable)
        }
    }

    /**
     * A refusal names its own reason.
     *
     * The three ways this can fail have nothing in common but their outcome — a workspace missing a
     * file, a build missing its model, a recording that will not open twice — and telling a user who
     * has loaded both files that they need to load both files is worse than saying nothing.
     */
    private fun failed(resource: StringResource) {
        emit(LogPlayerEffect.ShowMessage(text = UiText.Resource(resource = resource), isError = true))
    }

    /**
     * Puts the found moment where the user can see it, check it, and correct it.
     *
     * The frame time field is the manual way of stating exactly this — the wall clock time a frame
     * shows — so an automatic reading belongs in it: it turns a result the user has to take on trust
     * into one they can compare against the picture, and edit if the recognizer was a digit out.
     *
     * The playhead moves to the frame the reading came from, and that is not a flourish. The field
     * describes *this* frame, and pressing "Use this time" pins whatever it holds to wherever the
     * playhead is; filling it without moving there would leave a field that contradicts the picture
     * above it, and one click from a wrong anchor.
     */
    private fun show(anchor: SyncAnchor) {
        local.update {
            it.copy(frameTime = formatLogDateTime(instant = anchor.logTimestamp), frameTimeError = false)
        }
        seekTo(anchor.videoPositionMillis)
    }

    /**
     * Confirmation of an anchor nobody placed by hand.
     *
     * It names the evidence, because the two sources are worth different amounts and the user is the
     * one who has to decide whether a second of uncertainty matters for what they are looking at.
     */
    private fun synchronizedMessage(anchor: SyncAnchor): UiText = UiText.Resource(
        resource = when (anchor.origin) {
            SyncOrigin.VIDEO_METADATA -> Res.string.message_auto_synchronized_metadata
            else -> Res.string.message_auto_synchronized_clock
        },
        arguments = listOf(formatLogDateTime(instant = anchor.logTimestamp)),
    )
}
