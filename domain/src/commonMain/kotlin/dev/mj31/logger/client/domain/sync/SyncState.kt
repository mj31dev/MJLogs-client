package dev.mj31.logger.client.domain.sync

/** Whether the video and the log list move together. Both timelines are independent until synced. */
sealed interface SyncState {

    data object Unsynced : SyncState

    data class Synced(val anchor: SyncAnchor) : SyncState

    val anchorOrNull: SyncAnchor?
        get() = (this as? Synced)?.anchor

    val isSynced: Boolean
        get() = this is Synced
}
