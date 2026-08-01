---
name: test-automation-tdd
description: Guidelines for unit testing, test-driven development, coroutine testing, and regression prevention in KMP Desktop.
---

# Test Automation & TDD Skill

## Workflow
1. **Red Stage**: Write failing unit test covering target domain logic or view model state behavior in `src/commonTest`.
2. **Green Stage**: Write minimal production code to pass the unit test cleanly.
3. **Refactor Stage**: Refactor code while running `./gradlew test` and `./gradlew detektFormat` to maintain green build and pristine formatting.

## Best Practices
- Structure tests using AAA pattern (Arrange, Act, Assert).
- Use `kotlin.test.assertEquals`, `assertNull`, `assertTrue` for clean assertions.
- Use `runTest` from kotlinx-coroutines-test for coroutine testing.

## Language Rule
- All test code and comments strictly in English.
- Chat responses match the user's prompt language.
