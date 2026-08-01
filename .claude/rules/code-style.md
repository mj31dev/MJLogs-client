---
description: Code style, Kotlin naming conventions, Detekt integration, mandatory named arguments, and strict language policies.
globs: "**/*.kt, **/*.kts"
---

# Rule: Code Style & Detekt Integration

1. **Formatting Enforcement**:
   - All Kotlin files must adhere strictly to Detekt formatting rules (`detekt.yml`).
   - Detekt auto-formatting is automatically applied to modified/staged Kotlin files during Git commit (`.githooks/pre-commit`).
   - Claude Code must automatically execute `./gradlew detektFormat` after modifying any `.kt` or `.kts` files.
2. **Naming Conventions**:
   - Packages: Lowercase dot-separated under `dev.mj31.logger.client`.
   - Classes & Interfaces: PascalCase.
   - Functions & Properties: camelCase.
   - Composable Functions: PascalCase.
3. **Mandatory Named Arguments**:
   - All function calls, constructor invocations, and Composable calls with 2 or more parameters MUST explicitly name their arguments (e.g., `LogEntry(id = "1", tag = "Network", message = "Connected")`).
   - Positional arguments are strictly forbidden for multi-parameter calls. Enforced via Detekt `style > NamedArguments` rule.
4. **Language Policy**:
   - Repository Files: ALL source code, comments, docstrings, build scripts, markdown files, and commit messages MUST be in English.
   - Chat Responses: Respond in the chat using the exact language used by the user in their prompt.
