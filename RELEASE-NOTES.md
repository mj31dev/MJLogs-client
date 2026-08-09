# MJLogs 1.0.0-alpha2 — it lines the timelines up by itself, and it remembers

The first alpha put a screen recording beside the logs it produced and let you align the two
timelines by hand. This one does the aligning for you, and stops forgetting everything the moment
you close the window.

Both of the things the previous notes listed as known limitations — "sessions live in memory only"
and "synchronization is manual by design" — are what this release is about.

## Automatic synchronization

Press *Synchronize automatically* and MJLogs finds the correlation point on its own, in two steps:

- **The container's creation time.** A recording knows when it started; that places the video on the
  wall clock to about a second.
- **The clock on the screen.** If the recording shows one, MJLogs reads it — with Tesseract, bundled,
  entirely on the machine — and waits for the moment it *changes minute*. A clock that reads 10:23
  could be anywhere inside that minute; the frame where it becomes 10:24 is exact. That takes the
  anchor from about a second down to a frame.

If the clock is somewhere unusual, *Point at the clock…* lets you drag a rectangle around it.

Every anchor carries **where it came from and how far off it can be** — the sync bar says
`from the clock on screen, ±40ms` or `by the selected record`, so you always know whether you are
looking at something exact or something approximate. Pinning a record by hand still works and still
wins; nothing downstream cares which of the three produced the anchor.

## Nothing is lost between runs

There is now a store on disk, and the workspace reaches it the moment anything changes — the files
you opened, the format each was read under, the anchor, the filters, the playhead. Close the window
mid-investigation, or lose the process outright, and it comes back where you left it.

Log files are **referenced, never copied** into it. They stay the source of truth and are read again
on restore, so a log that grew since your last visit comes back whole rather than truncated to
whatever was captured then.

## Sessions as files you can hand to someone

*Save session as…* writes a **`.mjclog`** — a single self-contained file carrying copies of every log
and of the screencast. Mail it, archive it, open it on another machine; nothing inside points at a
path that only existed on yours.

It is written when you ask, when you close the session and when you leave the application — never
behind your back, because it carries a screencast and rewriting it copies every byte. Between the
change and the write the window title carries a `•` to say the file is behind what is on screen.

Double-clicking one in the Finder opens it, once the application has been installed and launched
once.

## A start screen

Launching now lands on a list rather than an empty workspace: **continue where you left off**, reopen
a saved session, or start a new one. The last few sessions are also an inline submenu under
*Session → Recent sessions*, two clicks from anywhere.

## Light theme

*View → Match the system / Light / Dark.* The default follows the operating system; the choice is
remembered. The severity colours are defined per scheme — the greens and ambers that read against a
near-black workspace are close to invisible on white, so they are not reused.

## Smaller things

- **Jump to playhead** in the log pane header: the way back to the record under the current frame
  after you have scrolled away or switched *Follow video* off.
- **New session** empties the workspace, writing out and releasing the session file first rather than
  abandoning it mid-change.
- A missing file no longer takes the whole session down: it drops out with a message and the rest
  opens.

## Download

`MJLogs-1.0.0-alpha2.dmg` — macOS on **Apple Silicon**, 111 MB. The bundle carries its own Java 21
runtime, the FFmpeg libraries, the Tesseract recognizer and its English model, so it is noticeably
larger than the first alpha.

The disk image itself has been laid out rather than left to the default: the application and the
`Applications` folder sit side by side with the gesture between them drawn on the background, the
volume carries the application's own icon, and the `Licenses` folder is in plain sight instead of
buried in the bundle.

The app is **not notarized**, so macOS refuses it on first launch. Open it once with right-click →
*Open*, or clear the quarantine flag:

```bash
xattr -dr com.apple.quarantine /Applications/MJLogs.app
```

For `.mjclog` files to open from the Finder, the application has to live somewhere macOS scans —
`/Applications` — and be launched at least once, which is when the system learns what it handles.

Windows and Linux are supported by the code but not built here — the native decoder is resolved for
the host platform, so build on the target machine with `./gradlew :app:packageMsi` or
`./gradlew :app:packageDeb` (JDK 21).

The disk image carries a `Licenses` folder next to the application with the Apache 2.0 text, the LGPL
and GPL texts the bundled FFmpeg refers to, and the notice naming every bundled component. The same
files are readable from **About MJLogs** inside the app.

## Try it in one command

```bash
./gradlew :app:desktopRun --args="samples/device-screencast.mov samples/network.txt samples/device-ui.txt samples/backend-service.txt"
```

The recording shows a clock, so *Synchronize automatically* has something to read.

## Known limitations

- Logs must still be line-separated text (`.txt`, `.log`). JSON, CSV and compressed logs were
  scoped and deliberately deferred.
- Only the start screen has been rebuilt on the new design system; the player panes still carry the
  first alpha's spacing.
- The automatic clock reader needs a clock that is legible and that changes minute somewhere in the
  recording. When it finds neither, it says so and leaves the metadata anchor in place.
- The time picker reaches minutes (all Material 3 offers); seconds stay typed.
- One session is remembered as "last"; opening another replaces it. Anything you want to keep, save
  to a file.
- The Apple Silicon build is the only published artifact.

## Under the hood

Kotlin 2.3.21, Compose Multiplatform 1.11.1, Gradle 9.7, Room 2.8.4 with a bundled SQLite. Clean
architecture across `:domain`, `:data` and `:app` with a compile-time DI graph; the visual rules are
written down rather than implied, and screens are rendered and inspected as part of building them.
A suite of 608 tests reaches from the parsers to rendered UI, with Detekt on every build.

Apache 2.0; the bundled FFmpeg binaries are LGPL v3 and dynamically loaded, details in
[THIRD-PARTY.md](THIRD-PARTY.md).
