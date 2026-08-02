---
description: Legal compliance, third-party license compatibility (Apache-2.0, LGPL-3.0, MIT), and distribution artifact requirements.
globs: "**/build.gradle.kts, LICENSE, THIRD-PARTY*, app/legal/**"
---

# Rule: Legal Compliance & Third-Party Licenses

1. **Project License**:
   - The primary code in this repository is licensed under **Apache License, Version 2.0**.
   - All newly created files belong to the project copyright under Apache 2.0 unless otherwise stated.

2. **Third-Party Dependency Policy**:
   - **Permissive Licenses (Apache 2.0, MIT, BSD)**: Preferred for all direct and transitive dependencies (KotlinX, Compose, Detekt, Truth, etc.).
   - **Weak Copyleft Licenses (LGPL v3.0)**: Allowed for native binary wrappers (FFmpeg / JavaCPP / libVLC) ONLY when dynamically linked and not statically bundled into proprietary source units.

3. **Distribution Requirements (jpackage / Native Installers)**:
   - Every binary distribution (DMG, MSI, DEB, App Bundle) MUST ship with third-party license texts.
   - Files under `app/legal/common/` (`LGPL-3.0.txt`, `GPL-3.0.txt`, `THIRD-PARTY.txt`) MUST be preserved and referenced by `appResourcesRootDir` in `app/build.gradle.kts`.
   - Never remove or bypass the `app/legal/` directory configuration in Gradle.

4. **Language & Headers**:
   - License notices, legal documentation, and DISCLAIMER files MUST strictly be in English.
