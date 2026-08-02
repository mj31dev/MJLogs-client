package dev.mj31.logger.client.app.features.logplayer

import dev.mj31.logger.client.app.features.logplayer.state.format.FormatDefaults
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatError
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerLocalState
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerState
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerStateAssembler
import dev.mj31.logger.client.app.features.logplayer.state.VideoSnapshot
import dev.mj31.logger.client.app.usecase.ingest.LogImportResult
import dev.mj31.logger.client.domain.format.compile.FormatCompilationResult
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.model.log.LogEntry
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.log.LogSession
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.domain.player.VideoFrame
import dev.mj31.logger.client.domain.player.VideoPlayer
import dev.mj31.logger.client.domain.source.MediaKind
import dev.mj31.logger.client.domain.source.SupportedFileTypes
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerRepositories
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerUseCases
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerFormatTools
import dev.mj31.logger.client.app.view.text.UiText
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_import_success
import dev.mj31.logger.client.app.resources.message_load_screencast_first
import dev.mj31.logger.client.app.resources.message_record_outside_video
import dev.mj31.logger.client.app.resources.message_select_record_first
import dev.mj31.logger.client.app.resources.message_synchronized_with_line
import dev.mj31.logger.client.app.view.format.formatLogDateTime
import dev.mj31.logger.client.app.resources.message_synchronized_with_time
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * MVI store of the single PoC screen.
 *
 * The cycle is strictly unidirectional: the view emits a [LogPlayerIntent] into [handleIntent], the
 * store updates its own state or delegates to a domain use case, and the view re-renders from
 * [state]. Anything that must happen exactly once leaves through [effects].
 *
 * The store owns no business rule: every decision belongs to a use case, so the same behaviour
 * survives a change of UI toolkit or of storage backend.
 */
