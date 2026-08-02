package dev.mj31.logger.client.app.features.logplayer.state

import com.google.common.truth.Truth.assertThat
import dev.mj31.logger.client.app.fake.LogPlayerFixtures
import dev.mj31.logger.client.domain.format.compile.FormatErrorField
import dev.mj31.logger.client.domain.format.preview.FormatPreview
import dev.mj31.logger.client.domain.format.compile.ManualFormatInput
import dev.mj31.logger.client.domain.format.preview.PreviewLine
import kotlin.test.Test
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatRequestUiState
import dev.mj31.logger.client.app.features.logplayer.state.format.FormatError

class LogPlayerStateTest {

    @Test
    fun `a session is only considered loaded once it holds records`() {
        assertThat(LogPlayerState().hasLogs).isFalse()
        assertThat(LogPlayerState(totalEntryCount = 1).hasLogs).isTrue()
    }

    @Test
    fun `the view is filtered when fewer records are visible than loaded`() {
        val entries = listOf(LogPlayerFixtures.entry(id = "e1"))

        assertThat(LogPlayerState(entries = entries, totalEntryCount = 1).isFiltered).isFalse()
        assertThat(LogPlayerState(entries = entries, totalEntryCount = 2).isFiltered).isTrue()
        assertThat(LogPlayerState(entries = emptyList(), totalEntryCount = 2).isFiltered).isTrue()
    }

    @Test
    fun `the draft mirrors both inputs of the dialog`() {
        val request = request(preview = ready(matched = 1))

        assertThat(request.draft).isEqualTo(
            ManualFormatInput(timestampPattern = "HH:mm:ss", structureTemplate = "{timestamp} {message}"),
        )
    }

    @Test
    fun `a format can only be applied when it reads at least one line`() {
        assertThat(request(preview = ready(matched = 1)).canApply).isTrue()
        assertThat(request(preview = ready(matched = 0)).canApply).isFalse()
        assertThat(request(preview = FormatPreview.Empty).canApply).isFalse()
        assertThat(request(preview = FormatPreview.Invalid(message = "broken")).canApply).isFalse()
    }

    @Test
    fun `a blank input can never be applied`() {
        val blank = request(preview = ready(matched = 1)).copy(timestampPattern = "  ")

        assertThat(blank.canApply).isFalse()
    }

    @Test
    fun `the live preview reports the error while the user types`() {
        val request = request(
            preview = FormatPreview.Invalid(message = "no known token", field = FormatErrorField.TIMESTAMP_PATTERN),
        )

        assertThat(request.timestampPatternError).isEqualTo("no known token")
        assertThat(request.structureTemplateError).isNull()
        assertThat(request.generalError).isNull()
    }

    @Test
    fun `an import failure wins over the live preview`() {
        val request = request(
            preview = FormatPreview.Invalid(message = "no known token", field = FormatErrorField.TIMESTAMP_PATTERN),
        ).copy(error = FormatError(message = "No line matched", field = FormatErrorField.NONE))

        assertThat(request.generalError).isEqualTo("No line matched")
        assertThat(request.timestampPatternError).isNull()
    }

    @Test
    fun `a valid preview carries no error at all`() {
        val request = request(preview = ready(matched = 1))

        assertThat(request.activeError).isNull()
        assertThat(request.timestampPatternError).isNull()
        assertThat(request.structureTemplateError).isNull()
        assertThat(request.generalError).isNull()
    }

    private fun ready(matched: Int): FormatPreview.Ready = FormatPreview.Ready(
        lines = listOf(
            PreviewLine(text = "line", isRecord = matched > 0),
        ),
    )

    private fun request(preview: FormatPreview): FormatRequestUiState = FormatRequestUiState(
        path = "/logs/app.txt",
        fileName = "app.txt",
        sampleLines = listOf("line"),
        reason = "unknown",
        timestampPattern = "HH:mm:ss",
        structureTemplate = "{timestamp} {message}",
        preview = preview,
    )
}
