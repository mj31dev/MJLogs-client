---
description: Compose Desktop UI design system tokens, dark theme palettes, and responsiveness rules.
globs: "src/commonMain/kotlin/dev/mj31/logger/client/ui/**/*.kt, src/commonMain/kotlin/dev/mj31/logger/client/theme/**/*.kt"
---

# Rule: UI Guidelines & Desktop Aesthetics

1. **Design System**:
   - Use tokenized dark palette from `dev.mj31.logger.client.theme`.
   - Maintain color coding for Log Levels (`VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`).
2. **Desktop Layout**:
   - Resizable window support with minimum dimensions.
   - Clean spacing, clear typography hierarchy, lazy list virtualization for log streams.
3. **Language Policy**:
   - All UI string constants (unless localized), code comments, and design specs in English.
