package dev.mj31.logger.client.app.features.logplayer.dependencies

import dev.mj31.logger.client.app.usecase.workspace.CaptureWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.ClearWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.PersistWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.RestoreWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.CloseSessionPackageUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.OpenSessionPackageUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.SaveSessionPackageUseCase
import dev.mj31.logger.client.domain.repository.WorkspaceRepository

/** Everything the player needs to remember a workspace and to move it in and out of a file. */
data class LogPlayerWorkspace(
    val repository: WorkspaceRepository,
    val capture: CaptureWorkspaceUseCase,
    val restore: RestoreWorkspaceUseCase,
    val clear: ClearWorkspaceUseCase,
    val persist: PersistWorkspaceUseCase,
    val savePackage: SaveSessionPackageUseCase,
    val openPackage: OpenSessionPackageUseCase,
    val closePackage: CloseSessionPackageUseCase,
)
