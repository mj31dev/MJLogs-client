package dev.mj31.logger.client.data.workspace

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.mj31.logger.client.data.workspace.db.MjLogsDatabase
import dev.mj31.logger.client.data.workspace.db.MjLogsMigrations
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Opens a store, whether it is the application's own or the one inside a session package.
 *
 * The bundled driver carries its own SQLite build, so an installed application depends on no system
 * library and behaves the same on all three platforms.
 */
object MjLogsDatabaseFactory {

    fun open(path: String, dispatcher: CoroutineDispatcher): MjLogsDatabase {
        File(path).parentFile?.mkdirs()
        return Room.databaseBuilder<MjLogsDatabase>(name = path)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*MjLogsMigrations.ALL)
            .setQueryCoroutineContext(context = dispatcher)
            .build()
    }
}
