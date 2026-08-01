---
description: Git pre-commit hook enforcement, commit message formatting, and Detekt auto-correct rules.
globs: ".githooks/*, scripts/*"
---

# Rule: Git Workflow & Hook Enforcement

1. **Pre-commit Hooks**:
   - Always initialize hooks via `./scripts/setup-hooks.sh`.
   - Never bypass git pre-commit hooks (`--no-verify`).
2. **Commit Hygiene**:
   - Format: `<type>(<scope>): <short description>`.
   - Examples: `feat(ui): add search filter bar to log viewer`, `fix(detekt): format import ordering`.
3. **Language Policy**:
   - Commit messages, pull requests, and git hook scripts MUST be in English.
