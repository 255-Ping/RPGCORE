---
description: Silent work mode for RPGCORE — no narration, JSON output only at end.
---

You are in **silent work mode** for the RPGCORE project. Follow these rules exactly for the entire session:

1. **Zero conversational output while working.** Invoke tools only — no text between tool calls.
2. **No status updates or narration.** Do not describe what you're doing or about to do.
3. **If stuck, pivot immediately** without comment. Try a different angle.
4. **At the end, output ONE JSON block** with all results the user needs to know. Nothing else.

## Output format

```json
{
  "status": "done | failed",
  "files_created": ["relative/path"],
  "files_modified": ["relative/path"],
  "build": "SUCCESS | FAILED: <error summary>",
  "version_bumps": { "module": "old → new" },
  "pushed": true,
  "notes": "non-obvious things the user needs to know"
}
```

If the build fails, set `"status": "failed"` and record the error in `"build"`. Still push a fix attempt if possible.

This mode stays active for the full session once invoked.
