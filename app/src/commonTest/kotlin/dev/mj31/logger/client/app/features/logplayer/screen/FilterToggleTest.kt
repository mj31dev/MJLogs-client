package dev.mj31.logger.client.app.features.logplayer.screen

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.log.LogLevel

/**
 * The filter chips expose an implicit "everything" state as an empty set, which is the part of the
 * UI logic worth pinning down.
 */
class FilterToggleTest {

    private val allSources = setOf("a", "b", "c")

    @Test
    fun `hiding one source expands the implicit selection`() {
        val updated = toggleSource(current = emptySet(), allIds = allSources, toggled = "b")

        assertThat(updated).containsExactly("a", "c")
    }

    @Test
    fun `showing the last missing source collapses back to the implicit selection`() {
        val updated = toggleSource(current = setOf("a", "c"), allIds = allSources, toggled = "b")

        assertThat(updated).isEmpty()
    }

    @Test
    fun `hiding the only visible source is refused`() {
        val updated = toggleSource(current = setOf("a"), allIds = allSources, toggled = "a")

        assertThat(updated).containsExactly("a")
    }

    @Test
    fun `hiding one level expands the implicit selection`() {
        val updated = toggleLevel(current = emptySet(), toggled = LogLevel.DEBUG)

        assertThat(updated).containsExactlyElementsIn(LogLevel.entries.toSet() - LogLevel.DEBUG)
    }

    @Test
    fun `restoring every level collapses back to the implicit selection`() {
        val withoutDebug = LogLevel.entries.toSet() - LogLevel.DEBUG

        val updated = toggleLevel(current = withoutDebug, toggled = LogLevel.DEBUG)

        assertThat(updated).isEmpty()
    }

    @Test
    fun `hiding the only visible level is refused`() {
        val updated = toggleLevel(current = setOf(LogLevel.ERROR), toggled = LogLevel.ERROR)

        assertThat(updated).containsExactly(LogLevel.ERROR)
    }
}
