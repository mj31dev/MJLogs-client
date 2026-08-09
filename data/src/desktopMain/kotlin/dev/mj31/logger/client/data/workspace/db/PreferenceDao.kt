package dev.mj31.logger.client.data.workspace.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.mj31.logger.client.data.workspace.db.entity.PreferenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Settings, kept apart from the workspace statements.
 *
 * They share a database and nothing else: a workspace is replaced whenever a session is opened, and
 * a preference has to outlive that.
 */
@Dao
interface PreferenceDao {

    @Query("SELECT value FROM preference WHERE key = :key")
    fun observe(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: PreferenceEntity)
}
