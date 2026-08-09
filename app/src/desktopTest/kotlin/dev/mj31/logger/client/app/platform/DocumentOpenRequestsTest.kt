package dev.mj31.logger.client.app.platform

import com.google.common.truth.Truth.assertThat
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The buffering, which is the part that has to be right.
 *
 * macOS delivers the document that launched the application once and early — reliably before there
 * is a window, and often before the composition exists at all. Everything else here is Apple Event
 * plumbing that cannot be exercised without a Finder.
 */
class DocumentOpenRequestsTest {

    @BeforeTest
    fun setUp() = DocumentOpenRequests.reset()

    @AfterTest
    fun tearDown() = DocumentOpenRequests.reset()

    @Test
    fun `a request that arrives before anyone is listening is not lost`() {
        DocumentOpenRequests.accept(paths = listOf("/cases/one.mjclog"))

        val received = mutableListOf<String>()
        DocumentOpenRequests.onOpen { received += it }

        assertThat(received).containsExactly("/cases/one.mjclog")
    }

    @Test
    fun `requests that arrive afterwards go straight through`() {
        val received = mutableListOf<String>()
        DocumentOpenRequests.onOpen { received += it }

        DocumentOpenRequests.accept(paths = listOf("/cases/later.mjclog"))

        assertThat(received).containsExactly("/cases/later.mjclog")
    }

    /** Everything queued before the window appeared belongs to that window, in the order it came. */
    @Test
    fun `several queued requests are handed over together and in order`() {
        DocumentOpenRequests.accept(paths = listOf("/cases/one.mjclog"))
        DocumentOpenRequests.accept(paths = listOf("/cases/two.mjclog"))

        val received = mutableListOf<String>()
        DocumentOpenRequests.onOpen { received += it }

        assertThat(received).containsExactly("/cases/one.mjclog", "/cases/two.mjclog").inOrder()
    }

    /** The queue is handed over once; a second listener starts from nothing rather than replaying. */
    @Test
    fun `a queued request is delivered only once`() {
        DocumentOpenRequests.accept(paths = listOf("/cases/one.mjclog"))
        DocumentOpenRequests.onOpen { }

        val second = mutableListOf<String>()
        DocumentOpenRequests.onOpen { second += it }

        assertThat(second).isEmpty()
    }

    @Test
    fun `nothing is delivered after the listener has gone`() {
        val received = mutableListOf<String>()
        DocumentOpenRequests.onOpen { received += it }
        DocumentOpenRequests.stopListening()

        DocumentOpenRequests.accept(paths = listOf("/cases/late.mjclog"))

        assertThat(received).isEmpty()
    }
}
