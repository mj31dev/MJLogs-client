# MJLogs client

Compose Multiplatform **Desktop** application (macOS, Windows, Linux) that plays a screencast beside
the log files recorded with it. Kotlin 2.0+, Apache 2.0.

## Language policy

Everything committed to this repository — code, comments, KDoc, build scripts, markdown, resource
strings, hook scripts, agent and skill definitions — is written in **English**. Chat replies are
written in the language the user wrote in.

## Modules

| Module    | Package                            | Holds                                                                   |
| --------- | ---------------------------------- | ----------------------------------------------------------------------- |
| `:domain` | `dev.mj31.logger.client.domain`    | Immutable models and ports (interfaces) **only**. No use cases, no impls. |
| `:data`   | `dev.mj31.logger.client.data`      | Port implementations: parsers, file sources, players, repositories.       |
| `:app`    | `dev.mj31.logger.client.app`       | Use cases, MVI store, Compose UI, string resources, DI, entry point.      |

Dependencies run one way: `:app` → `:domain`, `:data`; `:data` → `:domain`; `:domain` depends on
nothing. Use cases live in `app/usecase`, never in `:domain` — the domain layer carries no behaviour
beyond what a model can answer about itself.

## Commands

```bash
./gradlew detekt              # formats and fails on what the formatter cannot fix; runs verifySourceLayout
./gradlew test                # unit tests across :domain, :data, :app
./gradlew :app:desktopTest    # includes the Compose UI tests in app/src/desktopTest
./gradlew :app:desktopRun     # launch the application
./gradlew :app:dmg            # macOS installer into app/build/distributions
```

There is no separate format task: `detekt` runs with `autoCorrect` on, so it is both the formatter
and the gate. A `Stop` hook runs it at the end of any turn that changed Kotlin files, and the
`.githooks/pre-commit` hook runs it again before a commit is created (`./scripts/setup-hooks.sh`
enables it).

## Rules that are not visible in the code

**Named arguments.** Every call, constructor and Composable invocation with two or more arguments
names its parameters: `LogEntry(id = "1", tag = "Network", message = "Connected")`. Enforced by
Detekt `style > NamedArguments`.

**One declaration per file**, named after it. Exceptions: private helpers inside an implementation
or test file, and Composables, which are grouped by screen or component.

**At most 5 Kotlin files per directory.** A fuller package is split into sub-packages by meaning —
pipeline stage, workflow, feature area — never into alphabetical chunks. Enforced by the Gradle task
`verifySourceLayout`, which `detekt` depends on.

**No hardcoded user-visible strings.** They live in
`app/src/commonMain/composeResources/values/strings.xml` and reach the screen through
`stringResource`. Messages produced by the store travel as `UiText` (`app/view/text/UiText.kt`) —
a string resource plus arguments — and are resolved by the composable that renders them.

**MVI, one direction.** `LogPlayerState` is a single immutable data class updated through `copy()`
inside a `MutableStateFlow`; derived state is assembled only in `LogPlayerStateAssembler`. User
actions are `LogPlayerIntent` values handled in `LogPlayerStore.handleIntent`; composables receive
one `onIntent: (LogPlayerIntent) -> Unit` and never call behaviour directly. Anything that must
happen exactly once — a notification, a native file dialog — is a `LogPlayerEffect` delivered
through a `Channel`, never stored in the state, where a recomposition would replay it.

**Tests** live in `src/commonTest` (multiplatform) or `src/desktopTest` (JVM and Compose UI) and
assert with Google Truth: `assertThat(actual).isEqualTo(expected)`. Every change ships with tests;
the order in which they are written is free. Never delete or comment out a failing assertion.

