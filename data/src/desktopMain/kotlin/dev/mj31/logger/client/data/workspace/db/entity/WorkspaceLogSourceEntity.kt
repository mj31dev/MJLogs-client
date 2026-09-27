package dev.mj31.logger.client.data.workspace.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One log file of the stored workspace, with the format it was read under.
 *
 * The format is flattened into columns instead of being serialized: a stored blob would have to be
 * versioned separately from the schema, and every field of a format specification is a scalar.
 * [formatKind] is the discriminator that says which of the remaining columns carry meaning — the
 * line pattern belongs to a regular expression format, the delimiter and the header flag to a
 * delimited one — and the domain type they are read back into keeps those combinations impossible.
 *
 * The four `…Field` columns locate a component inside a structured record. One column per component
 * rather than one encoded map keeps the store greppable and the migration a plain column addition;
 * a value of `#3` means the fourth column of a delimited row, anything else is a key or header name.
 *
 * [referenceDate] is the day a timestamp without a date belongs to. It is stored because it is
 * inferred once — from the file name, from the modification time, or by asking — and re-inferring it
 * on every restore would silently move an old log onto whatever day it was reopened.
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
    val referenceDate: String?,
    val formatName: String,
    val formatKind: String,
    val formatLinePattern: String?,
    val formatDelimiter: String?,
    val formatHasHeader: Boolean?,
    val formatTimestampField: String?,
    val formatLevelField: String?,
    val formatTagField: String?,
    val formatMessageField: String?,
    val formatTimestampPattern: String,
    val formatFallbackLevel: String,
    val formatZoneId: String?,
    val formatOrigin: String,
)
