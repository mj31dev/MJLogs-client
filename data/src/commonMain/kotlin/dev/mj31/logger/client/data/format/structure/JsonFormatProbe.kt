package dev.mj31.logger.client.data.format.structure

import dev.mj31.logger.client.data.format.timestamp.TimestampShapeInference
import dev.mj31.logger.client.domain.format.LogComponent
import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.domain.format.spec.field.ComponentLocator
import dev.mj31.logger.client.domain.format.spec.field.RecordFieldMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Decides whether a sample is JSON Lines, and if so how its components are named.
 *
 * The test is deliberately strict: *every* probe line has to be a JSON object. A single `{"a":1}`
 * inside an otherwise plain text log would otherwise drag the whole file into the JSON branch, and a
 * file read under the wrong shape is a worse outcome than one that falls through to the dialog.
 */
internal object JsonFormatProbe {

    private val JSON = Json { ignoreUnknownKeys = true; isLenient = true }

    fun probe(probes: List<String>): LogFormatSpec.Json? {
        val records = probes.map { line -> objectOf(line = line) ?: return null }
        if (records.isEmpty()) return null

        val keys = records.flatMap { it.keys }.distinct()
        val (timestampKey, timestampPattern) = timestampOf(records = records, keys = keys) ?: return null

        return LogFormatSpec.Json(
            name = FORMAT_NAME,
            fields = RecordFieldMap.of(
                timestamp = ComponentLocator.Key(name = timestampKey),
                level = locatorOf(component = LogComponent.LEVEL, keys = keys),
                tag = locatorOf(component = LogComponent.TAG, keys = keys),
                message = locatorOf(component = LogComponent.MESSAGE, keys = keys),
            ),
            timestampPattern = timestampPattern,
            origin = FormatOrigin.DETECTED,
        )
    }

    private fun objectOf(line: String): JsonObject? {
        val trimmed = line.trim()
        if (!trimmed.startsWith(prefix = "{")) return null
        return runCatching { JSON.parseToJsonElement(string = trimmed) as? JsonObject }.getOrNull()
    }

    /**
     * Finds the key holding the timestamp and the shape of its values.
     *
     * A key named like a timestamp is tried first, but the name alone does not settle it — the values
     * still have to agree on one layout. Failing that, any key whose values do is taken, which is
     * what recognizes a logger that calls the field something entirely of its own.
     */
    private fun timestampOf(records: List<JsonObject>, keys: List<String>): Pair<String, String>? {
        val named = FieldSynonyms.match(component = LogComponent.TIMESTAMP, names = keys)
        val ordered = listOfNotNull(named) + keys.filterNot { it == named }
        return ordered.firstNotNullOfOrNull { key ->
            patternOf(records = records, key = key)?.let { pattern -> key to pattern }
        }
    }

    private fun patternOf(records: List<JsonObject>, key: String): String? {
        val regions = records.mapNotNull { record ->
            textOf(record = record, key = key)?.let { value -> TimestampShapeInference.findRegion(line = value) }
        }
        if (regions.size != records.size) return null
        return TimestampShapeInference.infer(regions = regions)
    }

    private fun textOf(record: JsonObject, key: String): String? =
        (record[key] as? JsonPrimitive)?.content

    private fun locatorOf(component: LogComponent, keys: List<String>): ComponentLocator? =
        FieldSynonyms.match(component = component, names = keys)?.let { ComponentLocator.Key(name = it) }

    private const val FORMAT_NAME = "JSON Lines"
}
