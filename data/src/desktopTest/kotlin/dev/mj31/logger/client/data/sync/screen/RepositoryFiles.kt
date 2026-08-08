package dev.mj31.logger.client.data.sync.screen

import java.io.File

/**
 * Locates the sample recordings and the bundled recognition model from wherever the suite was
 * started: Gradle runs a module test from the module directory, an IDE often from the repository
 * root, and both have to find the same files.
 */
object RepositoryFiles {

    val root: File by lazy {
        generateSequence(seed = File(".").absoluteFile.normalize()) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
    }

    fun sample(name: String): File = File(root, "samples/$name")

    /** The directory the model sits in, which is exactly what Tesseract's `Init` expects. */
    val tessdataDirectory: File
        get() = File(root, "app/appResources/common/tessdata")
}
