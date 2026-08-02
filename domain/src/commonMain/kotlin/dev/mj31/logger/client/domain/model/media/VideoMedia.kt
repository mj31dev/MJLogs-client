package dev.mj31.logger.client.domain.model.media

/** Screencast selected by the user. The PoC keeps only a reference; decoding lives in the data layer. */
data class VideoMedia(
    val path: String,
    val name: String,
)
