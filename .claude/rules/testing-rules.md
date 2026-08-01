---
description: Testing standards, TDD workflows, Google Truth assertion style, and coroutine test execution rules.
globs: "src/commonTest/**/*.kt, src/desktopTest/**/*.kt"
---

# Rule: Testing Standards & Quality Assurance

1. **Test Location**:
   - Multiplatform unit tests go under `src/commonTest/kotlin/dev/mj31/logger/client/`.
2. **Assertion Library**:
   - Use **Google Truth** (`com.google.common.truth.Truth.assertThat`) for all unit test assertions (e.g., `assertThat(actual).isEqualTo(expected)`, `assertThat(list).hasSize(count)`, `assertThat(item).isNull()`).
3. **Quality Rules**:
   - Zero tolerance for commented-out broken assertions or deleted failing tests.
   - Tests must run cleanly with `./gradlew test`.
4. **Language Policy**:
   - All test names, assertion messages, and comments strictly in English.
