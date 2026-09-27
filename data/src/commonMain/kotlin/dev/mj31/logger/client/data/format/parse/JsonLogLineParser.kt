package dev.mj31.logger.client.data.format.parse

import dev.mj31.logger.client.data.format.timestamp.CompiledTimestampPattern
import dev.mj31.logger.client.data.format.timestamp.TimestampResolutionContext
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.parse.LogLineParser
import dev.mj31.logger.client.domain.format.parse.ParsedLine
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import dev.mj31.logger.client.domain.model.log.LogLevel
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads a line holding one JSON object, addressing components by key.
 *
 * A key may be a dotted path (`log.level`), because structured loggers routinely nest the level and
 * the message under an envelope; without that a nested record could not even be described by hand.
 *
 * Like its regex counterpart an instance is bound to a single file — it remembers the previous
 * timestamp so that time-only patterns stay monotonic across midnight — and is therefore
 * **not thread safe**.
 */
class JsonLogLineParser internal constructor(
    private val spec: LogFormatSpec.Json,
    private val timestamp: CompiledTimestampPattern,
    private val referenceDate: LocalDate,
) : LogLineParser {

    private var previousTimestamp: Instant? = null
    private val zone: TimeZone = spec.zoneId?.let { TimeZone.of(zoneId = it) } ?: TimeZone.UTC

    override fun parse(line: String): ParsedLine {
        val record = objectOf(line = line) ?: return continuationOf(line = line)
        val rawTimestamp = valueOf(record = record, locator = spec.fields.timestamp)
            ?: return continuationOf(line = line)
        val context = TimestampResolutionContext(
            referenceDate = referenceDate,
            zone = zone,
            previous = previousTimestamp,
        )
        val resolved = timestamp.resolve(text = rawTimestamp, context = context)
            ?: return continuationOf(line = line)
        previousTimestamp = resolved

        return ParsedLine.Record(
            utcOffsetSeconds = timestamp.explicitOffsetSeconds(text = rawTimestamp),
            timestamp = resolved,
            level = levelOf(record = record),
            tag = textOf(record = record, component = LogComponent.TAG),
            message = textOf(record = record, component = LogComponent.MESSAGE),
        )
    }

    private fun continuationOf(line: String): ParsedLine.Continuation = ParsedLine.Continuation(text = line.trim())

    private fun objectOf(line: String): JsonObject? {
        val trimmed = line.trim()
        if (!trimmed.startsWith(prefix = "{")) return null
        return runCatching { JSON.parseToJsonElement(string = trimmed) as? JsonObject }.getOrNull()
    }

    private fun levelOf(record: JsonObject): LogLevel {
        val token = textOf(record = record, component = LogComponent.LEVEL)
        if (token.isEmpty()) return spec.fallbackLevel
        return LogLevel.fromToken(token = token) ?: spec.fallbackLevel
    }

    private fun textOf(record: JsonObject, component: LogComponent): String {
        val locator = spec.fields[component] ?: return ""
        return valueOf(record = record, locator = locator).orEmpty()
    }

    /** Resolves a dotted path; a value that is itself a structure is handed back as its JSON text. */
    private fun valueOf(record: JsonObject, locator: ComponentLocator): String? {
        val path = (locator as? ComponentLocator.Key)?.name ?: return null
        var current: JsonElement = record
        for (segment in path.split('.')) {
            val container = current as? JsonObject ?: return null
            current = container[segment] ?: return null
        }
        return when (val leaf = current) {
            is JsonPrimitive -> leaf.content
            else -> leaf.toString()
        }
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
