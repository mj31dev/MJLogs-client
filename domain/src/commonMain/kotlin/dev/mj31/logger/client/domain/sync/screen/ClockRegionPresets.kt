package dev.mj31.logger.client.domain.sync.screen

/**
 * The bands of a frame a clock lives in.
 *
 * A clock belongs to a status bar, and a status bar runs the full width of the screen at the top or
 * at the bottom. That, and not a device model, is what these describe — an earlier version aimed a
 * narrow rectangle at where each phone puts its clock, and it was wrong in a way worth recording.
 *
 * A narrow rectangle has to be right about the horizontal position, and on iPhone the horizontal
 * position moves: arrive in an app by tapping a link in another one and iOS puts a `◀ Safari` chip in
 * the status bar, shifting the clock along. The rectangle then clipped the chip and the clock
 * together, and the recognizer made one mangled string of the two — reading `00:54` off a bar plainly
 * showing `08:44`, on every frame, consistently enough to look like an answer.
 *
 * Taking the whole band instead removes the question. Nothing else in a status bar is shaped like a
 * time, so there is no cost to the extra width: the battery percentage, the carrier and the signal
 * bars are simply not `H:MM`, and the reader is told to expect scattered fragments rather than one
 * line of prose.
 */
object ClockRegionPresets {

    /** The status bar of a phone recording, which is where nearly every screencast keeps its clock. */
    val TOP: ClockRegion = ClockRegion(left = 0f, top = 0f, right = 1f, bottom = 0.06f)

    /** The same band, deeper: a taller bar, a lower resolution, a recording with a title above it. */
    val TOP_TALL: ClockRegion = ClockRegion(left = 0f, top = 0f, right = 1f, bottom = 0.11f)

    /** A desktop capture keeps its clock in the task bar, and a task bar is usually at the bottom. */
    val BOTTOM: ClockRegion = ClockRegion(left = 0f, top = 0.94f, right = 1f, bottom = 1f)

    val BOTTOM_TALL: ClockRegion = ClockRegion(left = 0f, top = 0.89f, right = 1f, bottom = 1f)

    /** Tried in this order; the first band whose readings behave like a clock wins. */
    val ordered: List<ClockRegion> = listOf(TOP, TOP_TALL, BOTTOM, BOTTOM_TALL)
}
