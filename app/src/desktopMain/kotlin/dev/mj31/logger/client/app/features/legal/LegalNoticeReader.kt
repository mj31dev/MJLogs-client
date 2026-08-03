package dev.mj31.logger.client.app.features.legal

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.about_back
import dev.mj31.logger.client.domain.model.legal.LegalNotice
import org.jetbrains.compose.resources.stringResource

/**
 * The licence texts themselves, one tab per file that shipped.
 *
 * The tabs are labelled with the file names rather than with prettier titles: the same names appear
 * in the disk image and inside the installed application, so a user comparing the three sees one
 * document instead of three that merely look alike.
 */
@Composable
internal fun LegalNoticeReader(
    notices: List<LegalNotice>,
    onBack: () -> Unit,
) {
    var selected by remember(key1 = notices) { mutableStateOf(value = 0) }
    val current = notices[selected.coerceIn(minimumValue = 0, maximumValue = notices.lastIndex)]

    Row(modifier = Modifier.fillMaxWidth().padding(start = BACK_PADDING.dp, top = BACK_PADDING.dp)) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(resource = Res.string.about_back))
        }
    }

    ScrollableTabRow(
        selectedTabIndex = selected,
        containerColor = MaterialTheme.colorScheme.surface,
        edgePadding = TAB_EDGE_PADDING.dp,
    ) {
        notices.forEachIndexed { index, notice ->
            Tab(
                selected = index == selected,
                onClick = { selected = index },
                text = { Text(text = notice.fileName, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }

    // A fresh scroll position per document: reopening a licence half way down would only confuse.
    val readerState = remember(key1 = current.fileName) { LazyListState() }

    // One item per line rather than one Text for the whole document: the GNU texts run to tens of
    // thousands of characters, which a single paragraph layout measures in one go.
    SelectionContainer {
        LazyColumn(
            state = readerState,
            modifier = Modifier.fillMaxSize().padding(all = READER_PADDING.dp),
        ) {
            items(items = current.text.lines()) { line ->
                Text(
                    text = line,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = READER_FONT_SIZE.sp,
                    lineHeight = READER_LINE_HEIGHT.sp,
                )
            }
        }
    }
}

private const val BACK_PADDING = 8
private const val TAB_EDGE_PADDING = 12
private const val READER_PADDING = 16
private const val READER_FONT_SIZE = 12
private const val READER_LINE_HEIGHT = 17
