---
description: Run Detekt auto-formatting across all Kotlin source files and stage changes.
---

# Slash Command: /format

Run Detekt auto-formatting on all Kotlin source files across `src/`.

## Action Steps
1. Execute `./gradlew detektFormat`.
2. Stage formatted files using `git add -u`.
3. Run `./gradlew detekt` to confirm zero remaining violations.
