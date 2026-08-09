package dev.mj31.logger.client.app.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Every gap in the application, on a 4dp base.
 *
 * The list is deliberately short. A screen that needs a value between two of these needs one of the
 * two: the question "is this gap part of the row or between the rows?" has an answer, and the answer
 * picks the step.
 */
object Spacing {

    /** Between a label and the value it names — they are one thing, just on two lines. */
    val hairline: Dp = 2.dp

    /** Inside a chip; between an icon and the word it belongs to. */
    val tight: Dp = 4.dp

    /** Between sibling controls in a row. */
    val small: Dp = 8.dp

    /** Padding inside a card, or inside one row of a list. */
    val medium: Dp = 12.dp

    /** Padding of a pane; the gap between cards. */
    val large: Dp = 16.dp

    /** Between groups that are not related to each other. */
    val xlarge: Dp = 24.dp

    /** Above a heading that starts a new subject. */
    val section: Dp = 32.dp
}
