package dev.mj31.logger.client.app.view.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

/** Resolves a [UiText] against the locale of the running window. */
@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> if (arguments.isEmpty()) {
        stringResource(resource = resource)
    } else {
        stringResource(resource = resource, formatArgs = arguments.toTypedArray())
    }
}
