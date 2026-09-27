package dev.mj31.logger.client.data.workspace

import dev.mj31.logger.client.domain.model.log.part.LogSourcePart
import dev.mj31.logger.client.data.workspace.db.entity.WorkspaceLogSourcePartEntity
import dev.mj31.logger.client.data.workspace.db.entity.LastWorkspaceEntity
import dev.mj31.logger.client.data.workspace.db.entity.WorkspaceLogSourceEntity
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.model.log.LogFilter
import dev.mj31.logger.client.domain.model.log.LogLevel
import dev.mj31.logger.client.domain.model.media.VideoMedia
import dev.mj31.logger.client.domain.model.workspace.LogSourceRef
import dev.mj31.logger.client.domain.model.workspace.WorkspaceSnapshot
import dev.mj31.logger.client.domain.sync.SyncAnchor
import dev.mj31.logger.client.domain.sync.SyncOrigin
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Translation between the stored rows and the domain snapshot.
 *
 * Every enum is read back defensively. A store written by a newer version can hold a token this
 * build has never heard of, and the honest answer to that is to fall back rather than to crash on
 * the launch that follows an install.
 */
internal object WorkspaceMapping {

    private const val SEPARATOR = ","

    fun toEntity(snapshot: WorkspaceSnapshot): LastWorkspaceEntity = LastWorkspaceEntity(
        videoPath = snapshot.video?.path,
        videoName = snapshot.video?.name,
        anchorLogTimestampMillis = snapshot.anchor?.logTimestamp?.toEpochMilliseconds(),
        anchorVideoPositionMillis = snapshot.anchor?.videoPositionMillis,
        anchorOrigin = snapshot.anchor?.origin?.name,
        anchorLogEntryId = snapshot.anchor?.logEntryId,
        anchorAccuracyMillis = snapshot.anchor?.accuracyMillis,
        filterQuery = snapshot.filter.query,
        filterLevels = snapshot.filter.levels.joinToString(separator = SEPARATOR) { it.name },
        filterSourceIds = snapshot.filter.sourceIds.joinToString(separator = SEPARATOR),
        timeWindowMillis = snapshot.timeWindowMillis,
        followVideo = snapshot.followVideo,
        videoPositionMillis = snapshot.videoPositionMillis,
        packagePath = snapshot.packagePath,
    )

    fun toEntities(sources: List<LogSourceRef>): List<WorkspaceLogSourceEntity> =
        sources.mapIndexed { index, source ->
            WorkspaceLogSourceEntity(
                id = source.id,
                name = source.name,
                path = source.path,
                position = index,
                referenceDate = source.referenceDate?.toString(),
                formatName = source.format.name,
                formatKind = FormatColumns.kindOf(spec = source.format),
                formatLinePattern = FormatColumns.linePatternOf(spec = source.format),
                formatDelimiter = FormatColumns.delimiterOf(spec = source.format),
                formatHasHeader = FormatColumns.hasHeaderOf(spec = source.format),
                formatTimestampField = FormatColumns.fieldOf(
                    spec = source.format,
                    component = LogComponent.TIMESTAMP,
                ),
                formatLevelField = FormatColumns.fieldOf(spec = source.format, component = LogComponent.LEVEL),
                formatTagField = FormatColumns.fieldOf(spec = source.format, component = LogComponent.TAG),
                formatMessageField = FormatColumns.fieldOf(spec = source.format, component = LogComponent.MESSAGE),
                formatTimestampPattern = source.format.timestampPattern,
                formatFallbackLevel = source.format.fallbackLevel.name,
                formatZoneId = source.format.zoneId,
                formatOrigin = source.format.origin.name,
            )
        }

    fun toPartEntities(sources: List<LogSourceRef>): List<WorkspaceLogSourcePartEntity> =
        sources.flatMap { source ->
            source.extraParts.mapIndexed { index, part ->
                WorkspaceLogSourcePartEntity(
                    sourceId = source.id,
                    position = index,
                    path = part.path,
                    name = part.name,
                    referenceDate = part.referenceDate?.toString(),
                )
            }
        }

    fun toSnapshot(
        workspace: LastWorkspaceEntity,
        sources: List<WorkspaceLogSourceEntity>,
        parts: List<WorkspaceLogSourcePartEntity>,
    ): WorkspaceSnapshot = WorkspaceSnapshot(
        logSources = sources.map { source ->
            toRef(entity = source).copy(
                extraParts = parts.filter { it.sourceId == source.id }.sortedBy { it.position }.map { part ->
                    LogSourcePart(path = part.path, name = part.name, referenceDate = dateOf(text = part.referenceDate))
                },
            )
        },
        video = workspace.videoPath?.let { path ->
            VideoMedia(path = path, name = workspace.videoName.orEmpty())
        },
        anchor = toAnchor(workspace = workspace),
        filter = LogFilter(
            query = workspace.filterQuery,
            levels = workspace.filterLevels.splitTokens().mapNotNullTo(destination = mutableSetOf()) { token ->
                LogLevel.entries.firstOrNull { it.name == token }
            },
            sourceIds = workspace.filterSourceIds.splitTokens().toSet(),
        ),
        timeWindowMillis = workspace.timeWindowMillis,
        followVideo = workspace.followVideo,
        videoPositionMillis = workspace.videoPositionMillis,
        packagePath = workspace.packagePath,
    )

    private fun toRef(entity: WorkspaceLogSourceEntity): LogSourceRef = LogSourceRef(
        id = entity.id,
        name = entity.name,
        path = entity.path,
        format = FormatColumns.toSpec(entity = entity),
        referenceDate = dateOf(text = entity.referenceDate),
    )

    private fun dateOf(text: String?): LocalDate? =
        text?.let { stored -> runCatching { LocalDate.parse(input = stored) }.getOrNull() }

    /**
     * An anchor is stored across several nullable columns, and is only real when the two that carry
     * the correlation itself are both present.
     */
    private fun toAnchor(workspace: LastWorkspaceEntity): SyncAnchor? {
        val timestampMillis = workspace.anchorLogTimestampMillis ?: return null
        val videoPositionMillis = workspace.anchorVideoPositionMillis ?: return null
        return SyncAnchor(
            logTimestamp = Instant.fromEpochMilliseconds(epochMilliseconds = timestampMillis),
            videoPositionMillis = videoPositionMillis,
            origin = SyncOrigin.entries.firstOrNull { it.name == workspace.anchorOrigin }
                ?: SyncOrigin.SELECTED_ENTRY,
            logEntryId = workspace.anchorLogEntryId,
            accuracyMillis = workspace.anchorAccuracyMillis ?: 0L,
        )
    }

    private fun String.splitTokens(): List<String> =
        split(SEPARATOR).map { token -> token.trim() }.filter { token -> token.isNotEmpty() }
}
