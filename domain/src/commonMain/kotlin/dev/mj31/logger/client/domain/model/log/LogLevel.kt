package dev.mj31.logger.client.domain.model.log

/**
 * Severity of a single log record.
 *
 * The enum is intentionally kept independent of any concrete logging framework so that new
 * source formats only need to provide a mapping into [LogLevel] via [LogLevel.fromToken].
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
    ;

    companion object {

        /**
         * Tokens of the common logging frameworks, folded onto the six levels above.
         *
         * `TRACE` maps to [VERBOSE] because the enum has one level below `DEBUG`, and the two names
         * denote that same level in different ecosystems: Android, Apple's unified logging and
         * XCGLogger call it verbose, while SLF4J, Log4j, Timber and most backend loggers call it
         * trace. Keeping both would split one severity into two neighbouring filter chips that no
         * single file ever uses at the same time, and a merged session mixes files from both worlds.
         */
        private val tokenMapping: Map<String, LogLevel> = mapOf(
            "V" to VERBOSE,
            "VERBOSE" to VERBOSE,
            "T" to VERBOSE,
            "TRACE" to VERBOSE,
            "D" to DEBUG,
            "DEBUG" to DEBUG,
            "FINE" to DEBUG,
            "I" to INFO,
            "INFO" to INFO,
            "NOTICE" to INFO,
            "W" to WARN,
            "WARN" to WARN,
            "WARNING" to WARN,
            "E" to ERROR,
            "ERR" to ERROR,
            "ERROR" to ERROR,
            "F" to FATAL,
            "FATAL" to FATAL,
            "CRIT" to FATAL,
            "CRITICAL" to FATAL,
            "SEVERE" to FATAL,
            "ASSERT" to FATAL,
            "WTF" to FATAL,
        )

        /** All textual tokens understood by [fromToken], used to build detection patterns. */
        val knownTokens: Set<String> = tokenMapping.keys

        /** Returns the level for [token] ignoring case and surrounding noise, or `null` when unknown. */
        fun fromToken(token: String): LogLevel? = tokenMapping[token.trim().trim('[', ']', '(', ')', '<', '>').uppercase()]
    }
}
