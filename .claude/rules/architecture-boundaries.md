---
description: Multi-module Clean Architecture layer boundaries and package dependency rules (:domain, :data, :app).
globs: "**/*.kt, **/*.kts"
---

# Rule: Architecture Boundaries & Modularization

1. **Gradle Module Boundaries**:
   - `:domain` (`dev.mj31.logger.client.domain`): Models and ports ONLY (repository, parser, compiler, player and file source interfaces). No use cases, no implementations, zero dependencies on UI or Data.
   - `:data` (`dev.mj31.logger.client.data`): Repositories implementations, network streams, database DAOs. Depends on `:domain`.
   - `:app` (`dev.mj31.logger.client.app`): Use cases (`app/usecase`), MVI store, Compose Desktop UI screens, string resources, design tokens, main entry point. Depends on `:domain` and `:data`.

2. **Dependency Direction**:
   - `:app` -> `:domain`, `:data`
   - `:data` -> `:domain`
   - `:domain` has ZERO outgoing module dependencies.

3. **Language Policy**:
   - All code, build scripts, comments, and architecture rules MUST be in English.
   - Chat responses match the user's prompt language.
