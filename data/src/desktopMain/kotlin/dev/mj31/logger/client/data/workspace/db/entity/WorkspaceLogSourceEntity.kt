package dev.mj31.logger.client.data.workspace.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One log file of the stored workspace, with the format it was read under.
 *
 * The format is flattened into columns instead of being serialized: a stored blob would have to be
 * versioned separately from the schema, and the fields of a format specification are few and stable.
 *
 * [position] preserves the import order, which is the order the sources are listed in and the
 * tie-breaker when two records share a timestamp.
 */
@Entity(tableName = "workspace_log_source")
data class WorkspaceLogSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val position: Int,
    val formatName: String,
    val formatLinePattern: String,
    val formatTimestampPattern: String,
    val formatFallbackLevel: String,
    val formatUtcOffsetMinutes: Int,
    val formatOrigin: String,
)
