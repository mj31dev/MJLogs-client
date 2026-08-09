package dev.mj31.logger.client.app.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.mj31.logger.client.domain.model.preferences.ThemeChoice

/**
 * Scrollbars that can actually be seen on either palette.
 *
 * The desktop default is black at twelve percent opacity, which is a sensible thumb on a light
 * background and an invisible one on a workspace this dark: the scrollbar was present, worked,
 * scrolled, and simply could not be made out against the surface behind it. It is also the only
 * indication of how far through several thousand records the list has got, so it is worth more here
 * than in most applications.
 */
private fun scrollbarStyle(thumb: Color) = defaultScrollbarStyle().copy(
    thickness = 10.dp,
    unhoverColor = thumb.copy(alpha = 0.35f),
    hoverColor = thumb.copy(alpha = 0.75f),
    hoverDurationMillis = MotionTokens.ENTER_MILLIS,
)

@Composable
fun LoggerTheme(
    choice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val levels = if (dark) DarkLogLevelColors else LightLogLevelColors
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides scrollbarStyle(thumb = colors.onSurfaceVariant),
            LocalLogLevelColors provides levels,
            content = content,
        )
    }
}
