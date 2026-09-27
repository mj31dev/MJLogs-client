package dev.mj31.logger.client.app.features.logplayer.ingest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import dev.mj31.logger.client.app.theme.Spacing

private const val NAME_LINES = 2
private const val FOLDER_LINES = 2

/**
 * The file a question is about, inset into the dialog so that it reads as the subject and not as
 * more prose.
 *
 * One import can raise several questions and the state hands them over one at a time, so every
 * dialog in this package names its file the same way: answering about the wrong file is the only way
 * any of them can fail. Two files chosen in one go can carry the same name, which is why the folder
 * is part of the identity here rather than decoration. It is the folder and not the whole path: the
 * path ends in the name that is already on the line above, and a long one truncated at the end would
 * drop the only part that tells the two apart.
 */
@Composable
internal fun ImportFileCard(
    fileName: String,
    path: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .padding(all = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(space = Spacing.hairline),
    ) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = NAME_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = folderOf(fileName = fileName, path = path),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = FOLDER_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Everything the path says beyond the name, on either separator; the path itself if it says nothing. */
private fun folderOf(fileName: String, path: String): String = path
    .removeSuffix(suffix = fileName)
    .trimEnd { character -> character == '/' || character == '\\' }
    .ifEmpty { path }
