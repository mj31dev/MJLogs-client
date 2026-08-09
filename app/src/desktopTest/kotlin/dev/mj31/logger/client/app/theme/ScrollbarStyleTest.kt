package dev.mj31.logger.client.app.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.ui.graphics.luminance
import dev.mj31.logger.client.domain.model.preferences.ThemeChoice
import kotlin.math.abs
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * A scrollbar has to be visible against the workspace it sits on.
 *
 * The desktop default thumb is black at twelve percent opacity — drawn for a light background, and
 * indistinguishable from this one. The bar was there the whole time, scrolled correctly, and simply
 * could not be seen, which is the kind of defect no assertion about behaviour ever catches.
 */
@OptIn(ExperimentalTestApi::class)
class ScrollbarStyleTest {

    @Test
    fun `the theme replaces the light-background default`() = runComposeUiTest {
        var style: ScrollbarStyle? = null
        setContent { LoggerTheme(choice = ThemeChoice.DARK) { style = LocalScrollbarStyle.current } }

        assertThat(style).isNotNull()
        assertThat(style).isNotEqualTo(defaultScrollbarStyle())
    }

    /**
     * Stated as separation rather than as a direction.
     *
     * "Lighter than the background" was true while there was one scheme and became the wrong test
     * the moment a light one existed — on white the thumb has to be darker. What has to hold in both
     * is that it can be told apart from the surface at all.
     */
    @Test
    fun `the thumb separates from the background in both schemes`() {
        listOf(
            ThemeChoice.DARK to DarkColors.background,
            ThemeChoice.LIGHT to LightColors.background,
        ).forEach { (choice, background) ->
            var style: ScrollbarStyle? = null
            runComposeUiTest {
                setContent { LoggerTheme(choice = choice) { style = LocalScrollbarStyle.current } }
            }

            val thumb = requireNotNull(style)
            assertThat(abs(thumb.unhoverColor.luminance() - background.luminance()))
                .isGreaterThan(MINIMUM_SEPARATION)
            assertThat(thumb.unhoverColor.alpha).isGreaterThan(MINIMUM_ALPHA)
        }
    }

    private companion object {
        const val MINIMUM_ALPHA = 0.2f
        const val MINIMUM_SEPARATION = 0.1f
    }
}
