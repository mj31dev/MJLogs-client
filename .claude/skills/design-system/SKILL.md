---
name: design-system
description: The visual rules of the MJLogs client — spacing, type, surfaces, colour, action hierarchy, density and motion. Use when adding or changing any screen, pane, dialog or theme value.
---

# The MJLogs design system

Two things share the window: a screencast that moves and a log that scrolls. Everything below follows
from that. The interface is the frame around two pieces of content, and a frame that competes with
what it holds is a broken frame.

Tokens live in `app/theme/`. Never write a raw `dp` for spacing or a raw `Color` in a screen — if
the value you need is not in the theme, the value is wrong or the theme is missing it.

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

## Type

Material 3 roles, used for exactly one job each:

- `headlineSmall` — the name of a screen. At most one per view.
- `titleMedium` — the name of a pane or a section.
- `bodyLarge` — the identity of a row: a session name, a file name.
- `bodyMedium` — running prose. Empty states, explanations.
- `bodySmall` — metadata attached to something above it: a path, a timestamp, a count.
- `labelLarge` — the label of a group of rows.
- `labelMedium` — a badge or a state word.
- Log records are the exception: `FontFamily.Monospace`, and they are the only monospaced text in
  the application. Column alignment is what makes a log readable; nothing else needs it.

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

## Colour

The scheme is defined twice, in `LightColors` and `DarkColors`, and screens read it only through
`MaterialTheme.colorScheme` and `LocalLogLevelColors`. A screen that names a colour constant is a
screen that will be wrong in one of the two themes.

**Log level colours are part of the theme.** They are tuned per scheme: the greens and ambers that
read on `#0B1120` fail against white, so `LocalLogLevelColors` provides a set for each. Anything
colour-coding a level asks the theme, never a top-level `val`.

**Colour is never the only carrier of meaning.** A level is a colour *and* a letter; an error notice
is a colour *and* the word.

## Action hierarchy

Per view: **one** filled `Button` — the thing the view exists for. `New session` on the start
screen; `Synchronize` on the sync bar.

Everything else is an `OutlinedButton` (a real alternative) or a `TextButton` (housekeeping:
removing an entry, dismissing a notice, cancelling). A destructive action is never the loudest thing
on the row it belongs to.

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
