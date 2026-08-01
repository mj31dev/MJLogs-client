---
description: Enforces MVI (Model-View-Intent) unidirectional data flow pattern for UI state management in Compose Multiplatform.
globs: "src/commonMain/kotlin/dev/mj31/logger/client/ui/**/*.kt"
---

# Rule: MVI Unidirectional Data Flow

1. **State Immutability**:
   - UI State (`LogViewerState`) must be a single immutable `data class`.
   - Update state via `copy()` inside a `MutableStateFlow`.

2. **User Intents**:
   - Represent user actions as sealed interfaces (`LogViewerIntent`).
   - Process intents through a centralized `handleIntent(intent: LogViewerIntent)` function.

3. **Single-Shot Effects**:
   - Model non-persisted events (navigation, toast alerts, file export triggers) as sealed interfaces (`LogViewerEffect`).
   - Observe effects via `SharedFlow` or `Channel`.

4. **Language Policy**:
   - All Kotlin code, sealed classes, docstrings, and rules MUST be in English.
   - Chat responses match the user's prompt language.
