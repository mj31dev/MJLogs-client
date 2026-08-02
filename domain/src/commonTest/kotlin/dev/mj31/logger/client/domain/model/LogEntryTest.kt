package dev.mj31.logger.client.domain.model

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.domain.fake.TestLogEntries
import kotlin.test.Test

class LogEntryTest {

    private val entry = TestLogEntries.entry(
        id = "e1",
        tag = "NetworkClient",
        message = "GET /api/users -> 200",
    ).copy(rawLine = "2024-05-01 10:00:00 INFO NetworkClient: GET /api/users -> 200")

    @Test
    fun `free text matches the tag`() {
        assertThat(entry.matchesText(query = "networkclient")).isTrue()
    }

    @Test
    fun `free text matches the message`() {
        assertThat(entry.matchesText(query = "/api/users")).isTrue()
    }

    @Test
    fun `free text matches what only the raw line contains`() {
        assertThat(entry.matchesText(query = "10:00:00")).isTrue()
    }

    @Test
    fun `free text ignores case`() {
        assertThat(entry.matchesText(query = "GET")).isTrue()
        assertThat(entry.matchesText(query = "get")).isTrue()
    }

    @Test
    fun `an unrelated query matches nothing`() {
        assertThat(entry.matchesText(query = "database")).isFalse()
    }
}
