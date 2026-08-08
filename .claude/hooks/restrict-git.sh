#!/bin/bash
#
# PreToolUse guard: Claude may inspect and modify the working tree, but it must not create commits
# or communicate with Git remotes. Permission deny rules cover ordinary commands; this hook also
# catches absolute git paths, global git options, and git calls inside compound shell commands.
set -uo pipefail

COMMAND=$(/usr/bin/jq -r '.tool_input.command // ""')

# Put each shell subcommand on its own line. This is intentionally conservative: it does not execute
# or expand any part of the model-provided command.
SEGMENTS=$(printf '%s\n' "$COMMAND" | /usr/bin/sed -E $'s/(&&|\\|\\||[;|&()])/\\\n/g')

while IFS= read -r SEGMENT; do
    read -ra WORDS <<< "$SEGMENT"
    INDEX=0

    # Skip leading environment assignments and common transparent process wrappers.
    while [ $INDEX -lt ${#WORDS[@]} ]; do
        WORD=${WORDS[$INDEX]}
        case "$WORD" in
            *=*) INDEX=$((INDEX + 1)) ;;
            command|time|timeout|nice|nohup|stdbuf) INDEX=$((INDEX + 1)) ;;
            env)
                INDEX=$((INDEX + 1))
                while [ $INDEX -lt ${#WORDS[@]} ]; do
                    WORD=${WORDS[$INDEX]}
                    case "$WORD" in
                        -*|*=*) INDEX=$((INDEX + 1)) ;;
                        *) break ;;
                    esac
                done
                ;;
            *) break ;;
        esac
    done

    [ $INDEX -lt ${#WORDS[@]} ] || continue
    EXECUTABLE=${WORDS[$INDEX]}
    EXECUTABLE=${EXECUTABLE#\"}
    EXECUTABLE=${EXECUTABLE%\"}
    EXECUTABLE=${EXECUTABLE#\'}
    EXECUTABLE=${EXECUTABLE%\'}
    [ "${EXECUTABLE##*/}" = "git" ] || continue
    INDEX=$((INDEX + 1))

    # Locate the git subcommand after global options. -C, -c and the path-selecting options consume
    # the following word when their value is not attached with '='.
    while [ $INDEX -lt ${#WORDS[@]} ]; do
        WORD=${WORDS[$INDEX]}
        case "$WORD" in
            -C|-c|--git-dir|--work-tree|--namespace|--exec-path|--config-env)
                INDEX=$((INDEX + 2))
                ;;
            -*)
                INDEX=$((INDEX + 1))
                ;;
            *)
                break
                ;;
        esac
    done

    [ $INDEX -lt ${#WORDS[@]} ] || continue
    SUBCOMMAND=${WORDS[$INDEX]}
    SUBCOMMAND=${SUBCOMMAND#\"}
    SUBCOMMAND=${SUBCOMMAND%\"}
    SUBCOMMAND=${SUBCOMMAND#\'}
    SUBCOMMAND=${SUBCOMMAND%\'}

    case "$SUBCOMMAND" in
        commit|commit-tree|merge|rebase|cherry-pick|revert|am|stash|notes|fast-import|filter-branch)
            echo "Blocked by repository policy: Claude Code may not create or rewrite commits." >&2
            exit 2
            ;;
        push|fetch|pull|clone|ls-remote|remote|submodule|maintenance|scalar|lfs|send-pack|receive-pack|upload-pack|daemon|p4|svn)
            echo "Blocked by repository policy: Claude Code may not use Git remotes." >&2
            exit 2
            ;;
    esac
done <<< "$SEGMENTS"

exit 0
