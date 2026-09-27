---
name: design-system
description: The visual rules of the MJLogs client — spacing, shape, type, surfaces, colour, menus, glyphs, action hierarchy, density and motion. Use when adding or changing any screen, pane, dialog or theme value.
---

# The MJLogs design system

Two things share the window: a screencast that moves and a log that scrolls. Everything below follows
from that. The interface is the frame around two pieces of content, and a frame that competes with
what it holds is a broken frame.

Tokens live in `app/theme/` and its sub-packages `shape/`, `type/` and `video/`. Never write a raw
`dp` for spacing, a `RoundedCornerShape`, a `fontSize` or a raw `Color` in a screen — if the value
you need is not in the theme, the value is wrong or the theme is missing it.

The size of an element — the width of a column, the height of a marker, the 1dp of a border — is
not spacing, and is a named constant beside the composable that owns it.

## Spacing

A 4dp base. Only these steps exist:

| Token       | Value | Where |
| ----------- | ----- | ----- |
| `Spacing.hairline` | 2dp  | between a label and the value it names |
| `Spacing.tight`    | 4dp  | inside a chip, between an icon and its text |
| `Spacing.small`    | 8dp  | between sibling controls in a row |
| `Spacing.medium`   | 12dp | padding inside a card or a row of a list |
| `Spacing.large`    | 16dp | padding of a pane, gap between cards |
| `Spacing.xlarge`   | 24dp | gap between groups that are not related |
| `Spacing.section`  | 32dp | above a section heading that starts a new subject |

Anything between two steps is a step. A 10dp gap is 8 or 12, decided by which of its neighbours it
belongs to more.

## Shape

Corners come from `MaterialTheme.shapes`, set in `theme/shape/AppShapes.kt`. The role is picked by
what the shape holds, never by how round it should look:

