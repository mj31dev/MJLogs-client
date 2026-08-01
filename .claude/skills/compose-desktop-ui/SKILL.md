---
name: compose-desktop-ui
description: Best practices, layout rules, state handling, positive code examples, and styling guidelines for Compose Multiplatform Desktop apps.
---

# Compose Desktop UI Development Skill

## Overview
This skill provides comprehensive instructions for designing and implementing high-performance, aesthetically stunning Compose Multiplatform Desktop interfaces.

## Key Principles & Playbooks
1. **Design System & Aesthetics**:
   - Use tokenized colors (`dev.mj31.logger.client.theme.Color.kt`).
   - Implement dark themes with high-contrast log level indicators (`VERBOSE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`).
   - Use monospace fonts for log messages, timestamps, and payload previews.

2. **Compose State Guidelines & State Hoisting**:
   - Keep composable functions stateless wherever possible; hoist state to view models or state holders.
   - Use `remember` with key inputs to avoid unnecessary recomposition loops.
   - For high-frequency log updates, use key-based `LazyColumn` items (`key = { it.id }`).

3. **Positive Code Example (LogItemRow)**:
```kotlin
@Composable
fun LogItemRow(
    log: LogEntry,
    onLogSelected: (LogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(40.dp)
                    .background(log.level.toColor(), shape = RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = log.message,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
```

4. **Desktop Windowing & OS Integration**:
   - Configure window title, default dimensions (e.g. 1024x768), and min bounds in `Main.kt`.
   - Provide keyboard shortcuts for search clearing, filter toggling, and log export.

5. **Language Rule**:
   - All code, UI strings, comments, and skill definitions must be in English.
   - Respond in chat using the user's prompt language.
