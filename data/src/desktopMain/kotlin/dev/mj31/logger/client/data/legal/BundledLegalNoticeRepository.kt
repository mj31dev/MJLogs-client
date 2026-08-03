package dev.mj31.logger.client.data.legal

import dev.mj31.logger.client.domain.model.legal.LegalNotice
import dev.mj31.logger.client.domain.repository.LegalNoticeRepository
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads the licence texts jpackage copied next to the application.
 *
 * [resourcesDirectory] is the one Compose points at with `compose.application.resources.dir`. It is
 * missing only when the application runs from a raw class path, which is not a distribution and so
 * carries no obligation; the window then says where the texts live in a real build.
 */
class BundledLegalNoticeRepository(
    private val resourcesDirectory: File?,
    private val dispatcher: CoroutineDispatcher,
) : LegalNoticeRepository {

    override suspend fun read(): List<LegalNotice> = withContext(context = dispatcher) {
        val directory = resourcesDirectory?.takeIf { it.isDirectory } ?: return@withContext emptyList()
        directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals(other = "txt", ignoreCase = true) }
            .sortedWith(comparator = compareBy({ readingOrderOf(fileName = it.name) }, { it.name }))
            .map { file -> LegalNotice(fileName = file.name, text = file.readText(charset = Charsets.UTF_8)) }
    }

    /**
     * The summary names the components and points at the other texts, so it is read first; anything
     * added later lands after the known set rather than in the middle of it.
     */
    private fun readingOrderOf(fileName: String): Int =
        READING_ORDER.indexOf(element = fileName).takeIf { it >= 0 } ?: READING_ORDER.size

    private companion object {
        val READING_ORDER = listOf("THIRD-PARTY.txt", "LICENSE.txt", "LGPL-3.0.txt", "GPL-3.0.txt")
    }
}
