package dev.mj31.logger.client.domain.format.preview

import dev.mj31.logger.client.domain.format.LogComponent

/** Half open range `[startIndex, endIndex)` of a raw line that maps to [component]. */
data class HighlightedSpan(
    val component: LogComponent,
    val startIndex: Int,
    val endIndex: Int,
)
