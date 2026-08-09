package dev.mj31.logger.client.domain.repository.preferences

import dev.mj31.logger.client.domain.model.preferences.ThemeChoice
import kotlinx.coroutines.flow.Flow

/**
 * Settings that belong to the person rather than to the workspace.
 *
 * Kept apart from [dev.mj31.logger.client.domain.repository.WorkspaceRepository] on purpose: a
 * workspace is replaced every time a session is opened, and a preference has to survive that.
 */
interface PreferencesRepository {

    val themeChoice: Flow<ThemeChoice>

    suspend fun setThemeChoice(choice: ThemeChoice)
}
