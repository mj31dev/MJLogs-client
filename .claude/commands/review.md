---
description: Audit the current changes against Detekt and the project's layer, MVI and resource rules.
argument-hint: "[optional module or path]"
---

# /review

Audit `$ARGUMENTS` if given, otherwise everything `git status` reports as changed.

1. Run `./gradlew detekt`. It reformats what its formatter can fix and fails on the rest, so its
   output is the list of violations that need a human decision. `verifySourceLayout` runs with it
   and fails when a directory holds more than 5 Kotlin files.
2. Delegate the reading to the `code-reviewer` agent, which is read-only and knows what Detekt
   cannot see: layer leaks into `:domain`, business logic stranded in the store instead of a use
   case, hardcoded user-visible strings that should be `UiText` plus an entry in `strings.xml`,
   a transient notification kept in `LogPlayerState` instead of `LogPlayerEffect`, derived state
   computed inside a composable, missing tests.
3. Report findings ordered by severity with `file:line`, separating what must be fixed from what is
   worth considering. Apply fixes only if the user asks — the agent never edits.

If Detekt passes and nothing turns up, say so rather than manufacturing a finding.
