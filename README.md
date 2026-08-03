# MJLogs — watch a screencast next to the logs it produced

**MJLogs** is a free, offline desktop app (macOS, Windows, Linux) that puts a **screen recording of
a bug on the left and the merged application logs on the right**, and lets you **synchronize the two
timelines by hand** — pick the moment in the video, pick the matching log record, press one button.
From then on the log list follows the playhead, and clicking a record seeks the video.

It reads plain `.txt` / `.log` files from any logger — Android logcat, Apple unified logging,
XCGLogger, SLF4J/Log4j/Timber backends, syslog, epoch-millis lines — **detects the format of each
file automatically** and merges several files into one chronological session.

Built with Kotlin Multiplatform and Compose Multiplatform for Desktop; the video is decoded by a
**bundled FFmpeg**, so nothing has to be installed on the machine and an HEVC phone recording plays
as readily as an H.264 capture.

## Why this exists

A QA report is usually a screen recording. The engineering answer to it is usually in a log file —
often in several, from the app, the network layer and a backend service. Correlating them is done by
hand today: scrub the video, read the clock in the corner, search the log for that second, repeat.

Two things make it harder than it sounds:

- **The clocks do not agree.** A recording starts when the tester presses record, the log starts when
  the process starts. They overlap partially, and the video may well cover a moment no record
  describes.
- **The formats do not agree.** Every file in the pile is written by a different logger, so a single
  "open the logs" step means three different timestamp and level layouts.

MJLogs answers exactly that: automatic format detection per file, one merged chronological session,
and a manual anchor between the two timelines, because only a human knows which frame corresponds to
which record. Everything stays on the machine — no upload, no account, no telemetry.

*(Keywords, for the search that brought you here: video and log synchronization, screencast log
viewer, correlate screen recording with logs, merge multiple log files chronologically, log format
auto-detection, logcat viewer for desktop, Kotlin Multiplatform Compose Desktop log analyzer.)*

## What it does

- Load one screencast and **any number of log files in a single selection**. Accepted types are
  declared in one place (`SupportedFileTypes`): `.txt`/`.log` for logs, the usual container formats
  for video. The file chooser filters to them, and every import re-checks the path anyway, because a
  file can also arrive from the command line.
- Parse every file, **detect its format automatically** (timestamp, level, tag, everything else is
  the body) and **merge all files into one chronological session**.
- Filter the merged list by free text, by level, by source file, and by a time window around the
  video playhead.
- If a file's layout cannot be recognized, the app asks for a **timestamp pattern** and a **line
  structure** — pre-filled with the layout **inferred from the sample lines**, and every keystroke
  re-renders those lines **colour coded by component**, so the result is visible before applying it.
- The video timeline and the log timeline are **completely independent** until the user synchronizes
  them. There are two ways to do that:
  - select a record, position the playhead and press **Synchronize**;
  - or state **the exact time the frame shows** — type it (`18:50:07.267`, with or without a date)
    or press **Pick…** for a calendar and a clock — and press **Use this time**, which also covers a
    moment no log record describes. The picker reaches minutes, which is all Material3 offers, so
    the seconds and milliseconds already in the field survive it: the mouse sets the coarse moment,
    the keyboard refines it.
- The two timelines do **not** have to cover the same interval: the sync bar reports the overlap, and
  records outside the recording simply cannot be jumped to.

Everything lives in memory (a database backend can be added behind the existing repository ports).

## Icon

The application mark lives in `app/icons`: `icon.icns` for macOS, `icon.ico` for Windows and
`icon.png` for Linux and for the window itself. It says what the app is — the frame under the
playhead on the left, the records of the merged session on the right, and the violet anchor pinning
the two timelines together — and it is drawn in the colours of the design system.

## Running

```bash
./gradlew :app:desktopRun
```

Files can also be passed on the command line, which loads them straight into the workspace:

```bash
./gradlew :app:desktopRun --args="samples/sample-clip.mp4 samples/network.txt samples/device-ui.txt samples/backend-service.txt"
```

Demo material lives in [`samples/`](samples): three log files in three different formats that
interleave in time, a short screencast, plus `analytics-custom.txt` whose format is deliberately
unrecognizable so the manual-format dialog can be tried (timestamp `dd.MM.yyyy_HH.mm.ss`, structure
`<{any}>~{timestamp}~{tag}~{message}`).

