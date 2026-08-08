package dev.mj31.logger.client.domain.sync.screen

import kotlinx.datetime.LocalTime

/**
 * What a clock on screen said, and whether it settled which half of the day it meant.
 *
 * A status bar set to twelve hours shows `2:39` and stops there: iOS prints no `AM` or `PM` beside
 * it at all, and several Android launchers do the same. So the digits alone can be two moments
 * twelve hours apart, and reading them as they stand puts an afternoon recording in the middle of
 * the night — which is what happens on any device whose owner never chose the twenty-four hour
 * setting, meaning most of them.
 *
 * Two things can settle it, and the reader is what sees them. An `AM` or `PM` beside the digits
 * settles it outright. So does a **padded hour**: a twenty-four hour dial writes `02:39`, a twelve
 * hour dial writes `2:39` and never `02:39`, so the leading zero is a statement in itself. What
 * survives both tests — a bare `2:39`, or a `12:39` that either dial could have produced — arrives
 * here marked [isHalfDayAmbiguous], for whoever has the evidence to decide.
 */
data class ScreenClockTime(
    val time: LocalTime,
    val isHalfDayAmbiguous: Boolean,
)
