---
description: Perform Compose Desktop UI inspection, visual hierarchy checks, and rendering state validation.
---

# Slash Command: /verify-ui

Run Desktop UI inspection and visual verification workflow using the `ui-verifier` agent persona.

## Action Steps
1. Inspect Compose Desktop composables in `src/commonMain/kotlin/dev/mj31/logger/client/ui/`.
2. Check layout state handling, lazy column virtualization, and theme token usage.
3. Validate dark mode color accessibility and contrast ratios.