## Packaging

```bash
./gradlew :app:dmg
```

One command produces a self-contained `.app` with its own JRE, wraps it into a `.dmg` and writes it
to `app/build/distributions/MJLogs-1.0.0-alpha1.dmg`. The version comes from `version` in
`gradle.properties` and is the single source of truth for every module. (`:app:packageDmg` is the
plain Compose task underneath; it leaves the installer in `build/compose/binaries/main/dmg/`.)

Two things worth knowing about the packaged build:

- **Installer versions are numeric.** jpackage accepts `MAJOR[.MINOR][.PATCH]` with a major above
  zero on every platform, so a pre-release qualifier cannot live in installer metadata: the product
  is `1.0.0-alpha1`, the bundle is `1.0.0`. `installerVersion` in `app/build.gradle.kts` drops the
  qualifier for the metadata, and the `dmg` task restores it in the file name so two pre-releases of
  the same version are told apart.
- **`jdk.unsupported` is required.** jpackage links a minimal runtime, and the native bridge under
  the video decoder needs `sun.misc.Unsafe`. Without that module the packaged app starts and shows
  logs but cannot decode a single frame, which no unit test can catch: it only appears in the
  installed build.

## Modules

| Module    | Package                         | Contains                                                                                      |
|-----------|---------------------------------|-----------------------------------------------------------------------------------------------|
| `:domain` | `dev.mj31.logger.client.domain` | Models and ports only — repositories, parser, compiler, player, file source. Pure Kotlin.      |
| `:data`   | `dev.mj31.logger.client.data`   | Implementations of those ports: format detection & parsing, in-memory storage, FFmpeg player.  |
| `:app`    | `dev.mj31.logger.client.app`    | Use cases, MVI store, Compose Desktop UI, string resources, compile-time DI graph.             |

Dependency direction is strictly `:app -> :data -> :domain`.

`:domain` holds **only interfaces and the models they speak in**; the application logic that
orchestrates them (`app/usecase`, split into `ingest/`, `session/`, `timeline/` and `sync/`) belongs
to `:app`, the layer that decides what a user action actually does.

### Presentation: MVI

`app/features/logplayer` holds one feature end to end — store, intents and effects at its root,
`dependencies/` for the collaborator holders the store is built from, `state/` for the snapshot and
its assembler, `screen/` for the composables bound to that state, and `format/` for the manual-format
dialog. The cycle is why they live together: the view renders one immutable `LogPlayerState`, emits
`LogPlayerIntent` values into the single `LogPlayerStore.handleIntent`, and receives one-shot
`LogPlayerEffect` events (transient messages, requests to open a native file dialog) through a
`Channel`. Nothing else can mutate the state, and no one-shot event is kept in it.

`app/view` holds the opposite kind of composable: pieces that know nothing about the feature —
`LogRow`, `FormatPreviewView`, `MessageBar`, the timecode formatters, the frame converter. They take
domain types or plain values and are reusable by any future feature, which is enforced by direction:
`features` may import `view`, never the other way round.

### Text and localization

Every user visible string lives in `app/src/commonMain/composeResources/values/strings.xml`, so a
translation is a new `values-<locale>` folder and nothing else. Messages produced by the store travel
as `UiText` — a string resource plus its arguments — and are resolved against the running locale only
when a composable renders them, which is also why a test asserts on the identity of a message instead
of on one particular translation of it. `UiText.Raw` carries the exception: diagnostics from the
parsing engine quote the offending input, so they are data rather than application text.

### One declaration per file

Every class, interface, object and enum lives in a file named after it; the exceptions are private
helpers of an implementation or of a test, which stay with the code they serve. Directories are kept
under **5 Kotlin files**, so a package that grows is split into sub-packages that carry a meaning
(`domain/format` became `spec/`, `compile/`, `detect/`, `parse/` and `preview/`, one per stage of the
pipeline). The size rule is enforced by `./gradlew verifySourceLayout`, which the `detekt` task
depends on. Detekt itself cannot express it: its rules visit a single file and know nothing about the
directory around it, so the check lives in the build instead.

### File chooser

The picker is the native one (`java.awt.FileDialog`, i.e. `NSOpenPanel` on macOS and the common item
dialog on Windows), because a desktop app should open the dialog the user already knows. Its
filtering is best effort — AWT ignores `FilenameFilter` on Windows, and `NSOpenPanel.shouldEnableURL`
only greys an entry out instead of hiding it — so the selection is validated again after the dialog
closes, and an unsupported file is refused with an explanation rather than silently ignored.

