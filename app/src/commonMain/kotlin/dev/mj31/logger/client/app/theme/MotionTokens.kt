package dev.mj31.logger.client.app.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

/**
 * The only durations and curves the application animates with.
 *
 * Motion here explains a change that would otherwise be a jump; it is never decoration. Two things
 * are never animated at all, and both are load-bearing:
 *
 * - **content** — log rows and the video frame change instantly. A list of ten thousand rows that
 *   animates is a list that stutters, and a frame that cross-fades is a frame that cannot be trusted
 *   as evidence of what was on the screen at that moment;
 * - **anything the user is aiming at** — a control does not move under the pointer.
 *
 * A case that is not listed here does not animate. Wanting one is a design question rather than an
 * implementation detail, so it is raised rather than answered with a guessed duration.
 */
object MotionTokens {

    /** Something floats in over the workspace: a notice, the save bar. */
    const val ENTER_MILLIS: Int = 180

    /** The same thing leaving. Faster: on its way out it has already stopped being interesting. */
    const val EXIT_MILLIS: Int = 120

    /** A value the eye should be able to follow — a progress bar filling. */
    const val VALUE_MILLIS: Int = 100

    /** A surface changing state under the pointer: hover, selection. */
    const val STATE_MILLIS: Int = 80

    /** Material's standard curve: quick to commit, gentle to settle. */
    val enterEasing: Easing = CubicBezierEasing(a = 0.4f, b = 0f, c = 0.2f, d = 1f)

    /** A value that stands for a quantity moves at a constant rate, or it misreports the quantity. */
    val valueEasing: Easing = LinearEasing
}
