package dev.mj31.logger.client.app.theme

import androidx.compose.ui.graphics.Color
import dev.mj31.logger.client.domain.model.log.LogLevel

val DarkBackground = Color(0xFF0B1120)
val DarkSurface = Color(0xFF111C31)
val DarkSurfaceVariant = Color(0xFF1B2A45)
val DarkOutline = Color(0xFF243B63)
val DarkPrimary = Color(0xFF38BDF8)
val DarkSecondary = Color(0xFF818CF8)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkMuted = Color(0xFF94A3B8)

val LogLevelVerbose = Color(0xFF94A3B8)
val LogLevelDebug = Color(0xFF38BDF8)
val LogLevelInfo = Color(0xFF34D399)
val LogLevelWarn = Color(0xFFFBBF24)
val LogLevelError = Color(0xFFF87171)
val LogLevelFatal = Color(0xFFF43F5E)

val DarkErrorContainer = Color(0xFF4C1D24)
val DarkOnErrorContainer = Color(0xFFFECDD3)

val AccentSync = Color(0xFFA78BFA)
val AccentActive = Color(0xFF2563EB)

/** Single source of truth for the log level colour coding used across the UI. */
fun colorForLevel(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE -> LogLevelVerbose
    LogLevel.DEBUG -> LogLevelDebug
    LogLevel.INFO -> LogLevelInfo
    LogLevel.WARN -> LogLevelWarn
    LogLevel.ERROR -> LogLevelError
    LogLevel.FATAL -> LogLevelFatal
}
