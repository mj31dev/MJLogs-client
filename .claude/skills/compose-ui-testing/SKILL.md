---
name: compose-ui-testing
description: Writing and running Compose Desktop UI tests for MJLogs with runComposeUiTest, including how to capture a screen as a PNG to look at it. Use when adding or changing a composable, or when a screen needs visual verification.
---

# Compose Desktop UI testing

The suites live in `app/src/desktopTest/kotlin/dev/mj31/logger/client/app/features/logplayer/`
(`LogPaneTest`, `PlayerScreenTest`, `VideoPaneTest`, `SyncBarTest`, and the dialogs under `format/`
and `sync/`). Run them with `./gradlew :app:desktopTest`.

## The shape of a test

```kotlin
@OptIn(ExperimentalTestApi::class)
class LogPaneTest {

    @Test
    fun `renders every visible record and the session counts`() = runComposeUiTest {
        setContent { LogPane(state = loadedState(), onIntent = {}) }

        onNodeWithText(text = "connected to server").assertIsDisplayed()
        onNodeWithText(text = "2 records").assertIsDisplayed()
    }

    @Test
    fun `clicking a record selects it`() {
        val intents = mutableListOf<LogPlayerIntent>()

        runComposeUiTest {
            setContent { LogPane(state = loadedState(), onIntent = { intents += it }) }

            onNodeWithText(text = "write failed").performClick()
        }

        assertThat(intents).containsExactly(LogPlayerIntent.SelectEntry(entryId = "e2"))
    }
}
```

Two things make this work and both are worth copying:

**A composable is tested against state, not against a store.** Every pane takes `state` and
`onIntent`, so a test hands it a fixture and asserts on the intents it emits. Nothing is mocked.

**Fixtures are shared.** `UiTestFixtures.kt` holds `loadedState()`, `longSessionState()`,
`pendingFormatRequest()`, `uiEntries`, `formatSampleLines`. Reuse them; `LogPlayerFixtures` in
`app/src/commonTest/…/app/fake/` builds the entries underneath. Records carry distinct messages on
purpose, so a node can be addressed by its text alone.

Assert with Google Truth. Collect intents into a list and use `containsExactly` — asserting on the
emitted intent proves the wiring, while asserting on redrawn pixels does not.

## Looking at a screen

Assertions confirm what you already suspected; they do not tell you that the spacing is wrong. Two
ways to actually look, neither of which has standing infrastructure in this repository — write them
where you need them and delete them afterwards.

**Rendered offscreen (preferred).** Inside `runComposeUiTest`, capture a node and write the bytes:

```kotlin
val image = onRoot().captureToImage()
val bytes = Image.makeFromBitmap(image.asSkiaBitmap())
    .encodeToData(EncodedImageFormat.PNG)!!
    .bytes
File("build/screenshots/log-pane.png").apply { parentFile.mkdirs() }.writeBytes(bytes)
```

Then read the PNG. This is headless and reproducible, and it needs no macOS screen-recording
permission. What it does not reproduce is the real window: OS font rendering, DPI scaling, the
native menu bar, the video surface.

**The live window.** `./gradlew :app:desktopRun`, then capture the window with `screencapture` on
macOS. Use this only for what the offscreen render genuinely cannot show — the system tray, native
file dialogs, actual video playback. It needs screen-recording permission and it is not
reproducible, so never make it a gate.

## Rules that apply here too

Named arguments for every call with two or more parameters, tests included. One declaration per
file, at most 5 Kotlin files per directory — the logplayer test directories are close to the limit,
so a new suite may mean a new sub-package. Test names are backticked sentences. All English.
