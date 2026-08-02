package dev.mj31.logger.client.app.view.text

import org.jetbrains.compose.resources.StringResource

/**
 * A piece of text on its way to the screen, kept translatable until the very last moment.
 *
 * The store names a string resource instead of formatting English, so the same message renders in
 * whatever locale the window runs in, and a test can assert on the identity of the message rather
 * than on one particular translation of it.
 */
sealed interface UiText {

    /** Translatable text, optionally formatted with [arguments]. */
    data class Resource(
        val resource: StringResource,
        val arguments: List<Any> = emptyList(),
    ) : UiText

    /**
     * Text that is already final: a file name, or a diagnostic produced by the parsing engine.
     *
     * Engine diagnostics quote the offending input, so translating them would mean translating a
     * description of data rather than an application message.
     */
    data class Raw(val value: String) : UiText
}
