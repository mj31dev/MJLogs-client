#!/bin/bash
#
# Points git at the hooks tracked in this repository.
#
# `.git/hooks` is local to a clone and is never checked out, so a hook committed to the project does
# nothing until git is told where to look. One `core.hooksPath` does that for every hook at once and,
# unlike copying files into `.git/hooks`, cannot go stale when the tracked hook changes.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -d .githooks ]; then
    echo "No .githooks directory: run this from a checkout of the repository." >&2
    exit 1
fi

chmod +x .githooks/*
git config core.hooksPath .githooks

echo "Hooks enabled from .githooks:"
for hook in .githooks/*; do
    echo "  - $(basename "$hook")"
done
