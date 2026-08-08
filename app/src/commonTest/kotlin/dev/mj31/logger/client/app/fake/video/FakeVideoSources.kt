package dev.mj31.logger.client.app.fake.video

import dev.mj31.logger.client.app.usecase.sync.auto.AutoSynchronizeUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.metadata.MetadataAnchorUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.FindMinuteChangeUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.LocateClockRegionUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.screen.ReadClockUseCase
import dev.mj31.logger.client.app.usecase.sync.auto.zone.ResolveClockAnchorUseCase
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.media.VideoMetadata
import dev.mj31.logger.client.domain.repository.SyncRepository
import dev.mj31.logger.client.domain.source.video.VideoFrameScanner
import dev.mj31.logger.client.domain.source.video.VideoMetadataSource
import dev.mj31.logger.client.domain.source.video.VideoScan
import dev.mj31.logger.client.domain.sync.screen.ScreenClockReader

/** A container that declares whatever the test says it declares, including nothing. */
class FakeVideoMetadataSource(var metadata: VideoMetadata? = null) : VideoMetadataSource {

    override suspend fun read(media: VideoMedia): VideoMetadata? = metadata
}

/** A file that opens into the scripted recording, or refuses to open at all. */
class FakeVideoFrameScanner(var scan: VideoScan? = null) : VideoFrameScanner {

    override suspend fun open(media: VideoMedia): VideoScan? = scan
}

/**
 * Assembles the automatic synchronization out of parts the test controls.
 *
 * Every store test needs one, and almost none of them care what it does — the default reads no
 * metadata and opens no recording, so the cascade concludes it has nothing to work with and the rest
 * of the screen behaves exactly as it did before the feature existed.
 */
fun fakeAutoSynchronize(
    syncRepository: SyncRepository,
    metadataSource: VideoMetadataSource = FakeVideoMetadataSource(),
    scanner: VideoFrameScanner = FakeVideoFrameScanner(),
    clockReader: ScreenClockReader = SilentClockReader(),
): AutoSynchronizeUseCase {
    val readClock = ReadClockUseCase(reader = clockReader)
    return AutoSynchronizeUseCase(
        metadataAnchor = MetadataAnchorUseCase(metadataSource = metadataSource),
        scanner = scanner,
        clockReader = clockReader,
        locateClockRegion = LocateClockRegionUseCase(readClock = readClock),
        findMinuteChange = FindMinuteChangeUseCase(readClock = readClock),
        resolveClockAnchor = ResolveClockAnchorUseCase(),
        syncRepository = syncRepository,
    )
}
