package dev.mj31.logger.client.app.features.logplayer.state.ingest

import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import kotlinx.datetime.LocalDate

/**
 * A file whose records carry no date, and which two days fit equally well.
 *
 * The times in the file run past midnight, so it spans two days; which two is not written anywhere
 * in it, and the moment the file was last written rules out neither. Reading it either way would be
 * a guess that is wrong half the time and says nothing about it, so the question is put instead.
 *
 * [format] is the layout already worked out, carried so that answering costs one re-read and no
 * further questions.
 */
data class StartDayRequestUiState(
    val path: String,
    val fileName: String,
    val candidates: List<LocalDate>,
    val format: LogFormatSpec,
)
