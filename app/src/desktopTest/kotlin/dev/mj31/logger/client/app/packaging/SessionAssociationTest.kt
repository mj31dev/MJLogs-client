package dev.mj31.logger.client.app.packaging

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.session.SessionFile
import java.io.File
import kotlin.test.Test

/**
 * The session extension is spelled twice and has to agree with itself.
 *
 * The build cannot read a Kotlin constant it is in the middle of compiling, so `build.gradle.kts`
 * repeats the extension for the macOS document type and the jpackage descriptors. A rename that
 * changed only the constant would leave an application that still claimed the old extension and
 * silently stopped being the handler for its own files — and nothing else would notice, because
 * registration is only observable on an installed build.
 */
class SessionAssociationTest {

    @Test
    fun `the build declares the same extension the code writes`() {
        val bare = SessionFile.EXTENSION.removePrefix(prefix = ".")

        assertThat(buildScript()).contains("val sessionExtension = \"$bare\"")
    }

    /** macOS reads the bundle, not the jpackage option, so the document type has to be in the plist. */
    @Test
    fun `the macOS bundle declares a document type for it`() {
        val script = buildScript()

        assertThat(script).contains("<key>CFBundleDocumentTypes</key>")
        assertThat(script).contains("<string>\$sessionExtension</string>")
        assertThat(script).contains("extraKeysRawXml = macDocumentTypes")
    }

    /**
     * `Owner` is what makes the application the default handler rather than one candidate of several.
     */
    @Test
    fun `it claims the format rather than merely opening it`() {
        assertThat(buildScript()).contains("<string>Owner</string>")
    }

    private fun buildScript(): String {
        val script = File("build.gradle.kts")
        check(script.isFile) { "Expected the module build script at ${script.absolutePath}" }
        return script.readText()
    }
}
