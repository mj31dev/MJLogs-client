---
name: mvi-logplayer
description: The MVI contour of the MJLogs player screen — intents, state, effects, the assembler and where each piece of logic belongs. Use when touching LogPlayerStore, adding a user action or a screen field, or deciding whether something is state or an effect.
---

# The LogPlayer MVI contour

One screen, one store, one direction. The view emits a `LogPlayerIntent` into
`LogPlayerStore.handleIntent`; the store updates local state or delegates to a use case; the new
`LogPlayerState` flows back to the view. Anything that must happen exactly once leaves through
`effects`. Files live in `app/src/commonMain/kotlin/dev/mj31/logger/client/app/features/logplayer/`.

## Two states, and why

**`LogPlayerLocalState`** is what the store owns: filter, selection, `followVideo`, the pending
format requests, the frame-time text being typed. **`LogPlayerState`** is the immutable snapshot the
screen renders — sources, entries, video, sync, format request.

They are not the same thing, and the gap between them is `LogPlayerStateAssembler`. Repository
streams (session, video snapshot, sync state) are combined with the local state to produce the
snapshot, and the non-trivial derivations — the active record for the current playhead, the timeline
overlap between log and video — happen there, calling use cases from `app/usecase/timeline` and
`app/usecase/sync`.

The consequence for you: **a derived value never gets computed in a composable.** If the screen
needs to know something that follows from state, it is either a computed property on
`LogPlayerState` (see `hasLogs`, `isFiltered`) or it is assembled in `LogPlayerStateAssembler`.

## State or effect?

Ask whether replaying it after a window resize would be correct.

- A transient notification: **effect** (`LogPlayerEffect.ShowMessage`) — a bar that reappears every
  recomposition is a bug.
- A native file dialog: **effect** (`PickVideoFile`, `PickLogFiles`) — a dialog that reopens itself
  is worse than a bug.
- The format wizard: **state** (`LogPlayerState.formatRequest`) — it is meant to still be there.

Effects go through a buffered `Channel` exposed as `effects`. Never put one in the state.

## Adding a user action

1. A case in `LogPlayerIntent` — `data object` when it carries nothing, `data class` when it does.
   Document what answers it, as the existing cases do (`RequestVideoImport` names the effect that
   replies to it).
2. A branch in `LogPlayerStore.handleIntent`.
3. The decision itself in a use case under `app/usecase/<area>/`. The store owns no business rule:
   it routes, it does not decide. If you are writing an `if` about domain meaning inside the store,
   it belongs in a use case.
4. Whatever the action changes: a `LogPlayerLocalState` field, or a repository call, or an effect.
5. The composable emits it through the single `onIntent` it already receives — never a new callback
   parameter, and never a direct call into the store.

## Messages

Text produced by the store travels as `UiText` (`app/view/text/UiText.kt`) and is resolved by the
composable that renders it. `UiText.Resource(resource = Res.string.message_import_success,
arguments = listOf(name, count))` — the string itself lives in
`app/src/commonMain/composeResources/values/strings.xml`. `UiText.Raw` is only for text that is
already final: a file name, or a parser diagnostic that quotes the offending line.

This is why a test can assert on the identity of a message instead of on one translation of it.

## Testing it

`LogPlayerStore` and `LogPlayerStateAssembler` have suites in
`app/src/commonTest/…/features/logplayer/`. Drive the store with intents and assert on the resulting
state and on the effects collected from the channel; assemble the state directly to test a
derivation. Fakes are in `app/src/commonTest/…/app/fake/`. Inject the dispatcher — the store takes
one — and use `kotlinx-coroutines-test`.
