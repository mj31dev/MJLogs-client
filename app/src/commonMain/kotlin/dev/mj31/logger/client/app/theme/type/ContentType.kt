package dev.mj31.logger.client.app.theme.type

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * The two monospaced styles, and the only two.
 *
 * Monospace means "this is compared column by column": what a file says, and the figures read
 * against it. Everything else in the interface is set in the Material roles.
 */
object ContentType {

    /**
     * Text as a file wrote it — a record, a header, a sample line in the format dialog. One size for
     * all of it, so a line reads the same wherever it is shown.
     */
    val record: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = RECORD_SIZE.sp)

    /** Figures read against a record: a timecode, a clock time, a correlation. Sized as metadata. */
    val figures: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)

    private const val RECORD_SIZE = 12
}
