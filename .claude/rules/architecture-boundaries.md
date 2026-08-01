---
description: Multi-module Clean Architecture layer boundaries and package dependency rules (:domain, :data, :app).
globs: "**/*.kt, **/*.kts"
---

# Rule: Architecture Boundaries & Modularization

1. **Gradle Module Boundaries**:
   - `:domain` (`dev.mj31.logger.client.domain`): Pure business logic, models, use cases, repository interfaces. Zero dependencies on UI or Data.
   - `:data` (`dev.mj31.logger.client.data`): Repositories implementations, network streams, database DAOs. Depends on `:domain`.
   - `:app` (`dev.mj31.logger.client.app`): Compose Desktop UI screens, design tokens, main entry point. Depends on `:domain` and `:data`.

2. **Dependency Direction**:
   - `:app` -> `:domain`, `:data`
   - `:data` -> `:domain`
   - `:domain` has ZERO outgoing module dependencies.

3. **Language Policy**:
   - All code, build scripts, comments, and architecture rules MUST be in English.
   - Chat responses match the user's prompt language.
