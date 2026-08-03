# MJLogs 1.0.0-alpha1 — watch a screencast next to the logs it produced

MJLogs puts a **screen recording of a bug on the left and the merged application logs on the
right**, and lets you line the two timelines up by hand. From then on the log list follows the
playhead, and clicking a record seeks the video.

It exists because correlating a QA recording with logs is still done by eye — and two things make
that tedious: the clocks do not agree (the recording starts when the tester presses record), and the
formats do not agree (every file comes from a different logger). MJLogs handles both, and everything
stays on the machine: no upload, no account, no telemetry.

## Highlights

- **Automatic format detection.** Unrecognized layouts are inferred from the sample itself
  (`01.08.2026_10.23.45` yields `dd.MM.yyyy_HH.mm.ss`); whatever inference misses you describe in a
  dialog that colour-codes the sample lines as you type.
- **Several log files, one chronological session.** Pick them in a single selection, then filter by
  text, level, source file or a time window around the playhead.
- **Manual synchronization, two ways.** Select a record and press *Synchronize*, or state the exact
  time the frame shows — typed or picked from a calendar — which also covers moments no record
  describes.
- **Partial overlap is expected.** The sync bar reports how much of the session the recording covers.
- **No prerequisites for playback.** FFmpeg ships inside the app, so an HEVC phone recording plays as
  readily as an H.264 capture.

## Download

`MJLogs-1.0.0-alpha1.dmg` (87 MB) — macOS on **Apple Silicon**. The bundle carries its own Java 21
runtime and the FFmpeg libraries.

The app is **not notarized**, so macOS refuses it on first launch. Open it once with right-click →
*Open*, or clear the quarantine flag:

```bash
xattr -dr com.apple.quarantine /Applications/MJLogs.app
```

Windows and Linux are supported by the code but not built here — the native decoder is resolved for
the host platform, so build on the target machine with `./gradlew :app:packageMsi` or
`./gradlew :app:packageDeb` (JDK 21).

The disk image carries a `Licenses` folder next to the application with the Apache 2.0 text, the
LGPL and GPL texts the bundled FFmpeg refers to, and the notice naming the exact FFmpeg build and
where to get its sources. The same four files are readable from **About MJLogs** inside the app.

## Try it in one command

```bash
./gradlew :app:desktopRun --args="samples/sample-clip.mp4 samples/network.txt samples/device-ui.txt samples/backend-service.txt"
```

The samples are three log files in three different formats that interleave in time, a short clip,
and one file whose format is deliberately unrecognizable.

## Known limitations

- Sessions live in memory only; nothing is persisted between runs.
- Synchronization is manual by design — no automatic alignment yet.
- Logs must be line-separated text (`.txt`, `.log`).
- The time picker reaches minutes (all Material 3 offers); seconds stay typed.
- The Apple Silicon build is the only published artifact of this alpha.

## Under the hood

Kotlin Multiplatform and Compose Desktop, clean architecture across `:domain`, `:data` and `:app`
with a compile-time DI graph, a suite that reaches from unit tests to rendered UI, the real import
pipeline and actual playback, and Detekt on every build. Apache 2.0; the bundled FFmpeg binaries are LGPL v3 and dynamically loaded, details in
[THIRD-PARTY.md](THIRD-PARTY.md).
