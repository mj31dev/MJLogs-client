---
name: detekt-formatting
description: Execution, auto-correction, rule configuration, and git hook verification for Detekt static code analysis.
---

# Detekt & Auto-Formatting Skill

## Overview
This skill details how to maintain code quality, run auto-formatting, resolve Detekt violations, and enforce git hooks in the project.

## Workflow Instructions
1. **Auto-Formatting Modified Files**:
   - Run `./gradlew detektFormat` to automatically correct formatting violations on modified Kotlin source files.
2. **Git Pre-Commit Hook Automation**:
   - The `.githooks/pre-commit` script automatically filters staged `.kt` and `.kts` files (`git diff --cached`).
   - If modified Kotlin files exist, it executes `./gradlew detektFormat`, automatically re-stages formatted files, and runs `./gradlew detekt` validation.
   - If no Kotlin files were modified, it skips execution immediately.
3. **Static Analysis Check**:
   - Run `./gradlew detekt` to verify zero violations remain across codebases.
4. **Rule Maintenance**:
   - Modify `detekt.yml` to customize formatting rules, line length limits (max 140), and complexity thresholds.

## Language Rule
- Maintain all Detekt configuration files and skill docs in English.
- Chat responses match the user's prompt language.
