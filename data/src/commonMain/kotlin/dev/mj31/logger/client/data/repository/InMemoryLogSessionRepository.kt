package dev.mj31.logger.client.data.repository

import dev.mj31.logger.client.domain.repository.LogSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dev.mj31.logger.client.domain.model.log.LogSource

/**
 * Keeps the imported files in RAM, as required by the PoC.
 *
 * Replacing this with a database backed implementation only requires another [LogSessionRepository].
 */
class InMemoryLogSessionRepository : LogSessionRepository {

    private val mutex = Mutex()
    private val stored = mutableListOf<LogSource>()
    private val sourcesState = MutableStateFlow<List<LogSource>>(value = emptyList())

    override val sources: StateFlow<List<LogSource>> = sourcesState.asStateFlow()

    override suspend fun addSource(source: LogSource) = mutex.withLock {
        stored.removeAll { it.id == source.id }
        stored += source
        publish()
    }

    override suspend fun replaceSource(source: LogSource) = mutex.withLock {
        val index = stored.indexOfFirst { it.id == source.id }
        if (index >= 0) stored[index] = source else stored += source
        publish()
    }

    override suspend fun removeSource(sourceId: String) = mutex.withLock {
        stored.removeAll { it.id == sourceId }
        publish()
    }

    override suspend fun clear() = mutex.withLock {
        stored.clear()
        publish()
    }

    private fun publish() {
        sourcesState.value = stored.toList()
    }
}
