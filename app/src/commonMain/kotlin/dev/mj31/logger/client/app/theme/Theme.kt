package dev.mj31.logger.client.app.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkMuted,
    error = LogLevelError,
    errorContainer = DarkErrorContainer,
    onError = DarkBackground,
    onErrorContainer = DarkOnErrorContainer,
)

/**
 * Scrollbars that can actually be seen on this palette.
 *
 * The desktop default is black at twelve percent opacity, which is a sensible thumb on the light
 * background it was drawn for and an invisible one on a workspace this dark: the scrollbar was
 * present, worked, scrolled, and simply could not be made out against the surface behind it. It is
 * also the only indication of how far through several thousand records the list has got, so it is
 * worth more here than in most applications.
 */
private val DarkScrollbarStyle = defaultScrollbarStyle().copy(
    thickness = 10.dp,
    unhoverColor = DarkMuted.copy(alpha = 0.35f),
    hoverColor = DarkMuted.copy(alpha = 0.75f),
    hoverDurationMillis = HOVER_FADE_MILLIS,
)

@Composable
fun LoggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides DarkScrollbarStyle,
            content = content,
        )
    }
}

private const val HOVER_FADE_MILLIS = 200