### Dependency injection

The graph is built by [kotlin-inject](https://github.com/evant/kotlin-inject) (KSP, compile time): a
missing or ambiguous dependency fails the build, and there is no reflection at runtime. Bindings are
declared in `app/di` and split by layer — `DataBindings`, `UseCaseBindings`, `PresentationBindings` —
while `DesktopAppComponent` adds what only this platform can build (file access, FFmpeg playback,
native dialogs, dispatchers). `:domain` and `:data` carry no DI annotations at all: only the
composition root knows a container exists, so another target just implements its own component over
the same shared bindings.

### Key extension points

- **Storage**: implement `LogSessionRepository` / `VideoRepository` / `SyncRepository`.
- **Playback**: implement `VideoPlayer` (the UI only ever sees `PlaybackState` and `VideoFrame`).
- **New log layout**: add a candidate to the built-in catalogue, or let the user describe it —
  both paths end in the same `LogFormatSpec`.
- **Automatic synchronization** (future): replace the manual anchor with a computed `SyncAnchor`;
  nothing else in the app has to change.

## Supported log formats

Detection scores a catalogue of *timestamp pattern x line structure* candidates against the first
200 non-blank lines and accepts the best one above a confidence threshold. Recognized out of the
box, among others:

```
2026-08-01 10:23:45.123 INFO  [NetworkClient] GET /api/users -> 200
2026-08-01T10:23:45.123+03:00 WARN  CacheStore: evicted 15 entries
08-01 10:23:45.123 D/Renderer( 1234): frame skipped
10:23:45.123 | FATAL | DatabaseEngine | lock contention
1785555032085 ERROR Boot failed
```

Lines that do not start a new record (stack traces, wrapped payloads) are appended to the previous
record instead of being dropped.

Levels are folded onto six values (`VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`) so that
files from different ecosystems share one set of filters. `TRACE` therefore maps to `VERBOSE`: both
names denote the level below `DEBUG` — Android, Apple and XCGLogger call it verbose, SLF4J, Log4j and
Timber call it trace — and a merged session regularly contains files from both worlds.

When no candidate matches, the layout is inferred from the sample itself: the timestamp is located by
the shape of its digit runs (which is how `01.08.2026_10.23.45` yields `dd.MM.yyyy_HH.mm.ss`, a
pattern no candidate covers), parts that are identical in every line become literals and parts that
vary become placeholders. The guess is only offered after it has been compiled and verified against
the sample, so the dialog never starts from a broken template.

Anything the inference misses is described by the user in the format dialog. Literal punctuation is
matched verbatim (`{{` and `}}` stand for literal braces), a run of spaces matches any whitespace, and
`{any}` skips a varying fragment that carries no information — a counter, a pid, a thread name. For
example `<0042>~15.01.2024_10.23.45~ANALYTICS~event dispatched` is covered by the timestamp pattern
`dd.MM.yyyy_HH.mm.ss` and the structure `<{any}>~{timestamp}~{tag}~{message}`.

## Licensing

The project is under the [Apache License 2.0](LICENSE). The bundled components and what they require
are listed in [THIRD-PARTY.md](THIRD-PARTY.md); the only copyleft obligation is LGPL v3 for the
FFmpeg binaries, which dynamic loading and the licence text shipped inside the app satisfy.

## Quality gates

```bash
./gradlew test      # unit tests (Google Truth) for :domain, :data and :app
./gradlew detekt    # static analysis, plus the directory size check
```

Beyond plain unit tests the suite covers three more levels:

- **UI** — `LogPaneTest`, `LogRowTest`, `VideoPaneTest`, `SyncBarTest`, `FormatWizardDialogTest` and
  `PlayerScreenTest` render the real composables with `runComposeUiTest`, click and type into them,
  and assert on the intents that come out.
- **Import pipeline** — the shipped samples go through real detection, parsing and merging
  (`SampleLogFilesIntegrationTest`, `EndToEndSessionTest`).
- **Playback** — the FFmpeg backend really decodes `samples/sample-clip.mp4`, seeks in it and paces
  playback (`FFmpegVideoPlayerTest`), and the DI graph is instantiated for real
  (`DesktopAppComponentTest`).
