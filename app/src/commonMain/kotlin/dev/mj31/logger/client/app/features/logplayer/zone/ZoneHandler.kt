package dev.mj31.logger.client.app.features.logplayer.zone

import dev.mj31.logger.client.app.features.logplayer.LogPlayerEffect
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerLocalState
import dev.mj31.logger.client.app.features.logplayer.state.ingest.ZoneTarget
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.message_zone_change_failed
import dev.mj31.logger.client.app.usecase.ingest.zone.ChangeSourceZoneUseCase
import dev.mj31.logger.client.app.usecase.sync.ResolveSyncZoneUseCase
import dev.mj31.logger.client.app.view.text.UiText
import dev.mj31.logger.client.domain.model.log.LogSession
import dev.mj31.logger.client.domain.model.time.SourceZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The zone family: which zone a file, or the screen's clock, is read in.
 *
 * A file's zone changes its instants, so choosing one reads the file again; the screen's zone only
 * changes how a typed or recognized time is read, so it is simply remembered.
 */
internal class ZoneHandler(
    private val local: MutableStateFlow<LogPlayerLocalState>,
    private val session: StateFlow<LogSession>,
    private val changeSourceZone: ChangeSourceZoneUseCase,
    private val resolveSyncZone: ResolveSyncZoneUseCase,
    private val scope: CoroutineScope,
    private val emit: (LogPlayerEffect) -> Unit,
) {

    fun handle(intent: LogPlayerIntent.Zone) {
        when (intent) {
            is LogPlayerIntent.RequestSourceZone -> local.update {
                it.copy(zoneRequest = ZoneTarget.Source(sourceId = intent.sourceId))
            }
            LogPlayerIntent.RequestFrameTimeZone -> local.update { it.copy(zoneRequest = ZoneTarget.FrameTime) }
            LogPlayerIntent.DismissZoneRequest -> local.update { it.copy(zoneRequest = null) }
            is LogPlayerIntent.ChooseZone -> choose(zoneId = intent.zoneId)
        }
    }

    /** The zone the screen's clock is read in; see [ResolveSyncZoneUseCase]. */
    fun syncZone(): SourceZone = resolveSyncZone(
        sources = session.value.sources,
        selectedSourceId = local.value.selectedSourceId,
        chosenZoneId = local.value.frameTimeZoneId,
    )

    private fun choose(zoneId: String?) {
        val target = local.value.zoneRequest ?: return
        local.update { it.copy(zoneRequest = null) }
        when (target) {
            ZoneTarget.FrameTime -> local.update { it.copy(frameTimeZoneId = zoneId, frameTimeError = false) }
            is ZoneTarget.Source -> scope.launch {
                val name = session.value.sourceById(sourceId = target.sourceId)?.name ?: return@launch
                if (changeSourceZone(sourceId = target.sourceId, zoneId = zoneId) == null) {
                    emit(
                        LogPlayerEffect.ShowMessage(
                            text = UiText.Resource(resource = Res.string.message_zone_change_failed, arguments = listOf(name)),
                            isError = true,
                        ),
                    )
                }
            }
        }
    }
}
