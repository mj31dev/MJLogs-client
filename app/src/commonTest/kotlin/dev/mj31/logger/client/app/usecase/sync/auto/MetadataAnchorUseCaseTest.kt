package dev.mj31.logger.client.app.usecase.sync.auto

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.video.FakeVideoMetadataSource
import dev.mj31.logger.client.app.usecase.sync.auto.metadata.MetadataAnchorUseCase
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.media.VideoMetadata
import dev.mj31.logger.client.domain.model.time.TimeRange
import dev.mj31.logger.client.domain.sync.SyncOrigin
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant

/**
 * When the file's own account of itself is worth believing.
 *
 * A creation time costs nothing to read and is wrong often enough that taking it at face value would
 * be worse than not looking: a phone writes local time into a field defined as UTC, a re-encode
 * stamps the moment of the re-encode. The loaded session is the only check available, and half the
 * recording having to fall inside it is what turns a claim into evidence.
 */
class MetadataAnchorUseCaseTest {

    private val media = VideoMedia(path = "/clips/bug.mov", name = "bug.mov")

    private val logRange = TimeRange(
        start = Instant.parse(input = "2026-08-08T09:20:00Z"),
        end = Instant.parse(input = "2026-08-08T09:40:00Z"),
    )

    @Test
    fun `a recording that sits inside the session becomes the anchor`() = runTest {
        val anchor = anchorFor(
            metadata = metadata(created = "2026-08-08T09:25:00Z", durationMillis = 120_000L),
        )

        assertThat(anchor).isNotNull()
        assertThat(requireNotNull(anchor).logTimestamp).isEqualTo(Instant.parse(input = "2026-08-08T09:25:00Z"))
        assertThat(anchor.videoPositionMillis).isEqualTo(0L)
        assertThat(anchor.origin).isEqualTo(SyncOrigin.VIDEO_METADATA)
        assertThat(anchor.accuracyMillis).isEqualTo(1_000L)
    }

    /** A tester who keeps recording after the log ends is the ordinary case, not a failure. */
    @Test
    fun `a recording that runs past the end of the logs is still accepted`() = runTest {
        val anchor = anchorFor(
            metadata = metadata(created = "2026-08-08T09:35:00Z", durationMillis = 480_000L),
        )

        assertThat(anchor).isNotNull()
    }

    @Test
    fun `a creation time an hour out is refused`() = runTest {
        val anchor = anchorFor(
            metadata = metadata(created = "2026-08-08T10:25:00Z", durationMillis = 120_000L),
        )

        assertThat(anchor).isNull()
    }

    @Test
    fun `a container that declares nothing is refused`() = runTest {
        val anchor = anchorFor(
            metadata = VideoMetadata(
                creationTime = null,
                creationOffsetMinutes = null,
                durationMillis = 120_000L,
                width = 588,
                height = 1_280,
            ),
        )

        assertThat(anchor).isNull()
    }

    @Test
    fun `without logs there is nothing to check the claim against`() = runTest {
        val useCase = MetadataAnchorUseCase(
            metadataSource = FakeVideoMetadataSource(
                metadata = metadata(created = "2026-08-08T09:25:00Z", durationMillis = 120_000L),
            ),
        )

        assertThat(useCase(media = media, logRange = null)).isNull()
    }

    private suspend fun anchorFor(metadata: VideoMetadata) = MetadataAnchorUseCase(
        metadataSource = FakeVideoMetadataSource(metadata = metadata),
    )(media = media, logRange = logRange)

    private fun metadata(created: String, durationMillis: Long) = VideoMetadata(
        creationTime = Instant.parse(input = created),
        creationOffsetMinutes = null,
        durationMillis = durationMillis,
        width = 588,
        height = 1_280,
    )
}
