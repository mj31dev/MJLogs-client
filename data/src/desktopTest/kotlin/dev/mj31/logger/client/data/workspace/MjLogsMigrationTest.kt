package dev.mj31.logger.client.data.workspace

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.data.workspace.db.MjLogsDatabase
import dev.mj31.logger.client.data.workspace.db.MjLogsMigrations
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/**
 * Every step between two schema versions, driven against a real database.
 *
 * A store lives on the machine whether or not its owner wanted it there, so it is migrated rather
 * than thrown away — which only means anything if the migrations run. Until these existed, a
 * statement that threw would have surfaced on someone else's launch, in the one place there is no
 * way back from: the store is opened before the window appears.
 *
 * The schemas under `data/schemas` are what makes this possible and are committed for exactly this
 * reason: a version that only ever existed on one machine cannot be migrated away from.
 */
class MjLogsMigrationTest {

    private val directory = Files.createTempDirectory("mjlogs-migration").toFile()

    private val helper = MigrationTestHelper(
        schemaDirectoryPath = Path.of(SCHEMA_DIRECTORY),
        databasePath = directory.resolve("migration.db").toPath(),
        driver = BundledSQLiteDriver(),
        databaseClass = MjLogsDatabase::class,
    )

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    /**
     * The one that removes a column, which SQLite only learned to do in 3.35.
     *
     * The bundled driver is recent enough, but nothing said so until this ran.
     */
    @Test
    fun `dropping the package kind leaves the rest of a remembered file intact`() = runTest {
        helper.createDatabase(version = 1).use { connection ->
            connection.execSQL(
                sql = "INSERT INTO recent_package (path, name, kind, lastOpenedMillis) " +
                    "VALUES ('/cases/kept.mjclog', 'kept', 'FULL', 1700000000000)",
            )
        }

        helper.runMigrationsAndValidate(version = 2, migrations = listOf(MjLogsMigrations.FROM_1_TO_2))
            .use { connection ->
                val row = connection.prepare(sql = "SELECT path, name, lastOpenedMillis FROM recent_package")
                row.use {
                    assertThat(it.step()).isTrue()
                    assertThat(it.getText(index = 0)).isEqualTo("/cases/kept.mjclog")
                    assertThat(it.getText(index = 1)).isEqualTo("kept")
                    assertThat(it.getLong(index = 2)).isEqualTo(1_700_000_000_000L)
                }
            }
    }

    /**
     * Rows naming a file this build can no longer open go with the column.
     *
     * The two older extensions were dropped outright, so keeping their rows would leave a list whose
     * every entry fails on click — worse than a list that is shorter than the user remembers.
     */
    @Test
    fun `files saved under the extensions that were dropped are forgotten`() = runTest {
        helper.createDatabase(version = 1).use { connection ->
            listOf(
                "'/cases/old.mjlogs', 'old', 'LIGHT'",
                "'/cases/older.mjlogsx', 'older', 'FULL'",
                "'/cases/current.mjclog', 'current', 'FULL'",
            ).forEach { values ->
                connection.execSQL(
                    sql = "INSERT INTO recent_package (path, name, kind, lastOpenedMillis) " +
                        "VALUES ($values, 1700000000000)",
                )
            }
        }

        helper.runMigrationsAndValidate(version = 2, migrations = listOf(MjLogsMigrations.FROM_1_TO_2))
            .use { connection ->
                val paths = mutableListOf<String>()
                connection.prepare(sql = "SELECT path FROM recent_package").use { statement ->
                    while (statement.step()) paths += statement.getText(index = 0)
                }

                assertThat(paths).containsExactly("/cases/current.mjclog")
            }
    }

    /** Settings arrived in 3, and the workspace already stored has to survive their arrival. */
    @Test
    fun `adding settings keeps the workspace that was already stored`() = runTest {
        helper.createDatabase(version = 2).use { connection ->
            connection.execSQL(
                sql = "INSERT INTO last_workspace (id, filterQuery, filterLevels, filterSourceIds, " +
                    "followVideo, videoPositionMillis) VALUES (0, 'timeout', '', '', 1, 4200)",
            )
        }

        helper.runMigrationsAndValidate(version = 3, migrations = listOf(MjLogsMigrations.FROM_2_TO_3))
            .use { connection ->
                connection.prepare(sql = "SELECT filterQuery, videoPositionMillis FROM last_workspace")
                    .use { statement ->
                        assertThat(statement.step()).isTrue()
                        assertThat(statement.getText(index = 0)).isEqualTo("timeout")
                        assertThat(statement.getLong(index = 1)).isEqualTo(4_200L)
                    }
            }
    }

    /**
     * The whole chain at once, which is the path a store from the first build actually takes.
     *
     * Each migration passing on its own does not say they compose: the second runs on whatever the
     * first left behind, not on the schema it was written against.
     */
    @Test
    fun `a store from the first version reaches the current one`() = runTest {
        helper.createDatabase(version = 1).use { connection ->
            connection.execSQL(
                sql = "INSERT INTO recent_package (path, name, kind, lastOpenedMillis) " +
                    "VALUES ('/cases/kept.mjclog', 'kept', 'FULL', 1700000000000)",
            )
        }

        helper.runMigrationsAndValidate(version = 6, migrations = MjLogsMigrations.ALL.toList())
            .use { connection ->
                connection.prepare(sql = "SELECT name FROM recent_package").use { statement ->
                    assertThat(statement.step()).isTrue()
                    assertThat(statement.getText(index = 0)).isEqualTo("kept")
                }
                // `runMigrationsAndValidate` compares against the exported schema, so reaching this
                // line is the assertion that the result really is the current version.
                connection.prepare(sql = "SELECT COUNT(*) FROM preference").use { statement ->
                    assertThat(statement.step()).isTrue()
                }
            }
    }

