package dev.mj31.logger.client.app.features.logplayer.sync

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.app.fake.video.FakeVideoFrameScanner
import dev.mj31.logger.client.app.fake.video.FakeVideoMetadataSource
import dev.mj31.logger.client.app.fake.video.ScriptedClockReader
import dev.mj31.logger.client.app.fake.video.ScriptedVideoScan
import dev.mj31.logger.client.app.fake.video.fakeAutoSynchronize
import dev.mj31.logger.client.app.features.logplayer.AutoSyncHandler
import dev.mj31.logger.client.app.features.logplayer.LogPlayerEffect
import dev.mj31.logger.client.app.features.logplayer.LogPlayerIntent
import dev.mj31.logger.client.app.features.logplayer.state.LogPlayerLocalState
import dev.mj31.logger.client.app.usecase.session.MergeLogSourcesUseCase
import dev.mj31.logger.client.app.usecase.sync.manual.ParseFrameTimeUseCase
import dev.mj31.logger.client.data.repository.InMemorySyncRepository
import dev.mj31.logger.client.domain.model.log.LogSession
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.media.VideoMetadata
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant

/**
 * How the screen behaves while it is looking for an anchor, and what it does with each way the
 * search can end.
 *
 * The searching itself is tested elsewhere, against scripted clocks and against a real recording.
 * What matters here is the contour: that the indicator goes up and comes down, that a result the
 * application cannot settle by itself becomes a question rather than a guess, and — above all — that
 * none of it touches the manual anchor unless it has actually concluded something.
 */
class AutoSyncHandlerTest {

    private val media = VideoMedia(path = "/clips/bug.mov", name = "bug.mov")

    private val session: LogSession = MergeLogSourcesUseCase()(
        sources = listOf(
            LogPlayerFixtures.source(
                entries = listOf(
                    LogPlayerFixtures.entry(id = "a", lineNumber = 1, offsetMillis = 0L),
                    LogPlayerFixtures.entry(id = "b", lineNumber = 2, offsetMillis = 600_000L),
                ),
            ),
        ),
    )

    @Test
    fun `a container that names a moment inside the session pins the timelines`() = runTest {
        val world = world(
            metadata = VideoMetadata(
                creationTime = LogPlayerFixtures.at(offsetMillis = 60_000L),
                creationOffsetMinutes = null,
                durationMillis = 120_000L,
                width = 588,
                height = 1_280,
            ),
        )

        world.handler.automatic(media = media, session = session)
        runCurrent()

        assertThat(world.syncRepository.syncState.value.isSynced).isTrue()
        assertThat(world.local.value.isScanningClock).isFalse()
        assertThat(world.effects).hasSize(1)
    }

    /**
     * The found moment lands in the field the user would have typed it into, and the playhead moves
     * to the frame it describes — otherwise the field would contradict the picture above it, and
     * "Use this time" would pin the right time to the wrong frame.
     */
    @Test
    fun `the found moment is shown in the frame time field on the frame it came from`() = runTest {
        val world = world(
            metadata = VideoMetadata(
                creationTime = LogPlayerFixtures.at(offsetMillis = 60_000L),
                creationOffsetMinutes = null,
                durationMillis = 120_000L,
                width = 588,
                height = 1_280,
            ),
        )

        world.handler.automatic(media = media, session = session)
        runCurrent()

        assertThat(world.local.value.frameTime).isEqualTo("2024-05-01 10:01:00.000")
        assertThat(world.local.value.frameTimeError).isFalse()
        assertThat(world.seeks).containsExactly(0L)
    }

    /**
     * What is written into the field has to be something the field can read back: the user is one
     * click from applying it by hand, and that click must reproduce the anchor rather than reject
     * the text as unparseable.
     */
    @Test
    fun `the shown moment is in the spelling the field itself accepts`() = runTest {
        val world = world(
            metadata = VideoMetadata(
                creationTime = LogPlayerFixtures.at(offsetMillis = 60_000L),
                creationOffsetMinutes = null,
                durationMillis = 120_000L,
                width = 588,
                height = 1_280,
            ),
        )

        world.handler.automatic(media = media, session = session)
        runCurrent()

        assertThat(ParseFrameTimeUseCase()(text = world.local.value.frameTime, referenceDate = null))
            .isEqualTo(LogPlayerFixtures.at(offsetMillis = 60_000L))
    }

    /**
     * A build without the model must say so. Reporting the one thing the user demonstrably did do —
     * load a screencast and logs — sends them looking for a problem that is not there.
     */
    @Test
    fun `a build without a recognizer says that, not that files are missing`() = runTest {
        val world = world(scan = ScriptedVideoScan(durationMillis = 76_000L), recognizerAvailable = false)

        world.handler.refine(media = media, session = session, region = null)
        runCurrent()

        assertThat(world.effects).hasSize(1)
        assertThat(world.local.value.isSelectingClockRegion).isFalse()
    }

