package dev.mj31.logger.client.data.workspace.db

import dev.mj31.logger.client.data.workspace.db.entity.LastWorkspaceEntity
import dev.mj31.logger.client.data.workspace.db.entity.PreferenceEntity
import dev.mj31.logger.client.data.workspace.db.entity.RecentPackageEntity
import dev.mj31.logger.client.data.workspace.db.entity.WorkspaceLogSourceEntity
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The schema, used in two places at once.
 *
 * The application store keeps the workspace last open and the session files visited; a saved package
 * carries a database of this very schema holding its single workspace. One set of entities means one
 * history of migrations, and opening a package is then just reading the only workspace it contains.
 */
@Database(
    entities = [
        LastWorkspaceEntity::class,
        WorkspaceLogSourceEntity::class,
        RecentPackageEntity::class,
        PreferenceEntity::class,
    ],
    version = MjLogsDatabase.VERSION,
    exportSchema = true,
)
abstract class MjLogsDatabase : RoomDatabase() {

    abstract fun workspaceDao(): WorkspaceDao

    abstract fun preferenceDao(): PreferenceDao

    companion object {

        const val VERSION: Int = 3

        /** File name inside a session package; the application store is named by the platform layer. */
        const val PACKAGE_ENTRY_NAME: String = "session.db"
    }
}
