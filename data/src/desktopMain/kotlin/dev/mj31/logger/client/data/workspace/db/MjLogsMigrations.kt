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

    /**
     * A format stopped being one shape.
     *
     * A specification now describes a regular expression, a JSON object or a delimited row, so the
     * line pattern is no longer something every row has — which is a change of nullability, and
     * SQLite only expresses that by rebuilding the table. The reference date joins the row in the
     * same pass: it used to be re-derived as "today" on every read, which quietly moved an old log
     * onto the day it was reopened.
     *
     * Every row written before this version is a regular expression format, hence the constant.
     */
    val FROM_3_TO_4: Migration = object : Migration(startVersion = 3, endVersion = 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(sql = CREATE_REBUILT_SOURCE_TABLE)
            connection.execSQL(sql = COPY_INTO_REBUILT_SOURCE_TABLE)
            connection.execSQL(sql = "DROP TABLE `workspace_log_source`")
            connection.execSQL(sql = "ALTER TABLE `_new_workspace_log_source` RENAME TO `workspace_log_source`")
        }
    }

    /**
     * A format's clock became a zone rather than an offset.
     *
     * An offset cannot follow daylight saving time across a log, and a file may name its own zone,
     * which only an unset value leaves room for. An offset of zero was the default rather than a
     * choice, so it becomes unset; any other becomes the fixed zone it denoted, spelled the way
     * `SourceZone` spells one.
     */
    val FROM_4_TO_5: Migration = object : Migration(startVersion = 4, endVersion = 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(sql = "ALTER TABLE `workspace_log_source` ADD COLUMN `formatZoneId` TEXT")
            connection.execSQL(sql = COPY_OFFSET_INTO_ZONE)
            connection.execSQL(sql = "ALTER TABLE `workspace_log_source` DROP COLUMN `formatUtcOffsetMinutes`")
        }
    }

    /** A source can be several files merged, and the files beyond the first get a table of their own. */
    val FROM_5_TO_6: Migration = object : Migration(startVersion = 5, endVersion = 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                sql = "CREATE TABLE IF NOT EXISTS `workspace_log_source_part` (" +
                    "`sourceId` TEXT NOT NULL, `position` INTEGER NOT NULL, `path` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, `referenceDate` TEXT, PRIMARY KEY(`sourceId`, `position`))",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(FROM_1_TO_2, FROM_2_TO_3, FROM_3_TO_4, FROM_4_TO_5, FROM_5_TO_6)

    private const val COPY_OFFSET_INTO_ZONE: String =
        "UPDATE `workspace_log_source` SET `formatZoneId` = CASE " +
            "WHEN `formatUtcOffsetMinutes` = 0 THEN NULL " +
            "ELSE printf('UTC%s%02d:%02d', " +
            "CASE WHEN `formatUtcOffsetMinutes` < 0 THEN '-' ELSE '+' END, " +
            "abs(`formatUtcOffsetMinutes`) / 60, abs(`formatUtcOffsetMinutes`) % 60) END"

    private const val CREATE_REBUILT_SOURCE_TABLE: String =
        "CREATE TABLE IF NOT EXISTS `_new_workspace_log_source` (" +
            "`id` TEXT NOT NULL, " +
            "`name` TEXT NOT NULL, " +
            "`path` TEXT NOT NULL, " +
            "`position` INTEGER NOT NULL, " +
            "`referenceDate` TEXT, " +
            "`formatName` TEXT NOT NULL, " +
            "`formatKind` TEXT NOT NULL, " +
            "`formatLinePattern` TEXT, " +
            "`formatDelimiter` TEXT, " +
            "`formatHasHeader` INTEGER, " +
            "`formatTimestampField` TEXT, " +
            "`formatLevelField` TEXT, " +
            "`formatTagField` TEXT, " +
            "`formatMessageField` TEXT, " +
            "`formatTimestampPattern` TEXT NOT NULL, " +
            "`formatFallbackLevel` TEXT NOT NULL, " +
            "`formatUtcOffsetMinutes` INTEGER NOT NULL, " +
            "`formatOrigin` TEXT NOT NULL, " +
            "PRIMARY KEY(`id`))"

    private const val COPY_INTO_REBUILT_SOURCE_TABLE: String =
        "INSERT INTO `_new_workspace_log_source` (" +
            "`id`, `name`, `path`, `position`, `referenceDate`, `formatName`, `formatKind`, " +
            "`formatLinePattern`, `formatDelimiter`, `formatHasHeader`, `formatTimestampField`, " +
            "`formatLevelField`, `formatTagField`, `formatMessageField`, `formatTimestampPattern`, " +
            "`formatFallbackLevel`, `formatUtcOffsetMinutes`, `formatOrigin`) " +
            "SELECT `id`, `name`, `path`, `position`, NULL, `formatName`, 'REGEX', " +
            "`formatLinePattern`, NULL, NULL, NULL, NULL, NULL, NULL, `formatTimestampPattern`, " +
            "`formatFallbackLevel`, `formatUtcOffsetMinutes`, `formatOrigin` " +
            "FROM `workspace_log_source`"
}
