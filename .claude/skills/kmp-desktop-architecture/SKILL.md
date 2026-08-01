---
name: kmp-desktop-architecture
description: Clean Architecture guidelines, layer boundaries, domain immutability, and state management for KMP Desktop.
---

# KMP Desktop Architecture Skill

## Layer Responsibilities
1. **Domain Layer** (`dev.mj31.logger.client.domain`):
   - Contains pure Kotlin business models (`LogEntry`, `LogLevel`), use cases, and repository interfaces.
   - Zero framework dependencies (no Compose, no Swing, no platform imports).
2. **Data Layer** (`dev.mj31.logger.client.data`):
   - Implements repositories, network/socket streaming, file caching, and JSON parsing.
   - Uses `kotlinx.coroutines` and `kotlinx.datetime`.
3. **UI Layer** (`dev.mj31.logger.client.ui`):
   - Contains Compose Multiplatform UI screens, components, theme definitions, and view state holders.

## Coroutine & Dispatcher Rules
- Never use `GlobalScope`.
- Inject dispatchers or scope managers so asynchronous behavior can be unit-tested deterministically.
- Offload disk/socket operations to `Dispatchers.IO`.

## Language Rule
- All codebase files must be in English.
- Chat responses match the user's prompt language.
