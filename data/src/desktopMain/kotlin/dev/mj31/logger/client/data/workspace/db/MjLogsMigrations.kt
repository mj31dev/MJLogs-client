package dev.mj31.logger.client.data.workspace.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Every step between two schema versions, in order.
 *
 * A store lives on the machine whether or not its owner wanted it there, so it is migrated rather
 * than thrown away. That is the opposite of the policy for session files, which their owner creates
 * deliberately and can always save again.
 */
internal object MjLogsMigrations {

    /**
     * Session files lost their second shape.
     *
     * There used to be two kinds, one bundling copies and one holding paths, and the row recorded
     * which it was. With one shape left the column says nothing — and every remembered file written
     * under the old extensions can no longer be opened, so the rows pointing at them go too. Keeping
     * them would leave a list whose entries all fail on click.
     */
    val FROM_1_TO_2: Migration = object : Migration(startVersion = 1, endVersion = 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(sql = "DELETE FROM recent_package WHERE path NOT LIKE '%.mjclog'")
            connection.execSQL(sql = "ALTER TABLE recent_package DROP COLUMN kind")
        }
    }

    /** Settings arrived, and they outlive the workspace they were set in. */
    val FROM_2_TO_3: Migration = object : Migration(startVersion = 2, endVersion = 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                sql = "CREATE TABLE IF NOT EXISTS preference " +
                    "(`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(FROM_1_TO_2, FROM_2_TO_3)
}
