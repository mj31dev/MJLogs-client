package dev.mj31.logger.client.app.features.legal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import dev.mj31.logger.client.app.BuildInfo
import dev.mj31.logger.client.app.resources.Res
import dev.mj31.logger.client.app.resources.about_app_name
import dev.mj31.logger.client.app.resources.about_bundled
import dev.mj31.logger.client.app.resources.about_copyright
import dev.mj31.logger.client.app.resources.about_license
import dev.mj31.logger.client.app.resources.about_show_notices
import dev.mj31.logger.client.app.resources.about_version
import dev.mj31.logger.client.app.resources.app_icon
import dev.mj31.logger.client.app.resources.legal_loading
import dev.mj31.logger.client.app.resources.legal_unavailable_description
import dev.mj31.logger.client.app.resources.legal_unavailable_title
import dev.mj31.logger.client.app.resources.legal_window_title
import dev.mj31.logger.client.app.theme.LoggerTheme
import dev.mj31.logger.client.app.usecase.legal.ReadLegalNoticesUseCase
import dev.mj31.logger.client.domain.model.legal.LegalNotice
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What this application is, and what it is made of.
 *
 * A window rather than a dialog on purpose: the licences are read next to what they cover, and the
 * decision they inform is whether to keep using the application, not how to answer a prompt.
 */
@Composable
fun AboutWindow(
    readLegalNotices: ReadLegalNoticesUseCase,
    onCloseRequest: () -> Unit,
) {
    val windowState = rememberWindowState(size = DpSize(width = 860.dp, height = 660.dp))
    var notices by remember { mutableStateOf<List<LegalNotice>?>(value = null) }

    LaunchedEffect(key1 = readLegalNotices) { notices = readLegalNotices() }

    Window(
        onCloseRequest = onCloseRequest,
        title = stringResource(resource = Res.string.legal_window_title),
        icon = painterResource(resource = Res.drawable.app_icon),
        state = windowState,
    ) {
        LoggerTheme {
            AboutContent(notices = notices)
        }
    }
}

/** Split from the window so both panes can be exercised without opening one. */
@Composable
internal fun AboutContent(
    notices: List<LegalNotice>?,
    modifier: Modifier = Modifier,
) {
    var isReadingNotices by remember { mutableStateOf(value = false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        if (isReadingNotices) {
            LegalNoticeReader(
                notices = notices.orEmpty(),
                onBack = { isReadingNotices = false },
            )
        } else {
            AboutSummary(notices = notices, onReadNotices = { isReadingNotices = true })
        }
    }
}

@Composable
private fun AboutSummary(
    notices: List<LegalNotice>?,
    onReadNotices: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(all = SUMMARY_PADDING.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(resource = Res.drawable.app_icon),
            contentDescription = null,
            modifier = Modifier.size(size = ICON_SIZE.dp),
        )

        SummaryLine(
            text = stringResource(resource = Res.string.about_app_name),
            style = MaterialTheme.typography.headlineSmall,
            topPadding = TITLE_SPACING,
        )
        SummaryLine(
            text = stringResource(resource = Res.string.about_version, BuildInfo.PRODUCT_VERSION),
            style = MaterialTheme.typography.bodyMedium,
            isMuted = true,
        )
        SummaryLine(
            text = stringResource(resource = Res.string.about_copyright),
            style = MaterialTheme.typography.bodyMedium,
            topPadding = BLOCK_SPACING,
        )
        SummaryLine(
            text = stringResource(resource = Res.string.about_license),
            style = MaterialTheme.typography.bodyMedium,
        )
        SummaryLine(
            text = stringResource(resource = Res.string.about_bundled),
            style = MaterialTheme.typography.bodySmall,
            isMuted = true,
            topPadding = BLOCK_SPACING,
        )

        NoticesAction(notices = notices, onReadNotices = onReadNotices)
    }
}

/**
 * The way into the full texts, or the reason there is none.
 *
 * An unpackaged run carries no notice because it is not a distribution; saying where the texts live
 * in a real build is more use than a button that would open an empty reader.
 */
@Composable
private fun NoticesAction(
    notices: List<LegalNotice>?,
    onReadNotices: () -> Unit,
) {
    when {
        notices == null -> SummaryLine(
            text = stringResource(resource = Res.string.legal_loading),
            style = MaterialTheme.typography.bodyMedium,
            isMuted = true,
            topPadding = BLOCK_SPACING,
        )

        notices.isEmpty() -> {
            SummaryLine(
                text = stringResource(resource = Res.string.legal_unavailable_title),
                style = MaterialTheme.typography.bodyMedium,
                topPadding = BLOCK_SPACING,
            )
            SummaryLine(
                text = stringResource(resource = Res.string.legal_unavailable_description),
                style = MaterialTheme.typography.bodySmall,
                isMuted = true,
            )
        }

        else -> Button(
            onClick = onReadNotices,
            modifier = Modifier.padding(top = BLOCK_SPACING.dp),
        ) {
            Text(text = stringResource(resource = Res.string.about_show_notices))
        }
    }
}

@Composable
private fun SummaryLine(
    text: String,
    style: TextStyle,
    isMuted: Boolean = false,
    topPadding: Int = LINE_SPACING,
) {
    Text(
        text = text,
        style = style,
        textAlign = TextAlign.Center,
        color = if (isMuted) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        modifier = Modifier
            .widthIn(max = SUMMARY_WIDTH.dp)
            .padding(top = topPadding.dp),
    )
}

private const val SUMMARY_PADDING = 32
private const val SUMMARY_WIDTH = 560
private const val ICON_SIZE = 96
private const val TITLE_SPACING = 20
private const val BLOCK_SPACING = 20
private const val LINE_SPACING = 4
