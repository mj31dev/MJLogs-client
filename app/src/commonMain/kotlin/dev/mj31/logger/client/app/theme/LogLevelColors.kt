package dev.mj31.logger.client.app.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.mj31.logger.client.domain.model.log.LogLevel

/**
 * The colour coding of severity, which belongs to the theme rather than to a constant.
 *
 * The set tuned for a near-black workspace does not survive on white: `#34D399` and `#FBBF24` sit at
 * roughly 1.6:1 against `#FFFFFF`, which is not a colour so much as a rumour of one. So each scheme
 * carries its own set, darkened for light and lightened for dark, and the six stay distinguishable
 * from each other within a scheme rather than merely being six different values.
 *
 * Colour is never the only carrier: a level is shown as a colour *and* as its letter.
 */
data class LogLevelColors(
    val verbose: Color,
    val debug: Color,
    val info: Color,
    val warn: Color,
    val error: Color,
    val fatal: Color,
) {

    fun of(level: LogLevel): Color = when (level) {
        LogLevel.VERBOSE -> verbose
        LogLevel.DEBUG -> debug
        LogLevel.INFO -> info
        LogLevel.WARN -> warn
        LogLevel.ERROR -> error
        LogLevel.FATAL -> fatal
    }
}

/** Bright enough to read on `#0B1120`, muted enough not to fight the video beside them. */
val DarkLogLevelColors: LogLevelColors = LogLevelColors(
    verbose = Color(0xFF94A3B8),
    debug = Color(0xFF38BDF8),
    info = Color(0xFF34D399),
    warn = Color(0xFFFBBF24),
    error = Color(0xFFF87171),
    fatal = Color(0xFFF43F5E),
)

/** The same six hues taken down until each carries its weight against a white surface. */
val LightLogLevelColors: LogLevelColors = LogLevelColors(
    verbose = Color(0xFF64748B),
    debug = Color(0xFF0369A1),
    info = Color(0xFF047857),
    warn = Color(0xFFB45309),
    error = Color(0xFFB91C1C),
    fatal = Color(0xFF9F1239),
)

/**
 * Read by anything that colour-codes a level.
 *
 * Static because the whole set changes at once, when the theme does, and never independently.
 */
val LocalLogLevelColors = staticCompositionLocalOf { DarkLogLevelColors }
