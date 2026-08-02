package dev.mj31.logger.client.app.di

import dev.mj31.logger.client.app.features.logplayer.LogPlayerStore
import dev.mj31.logger.client.app.platform.NativeFileChooser
import dev.mj31.logger.client.app.platform.FileChooser
import dev.mj31.logger.client.data.player.DesktopVideoPlayerProvider
import dev.mj31.logger.client.data.source.LocalTextFileDataSource
import dev.mj31.logger.client.data.source.UuidIdGenerator
import dev.mj31.logger.client.domain.player.VideoPlayer
import dev.mj31.logger.client.domain.source.IdGenerator
import dev.mj31.logger.client.domain.source.TextFileDataSource
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
