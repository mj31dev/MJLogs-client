---
description: Enforces MVI (Model-View-Intent) unidirectional data flow pattern for UI state management in Compose Multiplatform.
globs: "app/src/commonMain/kotlin/dev/mj31/logger/client/app/ui/**/*.kt, app/src/commonMain/kotlin/dev/mj31/logger/client/app/mvi/**/*.kt"
---

# Rule: MVI Unidirectional Data Flow

1. **State Immutability**:
   - UI State (`LogPlayerState`) must be a single immutable `data class`.
   - Update state via `copy()` inside a `MutableStateFlow`.
   - Derived state is assembled in one place (`LogPlayerStateAssembler`), never in composables.

2. **User Intents**:
   - Represent user actions as sealed interfaces (`LogPlayerIntent`).
   - Process intents through a centralized `handleIntent(intent: LogPlayerIntent)` function.
   - Composables receive a single `onIntent: (LogPlayerIntent) -> Unit` callback; they never call behaviour methods directly.

3. **Single-Shot Effects**:
   - Model non-persisted events (transient messages, native file dialogs, navigation) as sealed interfaces (`LogPlayerEffect`).
   - Observe effects via `SharedFlow` or `Channel` (`LogPlayerStore.effects` uses a buffered `Channel`).
   - Never store a one-shot event in the state: it would replay on the next recomposition.

4. **Language Policy**:
   - All Kotlin code, sealed classes, docstrings, and rules MUST be in English.
   - Chat responses match the user's prompt language.
