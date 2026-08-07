#!/bin/bash
#
# Stop hook: keeps Detekt honest at the end of a turn instead of after every single edit.
#
# A PostToolUse hook would pay the Gradle round trip once per edited file, which measures around
# five seconds warm; over a session of twenty edits that is minutes of waiting for an analysis whose
# answer only matters once the work is finished. Running it here costs that round trip exactly once.
#
# The hook never fires when the user interrupts a turn: Claude Code raises `Stop` only when the
# assistant finishes on its own, so an interrupted turn is left alone by design.
set -uo pipefail

cd "$(dirname "$0")/../.." || exit 0

INPUT=$(cat)

# `stop_hook_active` is true on a turn that this hook already blocked once. Blocking again would
# loop forever on a violation the model cannot fix, so the second pass only reports.
ALREADY_BLOCKED=false
if echo "$INPUT" | grep -q '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
    ALREADY_BLOCKED=true
fi

# Nothing Kotlin changed, so there is nothing for Detekt to say. Costs one `git status` instead of a
# Gradle start-up on turns that only answered a question.
if [ -z "$(git status --porcelain -- '*.kt' '*.kts')" ]; then
    exit 0
fi

OUTPUT=$(./gradlew detekt --daemon --console=plain 2>&1)
STATUS=$?

if [ $STATUS -eq 0 ]; then
    exit 0
fi

# Detekt reports one issue per line as `e: /path/File.kt:LINE:COL message [RuleName]`. Everything
# else Gradle prints on a failure is noise that would drown the actual findings.
VIOLATIONS=$(echo "$OUTPUT" | grep -E '^[ew]: .*\.kts?:[0-9]+:[0-9]+' | head -40)
if [ -z "$VIOLATIONS" ]; then
    # No issue lines means the build broke for another reason: a compile error, a failed
    # verifySourceLayout. Pass the tail through so the cause is visible.
    VIOLATIONS=$(echo "$OUTPUT" | grep -vE '^(Deprecated Gradle|You can use|For more on this|\* Try:|> Run with|> Get more help|Successfully generated)' | tail -25)
fi

if [ "$ALREADY_BLOCKED" = true ]; then
    echo "Detekt is still failing after one attempt; reporting instead of blocking again:" >&2
    echo "$VIOLATIONS" >&2
    exit 0
fi

echo "Detekt failed on violations its formatter cannot fix automatically. Fix them, then finish:" >&2
echo "$VIOLATIONS" >&2
exit 2
