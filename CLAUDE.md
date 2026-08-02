# Claude Code Workspace Configuration & Master Prompt

Welcome to the `MJLogs` Kotlin Multiplatform (KMP) Desktop codebase (repository `MJLogs-client`).

## Language & Communication Policy
- **Chat Responses**: Always respond to the user in the chat in the language they used to communicate (e.g. Russian if the user speaks Russian, English if they speak English).
- **Repository Files**: ALL files, code, comments, KMP sources, documentation, git commit messages, subagent configurations, skills, rules, and commands MUST be written strictly in **English**.

## Isolated macOS Seatbelt Sandbox (Zero Permission Prompts)
To execute Claude Code CLI in an isolated native macOS sandbox with **100% full project access + internet access** while completely isolating the rest of the host system (blocking access to `~/.ssh`, host OS, and other directories), run:
```bash
./scripts/run-sandbox.sh
```
- **Capabilities & Host Isolation**:
  - `Project Access`: Full Read/Write access strictly inside `/Users/mj/Desktop/logger/client`.
  - `Network Access`: Standard outbound internet access enabled.
  - `Host Isolation`: `~/.ssh`, `~/.aws`, system files, and other Desktop folders are completely invisible and blocked by macOS kernel.
  - `Offline mode`: Run `./scripts/run-sandbox.sh --offline` to block 100% of network traffic.
- **Launcher Methods**:
  1. Terminal: `./scripts/run-sandbox.sh`
  2. Android Studio UI: Select `Run Claude Sandbox` configuration and click **Play ▶️**.
  3. macOS Finder: Double-click `scripts/run-sandbox.command`.
  4. Claude Desktop GUI App: Run `./scripts/setup-claude-desktop-mcp.sh` to link the sandbox via MCP protocol (`claude_desktop_config.json`).

## Multi-Module Architecture & Tech Stack
- **Target Platform**: Desktop (JVM / Compose Multiplatform Desktop for macOS, Windows, Linux).
- **Gradle Submodules**:
  - `:domain` (`dev.mj31.logger.client.domain`): Pure Kotlin models and ports (interfaces) only — no use cases, no implementations.
  - `:data` (`dev.mj31.logger.client.data`): Data sources, repository implementations, Ktor/socket logic. Depends on `:domain`.
  - `:app` (`dev.mj31.logger.client.app`): Use cases (`app/usecase`), MVI store, Compose Multiplatform Desktop UI screens & window application for macOS, Windows, and Linux, plus the string resources. Depends on `:domain` and `:data`.
- **Language**: Kotlin 2.0+.
- **Mandatory Named Arguments**: All function, constructor, and Composable invocations with 2 or more arguments MUST explicitly name their parameters (e.g. `LogEntry(id = "1", tag = "Network", message = "Connected")`).
- **UI Framework**: Compose Multiplatform (Desktop / Skiko / Material3).
- **Architecture Pattern**: Clean Architecture + MVI (Model-View-Intent) Unidirectional Data Flow.
- **Testing & Assertions**: Google Truth (`com.google.truth:truth:1.4.4`) fluent assertions (`assertThat(actual).isEqualTo(expected)`).
- **Static Analysis & Formatting**: Standard Detekt 2.0 (`dev.detekt:2.0.0-alpha.5`) + standard `detekt-formatting` ruleset (`ArgumentListWrapping`, `ParameterListWrapping`, `TrailingCommaOnCallSite`).
- **Git Pre-commit Hook**: Automatically executes Detekt auto-formatting (`./gradlew detektFormat detekt`) on modified staged files before committing.

## Key Gradle Commands
- `gradlew :app:desktopRun` : Build and launch Compose Desktop UI application (macOS, Windows, Linux).
- `gradlew :app:dmg` : Build the macOS `MJLogs` `.dmg` installer named after the full product version (`app/build/distributions/`).
- `gradlew :app:packageDmg` : Underlying Compose task; leaves the installer in `build/compose/binaries/main/dmg/`.
- `gradlew detektFormat` : Run Detekt 2.0 auto-formatting across all subprojects.
- `gradlew detekt` : Perform static analysis code quality checks via Detekt 2.0 (also runs `verifySourceLayout`).
- `gradlew verifySourceLayout` : Fail if any source directory holds more than 5 Kotlin files.
- `gradlew test` : Run multiplatform unit tests using Google Truth assertions across all subprojects (`:domain`, `:data`, `:app`).

