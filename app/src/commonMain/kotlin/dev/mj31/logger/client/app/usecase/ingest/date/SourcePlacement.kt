package dev.mj31.logger.client.app.usecase.ingest.date

import dev.mj31.logger.client.app.usecase.ingest.source.LogSourceLoader
import dev.mj31.logger.client.app.usecase.ingest.zone.ResolveSourceZoneUseCase
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.model.log.LogSource
import dev.mj31.logger.client.domain.source.TextFileContent
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Builds a source on the day its records belong to and in the zone its clock runs in, taking what
 * the file states in its preamble into account.
 *
 * The preamble is only known once the file has been read under its format, and reading it needs a
 * day and a zone, so the file is read once on the best guess available without it and read again
 * only when its own header says otherwise. Returns `null` when no day can be found at all.
 *
 * @param statedDay a day already settled — by the user, or stored with a restored workspace — which
 * no preamble overrides.
 */
@Suppress("LongParameterList")
internal fun ResolveReferenceDateUseCase.buildPlaced(
    loader: LogSourceLoader,
    content: TextFileContent,
    spec: LogFormatSpec,
    displayName: String,
    modifiedAt: Instant?,
    statedDay: LocalDate? = null,
    sourceId: String? = null,
    resolveZone: ResolveSourceZoneUseCase = ResolveSourceZoneUseCase(),
): Pair<LogSource, ReferenceDateOrigin>? {
    val provisionalDay = statedDay ?: invoke(fileName = displayName, modifiedAt = modifiedAt)?.first ?: UNDATED
    val provisionalZone = resolveZone(spec = spec, preamble = emptyList())
    val assumed = loader.buildSource(
        content = content,
        spec = spec,
        referenceDate = provisionalDay,
        sourceId = sourceId,
        displayName = displayName,
        zone = provisionalZone,
    )
    val (day, origin) = if (statedDay != null) {
        statedDay to ReferenceDateOrigin.STATED
    } else {
        invoke(fileName = displayName, modifiedAt = modifiedAt, preamble = assumed.preamble) ?: return null
    }
    val zone = resolveZone(spec = spec, preamble = assumed.preamble)
    val source = if (day == provisionalDay && zone == provisionalZone) {
        assumed
    } else {
        loader.buildSource(
            content = content,
            spec = spec,
            referenceDate = day,
            sourceId = assumed.id,
            displayName = displayName,
            zone = zone,
        )
    }
    return source to origin
}

/** Any day serves to find where the preamble ends; records are not placed on it. */
private val UNDATED = LocalDate(year = 1970, monthNumber = 1, dayOfMonth = 1)
