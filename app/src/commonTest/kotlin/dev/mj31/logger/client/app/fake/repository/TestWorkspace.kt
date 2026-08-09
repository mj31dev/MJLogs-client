package dev.mj31.logger.client.app.fake.repository

import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerRepositories
import dev.mj31.logger.client.app.features.logplayer.dependencies.LogPlayerWorkspace
import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.app.usecase.workspace.CaptureWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.ClearWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.PersistWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.RestoreWorkspaceUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.CloseSessionPackageUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.OpenSessionPackageUseCase
import dev.mj31.logger.client.app.usecase.workspace.session.SaveSessionPackageUseCase
import dev.mj31.logger.client.domain.repository.WorkspaceRepository
import dev.mj31.logger.client.domain.session.SessionPackageStore
import kotlin.time.Clock

/**
 * The persistence contour, wired exactly as the composition root wires it.
 *
 * Only the two boundaries are faked — the durable store and the archive on disk — so what a test
 * exercises is the real capture, restore and save logic rather than a rehearsal of it.
 */
fun testWorkspace(
    repositories: LogPlayerRepositories,
    loader: LogSourceLoader,
    clock: Clock,
    workspaceRepository: WorkspaceRepository = FakeWorkspaceRepository(),
    packageStore: SessionPackageStore = FakeSessionPackageStore(),
): LogPlayerWorkspace {
    val restore = RestoreWorkspaceUseCase(
        loader = loader,
        sessionRepository = repositories.session,
        videoRepository = repositories.video,
        syncRepository = repositories.sync,
    )
    val persist = PersistWorkspaceUseCase(
        workspaceRepository = workspaceRepository,
        packageStore = packageStore,
    )
    val closePackage = CloseSessionPackageUseCase(
        packageStore = packageStore,
        persistWorkspace = persist,
    )
    return LogPlayerWorkspace(
        repository = workspaceRepository,
        capture = CaptureWorkspaceUseCase(
            sessionRepository = repositories.session,
            videoRepository = repositories.video,
            syncRepository = repositories.sync,
        ),
        restore = restore,
        clear = ClearWorkspaceUseCase(
            closeSessionPackage = closePackage,
            sessionRepository = repositories.session,
            videoRepository = repositories.video,
            syncRepository = repositories.sync,
            workspaceRepository = workspaceRepository,
        ),
        persist = persist,
        savePackage = SaveSessionPackageUseCase(
            packageStore = packageStore,
            workspaceRepository = workspaceRepository,
            clock = clock,
        ),
        openPackage = OpenSessionPackageUseCase(
            packageStore = packageStore,
            restoreWorkspace = restore,
            workspaceRepository = workspaceRepository,
            clock = clock,
        ),
        closePackage = closePackage,
    )
}
