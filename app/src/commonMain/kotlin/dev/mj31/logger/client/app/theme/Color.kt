package dev.mj31.logger.client.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The two schemes, defined side by side so that a change to one is an obvious omission in the other.
 *
 * Depth is carried by the three surface levels rather than by shadows: a shadow over a video frame
 * reads as a rendering artefact, and a thousand shadowed log rows are a grey mess. `background` is
 * the window, `surface` is a pane or a card, `surfaceVariant` is something inset inside one.
 */
private val DarkBackground = Color(0xFF0B1120)
private val DarkSurface = Color(0xFF111C31)
private val DarkSurfaceVariant = Color(0xFF1B2A45)
private val DarkOutline = Color(0xFF243B63)
private val DarkPrimary = Color(0xFF38BDF8)
private val DarkSecondary = Color(0xFF818CF8)
private val DarkOnBackground = Color(0xFFF8FAFC)
private val DarkOnSurface = Color(0xFFE2E8F0)
private val DarkMuted = Color(0xFF94A3B8)
private val DarkErrorContainer = Color(0xFF4C1D24)
private val DarkOnErrorContainer = Color(0xFFFECDD3)

private val LightBackground = Color(0xFFF6F8FB)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFE8EDF5)
private val LightOutline = Color(0xFFCBD5E1)
private val LightPrimary = Color(0xFF0369A1)
private val LightSecondary = Color(0xFF4F46E5)
private val LightOnBackground = Color(0xFF0F172A)
private val LightOnSurface = Color(0xFF1E293B)
private val LightMuted = Color(0xFF556173)
private val LightErrorContainer = Color(0xFFFEE2E2)
private val LightOnErrorContainer = Color(0xFF7F1D1D)

/** Marks the anchor between the two timelines wherever it is drawn. */
val AccentSync = Color(0xFFA78BFA)

/** The record the playhead is currently standing on. */
val AccentActive = Color(0xFF2563EB)

val DarkColors: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkMuted,
    error = DarkLogLevelColors.error,
    errorContainer = DarkErrorContainer,
    onError = DarkBackground,
    onErrorContainer = DarkOnErrorContainer,
)

val LightColors: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightMuted,
    error = LightLogLevelColors.error,
    errorContainer = LightErrorContainer,
    onError = LightSurface,
    onErrorContainer = LightOnErrorContainer,
)
