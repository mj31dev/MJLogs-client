package dev.mj31.logger.client.app.platform

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlin.test.Test

/**
 * The recognizer has to find its model when the application is run from source, not only when it is
 * packaged.
 *
 * This is not a hypothetical. The Gradle task that launches the application during development is
 * the Kotlin plugin's, and it sets none of the properties the packaging plugin sets — so the model
 * went unfound in exactly the way the project's own README tells people to run the application,
 * while every packaged build worked. The failure was silent: automatic synchronization simply
 * reported itself unavailable.
 */
class TessdataDirectoryTest {

    @Test
    fun `the model is found without any packaging having taken place`() {
        val located = TessdataDirectory.locate()

        assertThat(located).isNotNull()
        assertThat(File(requireNotNull(located), "eng.traineddata").isFile).isTrue()
    }
}
