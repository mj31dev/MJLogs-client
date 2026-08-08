package dev.mj31.logger.client.app.platform

import java.io.File

/**
 * Finds the recognition model, in an installed application and in a working copy alike.
 *
 * jpackage stages it under the folder Compose points at with `compose.application.resources.dir`,
 * and that is the only place a distribution has. A development run has no such folder: the Gradle
 * task that launches the application from source is the Kotlin plugin's, not the one the Compose
 * packaging plugin creates, so it sets no such property — and the recognizer would quietly report
 * itself unavailable throughout development while working perfectly in every shipped build. The
 * repository copy is therefore searched for as well, which is exactly the file that gets packaged.
 */
object TessdataDirectory {

    fun locate(): File? = candidates().firstOrNull { directory -> File(directory, MODEL).isFile }

    private fun candidates(): List<File> = buildList {
        System.getProperty(RESOURCES_PROPERTY)?.let { root -> add(File(root, FOLDER)) }
        addAll(inRepository())
    }

    /**
     * Walks up from the working directory looking for the repository root, because Gradle starts the
     * application in the module and an IDE often starts it a level higher.
     */
    private fun inRepository(): List<File> =
        generateSequence(seed = File(".").absoluteFile.normalize()) { it.parentFile }
            .take(n = SEARCH_DEPTH)
            .map { directory -> File(directory, REPOSITORY_PATH) }
            .toList()

    private const val RESOURCES_PROPERTY = "compose.application.resources.dir"
    private const val FOLDER = "tessdata"
    private const val MODEL = "eng.traineddata"
    private const val REPOSITORY_PATH = "app/appResources/common/$FOLDER"
    private const val SEARCH_DEPTH = 4
}
