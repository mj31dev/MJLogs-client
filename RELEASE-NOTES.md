# MJLogs 1.0.0-alpha3 — it reads the logs you actually have

The second alpha lined a screencast up with its logs by itself and stopped forgetting between runs,
but it only read line-separated text, placed every dateless log on the day it was opened, read every
clock as UTC, and happily showed the same file twice.

The first item on its list of known limitations — "JSON, CSV and compressed logs were scoped and
deliberately deferred" — is the start of this release. The rest is about logs as they come out of
real devices and real servers: with a banner on top, from another time zone, and in overlapping
pieces.

## JSON, tables and archives

A log no longer has to be plain text:

- **JSON Lines** — one object per line. The time, level, tag and message are found by key, including
  keys a logger named its own way, as long as the values agree on one layout.
- **Comma, tab or semicolon separated tables**, with or without a header row. The delimiter is
  inferred: only one of them splits every line the same way.
- **`.gz` and `.zip`** — an archive is opened as the files inside it, as if you had picked them one by
  one.

When detection cannot tell, the format dialog now asks what shape the file is — *Plain lines*,
*JSON per line* or *Columns* — and describes each in its own terms: keys for JSON, columns by name or
position for a table.

A file with an extension MJLogs does not know is no longer refused outright: you are asked, because
a log carries no reserved extension and you may know better.

## The right day

A time without a date used to land on the day the file was opened, which moved last week's log onto
today. The day now comes from the file itself, strongest first:

- a date the file **writes in its header** (`Log started 2026-08-01 22:14`);
- a date in its **name**, as log rotation leaves them (`app.log.2026-08-01`);
- the moment it was **last written**.

A log that runs past midnight fits two days equally well, and reading it on the wrong one puts every
record a day away from the screencast. When nothing settles it, MJLogs asks which of the two it is,
once.

## Time zones

Every record is now placed on the real UTC timeline, and every file has a zone of its own: a region
such as `Europe/Berlin`, which follows daylight saving time across the file, or a fixed offset such as
`UTC+03:00`. Where the zone comes from, strongest first:

1. an offset written into the timestamps themselves (`+03:00`, `Z`);
2. the zone you chose for the file;
3. a zone the file names in its header (`TZ: Europe/Berlin`, `(UTC+3)`);
4. UTC, as before.

The log list shows each record's time **as the file wrote it**. Once the session holds a file that
is not in UTC, a quieter second column gives the same moment in UTC — that is the column files from
different zones interleave by. Each file's chip says which zone it is read in and where that came
from, and *Time zone…* in its menu changes it; the file is read again under the new zone and keeps
its identity, so filters and the anchor still point where they did.

The clock on the screen is read in the zone of the logs beside it — a phone in Berlin shows Berlin
time — whether you type what a frame shows or let *Synchronize automatically* read it. The sync bar
names that zone, and you can set it by hand.

## Headers

Text before a file's first record — a banner, the device, the build, when logging started — no longer
counts against its format and no longer shows up as skipped lines. It is kept as the file's header,
open verbatim from the chip's menu (*File header*), and its date and zone are used as described
above. JSON and table formats are recognized past a header too.

## The same file twice

- **The same path** is refused before anything is read: it is already open.
- **The same records under another path** — a copy — is put to you: skip it, or open it anyway.
- **An overlapping file** — the same log caught at another moment, a rotated file or a later copy —
  can be **merged** into the one already open: shared records are kept once, the rest joins them in
  time order, and the chip says `2 files`. A merged source remembers every file it was made from; they
  are saved into a `.mjclog` with it and read again on restore.

## The whole window on one design system

The player panes now follow the same written rules as the start screen: one spacing scale, one corner
scale by role, one menu without shadows, monospace only for text a file wrote and for the figures read
against it, and colours drawn on the video frame kept apart from the light and dark schemes. One bug
fell out of it: the placeholder shown before a video is loaded was dark text on the black frame in
the light scheme, and is now legible.

## Download

`MJLogs-1.0.0-alpha3.dmg` — macOS on **Apple Silicon**. The bundle carries its own Java 21 runtime,
the FFmpeg libraries, the Tesseract recognizer and its English model.

The app is **not notarized**, so macOS refuses it on first launch. Open it once with right-click →
*Open*, or clear the quarantine flag:

```bash
xattr -dr com.apple.quarantine /Applications/MJLogs.app
```

For `.mjclog` files to open from the Finder, the application has to live somewhere macOS scans —
`/Applications` — and be launched at least once, which is when the system learns what it handles.
A session file saved by alpha 2 opens here; one saved by this version does not open in alpha 2.

Windows and Linux are supported by the code but not built here — the native decoder is resolved for
the host platform, so build on the target machine with `./gradlew :app:packageMsi` or
`./gradlew :app:packageDeb` (JDK 21).

The disk image carries a `Licenses` folder next to the application with the Apache 2.0 text, the LGPL
and GPL texts the bundled FFmpeg refers to, and the notice naming every bundled component. The same
files are readable from **About MJLogs** inside the app.

## Known limitations

- A merge is offered only between files read under the same format and the same zone, and cannot be
  undone other than by closing the merged file and opening its parts again.
- A header is recognized as the text before the first record; a date is read from it only when written
  year first (`2026-08-01`, `2026/08/01`) or day first with dots (`01.08.2026`) — `03/04/2026` means
  two different days on two sides of the Atlantic and is left alone.
- A file whose lines carry their own offset is shown in the offset of its first record, even if later
  lines switch.
- The automatic clock reader needs a clock that is legible and that changes minute somewhere in the
  recording. When it finds neither, it says so and leaves the metadata anchor in place.
- The time picker reaches minutes (all Material 3 offers); seconds stay typed.
- One session is remembered as "last"; opening another replaces it. Anything you want to keep, save
  to a file.
- The Apple Silicon build is the only published artifact.

## Under the hood

Kotlin 2.3.21, Compose Multiplatform 1.11.1, Gradle 9.7, Room 2.8.4 with a bundled SQLite 2.7.0,
kotlinx.serialization 1.9.0 for reading JSON records, FFmpeg 8.0.1 and Tesseract 5.5.2 through
JavaCPP Presets 1.5.13. The store's schema moved from version 4 to 6 — a zone per format, and a table
of merged parts — and both the application store and saved session files are migrated in place.
Clean architecture across `:domain`, `:data` and `:app` with a compile-time DI graph; the visual rules
are written down rather than implied, and screens are rendered and inspected as part of building them.
A suite of 769 tests reaches from the parsers to rendered UI, with Detekt on every build.

Apache 2.0; the bundled FFmpeg binaries are LGPL v3 and dynamically loaded, details in
[THIRD-PARTY.md](THIRD-PARTY.md).
