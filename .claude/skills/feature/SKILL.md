---
name: feature
description: The pipeline for building a new feature in the MJLogs client, from reconnaissance through grilling to a layered implementation checklist. Invoked explicitly by the user as /feature; do not load it for bug fixes or small edits.
---

# Building a feature

Five stages, in order. Do not start writing production code before stage 4.

## 1. Reconnaissance — always first

The codebase has already solved a version of almost anything being asked for. Find it before
designing anything, and report what you found.

- A new dialog? Read `FormatWizardDialog` and `FrameTimePickerDialog` in
  `app/features/logplayer/{format,sync}/` — they show how a dialog is driven by state and returns
  intents.
- A new log format, or anything that reads a file? Read the `detect → parse → compile → preview`
  chain (see the `log-format-pipeline` skill).
- A new panel? `LogPane`, `VideoPane`, `SyncBar` in `app/features/logplayer/screen/`.
- New behaviour over the session? The use cases in `app/usecase/{ingest,session,sync,timeline}`.
- New state? `LogPlayerState`, `LogPlayerLocalState`, `LogPlayerStateAssembler`.

Say out loud which existing thing the new work resembles and where it differs. If nothing resembles
it, say that too — it usually means the feature is bigger than it sounded.

## 2. Grilling

Run the `grilling` skill on the feature before designing it. Reconnaissance comes first on purpose:
questions grounded in what the code already does are worth answering, and questions asked in
ignorance mostly reconstruct facts you could have read.

Grill until the frontier is empty, then get the user's confirmation of the shared understanding.

## 3. Design — whenever the work touches a screen

Skip this stage explicitly when the work adds no screen and changes none: a parser, a repository, a
migration. Say that you are skipping it and why.

Otherwise, hand the screen to the `designer` agent before writing it. Give it what grilling settled —
what the screen is for, what it holds, which action is the one it exists for — and let it come back
with the layout and the diffs. It reads `.claude/skills/design-system/SKILL.md`, renders what it
builds in both schemes, and looks at it.

Two things come back to you rather than to it: a design that needs a new field on the state, and any
rule of the design system it found itself fighting. The first is an MVI change and belongs in stage
4; the second is worth more than the screen it came up on, and the system is what gets fixed.

## 4. Implementation, bottom-up

Work the layers in this order. Each step is complete before the next begins — a screen written
against a use case that does not exist yet is a screen written against a guess.

1. **`:domain` model and port.** Immutable data class in
   `domain/src/commonMain/kotlin/dev/mj31/logger/client/domain/model/…`; the interface it is reached
   through in `domain/…/{format,player,repository,source,sync}/`. No implementation, no Compose,
   no coroutine infrastructure.
2. **`:data` implementation** of that port, under the matching `data/…` sub-package.
3. **Use case** in `app/usecase/<area>/`. One class, one verb, named `…UseCase`. All business
   decisions live here, never in the store and never in a composable.
4. **MVI wiring** in `app/features/logplayer/`: a `LogPlayerIntent` case for the user action, a
   `LogPlayerState` field if it must survive recomposition, a `LogPlayerEffect` case if it must
   happen exactly once, a branch in `LogPlayerStore.handleIntent`. Derived values go into
   `LogPlayerStateAssembler`. See the `mvi-logplayer` skill.
5. **Composable** in `app/features/logplayer/screen/` or `app/view/`, taking state plus a single
   `onIntent: (LogPlayerIntent) -> Unit`.
6. **Strings** in `app/src/commonMain/composeResources/values/strings.xml`, read via
   `stringResource`. Anything the store produces travels as `UiText.Resource`.
7. **DI** binding in `app/di/{DataBindings,UseCaseBindings,PresentationBindings}.kt`.

Throughout: named arguments for every call with two or more parameters; one declaration per file;
at most 5 Kotlin files per directory — when a package fills up, split it by meaning and say which
meaning.

## 5. Tests and the gate

Tests are mandatory; the order in which you write them is yours to choose. Domain and use-case logic
goes in `src/commonTest` with Google Truth; screen behaviour goes in `src/desktopTest` with
`runComposeUiTest` (see the `compose-ui-testing` skill).

Finish with `./gradlew test` and `./gradlew detekt`. The `Stop` hook runs Detekt anyway, so a
violation left behind will simply come back.
