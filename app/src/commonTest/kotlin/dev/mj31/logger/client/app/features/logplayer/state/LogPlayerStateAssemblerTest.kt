package dev.mj31.logger.client.app.features.logplayer.state

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.domain.player.PlaybackState
import dev.mj31.logger.client.domain.player.PlaybackStatus
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import dev.mj31.logger.client.domain.sync.SyncState
import kotlin.test.Test
import dev.mj31.logger.client.app.usecase.timeline.ResolveTimelineOverlapUseCase
import dev.mj31.logger.client.app.usecase.timeline.MapVideoPositionToLogTimeUseCase
import dev.mj31.logger.client.app.usecase.timeline.FindEntryAtVideoPositionUseCase
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.app.usecase.sync.manual.ParseFrameTimeUseCase

class LogPlayerStateAssemblerTest {

    private val assembler = LogPlayerStateAssembler(
        parseFrameTime = ParseFrameTimeUseCase(),
        findEntryAtVideoPosition = FindEntryAtVideoPositionUseCase(),
        mapVideoPositionToLogTime = MapVideoPositionToLogTimeUseCase(),
        resolveTimelineOverlap = ResolveTimelineOverlapUseCase(),
    )

    private val entries = listOf(
        LogPlayerFixtures.entry(id = "a", lineNumber = 1, offsetMillis = 0L),
        LogPlayerFixtures.entry(id = "b", lineNumber = 2, offsetMillis = 10_000L),
        LogPlayerFixtures.entry(id = "c", lineNumber = 3, offsetMillis = 20_000L),
    )

    private val session = MergeLogSourcesUseCase()(
        sources = listOf(
            LogPlayerFixtures.source(entries = entries),
            LogPlayerFixtures.source(
                id = LogPlayerFixtures.SECOND_SOURCE_ID,
                name = LogPlayerFixtures.SECOND_NAME,
                path = LogPlayerFixtures.SECOND_PATH,
            ),
        ),
    )

    @Test
    fun `without an anchor no record is active`() {
        val state = assemble(syncState = SyncState.Unsynced, positionMillis = 15_000L)

        assertThat(state.activeEntryId).isNull()
        assertThat(state.sync.isSynced).isFalse()
        assertThat(state.sync.logTimeAtPlayhead).isNull()
    }

    @Test
    fun `with an anchor the active record is the last one before the playhead`() {
        val state = assemble(syncState = syncedAtStart(), positionMillis = 15_000L)

        assertThat(state.activeEntryId).isEqualTo("b")
        assertThat(state.sync.logTimeAtPlayhead).isEqualTo(LogPlayerFixtures.at(offsetMillis = 15_000L))
    }

    @Test
    fun `an empty source selection marks every source as visible`() {
        val state = assemble(syncState = SyncState.Unsynced, positionMillis = 0L)

        assertThat(state.sources.map { it.isSelected }).containsExactly(true, true)
    }

    @Test
    fun `an explicit source selection marks only the chosen sources`() {
        val state = assemble(
            syncState = SyncState.Unsynced,
            positionMillis = 0L,
            filter = LogFilter(sourceIds = setOf(LogPlayerFixtures.SECOND_SOURCE_ID)),
        )

        assertThat(state.sources.map { it.isSelected }).containsExactly(false, true).inOrder()
    }

    @Test
    fun `synchronizing is only offered with both a selected record and a screencast`() {
        val withoutSelection = assemble(syncState = SyncState.Unsynced, positionMillis = 0L)
        assertThat(withoutSelection.sync.canSynchronize).isFalse()

        val ready = assemble(
            syncState = SyncState.Unsynced,
            positionMillis = 0L,
            selectedEntryId = "a",
        )
        assertThat(ready.sync.canSynchronize).isTrue()

        val withoutVideo = assembler.assemble(
            session = session,
            visibleEntries = entries,
            video = VideoSnapshot(media = null, playback = PlaybackState.IDLE),
            syncState = SyncState.Unsynced,
            local = LogPlayerLocalState(selectedEntryId = "a"),
        )
        assertThat(withoutVideo.sync.canSynchronize).isFalse()
    }

    private fun syncedAtStart(): SyncState.Synced = SyncState.Synced(
        anchor = SyncAnchor(
            logTimestamp = LogPlayerFixtures.at(offsetMillis = 0L),
            videoPositionMillis = 0L,
            origin = SyncOrigin.SELECTED_ENTRY,
            logEntryId = "a",
        ),
    )

    private fun assemble(
        syncState: SyncState,
        positionMillis: Long,
        filter: LogFilter = LogFilter(),
        selectedEntryId: String? = null,
    ): LogPlayerState = assembler.assemble(
        session = session,
        visibleEntries = entries,
        video = VideoSnapshot(
            media = VideoMedia(path = "/media/clip.mp4", name = "clip.mp4"),
            playback = PlaybackState(
                status = PlaybackStatus.PAUSED,
                positionMillis = positionMillis,
                durationMillis = 60_000L,
            ),
        ),
        syncState = syncState,
        local = LogPlayerLocalState(filter = filter, selectedEntryId = selectedEntryId),
    )
}
