package dev.mj31.logger.client.data.format.detect

import dev.mj31.logger.client.domain.format.spec.FormatOrigin
import dev.mj31.logger.client.domain.format.spec.LogFormatSpec
import dev.mj31.logger.client.data.format.timestamp.TimestampPatternCompiler
import dev.mj31.logger.client.data.format.line.LineFormatCompiler

/**
 * Catalogue of candidate log formats probed by [HeuristicLogFormatDetector].
 *
 * The catalogue is the cross product of the known timestamp layouts and the known line structures.
 * Both lists are ordered from the most specific to the most permissive entry, which is also the order
 * used to break ties between candidates that match the same number of sample lines.
 */
object BuiltInLogFormats {

    /** A timestamp layout together with the label shown in the user interface. */
    data class TimestampVariant(val label: String, val pattern: String)

    /** A line structure; [template] is a regex containing the placeholders of [LineFormatCompiler]. */
    data class StructureVariant(val label: String, val template: String)

    /** Timestamp layouts, most specific first. */
    val timestampVariants: List<TimestampVariant> = listOf(
        TimestampVariant(label = "ISO-8601", pattern = "yyyy-MM-ddTHH:mm:ss.SSSXXX"),
        TimestampVariant(label = "ISO-8601 with space", pattern = "yyyy-MM-dd HH:mm:ss.SSSXXX"),
        TimestampVariant(label = "ISO-8601 local", pattern = "yyyy-MM-ddTHH:mm:ss.SSS"),
        TimestampVariant(label = "date time millis", pattern = "yyyy-MM-dd HH:mm:ss.SSS"),
        TimestampVariant(label = "date time comma millis", pattern = "yyyy-MM-dd HH:mm:ss,SSS"),
        TimestampVariant(label = "date time seconds", pattern = "yyyy-MM-dd HH:mm:ss"),
        TimestampVariant(label = "slash date millis", pattern = "yyyy/MM/dd HH:mm:ss.SSS"),
        TimestampVariant(label = "slash date colon millis", pattern = "yyyy/MM/dd HH:mm:ss:SSS"),
        TimestampVariant(label = "date time colon millis", pattern = "yyyy-MM-dd HH:mm:ss:SSS"),
        TimestampVariant(label = "european date millis", pattern = "dd.MM.yyyy HH:mm:ss.SSS"),
        TimestampVariant(label = "common log format", pattern = "dd/MMM/yyyy:HH:mm:ss XXX"),
        TimestampVariant(label = "logcat date", pattern = "MM-dd HH:mm:ss.SSS"),
        TimestampVariant(label = "time millis", pattern = "HH:mm:ss.SSS"),
        TimestampVariant(label = "time seconds", pattern = "HH:mm:ss"),
        TimestampVariant(label = "epoch millis", pattern = "epochMillis"),
        TimestampVariant(label = "epoch seconds", pattern = "epochSeconds"),
    )

    /** Line structures, richest first so that formats capturing level and tag win ties. */
    val structureVariants: List<StructureVariant> = listOf(
        StructureVariant(
            label = "LEVEL tag message",
            template = "^\\s*%TS%\\s+(?<lvl>%LEVEL%)\\s+\\[?(?<tag>[\\w.\$/-]{1,60})\\]?\\s*[:\\-]?\\s*(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "[LEVEL] [tag] message",
            template = "^\\s*%TS%\\s+\\[(?<lvl>%LEVEL%)\\]\\s*\\[(?<tag>[^\\]]{1,60})\\]\\s*[:\\-]?\\s*(?<msg>.*)\$",
        ),
        // Bracketed level with an optional bracketed origin, a shape several mobile loggers emit:
        // `2024-01-15 10:23:45.123 [Info] > message` and `… [Debug] [Uploader.kt] enqueue() > message`.
        StructureVariant(
            label = "[LEVEL] optional [tag] message",
            template = "^\\s*%TS%\\s+\\[(?<lvl>%LEVEL%)\\]\\s*(?:\\[(?<tag>[^\\]]{1,60})\\]\\s*)?[>:\\-]?\\s*(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "logcat brief",
            template = "^\\s*%TS%\\s+(?<lvl>%LEVEL%)/(?<tag>[^(\\s]{1,60})\\(\\s*\\d+\\)\\s*:\\s?(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "logcat threadtime",
            template = "^\\s*%TS%\\s+\\d+\\s+\\d+\\s+(?<lvl>%LEVEL%)\\s+(?<tag>[^:]{1,60}?)\\s*:\\s?(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "pipe separated",
            template = "^\\s*%TS%\\s*\\|\\s*(?<lvl>%LEVEL%)\\s*\\|\\s*(?<tag>[^|]{1,60}?)\\s*\\|\\s*(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "bracketed time LEVEL tag message",
            template = "^\\s*\\[%TS%\\]\\s+(?<lvl>%LEVEL%)\\s+\\[?(?<tag>[\\w.\$/-]{1,60})\\]?\\s*[:\\-]?\\s*(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "bracketed time [LEVEL] message",
            template = "^\\s*\\[%TS%\\]\\s*\\[?(?<lvl>%LEVEL%)\\]?\\s*[:\\-]?\\s*(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "pipe separated without tag",
            template = "^\\s*%TS%\\s*\\|\\s*(?<lvl>%LEVEL%)\\s*\\|\\s*(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "LEVEL message",
            template = "^\\s*%TS%\\s+(?<lvl>%LEVEL%)\\s*:\\s*(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "bracketed time message",
            template = "^\\s*\\[%TS%\\]\\s*(?<msg>.*)\$",
        ),
        StructureVariant(
            label = "message only",
            template = "^\\s*%TS%\\s+(?<msg>.*)\$",
        ),
    )

    /** Every candidate specification, ordered by decreasing preference. */
    val candidates: List<LogFormatSpec> by lazy { buildCandidates() }

    private fun buildCandidates(): List<LogFormatSpec> {
        val result = mutableListOf<LogFormatSpec>()
        for (timestamp in timestampVariants) {
            val compiled = runCatching { TimestampPatternCompiler.compile(pattern = timestamp.pattern) }.getOrNull()
                ?: continue
            for (structure in structureVariants) {
                result += LogFormatSpec(
                    name = "${timestamp.label} - ${structure.label}",
                    linePattern = LineFormatCompiler.buildLinePattern(
                        template = structure.template,
                        timestampRegex = compiled.regexSource,
                    ),
                    timestampPattern = timestamp.pattern,
                    origin = FormatOrigin.DETECTED,
                )
            }
        }
        return result
    }
}
