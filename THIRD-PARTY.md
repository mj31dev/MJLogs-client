# Third-party components

`MJLogs` itself is distributed under the Apache License 2.0 (see [LICENSE](LICENSE)).
It bundles the components below; their licenses are listed as declared by the artifacts themselves.

## Runtime dependencies

| Component | License | Notes |
|---|---|---|
| Kotlin standard library, `kotlinx.coroutines`, `kotlinx.datetime` | Apache-2.0 | |
| Compose Multiplatform, Skiko, AndroidX Lifecycle | Apache-2.0 | Skiko embeds Skia (BSD-3-Clause) |
| AndroidX support libraries pulled in with the above (`annotation`, `arch.core`, `collection`, `savedstate`, `navigationevent`) | Apache-2.0 | Transitive; nobody chose them, they still ship |
| JSpecify | Apache-2.0 | Nullness annotations, transitive |
| kotlin-inject | Apache-2.0 | |
| AndroidX Room (`androidx.room`) | Apache-2.0 | Stores the workspace and the saved sessions |
| AndroidX SQLite with the bundled driver (`androidx.sqlite:sqlite-bundled`) | Apache-2.0 | Carries its own SQLite build, so no system library is needed |
| SQLite | Public Domain | The engine inside that driver; its authors dedicated it to the public domain |
| JavaCV, JavaCPP | Apache-2.0 | Triple licensed (Apache-2.0 / GPLv2 / GPLv2+CPE); Apache-2.0 is the option taken here |
| Tesseract OCR 5.5.2 (`org.bytedeco:tesseract`) | Apache-2.0 | Reads the clock a screencast displays, entirely on the machine |
| `eng.traineddata` (from `tessdata_best`) | Apache-2.0 | The recognition model, bundled so the feature needs no network |
| Leptonica 1.87.0 (`org.bytedeco:leptonica`) | BSD-2-Clause | Tesseract's image layer; pulled in with it |
| **FFmpeg** native libraries (`org.bytedeco:ffmpeg`) | **LGPL-3.0-or-later** | See below |
| Bundled OpenJDK runtime (added by `jpackage`) | GPL-2.0 with Classpath Exception | The exception explicitly permits bundling with an application under any license |

## FFmpeg

The binaries shipped inside the application are the ones published by the
[JavaCPP Presets](https://github.com/bytedeco/javacpp-presets) project. Their build is configured
with `--enable-version3` and **without** `--enable-gpl` or `--enable-nonfree`, which makes them
**LGPL v3 or later**; this was verified against the configuration string embedded in every bundled
library (`libavcodec`, `libavformat`, `libavutil`, `libavfilter`, `libswscale`, `libswresample`).

Obligations honoured here, each verified against the artifact that actually ships:

- **Dynamic linking.** `libavcodec.62.dylib` and its siblings are `Mach-O dynamically linked shared
  library` files inside an ordinary zip (`Contents/app/ffmpeg-…-macosx-arm64.jar`), loaded at runtime.
  Nothing is linked statically, so the user can substitute another build of the same version.
- **Licence text travels with the binaries.** `Contents/app/resources/LGPL-3.0.txt`, plus
  `GPL-3.0.txt`, because the LGPL is written as additional permissions on top of the GPL and refers
  to its text, and `LICENSE.txt` for the project itself.
- **Third-party notice and written offer of source.** `Contents/app/resources/THIRD-PARTY.txt`,
  naming the exact version (FFmpeg 8.0.1 as packaged by JavaCPP Presets 1.5.13) and where to get its
  corresponding source.
- **The notice can actually be read.** macOS shows an application bundle as one opaque file, so a
  text that only lives inside it is not notice to anyone. The same four files therefore also sit in
  a `Licenses` folder in the root of the disk image (`--mac-dmg-content`), next to the application,
  and the application itself displays them under **About MJLogs** — in the application menu on
  macOS, in the **Help** menu on Windows and Linux, which have no application menu — where the
  summary of what is bundled leads to the full texts. The MSI and DEB installers additionally carry
  `LICENSE.txt` as their installer licence.

`app/appResources/common/` is the single source for all of this: `app/build.gradle.kts` points
`appResourcesRootDir` at it, stages it into the disk image and the app reads it back at runtime, and
`LegalNoticeAssetsTest` fails the build if a file goes missing, if the shipped `LICENSE.txt` drifts
from the repository `LICENSE`, or if the FFmpeg version in the version catalog stops matching the one
the written offer names.

One thing to keep in mind for later: the bundle currently carries only an ad-hoc signature. Signing
it with a Developer ID and notarising it would not change the licence position — LGPL asks for a
shared-library mechanism, which is what is used — but it would make replacing the library break the
signature, so the replacement path should stay documented if that step is ever taken.

No FFmpeg component under GPL (such as `libx264` or `libx265`) is included, so nothing in this
project is subject to the GPL.

## Not bundled

`vlcj` (GPL v3) was used by an earlier revision and has been removed: it required VLC to be installed
and would have forced the whole application under the GPL.
