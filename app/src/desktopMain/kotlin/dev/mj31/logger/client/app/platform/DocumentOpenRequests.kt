package dev.mj31.logger.client.app.platform

import java.awt.Desktop

/**
 * Files the desktop environment asks the running application to open.
 *
 * macOS does not put a double-clicked document in `argv`. The Finder sends an `odoc` Apple Event,
 * and the JVM surfaces it here — so an application that only reads its command line opens on its
 * start screen and ignores the file it was launched for. Windows and Linux pass the path as an
 * argument, which is why this is needed on exactly one platform and harmless on the others.
 *
 * The event routinely arrives before there is a window to show it in, so requests are buffered until
 * something is listening and handed over the moment one is. The handler runs on the AWT thread and
 * the listener is registered from the composition, which is why every access is guarded.
 */
object DocumentOpenRequests {

    private val pending = mutableListOf<String>()
    private var listener: ((List<String>) -> Unit)? = null

    private val isSupported: Boolean
        get() = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)

    /**
     * Starts listening as early as possible, which is before the first window exists.
     *
     * Registering later loses the event that launched the application: the system delivers it once,
     * whether or not anyone asked for it.
     */
    fun install() {
        if (!isSupported) return
        Desktop.getDesktop().setOpenFileHandler { event ->
            accept(paths = event.files.map { it.absolutePath })
        }
    }

    /** Hands over anything already queued, then everything that arrives afterwards. */
    fun onOpen(listener: (List<String>) -> Unit) {
        val queued = synchronized(lock = this) {
            this.listener = listener
            pending.toList().also { pending.clear() }
        }
        if (queued.isNotEmpty()) listener(queued)
    }

    fun stopListening() {
        synchronized(lock = this) { listener = null }
    }

    /** Visible for tests: the buffering is the part worth proving, not the Apple Event plumbing. */
    internal fun accept(paths: List<String>) {
        if (paths.isEmpty()) return
        val target = synchronized(lock = this) {
            listener ?: run {
                pending += paths
                null
            }
        }
        target?.invoke(paths)
    }

    internal fun reset() {
        synchronized(lock = this) {
            pending.clear()
            listener = null
        }
    }
}