    /**
     * Version 4 rebuilds the source table, which is the one migration here that can lose rows.
     *
     * SQLite cannot relax a column to nullable in place, so the table is recreated and copied into.
     * A copy that silently dropped a source would leave a workspace that reopens with fewer files
     * than it was saved with, and nothing in the application would report it.
     */
    @Test
    fun `rebuilding the source table keeps every stored format and calls it a regular expression`() = runTest {
        helper.createDatabase(version = 3).use { connection ->
            connection.execSQL(
                sql = "INSERT INTO workspace_log_source (id, name, path, position, formatName, " +
                    "formatLinePattern, formatTimestampPattern, formatFallbackLevel, " +
                    "formatUtcOffsetMinutes, formatOrigin) VALUES " +
                    "('src-1', 'app.log', '/logs/app.log', 0, 'logcat', '(?<ts>.*)', 'HH:mm:ss', " +
                    "'INFO', 0, 'DETECTED')",
            )
        }

        helper.runMigrationsAndValidate(version = 4, migrations = listOf(MjLogsMigrations.FROM_3_TO_4))
            .use { connection ->
                connection.prepare(
                    sql = "SELECT id, formatKind, formatLinePattern, formatTimestampPattern, " +
                        "referenceDate, formatDelimiter FROM workspace_log_source",
                ).use { statement ->
                    assertThat(statement.step()).isTrue()
                    assertThat(statement.getText(index = 0)).isEqualTo("src-1")
                    assertThat(statement.getText(index = 1)).isEqualTo("REGEX")
                    assertThat(statement.getText(index = 2)).isEqualTo("(?<ts>.*)")
                    assertThat(statement.getText(index = 3)).isEqualTo("HH:mm:ss")
                    // Never recorded before, and deliberately not invented here: it is resolved on
                    // restore through the same chain a fresh import uses.
                    assertThat(statement.isNull(index = 4)).isTrue()
                    assertThat(statement.isNull(index = 5)).isTrue()
                    assertThat(statement.step()).isFalse()
                }
            }
    }

    /**
     * Version 5 turns an offset into a zone, and zero into "not stated".
     *
     * Zero was what every format was given when nobody chose, so carrying it over as `UTC` would
     * make a default look like a choice and stop a file from naming its own zone.
     */
    @Test
    fun `an offset becomes the fixed zone it denoted and zero becomes unset`() = runTest {
        helper.createDatabase(version = 4).use { connection ->
            listOf("'plus', 180", "'minus', -330", "'zero', 0").forEachIndexed { position, values ->
                connection.execSQL(
                    sql = "INSERT INTO workspace_log_source (id, formatUtcOffsetMinutes, name, path, position, " +
                        "formatName, formatKind, formatTimestampPattern, formatFallbackLevel, formatOrigin) " +
                        "VALUES ($values, 'a.log', '/a.log', $position, 'f', 'REGEX', 'HH:mm', 'INFO', 'DETECTED')",
                )
            }
        }

        helper.runMigrationsAndValidate(version = 5, migrations = listOf(MjLogsMigrations.FROM_4_TO_5))
            .use { connection ->
                connection.prepare(sql = "SELECT id, formatZoneId FROM workspace_log_source ORDER BY position")
                    .use { statement ->
                        val zones = buildMap {
                            while (statement.step()) {
                                put(
                                    key = statement.getText(index = 0),
                                    value = if (statement.isNull(index = 1)) null else statement.getText(index = 1),
                                )
                            }
                        }
                        assertThat(zones).containsExactly("plus", "UTC+03:00", "minus", "UTC-05:30", "zero", null)
                    }
            }
    }

    /** Version 6 adds the table of merged parts, empty: nothing stored before it was ever merged. */
    @Test
    fun `merged parts get a table of their own and every stored source is kept`() = runTest {
        helper.createDatabase(version = 5).use { connection ->
            connection.execSQL(
                sql = "INSERT INTO workspace_log_source (id, name, path, position, formatName, formatKind, " +
                    "formatTimestampPattern, formatFallbackLevel, formatOrigin) " +
                    "VALUES ('src-1', 'a.log', '/a.log', 0, 'f', 'REGEX', 'HH:mm', 'INFO', 'DETECTED')",
            )
        }

        helper.runMigrationsAndValidate(version = 6, migrations = listOf(MjLogsMigrations.FROM_5_TO_6))
            .use { connection ->
                connection.prepare(sql = "SELECT COUNT(*) FROM workspace_log_source").use { statement ->
                    assertThat(statement.step()).isTrue()
                    assertThat(statement.getLong(index = 0)).isEqualTo(1L)
                }
                connection.prepare(sql = "SELECT COUNT(*) FROM workspace_log_source_part").use { statement ->
                    assertThat(statement.step()).isTrue()
                    assertThat(statement.getLong(index = 0)).isEqualTo(0L)
                }
            }
    }

    private companion object {
        const val SCHEMA_DIRECTORY = "schemas"
    }
}
