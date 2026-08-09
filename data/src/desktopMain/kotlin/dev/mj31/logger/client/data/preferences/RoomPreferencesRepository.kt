package dev.mj31.logger.client.data.preferences

import dev.mj31.logger.client.data.workspace.db.MjLogsDatabase
import dev.mj31.logger.client.data.workspace.db.entity.PreferenceEntity
import dev.mj31.logger.client.domain.model.preferences.ThemeChoice
import dev.mj31.logger.client.domain.repository.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Settings, in the application store beside the workspace.
 *
 * A value this build has never heard of falls back to the default rather than failing: a store
 * written by a newer version is a thing that happens to anyone who downgrades once, and the honest
 * answer to it is a sensible window rather than a crash on launch.
 */
class RoomPreferencesRepository(
    database: MjLogsDatabase,
    private val dispatcher: CoroutineDispatcher,
) : PreferencesRepository {

    private val dao = database.preferenceDao()

    override val themeChoice: Flow<ThemeChoice> = dao
        .observe(key = PreferenceEntity.KEY_THEME)
        .map { stored -> ThemeChoice.entries.firstOrNull { it.name == stored } ?: ThemeChoice.SYSTEM }

    override suspend fun setThemeChoice(choice: ThemeChoice) {
        withContext(context = dispatcher) {
            dao.upsert(
                preference = PreferenceEntity(key = PreferenceEntity.KEY_THEME, value = choice.name),
            )
        }
    }
}
