package dev.mj31.logger.client.data

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.data.repository.DefaultLogRepository
import dev.mj31.logger.client.domain.model.LogEntry
import dev.mj31.logger.client.domain.model.LogLevel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test

class DefaultLogRepositoryTest {

    @Test
    fun testAddAndClearLogs() = runTest {
        val repository = DefaultLogRepository()
        val initialCount = repository.getLogs().size

        val newEntry = LogEntry(
            id = "new-1",
            timestamp = Clock.System.now(),
            level = LogLevel.DEBUG,
            tag = "Test",
            message = "New test message",
        )

        repository.addLog(entry = newEntry)
        assertThat(repository.getLogs()).hasSize(initialCount + 1)

        repository.clearLogs()
        assertThat(repository.getLogs()).isEmpty()
    }
}
