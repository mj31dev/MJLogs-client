package dev.mj31.logger.client.data.format.structure

import dev.mj31.logger.client.domain.format.LogComponent

/**
 * The names structured loggers give to each component.
 *
 * Recognition by name is a guess and is allowed to fail: when it does, the import falls through to
 * the format dialog, which is the same escape an unrecognized plain text file already takes. That is
 * why the lists stay short and conventional rather than trying to cover every logger ever written.
 */
internal object FieldSynonyms {

    private val byComponent: Map<LogComponent, List<String>> = mapOf(
        LogComponent.TIMESTAMP to listOf("timestamp", "time", "ts", "@timestamp", "date", "datetime", "eventtime"),
        LogComponent.LEVEL to listOf("level", "severity", "loglevel", "lvl", "priority"),
        LogComponent.TAG to listOf("tag", "logger", "logger_name", "category", "source", "component", "module"),
        LogComponent.MESSAGE to listOf("message", "msg", "text", "body", "event"),
    )

    /** Returns the first of [names] that is a known synonym of [component], ignoring case and `_`. */
    fun match(component: LogComponent, names: Collection<String>): String? {
        val synonyms = byComponent.getValue(key = component)
        return synonyms.firstNotNullOfOrNull { synonym ->
            names.firstOrNull { name -> normalize(text = name) == normalize(text = synonym) }
        }
    }

    private fun normalize(text: String): String =
        text.trim().lowercase().replace(oldValue = "_", newValue = "").replace(oldValue = "-", newValue = "")
}
