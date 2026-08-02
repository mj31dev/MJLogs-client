package dev.mj31.logger.client.domain.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import dev.mj31.logger.client.domain.model.log.LogLevel

class LogLevelTest {

    @Test
    fun `single letter tokens map to their level`() {
        assertThat(LogLevel.fromToken(token = "V")).isEqualTo(LogLevel.VERBOSE)
        assertThat(LogLevel.fromToken(token = "D")).isEqualTo(LogLevel.DEBUG)
        assertThat(LogLevel.fromToken(token = "I")).isEqualTo(LogLevel.INFO)
        assertThat(LogLevel.fromToken(token = "W")).isEqualTo(LogLevel.WARN)
        assertThat(LogLevel.fromToken(token = "E")).isEqualTo(LogLevel.ERROR)
        assertThat(LogLevel.fromToken(token = "F")).isEqualTo(LogLevel.FATAL)
    }

    @Test
    fun `full name tokens map to their level`() {
        assertThat(LogLevel.fromToken(token = "VERBOSE")).isEqualTo(LogLevel.VERBOSE)
        assertThat(LogLevel.fromToken(token = "DEBUG")).isEqualTo(LogLevel.DEBUG)
        assertThat(LogLevel.fromToken(token = "INFO")).isEqualTo(LogLevel.INFO)
        assertThat(LogLevel.fromToken(token = "WARN")).isEqualTo(LogLevel.WARN)
        assertThat(LogLevel.fromToken(token = "ERROR")).isEqualTo(LogLevel.ERROR)
        assertThat(LogLevel.fromToken(token = "FATAL")).isEqualTo(LogLevel.FATAL)
    }

    @Test
    fun `token resolution ignores case`() {
        assertThat(LogLevel.fromToken(token = "warning")).isEqualTo(LogLevel.WARN)
        assertThat(LogLevel.fromToken(token = "Error")).isEqualTo(LogLevel.ERROR)
        assertThat(LogLevel.fromToken(token = "d")).isEqualTo(LogLevel.DEBUG)
    }

    @Test
    fun `surrounding brackets and whitespace are trimmed`() {
        assertThat(LogLevel.fromToken(token = "[W]")).isEqualTo(LogLevel.WARN)
        assertThat(LogLevel.fromToken(token = "(error)")).isEqualTo(LogLevel.ERROR)
        assertThat(LogLevel.fromToken(token = "<I>")).isEqualTo(LogLevel.INFO)
        assertThat(LogLevel.fromToken(token = "  DEBUG  ")).isEqualTo(LogLevel.DEBUG)
    }

    @Test
    fun `trace and fine are aliases of verbose and debug`() {
        assertThat(LogLevel.fromToken(token = "T")).isEqualTo(LogLevel.VERBOSE)
        assertThat(LogLevel.fromToken(token = "TRACE")).isEqualTo(LogLevel.VERBOSE)
        assertThat(LogLevel.fromToken(token = "FINE")).isEqualTo(LogLevel.DEBUG)
    }

    @Test
    fun `notice is an alias of info`() {
        assertThat(LogLevel.fromToken(token = "NOTICE")).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun `warning is an alias of warn`() {
        assertThat(LogLevel.fromToken(token = "WARNING")).isEqualTo(LogLevel.WARN)
    }

    @Test
    fun `err is an alias of error`() {
        assertThat(LogLevel.fromToken(token = "ERR")).isEqualTo(LogLevel.ERROR)
    }

    @Test
    fun `critical severe assert and wtf are aliases of fatal`() {
        assertThat(LogLevel.fromToken(token = "CRIT")).isEqualTo(LogLevel.FATAL)
        assertThat(LogLevel.fromToken(token = "CRITICAL")).isEqualTo(LogLevel.FATAL)
        assertThat(LogLevel.fromToken(token = "SEVERE")).isEqualTo(LogLevel.FATAL)
        assertThat(LogLevel.fromToken(token = "ASSERT")).isEqualTo(LogLevel.FATAL)
        assertThat(LogLevel.fromToken(token = "WTF")).isEqualTo(LogLevel.FATAL)
    }

    @Test
    fun `unknown tokens return null`() {
        assertThat(LogLevel.fromToken(token = "PANIC")).isNull()
        assertThat(LogLevel.fromToken(token = "")).isNull()
        assertThat(LogLevel.fromToken(token = "[]")).isNull()
        assertThat(LogLevel.fromToken(token = "Warn me")).isNull()
    }

    @Test
    fun `known tokens expose every accepted spelling`() {
        assertThat(LogLevel.knownTokens).contains("WTF")
        assertThat(LogLevel.knownTokens).contains("VERBOSE")
        assertThat(LogLevel.knownTokens).doesNotContain("PANIC")
        LogLevel.knownTokens.forEach { token ->
            assertThat(LogLevel.fromToken(token = token)).isNotNull()
        }
    }

    @Test
    fun `levels are declared from least to most severe`() {
        assertThat(LogLevel.entries)
            .containsExactly(
                LogLevel.VERBOSE,
                LogLevel.DEBUG,
                LogLevel.INFO,
                LogLevel.WARN,
                LogLevel.ERROR,
                LogLevel.FATAL,
            )
            .inOrder()
    }
}
