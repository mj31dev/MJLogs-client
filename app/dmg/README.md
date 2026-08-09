# The macOS disk image

Two files decide what a user sees when they mount `MJLogs-<version>.dmg`: `background@2x.png`, the
artwork behind the icons, and `dmg-setup.scpt`, the AppleScript that arranges the window over it.
They share one grid, and that grid is the whole point of this file.

Finder never scales a background picture. It anchors it at the top left of the content area and
leaves whatever does not fit as empty grey. So the artwork is not decoration laid under an existing
window — it *is* the window, and every icon position is a promise about a region of the drawing.

## The grid

Sizes are in points; the artwork is committed at twice that, so every number below is doubled in the
PNG. Positions are the **centre** of an icon, measured from the top left of the content area.

| | | |
| --- | --- | --- |
| Window content | 660 × 440 | window frame is 476 tall: 440 plus 36 of title bar, measured |
| Icon size | 128 | one value for the whole window — Finder has no per-item size |
| `MJLogs.app` | 185, 158 | |
| `Applications` alias | 475, 158 | |
| `Licenses` | 560, 328 | the notices the LGPL asks to keep in sight |
| dotted names | 68, 250 | only ever seen on a machine that shows hidden items |

A position is the icon, and the caption lands about **80 points below it** — so an icon centre is the
centre of its plate minus 80, not the plate itself. Placing the icons on their plates is what put the
captions a plate's height too low the first time this was built.

The arrow between the first two lives in the corridor `x 265–395, y 130–200`. The wordmark sits in
`x 40–300, y 30–80`, the instruction in `x 40–430, y 355–395`.

## Why there are plates under the captions

Finder draws icon captions in black over a background picture, in both system appearances — that was
read off a screenshot of this image mounted under the dark one. There is no property for the colour:
not in Finder's AppleScript dictionary, not in `.DS_Store`, not in `defaults`. So a dark background
needs somewhere for black text to land, and the artwork carries three soft plates, one under each
caption:

| | centre, points | centre, pixels in the PNG |
| --- | --- | --- |
| `MJLogs.app` | 185, 238 | 370, 476 |
| `Applications` | 475, 238 | 950, 476 |
| `Licenses` | 560, 408 | 1120, 816 |

Each is 160 × 26 points, corner radius 13, roughly `#7C8798` at a little over half opacity, with a
feathered edge so it reads as shading rather than as a button.

## Replacing the artwork

Any size works as long as the proportions are 3:2 — `scripts/make-dmg-background.sh` resamples it to
660 × 440 and 1320 × 880 and merges the pair into one TIFF, and rejects anything that is not 3:2
rather than squashing it and sliding the reserved regions out from under the icons.

The current picture was generated from this prompt, kept here so the next one can match it:

```
Create a macOS disk image (.dmg) background artwork. Output a single PNG,
exactly 1320 x 880 pixels, no transparency, no border, no rounded corners,
no drop shadows around the edges — it will be tiled edge to edge as a Finder
window background.

Style: dark, calm, technical-premium. Think the empty state of a professional
developer tool, not a game splash screen. Flat and restrained, no 3D, no
skeuomorphism, no glossy reflections, no lens flare.

Palette (use these exact values):
- base gradient from #0B1120 (top-left) to #111C31 (bottom-right)
- a soft, wide cyan glow #38BDF8 at about 12% opacity, centred around
  x=330, y=330 (in this 1320x880 canvas), fading out smoothly
- a much fainter indigo #818CF8 wash in the bottom-right quadrant
- hairlines and any texture in #243B63

Composition — these regions MUST stay visually quiet and free of detail,
because macOS draws icons on top of them:
- 242..498 x, 188..444 y  (application icon)
- 822..1078 x, 188..444 y (Applications folder icon)
- 992..1248 x, 528..784 y (a small folder icon, bottom right)

Draw these elements:
1. A thin horizontal arrow pointing right, centred at y=330, spanning
   x=530 to x=790. Line weight about 4px, colour #38BDF8, with a simple
   triangular head. Give it a subtle fade-in from the left, as if drawn
   with a tapering stroke.
2. The word "MJLogs" as a wordmark in the top-left, baseline around y=130,
   starting at x=80. Clean geometric grotesque sans-serif, medium weight,
   colour #F8FAFC, cap height about 44px. Spell it exactly: capital M,
   capital J, capital L, lowercase o, lowercase g, lowercase s.
3. The sentence "Drag MJLogs to Applications" in the lower-left area,
   baseline around y=760, starting at x=80. Same typeface, regular weight,
   cap height about 22px, colour #94A3B8, generous letter spacing.
4. Three horizontal rounded-rectangle plates, each 320 x 52 pixels,
   corner radius 26, filled with #7C8798 at 55% opacity, softly blurred
   edges (about 8px feather), centred at:
   - x=370, y=476
   - x=950, y=476
   - x=1120, y=816
   These sit behind icon captions and must read as soft shading, not as
   hard buttons. No stroke, no text inside them.
5. Optional, very subtle: a faint grid or scanline texture in #243B63 at
   under 6% opacity across the whole canvas, evoking a log viewer.

Do not draw: any application icon, any folder icon, any macOS UI chrome,
any window frame, any additional text, any watermark, any logo other than
the "MJLogs" wordmark described above.

A reference image of the product icon is attached. Use it ONLY as a palette
and mood reference. Do NOT reproduce the icon, its play triangle, its rounded
square tile, or any part of it anywhere in the artwork.

If you want one motif tying the artwork to the product, use a single thin
vertical line in #A78BFA at low opacity somewhere in the left third,
away from the reserved regions listed above.
```

The artwork carries no version number on purpose: it would have to be redrawn for every release, and
the version is already in the file name of the installer.

## Looking at it

Nothing here can be asserted in a test — the result is a picture arranged by Finder. Build the image,
mount it, and look:

```
./gradlew :app:dmg
open app/build/distributions/MJLogs-<version>.dmg
```

Check both system appearances. The captions are the thing that breaks first.
