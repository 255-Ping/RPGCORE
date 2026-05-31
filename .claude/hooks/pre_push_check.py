#!/usr/bin/env python3
"""
Pre-push documentation drift gate for RPGCORE.

Reads the Bash tool input from stdin. If the command is a git push:
  - Checks whether /update-docs was run at the current HEAD
    (stamped in .claude/last-docs-check by the update-docs skill).
  - If yes: exits 0 and lets the push proceed silently.
  - If no:  exits 2 (soft-block) and prints a reminder so Claude
            runs /update-docs before retrying the push.

Exit codes:
  0  — proceed normally
  2  — soft-block: output shown to Claude, tool call cancelled so Claude
       can act on the message before retrying
"""

import sys
import json
import subprocess
import os


def current_head():
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "HEAD"],
            stderr=subprocess.DEVNULL,
            cwd=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        ).decode().strip()
    except Exception:
        return None


def last_checked_head():
    marker = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "last-docs-check"
    )
    try:
        with open(marker) as f:
            return f.read().strip()
    except Exception:
        return None


def main():
    try:
        raw = sys.stdin.read()
        data = json.loads(raw) if raw.strip() else {}
        command = data.get("tool_input", {}).get("command", "")

        if "git push" not in command:
            sys.exit(0)

        head = current_head()
        last = last_checked_head()

        if head and head == last:
            # /update-docs was already run at this exact commit — proceed
            sys.exit(0)

        # Docs haven't been checked yet — soft-block and remind
        print(
            "PRE-PUSH GATE — documentation drift check required.\n\n"
            "Run /update-docs before this push. It will:\n"
            "  1. Sync every doc page affected by the current changes\n"
            "  2. Resolve completed todo items in docs/planned/\n"
            "  3. Stamp a marker so this check passes on the next push\n\n"
            "After /update-docs finishes, retry the push."
        )
        sys.exit(2)

    except Exception:
        # Never block a push due to a hook error
        sys.exit(0)


if __name__ == "__main__":
    main()
