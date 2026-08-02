package dev.mj31.logger.client.app.theme

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.log.LogLevel

class ColorTest {

    @Test
    fun `every level has its own colour`() {
        val colors = LogLevel.entries.map { colorForLevel(level = it) }

        assertThat(colors.toSet()).hasSize(LogLevel.entries.size)
    }

    @Test
    fun `every level colour is opaque`() {
        assertThat(LogLevel.entries.all { colorForLevel(level = it).alpha == 1f }).isTrue()
    }
}
