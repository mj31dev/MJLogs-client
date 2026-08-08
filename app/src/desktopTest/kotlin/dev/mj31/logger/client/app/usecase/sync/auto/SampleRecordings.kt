package dev.mj31.logger.client.app.usecase.sync.auto

import dev.mj31.logger.client.domain.model.media.VideoMedia
import java.io.File

/**
 * The screen recordings the automatic synchronization is measured against, and the model that reads
 * them.
 *
 * Both are found from the repository root rather than from the working directory, because Gradle
 * starts a module's tests inside the module and an IDE often starts them from the root.
 */
object SampleRecordings {

    /** Seventy-six seconds of an iPhone: the clock changes minute twice inside the search window. */
    const val WITH_CLOCK = "device-screencast.mov"

    /** Twelve seconds recorded between two ticks: a clock is visible and never changes. */
    const val WITHOUT_MINUTE_CHANGE = "device-screencast-short.mov"

    /** A synthetic clip with no status bar at all, and so no clock to find anywhere on it. */
    const val WITHOUT_CLOCK = "sample-clip.mp4"

    val tessdataDirectory: File
        get() = File(root, "app/appResources/common/tessdata")

    fun media(name: String): VideoMedia = VideoMedia(path = File(root, "samples/$name").absolutePath, name = name)

    private val root: File by lazy {
        generateSequence(seed = File(".").absoluteFile.normalize()) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
    }
}
