package dev.mj31.logger.client.app.di

import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Scope

/**
 * Lifetime of the workspace: a binding annotated with it is created once per component.
 *
 * Repositories, the video player and the store hold the session state, so sharing a single instance
 * is a correctness requirement, not an optimization.
 */
@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

/** Dispatcher for blocking file and native calls; a typealias is how kotlin-inject tells them apart. */
typealias IoDispatcher = CoroutineDispatcher

/** Dispatcher for CPU bound work such as filtering a large session. */
typealias DefaultDispatcher = CoroutineDispatcher

/**
 * Dispatcher of width one, on which every recognition runs.
 *
 * The recognition handle is a native object that is not thread safe, and neither is the decoder the
 * frames come from. Confining both to a single thread is what makes them safe to share, exactly as
 * the player already confines its own grabber.
 */
typealias ScreenClockDispatcher = CoroutineDispatcher
