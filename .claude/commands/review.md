---
description: Perform a full code review audit against Detekt static analysis and architecture layer rules.
argument-hint: "[optional-module-or-path]"
---

# Slash Command: /review

Perform a full code review audit using the `code-reviewer` agent persona.

## Action Steps
1. Run `./gradlew detekt` to check for formatting and quality violations.
2. Inspect overall project architecture and layer boundaries.
3. Report code smells, unused imports, or missing unit tests.
4. Output review summary strictly in English (and respond in chat in user's prompt language).
