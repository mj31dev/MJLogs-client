package dev.mj31.logger.client.app.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.ui.graphics.luminance
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
        setContent { LoggerTheme { style = LocalScrollbarStyle.current } }

        assertThat(style).isNotNull()
        assertThat(style).isNotEqualTo(defaultScrollbarStyle())
    }

    /** Lighter than the surface it is drawn on, which is what makes it show at all. */
    @Test
    fun `the thumb is lighter than the background behind it`() = runComposeUiTest {
        var style: ScrollbarStyle? = null
        setContent { LoggerTheme { style = LocalScrollbarStyle.current } }

        val thumb = requireNotNull(style)
        assertThat(thumb.unhoverColor.luminance()).isGreaterThan(DarkBackground.luminance())
        assertThat(thumb.unhoverColor.alpha).isGreaterThan(MINIMUM_ALPHA)
    }

    private companion object {
        const val MINIMUM_ALPHA = 0.2f
    }
}