**Legal texts are load-bearing.** `app/appResources/common/` (`LGPL-3.0.txt`, `GPL-3.0.txt`,
`THIRD-PARTY.txt`) is wired into packaging through `appResourcesRootDir` and read at runtime by the
About window. FFmpeg is LGPL and must stay dynamically linked. Do not remove or bypass that
configuration. The same folder carries `tessdata/eng.traineddata`, the model the automatic
synchronization reads the on-screen clock with; only the `*.txt` files are staged into the disk
image's `Licenses` folder, so a binary never lands among the notices.

**Synchronization has three producers, one product.** Pinning a selected record, typing the time a
frame shows, and finding it automatically all end in a single `SyncAnchor`, which carries its own
`origin` and `accuracyMillis`. Manual use cases live in `app/usecase/sync/manual/`, automatic ones in
`app/usecase/sync/auto/{metadata,screen,zone}/`. Nothing downstream of the anchor knows or cares
which produced it.

**A workspace is described, never copied.** What survives a restart is a `WorkspaceSnapshot` — file
paths, the format each was read under, the anchor, the filter, the playhead — and never the parsed
`LogEntry` values. Files on disk are the source of truth and are read again on restore, so a log that
grew since the last visit comes back whole. Room lives in `data/…/workspace/` (JVM only, hence
`desktopMain`), and the same schema serves two databases: the application's own store and the one
inside a saved session file.

**One session format, written only when asked.** `.mjclog` bundles copies of the logs and the
screencast, so it is self-contained and can be handed to anyone. No archive can grow one entry in
place, so rewriting it copies every byte: it is written on an explicit save, on closing the session
and on leaving the application, and the window carries a marker while it is behind. There used to be
a second, reference-only format that was rewritten on every change; it made "saved" mean two
different things and produced files that silently stopped working when a log moved. The application
store is the live carrier regardless, so a crash loses nothing — everything reaches it the moment it
changes, except the playhead, which is written on a timer.

**Design is written down.** `.claude/skills/design-system/SKILL.md` holds the spacing steps, the
type roles, the three surface levels, the action hierarchy, the density rule and the motion table;
the tokens themselves live in `app/theme/`. A screen never writes a raw `dp` for spacing or a literal
`Color`. Two rules there are load-bearing rather than stylistic: depth is a change of surface colour
and never a shadow, because a shadow over a video frame reads as a rendering artefact; and content
never animates — a list of ten thousand rows that animates stutters, and a frame that cross-fades
cannot be trusted as evidence. The scheme comes in light and dark, chosen in `View` and stored in the
application store, and the severity palette is part of it: the greens and ambers that read on
`#0B1120` are invisible on white, so `LocalLogLevelColors` carries a set per scheme.

## Agents, skills, commands

`.claude/agents/` holds four: `architect`, `code-reviewer` and `test-engineer` — the first two
read-only, the third writing inside test directories — and `designer`, which both designs and
implements how a screen looks and writes inside `:app`. Its write access is wide and its rules are
narrow: the MVI cycle, the use cases, `:domain`, `:data`, behavioural assertions and the
layout-stability tests are off limits, and a design that needs a new state field is reported rather
than made. It renders what it builds and looks at it, in both schemes, because reasoning about
layout has already failed here twice past a green suite.

`.claude/skills/` holds `feature` (invoked explicitly as `/feature`; reconnaissance, grilling,
design, then a layered checklist) and `release` (the gate, the legal checks, version consistency,
the artifact, then the notes), plus `design-system`, `log-format-pipeline`, `compose-ui-testing` and
`mvi-logplayer`, which load themselves when relevant. `.claude/commands/` holds `/review` and
`/verify-ui`.

Claude Code is sandboxed to this repository in `.claude/settings.json`: ordinary local work runs
without permission prompts, while commit-producing commands and all Git remote operations are
denied. The user creates commits and handles remotes. Gradle Wrapper and JVM commands are explicit
sandbox exceptions because JVM networking is incompatible with the macOS sandbox proxy. Gradle may
read and write the user's `~/.gradle` cache; Kotlin/Native keeps its home in an ignored directory
inside this repository.
