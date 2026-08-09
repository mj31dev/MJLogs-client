package dev.mj31.logger.client.data.workspace.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved session file that has been opened, keyed by where it lives.
 *
 * The path is the identity: opening the same file again is the same entry with a newer timestamp,
 * not a second row, so the list stays a list of files rather than a log of openings.
 */
@Entity(tableName = "recent_package")
data class RecentPackageEntity(
    @PrimaryKey val path: String,
    val name: String,
    val lastOpenedMillis: Long,
)
