---
name: desktop-os-integration
description: Desktop system integration patterns including tray icons, native file dialogs, window state restoration, and OS notifications.
---

# Desktop OS Integration Skill

## Overview
Guidelines and playbooks for integrating Compose Multiplatform Desktop apps with native desktop operating systems (macOS, Windows, Linux).

## Key Patterns
1. **System Tray & Window Management**:
   - Use Compose Desktop `Tray` API to minimize log client to tray.
   - Remember window size and position preferences across restarts using `WindowState`.

2. **Native File Dialogs**:
   - Use Swing `JFileChooser` or LWJGL native file pickers for log export (.txt, .json, .csv).
   - Perform file IO asynchronously using `withContext(Dispatchers.IO)`.

3. **OS Notifications**:
   - Trigger desktop native notifications for `FATAL` and `ERROR` log alerts.

## Language Policy
- All code and documentation in English.
- Chat responses in user's prompt language.
