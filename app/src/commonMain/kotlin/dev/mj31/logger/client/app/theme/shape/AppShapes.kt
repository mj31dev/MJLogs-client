package dev.mj31.logger.client.app.theme.shape

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Every corner in the application, as the five Material shape roles.
 *
 * A screen reads them through `MaterialTheme.shapes` and never writes a `RoundedCornerShape` of its
 * own. The roles are picked by what the shape holds, not by how round it should look:
 *
 * - `extraSmall` — a token inside a row: a level chip, a marker, a swatch, a menu.
 * - `small` — a control or an inset block: a file chip, a field, a block of file text.
 * - `medium` — a card, and the frame of the video.
 * - `large` — a pane that floats: a notice, the save bar.
 * - `extraLarge` — a dialog.
 */
val AppShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(size = 4.dp),
    small = RoundedCornerShape(size = 6.dp),
    medium = RoundedCornerShape(size = 10.dp),
    large = RoundedCornerShape(size = 16.dp),
    extraLarge = RoundedCornerShape(size = 28.dp),
)
