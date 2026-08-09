package dev.mj31.logger.client.data.workspace.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The single workspace the application reopens on the next launch.
 *
 * One row, always at [SINGLE_ROW_ID]: there is exactly one "where I left off", and giving it a fixed
 * key makes every write an upsert instead of a read-modify-write.
 *
 * The severity set and the source filter are stored as delimited text rather than through a type
 * converter. Both are short, closed lists of tokens, and a converter would add a layer whose only
 * job is to hide a `joinToString`.
 */
@Entity(tableName = "last_workspace")
data class LastWorkspaceEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val videoPath: String?,
    val videoName: String?,
    val anchorLogTimestampMillis: Long?,
    val anchorVideoPositionMillis: Long?,
    val anchorOrigin: String?,
    val anchorLogEntryId: String?,
    val anchorAccuracyMillis: Long?,
    val filterQuery: String,
    val filterLevels: String,
    val filterSourceIds: String,
    val timeWindowMillis: Long?,
    val followVideo: Boolean,
    val videoPositionMillis: Long,
    val packagePath: String?,
) {

    companion object {
        const val SINGLE_ROW_ID: Int = 0
    }
}
