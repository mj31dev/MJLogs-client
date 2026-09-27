package dev.mj31.logger.client.data.workspace

import dev.mj31.logger.client.data.workspace.db.entity.WorkspaceLogSourceEntity
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import dev.mj31.logger.client.domain.format.spec.field.RecordFieldMap
import dev.mj31.logger.client.domain.model.log.LogLevel

/**
 * Translation between a format specification and the columns that hold it.
 *
 * Reading is deliberately forgiving. A store written by a newer build can name a kind this one has
 * never heard of, and a row can be internally inconsistent — a JSON format with no timestamp field
 * cannot happen through the application but can happen through a partially applied migration. Either
 * way the answer is a specification that reads nothing rather than an exception on the launch after
 * an install: the source then reports zero records, which the user can see and fix.
 */
internal object FormatColumns {

    private const val KIND_REGEX = "REGEX"
    private const val KIND_JSON = "JSON"
    private const val KIND_DELIMITED = "DELIMITED"

    /** Marks a locator that addresses a column by position rather than by name. */
    private const val INDEX_PREFIX = "#"

    fun kindOf(spec: LogFormatSpec): String = when (spec) {
        is LogFormatSpec.Regex -> KIND_REGEX
        is LogFormatSpec.Json -> KIND_JSON
        is LogFormatSpec.Delimited -> KIND_DELIMITED
    }

    fun linePatternOf(spec: LogFormatSpec): String? = (spec as? LogFormatSpec.Regex)?.linePattern

    fun delimiterOf(spec: LogFormatSpec): String? = (spec as? LogFormatSpec.Delimited)?.delimiter?.toString()

    fun hasHeaderOf(spec: LogFormatSpec): Boolean? = (spec as? LogFormatSpec.Delimited)?.hasHeader

    fun fieldOf(spec: LogFormatSpec, component: LogComponent): String? =
        fieldsOf(spec = spec)?.get(component = component)?.let(::textOf)

    fun toSpec(entity: WorkspaceLogSourceEntity): LogFormatSpec {
        val fields = fieldsOf(entity = entity)
        return when {
            entity.formatKind == KIND_JSON && fields != null -> LogFormatSpec.Json(
                name = entity.formatName,
                fields = fields,
                timestampPattern = entity.formatTimestampPattern,
                fallbackLevel = levelOf(entity = entity),
                zoneId = entity.formatZoneId,
                origin = originOf(entity = entity),
            )

            entity.formatKind == KIND_DELIMITED && fields != null -> LogFormatSpec.Delimited(
                name = entity.formatName,
                delimiter = entity.formatDelimiter?.firstOrNull() ?: DEFAULT_DELIMITER,
                hasHeader = entity.formatHasHeader ?: false,
                fields = fields,
                timestampPattern = entity.formatTimestampPattern,
                fallbackLevel = levelOf(entity = entity),
                zoneId = entity.formatZoneId,
                origin = originOf(entity = entity),
            )

            else -> LogFormatSpec.Regex(
                name = entity.formatName,
                linePattern = entity.formatLinePattern.orEmpty(),
                timestampPattern = entity.formatTimestampPattern,
                fallbackLevel = levelOf(entity = entity),
                zoneId = entity.formatZoneId,
                origin = originOf(entity = entity),
            )
        }
    }

    private fun fieldsOf(spec: LogFormatSpec): RecordFieldMap? = when (spec) {
        is LogFormatSpec.Regex -> null
        is LogFormatSpec.Json -> spec.fields
        is LogFormatSpec.Delimited -> spec.fields
    }

    private fun fieldsOf(entity: WorkspaceLogSourceEntity): RecordFieldMap? {
        val timestamp = locatorOf(text = entity.formatTimestampField) ?: return null
        return RecordFieldMap.of(
            timestamp = timestamp,
            level = locatorOf(text = entity.formatLevelField),
            tag = locatorOf(text = entity.formatTagField),
            message = locatorOf(text = entity.formatMessageField),
        )
    }

    private fun textOf(locator: ComponentLocator): String = when (locator) {
        is ComponentLocator.Key -> locator.name
        is ComponentLocator.Index -> "$INDEX_PREFIX${locator.position}"
    }

    private fun locatorOf(text: String?): ComponentLocator? {
        if (text.isNullOrEmpty()) return null
        if (!text.startsWith(prefix = INDEX_PREFIX)) return ComponentLocator.Key(name = text)
        val position = text.removePrefix(prefix = INDEX_PREFIX).toIntOrNull() ?: return null
        return ComponentLocator.Index(position = position)
    }

    private fun levelOf(entity: WorkspaceLogSourceEntity): LogLevel =
        LogLevel.entries.firstOrNull { it.name == entity.formatFallbackLevel } ?: LogLevel.INFO

    private fun originOf(entity: WorkspaceLogSourceEntity): FormatOrigin =
        FormatOrigin.entries.firstOrNull { it.name == entity.formatOrigin } ?: FormatOrigin.DETECTED

    private const val DEFAULT_DELIMITER = ','
}
