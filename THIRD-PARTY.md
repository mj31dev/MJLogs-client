# Third-party components

`MJLogs` itself is distributed under the Apache License 2.0 (see [LICENSE](LICENSE)).
It bundles the components below; their licenses are listed as declared by the artifacts themselves.

## Runtime dependencies

| Component | License | Notes |
|---|---|---|
| Kotlin standard library, `kotlinx.coroutines`, `kotlinx.datetime` | Apache-2.0 | |
| Compose Multiplatform, Skiko, AndroidX Lifecycle | Apache-2.0 | Skiko embeds Skia (BSD-3-Clause) |
| kotlin-inject | Apache-2.0 | |
| JavaCV, JavaCPP | Apache-2.0 | Triple licensed (Apache-2.0 / GPLv2 / GPLv2+CPE); Apache-2.0 is the option taken here |
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
  to its text.
- **Third-party notice and written offer of source.** `Contents/app/resources/THIRD-PARTY.txt`,
  naming the exact version (FFmpeg 8.0.1 as packaged by JavaCPP Presets 1.5.13) and where to get its
  corresponding source.

One thing to keep in mind for later: the bundle currently carries only an ad-hoc signature. Signing
it with a Developer ID and notarising it would not change the licence position — LGPL asks for a
shared-library mechanism, which is what is used — but it would make replacing the library break the
signature, so the replacement path should stay documented if that step is ever taken.

No FFmpeg component under GPL (such as `libx264` or `libx265`) is included, so nothing in this
project is subject to the GPL.

## Not bundled

`vlcj` (GPL v3) was used by an earlier revision and has been removed: it required VLC to be installed
and would have forced the whole application under the GPL.
