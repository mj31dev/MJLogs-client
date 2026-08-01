package dev.mj31.logger.client.domain

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.model.LogEntry
import dev.mj31.logger.client.domain.model.LogLevel
import kotlinx.datetime.Clock
import kotlin.test.Test

class LogEntryTest {

    @Test
    fun testLogEntryCreation() {
        val now = Clock.System.now()
        val entry = LogEntry(
            id = "test-1",
            timestamp = now,
            level = LogLevel.INFO,
            tag = "TestTag",
            message = "Test message body",
        )

        assertThat(entry.id).isEqualTo("test-1")
        assertThat(entry.level).isEqualTo(LogLevel.INFO)
        assertThat(entry.tag).isEqualTo("TestTag")
        assertThat(entry.message).isEqualTo("Test message body")
        assertThat(entry.payloadJson).isNull()
    }
}
