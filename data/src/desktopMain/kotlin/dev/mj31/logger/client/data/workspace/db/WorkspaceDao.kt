package dev.mj31.logger.client.data.workspace.db

import dev.mj31.logger.client.data.workspace.db.entity.WorkspaceLogSourcePartEntity
import dev.mj31.logger.client.data.workspace.db.entity.LastWorkspaceEntity
import dev.mj31.logger.client.data.workspace.db.entity.WorkspaceLogSourceEntity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Every statement the workspace store issues, against both the application store and a package. */
@Dao
interface WorkspaceDao {

    @Query("SELECT * FROM last_workspace WHERE id = :id")
    suspend fun loadWorkspace(id: Int = LastWorkspaceEntity.SINGLE_ROW_ID): LastWorkspaceEntity?

    @Query("SELECT * FROM workspace_log_source ORDER BY position ASC")
    suspend fun loadLogSources(): List<WorkspaceLogSourceEntity>

    @Query("SELECT * FROM workspace_log_source_part ORDER BY sourceId ASC, position ASC")
    suspend fun loadLogSourceParts(): List<WorkspaceLogSourcePartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspace(workspace: LastWorkspaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogSources(sources: List<WorkspaceLogSourceEntity>)

    @Query("DELETE FROM workspace_log_source")
    suspend fun deleteLogSources()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogSourceParts(parts: List<WorkspaceLogSourcePartEntity>)

    @Query("DELETE FROM workspace_log_source_part")
    suspend fun deleteLogSourceParts()

    /**
     * Replaces the whole workspace in one go.
     *
     * The sources are deleted and reinserted rather than diffed: a workspace holds a handful of
     * files, and a diff would be more code than the write it saves.
     */
    @Transaction
    suspend fun replaceWorkspace(
        workspace: LastWorkspaceEntity,
        sources: List<WorkspaceLogSourceEntity>,
        parts: List<WorkspaceLogSourcePartEntity>,
    ) {
        deleteLogSourceParts()
        deleteLogSources()
        upsertWorkspace(workspace = workspace)
        insertLogSources(sources = sources)
        insertLogSourceParts(parts = parts)
    }

    /** Partial write for the one value that changes many times a second. */
    @Query("UPDATE last_workspace SET videoPositionMillis = :positionMillis WHERE id = :id")
    suspend fun updatePlaybackPosition(positionMillis: Long, id: Int = LastWorkspaceEntity.SINGLE_ROW_ID)

}