    @Test
    fun `a recording nothing can be read from asks the user to point at the clock`() = runTest {
        val world = world(scan = ScriptedVideoScan(durationMillis = 76_000L))

        world.handler.refine(media = media, session = session, region = null)
        runCurrent()

        assertThat(world.local.value.isSelectingClockRegion).isTrue()
        assertThat(world.syncRepository.syncState.value.isSynced).isFalse()
    }

    /** The indicator is state, not a guess: it is up exactly while a scan is outstanding. */
    @Test
    fun `the indicator is raised for the duration of a scan and lowered after it`() = runTest {
        val world = world(scan = ScriptedVideoScan(durationMillis = 76_000L))

        world.handler.refine(media = media, session = session, region = null)
        assertThat(world.local.value.isScanningClock).isTrue()

        runCurrent()
        assertThat(world.local.value.isScanningClock).isFalse()
    }

    @Test
    fun `cancelling lowers the indicator and leaves the timelines alone`() = runTest {
        val world = world(scan = ScriptedVideoScan(durationMillis = 76_000L))

        world.handler.refine(media = media, session = session, region = null)
        world.handler.cancel()
        runCurrent()

        assertThat(world.local.value.isScanningClock).isFalse()
        assertThat(world.syncRepository.syncState.value.isSynced).isFalse()
    }

    @Test
    fun `a second scan is refused while the first is still running`() = runTest {
        val world = world(scan = ScriptedVideoScan(durationMillis = 76_000L))

        world.handler.refine(media = media, session = session, region = null)
        world.handler.automatic(media = media, session = session)
        runCurrent()

        assertThat(world.local.value.isScanningClock).isFalse()
    }

    @Test
    fun `a rectangle the user drew is remembered for the recording`() = runTest {
        val world = world(scan = ScriptedVideoScan(durationMillis = 76_000L))

        world.handler.applyRegion(
            media = media,
            session = session,
            drawn = LogPlayerIntent.SetClockRegion(left = 0.1f, top = 0.01f, right = 0.3f, bottom = 0.05f),
        )
        runCurrent()

        assertThat(world.local.value.clockRegion).isNotNull()
        assertThat(world.local.value.isSelectingClockRegion).isFalse()
    }

    /** A rectangle of nothing is not a statement about where the clock is. */
    @Test
    fun `a rectangle with no area is discarded`() = runTest {
        val world = world(scan = ScriptedVideoScan(durationMillis = 76_000L))

        world.handler.applyRegion(
            media = media,
            session = session,
            drawn = LogPlayerIntent.SetClockRegion(left = 0.2f, top = 0.02f, right = 0.2f, bottom = 0.02f),
        )
        runCurrent()

        assertThat(world.local.value.clockRegion).isNull()
        assertThat(world.local.value.isSelectingClockRegion).isFalse()
    }

    @Test
    fun `replacing the recording forgets everything concluded about the old one`() = runTest {
        val world = world(scan = ScriptedVideoScan(durationMillis = 76_000L))
        world.handler.applyRegion(
            media = media,
            session = session,
            drawn = LogPlayerIntent.SetClockRegion(left = 0.1f, top = 0.01f, right = 0.3f, bottom = 0.05f),
        )
        runCurrent()

        world.handler.forget()

        assertThat(world.local.value.clockRegion).isNull()
        assertThat(world.local.value.isScanningClock).isFalse()
    }

    private fun kotlinx.coroutines.test.TestScope.world(
        metadata: VideoMetadata? = null,
        scan: ScriptedVideoScan? = null,
        recognizerAvailable: Boolean = true,
    ): World {
        val syncRepository = InMemorySyncRepository()
        val local = MutableStateFlow(value = LogPlayerLocalState())
        val effects = mutableListOf<LogPlayerEffect>()
        val seeks = mutableListOf<Long>()
        val handler = AutoSyncHandler(
            local = local,
            autoSynchronize = fakeAutoSynchronize(
                syncRepository = syncRepository,
                metadataSource = FakeVideoMetadataSource(metadata = metadata),
                scanner = FakeVideoFrameScanner(scan = scan),
                clockReader = ScriptedClockReader(
                    startMillisOfDay = 0L,
                    unreadable = LongRange(0L, Long.MAX_VALUE),
                    isAvailable = recognizerAvailable,
                ),
            ),
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(scheduler = testScheduler),
            emit = { effect -> effects += effect },
            seekTo = { position -> seeks += position },
        )
        return World(
            handler = handler,
            local = local,
            effects = effects,
            seeks = seeks,
            syncRepository = syncRepository,
        )
    }

    private class World(
        val handler: AutoSyncHandler,
        val local: MutableStateFlow<LogPlayerLocalState>,
        val effects: MutableList<LogPlayerEffect>,
        val seeks: MutableList<Long>,
        val syncRepository: InMemorySyncRepository,
    )
}
