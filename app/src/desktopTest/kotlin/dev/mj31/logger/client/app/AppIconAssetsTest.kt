package dev.mj31.logger.client.app

import com.google.common.truth.Truth.assertThat
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * The installer icons are read by jpackage, which only runs while packaging, so a missing or
 * corrupt file would surface in a release build rather than in the suite. These assertions move
 * that failure back into the tests.
 */
class AppIconAssetsTest {

    @Test
    fun `every platform has the icon its installer expects`() {
        listOf("icon.icns", "icon.ico", "icon.png").forEach { name ->
            val icon = iconFile(name = name)
            assertThat(icon.isFile).isTrue()
            assertThat(icon.length()).isGreaterThan(1_000L)
        }
    }

    @Test
    fun `the macOS icon is an icns container`() {
        val magic = iconFile(name = "icon.icns").readBytes().take(n = 4).toByteArray().decodeToString()

        assertThat(magic).isEqualTo("icns")
    }

    @Test
    fun `the Windows icon declares its images`() {
        val header = iconFile(name = "icon.ico").readBytes()

        // Reserved word, type 1 (icon), then the number of images, all little endian.
        assertThat(header[0].toInt() or header[1].toInt()).isEqualTo(0)
        assertThat(header[2].toInt()).isEqualTo(1)
        assertThat(header[4].toInt()).isGreaterThan(0)
    }

    @Test
    fun `the window icon is a square png large enough to scale down`() {
        val image = ImageIO.read(iconFile(name = "icon.png"))

        assertThat(image.width).isEqualTo(image.height)
        assertThat(image.width).isAtLeast(256)
    }

    @Test
    fun `the window icon is also available as a compose resource`() {
        val drawable = File(moduleDirectory(), "src/commonMain/composeResources/drawable/app_icon.png")

        assertThat(drawable.isFile).isTrue()
        assertThat(ImageIO.read(drawable).width).isEqualTo(ImageIO.read(iconFile(name = "icon.png")).width)
    }

    private fun iconFile(name: String): File = File(moduleDirectory(), "icons/$name")

    /** Tests run from the module directory in Gradle and from the repository root in some IDEs. */
    private fun moduleDirectory(): File {
        val current = File(".").absoluteFile
        return if (File(current, "icons").isDirectory) current else File(current, "app")
    }
}
