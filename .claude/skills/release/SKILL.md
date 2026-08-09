---
name: release
description: Prepares a release of the MJLogs client — the gate, the legal checks, version consistency, the artifact, then rewrites RELEASE-NOTES.md. Use when cutting a version or asked to prepare release notes.
---

# Preparing a release

Seven steps, in order. The notes are written **last**, because most of what they have to say is what
the checks turn up.

Run every Gradle command **bare** — no `cd`, no `export`, no pipe feeding into it. The sandbox lets
`./gradlew …` out to the network and blocks the same command wrapped in a shell script, which looks
exactly like a broken build.

## 1. Know what is being released

```bash
git describe --tags --abbrev=0
```

That tag is the previous release. Everything since it is this one:

```bash
git log <previous-tag>..HEAD --oneline
git diff --stat <previous-tag>
git status --porcelain
```

Uncommitted work counts. A release prepared from `git log` alone will miss whatever is still in the
working tree, which on this project is routinely most of it.

**Read the previous `RELEASE-NOTES.md` before writing anything**, and specifically its *Known
limitations*. That list is the honest spine of the next release: the items it no longer contains are
the story. Notes that quietly drop a limitation without saying it was fixed waste the one section a
reader trusts.

## 2. The gate

```bash
./gradlew test detekt --console=plain
```

Both, green, no exceptions. `detekt` carries `verifySourceLayout`, so a package that overflowed five
files fails here rather than in review.

If the notes are going to quote a test count, derive it — do not carry the previous number forward:

```bash
grep -h -o 'tests="[0-9]*"' */build/test-results/desktopTest/*.xml | sed 's/[^0-9]//g' | awk '{s+=$1} END {print s}'
```

## 3. Legal — the part that is load-bearing

This project ships FFmpeg under the LGPL and a bundled OpenJDK under the GPL with Classpath
Exception. The notices are not paperwork; they are a condition of distributing the binary.

**Every shipped dependency group must appear in the notices.** Adding a library and forgetting this
has already happened here — Room and SQLite went in and the notices did not move.

```bash
./gradlew :app:dependencies --configuration desktopRuntimeClasspath --console=plain -q
```

Take the groups out of that tree and check each against both files. They are two different documents
for two different readers and both have to be right:

- `THIRD-PARTY.md` — the table in the repository;
- `app/appResources/common/THIRD-PARTY.txt` — the copy that travels **inside** the artifact and is
  read by *About MJLogs*.

A transitive group nobody chose still ships, and still belongs in the list.

**The four texts have to be present**, because packaging stages `*.txt` from that folder into the
disk image's `Licenses` folder:

```bash
ls app/appResources/common/
```

`LICENSE.txt`, `LGPL-3.0.txt`, `GPL-3.0.txt`, `THIRD-PARTY.txt`. The same folder also carries
`tessdata/eng.traineddata`; only the `*.txt` files are staged, so a binary never lands among the
notices. `LegalNoticeAssetsTest` asserts this — if it is green and the folder looks wrong, trust the
folder and fix the test.

**Editing the source is not shipping it.** The notice exists in three places, written by three
different tasks, and a partial build leaves some of them stale:

| Copy | Written by | Read by |
| ---- | ---------- | ------- |
| `app/build/compose/binaries/main/app/MJLogs.app/Contents/app/resources/` | `createDistributable` | *About MJLogs* |
| `app/build/legal/Licenses/` | `stageDmgLegalNotices` | the visible folder in the disk image |
| `app/build/compose/tmp/prepareAppResources/` | staging | neither, but it is what the other two come from |

So verify the built copies, not the file you edited:

```bash
find app/build -name "THIRD-PARTY.txt" -exec grep -c -i <new-component> {} +
```

Every one of them has to answer. This is a real failure, not a hypothetical: the source named Room
while all three shipped copies still did not, and only the `.app` copy updated on the next partial
build.

**FFmpeg must stay dynamically linked and LGPL.** Nothing in the build may add `--enable-gpl` or
`--enable-nonfree`, and the bundled binaries must remain the JavaCPP Presets build the notice names.
Changing the `ffmpeg` version in `gradle/libs.versions.toml` means re-checking the version stated in
`THIRD-PARTY.md` and in `THIRD-PARTY.txt` — both name it explicitly.

## 4. Version consistency

`gradle.properties` holds the single version. Three places have to agree with it:

- the `version=` line itself;
- the heading of `RELEASE-NOTES.md`;
- the tag that will be created.

Installers cannot carry a pre-release qualifier — jpackage takes `MAJOR[.MINOR][.PATCH]` only — so
`installerVersion()` strips it and the `dmg` task restores the full version in the file name. Two
pre-releases of one version would otherwise produce the same artifact name.

## 5. Build the artifact and look at it

```bash
./gradlew :app:dmg --console=plain
```

This is the one step that cannot be run unattended: the disk image is arranged by an AppleScript, and
macOS asks whether the build may control Finder the first time. A refusal fails the task with a
message saying so — it does not quietly ship an unarranged image.

Then check what actually shipped, rather than what the build script intended. Mount the image and
look at it in both system appearances, because the icon captions are drawn by Finder in a colour the
artwork cannot follow:

```bash
open app/build/distributions/MJLogs-<version>.dmg
```

```bash
ls -lh app/build/distributions/
plutil -lint app/build/compose/binaries/main/app/MJLogs.app/Contents/Info.plist
plutil -extract CFBundleDocumentTypes xml1 -o - app/build/compose/binaries/main/app/MJLogs.app/Contents/Info.plist
```

The document type check is there because jpackage silently ignores `--file-associations` when it
builds an app image: the option was accepted, nothing was written, and the association was declared
working for a whole release. The plist is the only witness.

Take the real size from `ls -lh` if the notes quote one.

## 6. Rewrite the notes

`RELEASE-NOTES.md` is the **body of one release**, not a cumulative changelog — it is replaced, not
appended to. Match the previous one's shape:

- a title that says what changed in one line, not a version number alone;
- an opening paragraph that names the previous release and what it could not do;
- one section per thing a user would notice, written as what it does for them and why it is built
  that way — not as a list of merged branches;
- **Download**, carrying forward whatever is still true: the notarization workaround, which platforms
  are actually published, where the licence texts are;
- **Known limitations**, re-derived from scratch. Everything scoped and deliberately deferred belongs
  here, said plainly;
- **Under the hood**, with the toolchain versions read out of `gradle/libs.versions.toml`.

Everything committed here is written in English, release notes included.

## 7. What only the person can do

Say these out loud rather than implying them:

- **the tag and the push** — this repository denies both to the assistant;
- **notarization**, which needs an Apple account;
- **double-clicking a `.mjclog`** from the Finder, which needs the app installed in `/Applications`
  and launched once before LaunchServices knows anything about it;
- **Windows and Linux installers**, which only build on their own host because the native decoder is
  resolved for the host platform.

Never claim any of these were verified.
