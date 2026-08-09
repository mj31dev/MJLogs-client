package dev.mj31.logger.client.data.workspace.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One stored setting, as a key and its text.
 *
 * A table of key-value pairs rather than a column per setting: preferences arrive one at a time over
 * the life of an application, and a column each would mean a migration for every one of them.
 */
@Entity(tableName = "preference")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
) {

    companion object {
        const val KEY_THEME: String = "theme"
    }
}