| Role | Radius | Holds |
| ---- | ------ | ----- |
| `extraSmall` | 4dp  | a token inside a row: level chip, time window chip, marker, swatch, menu |
| `small`      | 6dp  | a control or an inset block: file chip, field, a block of file text, a list inside a dialog |
| `medium`     | 10dp | a card; the frame of the video |
| `large`      | 16dp | a card that floats over the workspace without spanning it (none yet) |
| `extraLarge` | 28dp | a dialog (Material's own default) |

A shape nested inside another is never rounder than the one around it. A strip that spans the window
edge to edge — the notice, the save bar — has no corners at all; it is set apart from what it floats
over by its surface colour and a 1dp `outline`, never by a shadow.

## Type

Material 3 roles, used for exactly one job each:

- `headlineSmall` — the name of a screen. At most one per view.
- `titleMedium` — the name of a pane or a section.
- `bodyLarge` — the identity of a row: a session name, a file name.
- `bodyMedium` — running prose. Empty states, explanations.
- `bodySmall` — metadata attached to something above it: a path, a timestamp, a count.
- `labelLarge` — the label of a group of rows.
- `labelMedium` — a badge or a state word.
- `labelSmall` — the second line of a chip: counts, format, zone. A chip's first line is `bodySmall`;
  a chip of a single word or letter (a level, a time window) is `labelMedium`.

**Monospace means "compared column by column", and comes in exactly two styles** from
`theme/type/ContentType.kt`:

- `ContentType.record` — text as a file wrote it: a log record, a file's header, a sample line in
  the format dialog. One size everywhere, so a line reads the same wherever it is shown.
- `ContentType.figures` — figures read against a record: a timecode, a clock time, a correlation
  such as `video 0:12.4 = log 10:00:05.120`. Sized as `bodySmall`, because it is metadata.

Nothing else is monospaced — not a file name, not a path, not a zone name. A screen never sets
`fontSize` or `fontFamily` itself; it picks a role or one of these two styles.

Never signal importance with size alone. A row's name is `bodyLarge` on the default `onSurface`; its
metadata is `bodySmall` on `onSurfaceVariant`. That pair, repeated, is the whole hierarchy of a list.

## Surfaces

Three levels, and no shadows anywhere:

- `background` — the window. Nothing sits directly on it except panes.
- `surface` — a pane, a dialog, a card. The ordinary plane of the interface.
- `surfaceVariant` — something inset *inside* a surface: a selected row, a field, a chip.

Depth is a change of surface colour, never an elevation shadow. A shadow over a video frame reads as
a rendering artefact, and a log list of a thousand rows with shadowed rows is a grey mess.

Borders come from `outline` and are 1dp. Use one only where two surfaces of the same level meet.

Material carries further colour families that components reach for **without being asked**, and any
member left undefined falls back to a framework tone that belongs to no scheme here. That is how
every dialog in this application once came to be painted off-palette, and how a segmented button
nearly was.

- `surfaceContainerLowest` … `surfaceContainerHighest` — five steps, mapped in `Color.kt` onto the
  three levels above. A dialog takes `surfaceContainerHigh`, which is why the rule that a dialog is a
  `surface` is now true rather than merely stated. Never set `containerColor` on a dialog to work
  around it.
- `primaryContainer`, `secondaryContainer` — mapped onto `surfaceVariant`. Segmented buttons, chips
  and filled tonal buttons take these.
- `tertiaryContainer` — **still undefined**. Any component reaching for it will be off-palette;
  define it before using one, rather than overriding the colour at the call site.

The rule behind all of these: when a component picks its own colour, the scheme must already have an
answer. Reaching into a screen to correct it afterwards only hides the hole.

## Colour

The scheme is defined twice, in `LightColors` and `DarkColors`, and screens read it only through
`MaterialTheme.colorScheme` and `LocalLogLevelColors`. A screen that names a colour constant is a
screen that will be wrong in one of the two themes.

**Log level colours are part of the theme.** They are tuned per scheme: the greens and ambers that
read on `#0B1120` fail against white, so `LocalLogLevelColors` provides a set for each. Anything
colour-coding a level asks the theme, never a top-level `val`.

**Colour is never the only carrier of meaning.** A level is a colour *and* a letter; an error notice
is a colour *and* the word.

**Nothing is painted in "no colour".** A surface that is sometimes filled and sometimes not applies
its `background` only when filled; `Color.Transparent` never appears in a screen.

**The video frame has colours of its own.** It is the same picture in both schemes, so what is drawn
over or around it cannot follow the scheme: `VideoColors.letterbox`, `VideoColors.scrim` and
`VideoColors.onScrim` in `theme/video/`. They are the only colours a screen may take from outside
`MaterialTheme.colorScheme`, and only on or around the frame.

## Menus

There is one menu: `AppDropdownMenu` in `app/view/menu/`. It is a `surface` with a 1dp `outline`,
`extraSmall` corners and no elevation — Material's `DropdownMenu` casts a shadow by default, and a
menu opens over the log list and near the frame. Screens never call `DropdownMenu` directly.

A menu item is a verb or a noun ending in `…` when it opens a further question ("Time zone…"). An
item that does not apply to the thing the menu belongs to is absent, not disabled — unless its being
unavailable is itself news the user needs, in which case it is disabled and its reason is visible
elsewhere on screen.

## Glyphs

There is no icon library. Actions are words; the interface is a frame around text and video, and a
row of pictograms is one more thing to decode beside a log. A glyph is used only where a word would
not fit and the glyph is a convention every desktop user reads without thinking. The whole set:

| Glyph | Resource | Meaning |
| ----- | -------- | ------- |
| `⋮` | `source_chip_menu_glyph` | more actions for this item |
| `•` | `session_unsaved_marker` | unsaved changes |
| `…` | inside labels | this opens a further question |

A glyph is a string resource like any other text, drawn in the colour of the text beside it, and a
glyph that stands alone carries a `contentDescription` in words. Adding one to this table is a design
decision, not an implementation one.

## Action hierarchy

Per view: **one** filled `Button` — the thing the view exists for. `New session` on the start
screen; `Synchronize` on the sync bar.

Everything else is an `OutlinedButton` (a real alternative) or a `TextButton` (housekeeping:
removing an entry, dismissing a notice, cancelling). A destructive action is never the loudest thing
on the row it belongs to.

**A question with more than one answer** keeps the same weights in one row. The recommended answer
is filled and rightmost; the others stand to its left from least to most committed. For three —
merge, open separately, skip:

```
[ Skip ]        [ Open separately ]   [ Merge ]
TextButton      OutlinedButton        Button
```

The filled button is the answer recommended for *this* case, and it is also the safe one when the
case has a safe one: for an exact copy the recommendation is to skip, and skip is then the filled
button. Closing the dialog is the least committed answer, never the recommended one. `AlertDialog`
has two slots; the recommended answer takes `confirmButton`, the other two share `dismissButton` in
a `Row` spaced `Spacing.small`. Four answers is not a dialog but a design problem.

## Density

Two densities, chosen by what the surface holds:

- **Content** — log records, the frame. `Spacing.tight` vertical, no decoration between rows beyond
  a background change on selection. As many records on screen as will fit legibly.
- **Chrome** — everything else. `Spacing.medium` inside rows, `Spacing.large` around panes.

The start screen is chrome throughout: it is opened once per launch and holds a handful of items, so
it can afford to be comfortable. The log pane never can.

## Motion

Motion exists to explain a change that would otherwise be a jump. It is not decoration, and this
application has two hard limits.

**Never animate content.** Log rows do not fade, slide, or reorder with animation. The video frame is
never cross-faded. Content changes instantly, always — a list of ten thousand rows that animates is
a list that stutters, and a frame that fades is a frame you cannot trust as evidence.

**Never animate what the user is aiming at.** A control does not move under the pointer.

What may animate, with `MotionTokens`:

| Case | Duration | Curve |
| ---- | -------- | ----- |
| Something floats in or out over the workspace — a notice, the save bar | `MotionTokens.enter` 180ms / `exit` 120ms | `FastOutSlowIn` |
| A value the eye should follow — a progress bar, the playhead | `MotionTokens.value` 100ms | `LinearEasing` |
| A surface changes state — hover, selection | `MotionTokens.state` 80ms | `LinearEasing` |

Leaving is faster than arriving: an element on its way out has already stopped being interesting.

Anything not in that table does not animate. If a new case seems to need motion, it is a design
question, not an implementation one — say so rather than picking a duration.

## The check that is not optional

Render the screen and look at it. Assertions confirm what you suspected; they do not tell you that a
screen never painted its background, or that a timestamp carries a millisecond tail that means
nothing. Both of those shipped here, past a green suite, and both were obvious in a PNG.

See `compose-ui-testing` for how to capture one — and delete the harness afterwards.
