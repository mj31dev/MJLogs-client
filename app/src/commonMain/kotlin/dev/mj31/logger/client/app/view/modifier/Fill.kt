package dev.mj31.logger.client.app.view.modifier

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * Paints [color] behind the element, or nothing at all when it is `null`.
 *
 * A surface that is filled only in some states — a selected chip, the current row of a list — has no
 * colour in the others, and "no colour" is not a colour to paint: the design system keeps
 * `Color.Transparent` out of the screens by giving them this instead.
 */
fun Modifier.fill(color: Color?, shape: Shape = RectangleShape): Modifier =
    if (color == null) this else background(color = color, shape = shape)
