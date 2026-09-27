package dev.mj31.logger.client.data.workspace.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.mj31.logger.client.data.workspace.db.entity.RecentPackageEntity
import kotlinx.coroutines.flow.Flow

/**
 * The list of session files opened recently.
 *
 * Kept apart from the workspace statements because it lives only in the application store: a
 * session file carries one workspace and no memory of other files.
 */
@Dao
interface RecentPackageDao {

    @Query("SELECT * FROM recent_package ORDER BY lastOpenedMillis DESC")
    fun observeRecentPackages(): Flow<List<RecentPackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentPackage(entry: RecentPackageEntity)

    @Query("DELETE FROM recent_package WHERE path = :path")
    suspend fun deleteRecentPackage(path: String)
}
