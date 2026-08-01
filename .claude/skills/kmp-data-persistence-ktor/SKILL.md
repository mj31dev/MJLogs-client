---
name: kmp-data-persistence-ktor
description: Production playbooks for Ktor network streaming, Room/SQLDelight local storage, and async logging buffers in KMP.
---

# KMP Data Persistence & Network Streaming Skill

## Overview
Provides production patterns for handling data persistence, network streaming, socket connections, and async log buffering in Kotlin Multiplatform.

## Core Guidelines
1. **Networking (Ktor Client)**:
   - Use Ktor CIO / OkHttp engine abstraction for JVM Desktop.
   - Install `ContentNegotiation`, `WebSockets`, and `Logging` plugins.
   - Wrap socket log streams in `Flow<LogEntry>` with automatic reconnect strategies.

2. **Persistence (Room / SQLDelight)**:
   - Keep Database entities (`LogEntity`) separate from Domain models (`LogEntry`).
   - Provide DAOs with reactive Flow queries (`fun observeLogs(): Flow<List<LogEntity>>`).

3. **Memory Buffering**:
   - Use `Channel.BUFFERED` or `SharedFlow` with `BufferOverflow.DROP_OLDEST` to prevent OutOfMemory crashes during high-volume log spikes.

## Language Policy
- All code, interfaces, and skill documentation must be in English.
- Respond in chat using the user's prompt language.
