---
description: Verify the Compose Desktop screens by running the UI test suites and, when needed, rendering a screen to a PNG to look at it.
argument-hint: "[optional screen or composable name]"
---

# /verify-ui

Verify `$ARGUMENTS` if given, otherwise every screen with a suite.

1. Run `./gradlew :app:desktopTest`. The Compose UI suites live in
   `app/src/desktopTest/kotlin/dev/mj31/logger/client/app/features/logplayer/` — `LogPaneTest`,
   `PlayerScreenTest`, `VideoPaneTest`, `SyncBarTest`, plus the dialogs under `format/` and `sync/`.
   Report failures with the assertion that broke.
2. Check coverage: name any composable in `app/features/logplayer/screen/` or `app/view/` that has
   no suite, and say what its untested behaviour is.
3. Read the composables for what a test cannot assert — theme tokens from `app/theme` instead of
   literal colours, log-level colour coding, lazy virtualization for the entry list, minimum window
   dimensions, no user-visible string outside `strings.xml`.
4. When the question is visual rather than behavioural, look at the screen. Load the
   `compose-ui-testing` skill, write a throwaway test that captures the node with `captureToImage()`
   and writes a PNG under `build/screenshots/`, read the image, then delete the test. Fall back to
   `./gradlew :app:desktopRun` plus `screencapture` only for what an offscreen render cannot show:
   the system tray, native file dialogs, real video playback.

There is no standing screenshot infrastructure in the repository, and that is deliberate — capture
code is written for the question at hand and removed with it.
