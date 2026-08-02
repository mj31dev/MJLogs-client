package dev.mj31.logger.client.app.view.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Keeps what the user is typing in a local state instead of driving the text field straight from the
 * store.
 *
 * A text field owns its undo history, and that history is dropped every time the value is replaced
 * from the outside. Since the state travels through a `StateFlow`, it comes back a frame after the
 * keystroke, so an externally driven field would reset itself on every character: undo then restores
 * one step and the next undo lands on the text it just left, which reads as a redo.
 *
 * The draft therefore adopts [external] only when it really differs, which happens when the change
 * did not originate from typing (a preset button, another file being described).
 *
 * @param resetKey identity of the edited subject; changing it starts a fresh draft.
 */
@Composable
fun rememberTextDraft(external: String, resetKey: Any? = Unit): MutableState<String> {
    val draft = remember(key1 = resetKey) { mutableStateOf(value = external) }
    LaunchedEffect(key1 = external) {
        if (external != draft.value) draft.value = external
    }
    return draft
}