## Subagent Routing Matrix
When handling specialized sub-tasks, activate or delegate to the dedicated Claude Code subagents located in `.claude/agents/`:
1. **Architect** (`.claude/agents/architect.md`, `model: opus`): Domain modeling, multi-module boundaries (`:domain`, `:data`, `:app`), API design.
2. **UI/UX Designer** (`.claude/agents/ui-ux-designer.md`, `model: sonnet`): Compose Desktop UI design, aesthetics, dark modes, animations.
3. **UI Verifier** (`.claude/agents/ui-verifier.md`, `model: sonnet`): UI visual verification, component layout inspection, rendering checks.
4. **Test Engineer** (`.claude/agents/test-engineer.md`, `model: sonnet`): TDD, unit test creation with Google Truth, coroutine test suits.
5. **Code Reviewer** (`.claude/agents/code-reviewer.md`, `model: sonnet`): Code quality audits, Detekt enforcement, boundary reviews.
6. **Sec & Perf Expert** (`.claude/agents/sec-perf-expert.md`, `model: sonnet`): Memory usage, coroutine thread safety, desktop OS integration security.
7. **DevOps & Release Agent** (`.claude/agents/devops-release.md`, `model: haiku`): Git hook maintenance, Gradle task optimization, Detekt rule setup.

## Installed Skill Modules (`.claude/skills/`)
- `compose-desktop-ui`: Compose Desktop UI best practices & code playbooks.
- `detekt-formatting`: Detekt auto-formatting & SARIF report fix automation.
- `kmp-desktop-architecture`: Layer boundaries, coroutine flows & clean architecture.
- `kmp-data-persistence-ktor`: Ktor network streaming, Room/SQLDelight, memory buffering.
- `desktop-os-integration`: System tray, native file pickers, window state restoration.
- `ui-inspection-verification`: Visual layout debugging & contrast checking.
- `test-automation-tdd`: TDD workflows & coroutine test dispatchers.

## Modular Rules (`.claude/rules/`)
- `code-style.md`: Kotlin conventions, mandatory named arguments & Detekt formatting.
- `architecture-boundaries.md`: Package rules for `:domain`, `:data`, `:app`.
- `mvi-unidirectional-data-flow.md`: MVI state management pattern.
- `ui-guidelines.md`: Design system, dark mode & Compose Desktop rules.
- `testing-rules.md`: Testing guidelines, Google Truth assertion standards & test location rules.
- `git-workflow.md`: Commit rules & pre-commit hook enforcement.

## Source Layout Rule
- **One class/interface/object/enum per file**, named after the declaration. Exceptions: private helpers inside implementation files and inside tests; Composables stay grouped by screen or component.
- At most **5 Kotlin files per directory**. A larger package must be split into meaningful sub-packages (by pipeline stage, by workflow, by feature area), never by arbitrary alphabetical chunks.
- Enforced by the Gradle task `verifySourceLayout`, wired into `gradlew detekt`.

## Mandatory Workflow & Quality Gates
1. **Auto-Format via Settings (`.claude/settings.json`)**: Detekt auto-formatting (`./gradlew detektFormat`) is configured in `.claude/settings.json` to automatically trigger post-edit on all `*.kt` and `*.kts` files across all modules.
2. **Git Pre-commit Automation**: The Git pre-commit hook (`.githooks/pre-commit`) automatically detects modified staged Kotlin files, applies Detekt auto-formatting, re-stages the formatted files, and verifies static analysis checks.
3. **Zero Detekt Warnings**: No code changes may introduce new Detekt violations.
4. **Testing Gate**: Always run `./gradlew test` to ensure zero test regressions.
