package dev.mj31.logger.client.app.theme

import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlin.math.abs
import kotlin.test.Test

/**
 * The severity palette, asserted for both schemes.
 *
 * It stopped being a single set of constants when the light theme arrived: the greens and ambers
 * that read against `#0B1120` sit at roughly 1.6:1 on white, which is not a colour so much as a
 * rumour of one.
 */
class ColorTest {

    @Test
    fun `every level has its own colour in both schemes`() {
        listOf(DarkLogLevelColors, LightLogLevelColors).forEach { palette ->
            val colors = LogLevel.entries.map { palette.of(level = it) }

            assertThat(colors.toSet()).hasSize(LogLevel.entries.size)
        }
    }

    @Test
    fun `every level colour is opaque in both schemes`() {
        listOf(DarkLogLevelColors, LightLogLevelColors).forEach { palette ->
            assertThat(LogLevel.entries.all { palette.of(level = it).alpha == 1f }).isTrue()
        }
    }

    /** The point of a second set: each colour has to carry against the surface it is drawn on. */
    @Test
    fun `every level colour separates from the surface behind it`() {
        LogLevel.entries.forEach { level ->
            assertThat(contrast(DarkLogLevelColors.of(level = level), DarkColors.surface))
                .isGreaterThan(MINIMUM_CONTRAST)
            assertThat(contrast(LightLogLevelColors.of(level = level), LightColors.surface))
                .isGreaterThan(MINIMUM_CONTRAST)
        }
    }

    /** A palette reused across themes was the bug; the two sets have to actually differ. */
    @Test
    fun `the two schemes do not share a single level colour`() {
        LogLevel.entries.forEach { level ->
            assertThat(LightLogLevelColors.of(level = level))
                .isNotEqualTo(DarkLogLevelColors.of(level = level))
        }
    }

    private fun contrast(foreground: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color): Float =
        abs(foreground.luminance() - background.luminance())

    private companion object {
        const val MINIMUM_CONTRAST = 0.1f
    }
}
