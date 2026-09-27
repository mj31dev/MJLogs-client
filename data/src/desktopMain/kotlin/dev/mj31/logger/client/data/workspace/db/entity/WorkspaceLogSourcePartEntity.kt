package dev.mj31.logger.client.data.workspace.db.entity

import androidx.room.Entity

/**
 * A further file a merged log source was assembled from.
 *
 * The first file of a source stays in its own row, so a store that holds no merged source looks
 * exactly as it did before merging existed; only the files beyond it land here, in the order they
 * were merged, which is the order the source reads them in again.
 */
@Entity(tableName = "workspace_log_source_part", primaryKeys = ["sourceId", "position"])
data class WorkspaceLogSourcePartEntity(
    val sourceId: String,
    val position: Int,
    val path: String,
    val name: String,
    val referenceDate: String?,
)
