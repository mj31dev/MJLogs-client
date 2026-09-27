package dev.mj31.logger.client.domain.format.spec.field

import dev.mj31.logger.client.domain.format.LogComponent

/**
 * Which part of a structured record carries which component.
 *
 * Only [LogComponent.TIMESTAMP] is mandatory: a record with no level or no tag is an ordinary log,
 * and the parser falls back to the format's default rather than refusing the line. The map is
 * deliberately partial for that reason instead of holding blank locators.
 */
data class RecordFieldMap(
    val locators: Map<LogComponent, ComponentLocator>,
) {

    init {
        require(value = locators.containsKey(key = LogComponent.TIMESTAMP)) {
            "A record field map must locate the timestamp; the other components are optional."
        }
    }

    operator fun get(component: LogComponent): ComponentLocator? = locators[component]

    val timestamp: ComponentLocator
        get() = locators.getValue(key = LogComponent.TIMESTAMP)

    companion object {

        /** Builds a map from the components that were located, dropping the ones that were not. */
        fun of(
            timestamp: ComponentLocator,
            level: ComponentLocator? = null,
            tag: ComponentLocator? = null,
            message: ComponentLocator? = null,
        ): RecordFieldMap = RecordFieldMap(
            locators = buildMap {
                put(LogComponent.TIMESTAMP, timestamp)
                level?.let { put(LogComponent.LEVEL, it) }
                tag?.let { put(LogComponent.TAG, it) }
                message?.let { put(LogComponent.MESSAGE, it) }
            },
        )
    }
}
