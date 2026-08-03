package dev.mj31.logger.client.app.di

import dev.mj31.logger.client.app.features.logplayer.LogPlayerStore
import dev.mj31.logger.client.app.platform.NativeFileChooser
import dev.mj31.logger.client.app.platform.FileChooser
import dev.mj31.logger.client.app.usecase.legal.ReadLegalNoticesUseCase
import dev.mj31.logger.client.data.legal.BundledLegalNoticeRepository
import dev.mj31.logger.client.data.player.DesktopVideoPlayerProvider
import dev.mj31.logger.client.data.source.LocalTextFileDataSource
import dev.mj31.logger.client.data.source.UuidIdGenerator
import dev.mj31.logger.client.domain.player.VideoPlayer
import dev.mj31.logger.client.domain.repository.LegalNoticeRepository
import dev.mj31.logger.client.domain.source.IdGenerator
import dev.mj31.logger.client.domain.source.TextFileDataSource
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

/**
 * Composition root of the desktop application.
 *
 * It adds the bindings only this platform can satisfy to the shared ones; the whole graph is
 * resolved by the compiler, so a missing or ambiguous dependency fails the build rather than the
 * application. Another target only has to provide its own component with these same bindings.
 */
@AppScope
@Component
abstract class DesktopAppComponent :
    DataBindings,
    UseCaseBindings,
    PresentationBindings {

    abstract val store: LogPlayerStore

    abstract val fileChooser: FileChooser

    abstract val readLegalNotices: ReadLegalNoticesUseCase

    /**
     * Accessor rather than a direct call to the provider: only the accessor returns the single
     * scoped instance, calling the provider function would build a second one.
     */
    abstract val applicationScope: CoroutineScope

    @AppScope
    @Provides
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(context = SupervisorJob() + Dispatchers.Main)

    @AppScope
    @Provides
    fun provideFileChooser(): FileChooser = NativeFileChooser()

    @Provides
    fun ioDispatcher(): IoDispatcher = Dispatchers.IO

    @Provides
    fun defaultDispatcher(): DefaultDispatcher = Dispatchers.Default

    @Provides
    fun textFileDataSource(dispatcher: IoDispatcher): TextFileDataSource =
        LocalTextFileDataSource(dispatcher = dispatcher)

    @Provides
    fun idGenerator(): IdGenerator = UuidIdGenerator()

    /**
     * Compose points this system property at the folder jpackage filled from `app/legal/common`.
     * It is absent when the application runs from a raw class path, and then there is nothing to read.
     */
    @Provides
    fun legalNoticeRepository(dispatcher: IoDispatcher): LegalNoticeRepository =
        BundledLegalNoticeRepository(
            resourcesDirectory = System.getProperty("compose.application.resources.dir")?.let(::File),
            dispatcher = dispatcher,
        )

    @AppScope
    @Provides
    fun videoPlayer(dispatcher: IoDispatcher): VideoPlayer =
        DesktopVideoPlayerProvider.create(dispatcher = dispatcher)

    /** Releases the native playback resources and stops every coroutine started by the store. */
    fun dispose() {
        store.release()
        applicationScope.cancel()
    }

    companion object
}
