package dev.mj31.logger.client.domain.model.preferences

/**
 * Which colour scheme the window uses.
 *
 * [SYSTEM] is the default because someone opening a log at night expects a dark window without
 * having configured anything. The two explicit values exist because this tool sits beside a video:
 * a recording of a light interface reads more easily against a light frame, which is a reason to pin
 * the scheme that the operating system knows nothing about.
 */
enum class ThemeChoice {
    SYSTEM,
    LIGHT,
    DARK,
}