class LogPlayerStore(
    private val repositories: LogPlayerRepositories,
    private val useCases: LogPlayerUseCases,
    private val player: VideoPlayer,
    private val formatTools: LogPlayerFormatTools,
    private val stateAssembler: LogPlayerStateAssembler,
    private val scope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher,
) {

    private val local = MutableStateFlow(value = LogPlayerLocalState())
    private val effectChannel = Channel<LogPlayerEffect>(capacity = Channel.BUFFERED)

    /**
     * Filter actually applied to the session: the user criteria plus, when a time window is active
     * and both timelines are synchronized, a range centred on the current playhead.
     */
    private val effectiveFilter: Flow<LogFilter> = combine(
        local.map { it.filter }.distinctUntilChanged(),
        local.map { it.timeWindowMillis }.distinctUntilChanged(),
        repositories.sync.syncState,
        player.state.map { it.positionMillis / WINDOW_TICK_MILLIS }.distinctUntilChanged(),
    ) { filter, window, syncState, positionTicks ->
        val anchor = syncState.anchorOrNull
        if (window == null || anchor == null) {
            filter.copy(timeRange = null)
        } else {
            val centre = useCases.mapVideoPositionToLogTime(
                anchor = anchor,
                videoPositionMillis = positionTicks * WINDOW_TICK_MILLIS,
            )
            filter.copy(
                timeRange = TimeRange(
                    start = centre - window.milliseconds,
                    end = centre + window.milliseconds,
                ),
            )
        }
    }

    /** Storage keeps the files apart; the chronological session is assembled here. */
    private val session: StateFlow<LogSession> = repositories.session.sources
        .map { sources -> useCases.mergeLogSources(sources = sources) }
        .flowOn(context = defaultDispatcher)
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = LogSession.EMPTY)

    private val visibleEntries: StateFlow<List<LogEntry>> = combine(
        session,
        effectiveFilter,
    ) { session, filter ->
        useCases.filterLogEntries(entries = session.entries, filter = filter)
    }
        .flowOn(context = defaultDispatcher)
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    /** The single immutable snapshot the view renders. */
    val state: StateFlow<LogPlayerState> = combine(
        session,
        visibleEntries,
        combine(repositories.video.media, player.state) { media, playback ->
            VideoSnapshot(media = media, playback = playback)
        },
        repositories.sync.syncState,
        local,
    ) { session, entries, video, syncState, localState ->
        stateAssembler.assemble(
            session = session,
            visibleEntries = entries,
            video = video,
            syncState = syncState,
            local = localState,
        )
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = LogPlayerState())

    /** One-shot events, delivered to a single collector exactly once. */
    val effects: Flow<LogPlayerEffect> = effectChannel.receiveAsFlow()

    /** Decoded frames are exposed separately so that a redraw does not recompose the log list. */
    val frames: StateFlow<VideoFrame?> = player.frames

    /** The single entry point of the cycle: everything the user does arrives here. */
    fun handleIntent(intent: LogPlayerIntent) {
        when (intent) {
            LogPlayerIntent.RequestVideoImport -> emit(effect = LogPlayerEffect.PickVideoFile)
            LogPlayerIntent.RequestLogImport -> emit(effect = LogPlayerEffect.PickLogFiles)
            is LogPlayerIntent.ImportVideo -> importVideo(path = intent.path)
            is LogPlayerIntent.ImportLogFiles -> importLogFiles(paths = intent.paths)
            is LogPlayerIntent.UpdateFormatDraft -> updateFormatDraft(
                draft = ManualFormatInput(
                    timestampPattern = intent.timestampPattern,
                    structureTemplate = intent.structureTemplate,
                ),
            )

            LogPlayerIntent.SubmitManualFormat -> submitManualFormat()

            LogPlayerIntent.AcceptDetectedFormat -> acceptDetectedFormat()
            LogPlayerIntent.DismissFormatRequest -> dropFormatRequest()
            is LogPlayerIntent.UpdateFilter -> local.update { it.copy(filter = intent.filter) }
            is LogPlayerIntent.SetTimeWindow -> local.update { it.copy(timeWindowMillis = intent.windowMillis) }
            is LogPlayerIntent.SelectEntry -> selectEntry(entryId = intent.entryId)
            LogPlayerIntent.TogglePlayback -> togglePlayback()
            is LogPlayerIntent.Seek -> player.seekTo(positionMillis = intent.positionMillis)
            LogPlayerIntent.Synchronize -> synchronize()
            is LogPlayerIntent.UpdateFrameTime -> local.update {
                it.copy(frameTime = intent.text, frameTimeError = false)
            }
            is LogPlayerIntent.PickFrameTime -> local.update {
                it.copy(
                    frameTime = useCases.composeFrameTime(
                        dateMillis = intent.dateMillis,
                        hour = intent.hour,
                        minute = intent.minute,
                        previousText = it.frameTime,
                    ),
                    frameTimeError = false,
                )
            }
            LogPlayerIntent.SynchronizeAtFrameTime -> synchronizeAtFrameTime()
            LogPlayerIntent.ClearSynchronization -> scope.launch { useCases.clearSynchronization() }
            is LogPlayerIntent.SetFollowVideo -> local.update { it.copy(followVideo = intent.enabled) }
        }
    }

    /** Lifecycle hook rather than a user intent: releases the native playback resources. */
    fun release() = player.release()

    private fun togglePlayback() {
        if (player.state.value.isPlaying) player.pause() else player.play()
    }

    private fun acceptDetectedFormat() {
        val source = local.value.formatRequests.firstOrNull()?.detectedSource ?: return
        scope.launch {
            repositories.session.addSource(source = source)
            dropFormatRequest()
            emit(
                effect = LogPlayerEffect.ShowMessage(text = importedMessage(source = source)),
            )
        }
    }

    private fun dropFormatRequest() {
        local.update { it.copy(formatRequests = it.formatRequests.drop(n = 1)) }
    }

    private fun importLogFiles(paths: List<String>) {
        if (paths.isEmpty()) return
        scope.launch {
            local.update { it.copy(isImporting = true) }
            paths.forEach { path -> handleImportResult(result = useCases.importLogFile(path = path)) }
            local.update { it.copy(isImporting = false) }
        }
    }

    private fun importVideo(path: String) {
        val media = VideoMedia(
            path = path,
            name = path.substringAfterLast(delimiter = '/').substringAfterLast(delimiter = '\\'),
        )
        if (!SupportedFileTypes.accepts(kind = MediaKind.VIDEO, path = path)) {
            emit(
                effect = LogPlayerEffect.ShowMessage(
                    text = UiText.Raw(
                        value = SupportedFileTypes.rejectionMessage(kind = MediaKind.VIDEO, fileName = media.name),
                    ),
                    isError = true,
                ),
            )
            return
        }
        scope.launch {
            repositories.video.setMedia(media = media)
            player.open(media = media)
        }
    }

    private fun updateFormatDraft(draft: ManualFormatInput) {
        val request = local.value.formatRequests.firstOrNull() ?: return
        replaceHeadRequest(
            request = request.copy(
                timestampPattern = draft.timestampPattern,
                structureTemplate = draft.structureTemplate,
                preview = previewOf(draft = draft, sampleLines = request.sampleLines),
                error = null,
            ),
        )
    }

    private fun previewOf(draft: ManualFormatInput, sampleLines: List<String>): FormatPreview =
        formatTools.previewer.preview(input = draft, sampleLines = sampleLines)

    private fun submitManualFormat() {
        val request = local.value.formatRequests.firstOrNull() ?: return
        when (val compiled = formatTools.compiler.compile(input = request.draft)) {
            is FormatCompilationResult.Failure -> updateRequestError(
                error = FormatError(message = compiled.message, field = compiled.field),
            )

            is FormatCompilationResult.Success -> scope.launch {
                when (val result = useCases.importLogFileWithFormat(path = request.path, spec = compiled.spec)) {
                    is LogImportResult.Success -> {
                        repositories.session.addSource(source = result.source)
                        dropFormatRequest()
                    }

                    // A format the user wrote themselves needs no confirmation of what it leaves out.
                    is LogImportResult.NeedsConfirmation -> {
                        repositories.session.addSource(source = result.source)
                        dropFormatRequest()
                    }

                    // Neither input is syntactically wrong: the format simply does not fit the file.
                    is LogImportResult.Failure -> updateRequestError(
                        error = FormatError(message = result.message, field = FormatErrorField.NONE),
                    )

                    is LogImportResult.FormatRequired -> updateRequestError(
                        error = FormatError(message = result.reason, field = FormatErrorField.NONE),
                    )
                }
            }
        }
    }

    /** Selecting a record moves the video too, but only once the timelines have been synchronized. */
    private fun selectEntry(entryId: String?) {
        local.update { it.copy(selectedEntryId = entryId) }
        val entry = entryId?.let { id -> visibleEntries.value.firstOrNull { it.id == id } } ?: return
        if (!local.value.followVideo) return
        val anchor = repositories.sync.syncState.value.anchorOrNull ?: return
        val position = useCases.mapLogTimeToVideoPosition(
            anchor = anchor,
            timestamp = entry.timestamp,
            videoDurationMillis = player.state.value.durationMillis,
        )
        if (position == null) {
            emit(
                effect = LogPlayerEffect.ShowMessage(
                    text = UiText.Resource(resource = Res.string.message_record_outside_video),
                ),
            )
        } else {
            player.seekTo(positionMillis = position)
        }
    }

    /** Pins the selected record to the current playhead; from now on both timelines move together. */
    private fun synchronize() {
        val entry = local.value.selectedEntryId?.let { id -> visibleEntries.value.firstOrNull { it.id == id } }
        if (entry == null) {
            emit(
                effect = LogPlayerEffect.ShowMessage(
                    text = UiText.Resource(resource = Res.string.message_select_record_first),
                ),
            )
            return
        }
        if (!player.state.value.hasMedia) {
            emit(
                effect = LogPlayerEffect.ShowMessage(
                    text = UiText.Resource(resource = Res.string.message_load_screencast_first),
                ),
            )
            return
        }
        val position = player.state.value.positionMillis
        scope.launch {
            useCases.synchronizeTimelines(entry = entry, videoPositionMillis = position)
            emit(
                effect = LogPlayerEffect.ShowMessage(
                    text = UiText.Resource(
                        resource = Res.string.message_synchronized_with_line,
                        arguments = listOf(entry.lineNumber),
                    ),
                ),
            )
        }
    }

    /**
     * Synchronizes on a time the user read off the frame itself.
     *
     * This is the way out when the recording covers a moment no log record describes: the anchor is
     * built from the typed instant, everything downstream keeps working on the same mapping.
     */
    private fun synchronizeAtFrameTime() {
        if (!player.state.value.hasMedia) {
            emit(
                effect = LogPlayerEffect.ShowMessage(
                    text = UiText.Resource(resource = Res.string.message_load_screencast_first),
                ),
            )
            return
        }
        val timestamp = useCases.parseFrameTime(
            text = local.value.frameTime,
            referenceDate = session.value.timeRange?.start?.toLocalDateTime(timeZone = TimeZone.UTC)?.date,
        )
        if (timestamp == null) {
            local.update { it.copy(frameTimeError = true) }
            return
        }
        local.update { it.copy(frameTimeError = false) }
        val position = player.state.value.positionMillis
        scope.launch {
            useCases.synchronizeAtTimestamp(timestamp = timestamp, videoPositionMillis = position)
            emit(
                effect = LogPlayerEffect.ShowMessage(
                    text = UiText.Resource(
                        resource = Res.string.message_synchronized_with_time,
                        arguments = listOf(formatLogDateTime(instant = timestamp)),
                    ),
                ),
            )
        }
    }

    private suspend fun handleImportResult(result: LogImportResult) {
        when (result) {
            is LogImportResult.Success -> {
                repositories.session.addSource(source = result.source)
                emit(effect = LogPlayerEffect.ShowMessage(text = importedMessage(source = result.source)))
            }

            is LogImportResult.FormatRequired -> local.update {
                it.copy(formatRequests = it.formatRequests + requestOf(result = result))
            }

            is LogImportResult.NeedsConfirmation -> local.update {
                it.copy(formatRequests = it.formatRequests + confirmationOf(result = result))
            }

            is LogImportResult.Failure -> emit(
                effect = LogPlayerEffect.ShowMessage(text = UiText.Raw(value = result.message), isError = true),
            )
        }
    }

    /** Opens the dialog on the inferred layout when there is one, on the neutral default otherwise. */
    private fun requestOf(result: LogImportResult.FormatRequired): FormatRequestUiState {
        val draft = result.suggestion ?: ManualFormatInput(
            timestampPattern = FormatDefaults.TIMESTAMP_PATTERN,
            structureTemplate = FormatDefaults.STRUCTURE_TEMPLATE,
        )
        return FormatRequestUiState(
            path = result.path,
            fileName = result.fileName,
            sampleLines = result.sampleLines,
            reason = result.reason,
            timestampPattern = draft.timestampPattern,
            structureTemplate = draft.structureTemplate,
            preview = previewOf(draft = draft, sampleLines = result.sampleLines),
            suggestion = result.suggestion,
        )
    }

    /** Same dialog as an unrecognized file, but with the parsed source ready behind the accept button. */
    private fun confirmationOf(result: LogImportResult.NeedsConfirmation): FormatRequestUiState {
        val draft = result.suggestion ?: ManualFormatInput(
            timestampPattern = result.source.format.timestampPattern,
            structureTemplate = FormatDefaults.STRUCTURE_TEMPLATE,
        )
        return FormatRequestUiState(
            path = result.source.path,
            fileName = result.source.name,
            sampleLines = result.sampleLines,
            reason = result.reason,
            timestampPattern = draft.timestampPattern,
            structureTemplate = draft.structureTemplate,
            preview = previewOf(draft = draft, sampleLines = result.sampleLines),
            suggestion = result.suggestion,
            detectedSource = result.source,
        )
    }

    private fun updateRequestError(error: FormatError) {
        local.value.formatRequests.firstOrNull()?.let { head ->
            replaceHeadRequest(request = head.copy(error = error))
        }
    }

    private fun replaceHeadRequest(request: FormatRequestUiState) {
        local.update { current ->
            current.copy(formatRequests = listOf(request) + current.formatRequests.drop(n = 1))
        }
    }

    private fun emit(effect: LogPlayerEffect) {
        effectChannel.trySend(effect)
    }

    private companion object {
        const val WINDOW_TICK_MILLIS = 1_000L
    }
}

/** Confirmation shown once a file has been read: what was imported, and under which format. */
private fun importedMessage(source: LogSource): UiText = UiText.Resource(
    resource = Res.string.message_import_success,
    arguments = listOf(source.name, source.entryCount, source.format.name),
)
