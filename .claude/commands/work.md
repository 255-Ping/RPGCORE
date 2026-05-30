---
description: Standard work loop for RPGCORE — implement, docs, build, fix, push. Zero narration; JSON output only at end.
---

You are in **work mode** for the RPGCORE project. Follow the loop below exactly. Zero conversational output while working — invoke tools only. No narration, no status updates, no "I'll now..." text. Output ONE JSON block at the very end, nothing else.

## The Loop

### 1. Implement
Do the work the user requested. Apply all rules from `CLAUDE.md`: Rule 1 (api-only deps), Rule 2 (scheduler), Rule 5 (everything configurable), Rule 7 (formatting standards). Do not declare done until the feature is actually complete.

### 2. Update docs + fix inconsistencies
Run `/update-docs` — it reads the actual config.yml and source for every module you touched, updates the relevant doc pages to match, fixes any other inconsistencies it finds in those pages while they're open, and updates `docs/changelog.md` + the CLAUDE.md versions table if versions changed.

Do not skip this step. A feature without correct docs is not done.

### 3. Build
```
.\gradlew.bat assemble
```

### 4. Fix errors and repeat
If the build fails:
- Read the compiler output carefully
- Fix every error
- Go back to Step 2 (re-run `/update-docs` if the fix changed anything docs-relevant)
- Re-run `.\gradlew.bat assemble`
- Repeat until the build is clean

### 5. Push to main
```
git add <specific files only — never git add -A or git add .>
git commit -m "<message>"
git push
```

Do not push if the build is not clean. Do not push without docs being updated first.

---

## Output format

After a successful push, output this JSON block and nothing else:

```json
{
  "status": "done | failed",
  "files_created": ["relative/path"],
  "files_modified": ["relative/path"],
  "docs_updated": ["relative/path"],
  "build": "SUCCESS | FAILED: <error summary>",
  "version_bumps": { "module": "old → new" },
  "pushed": true,
  "notes": "non-obvious things the user should know"
}
```

If the build ultimately cannot be fixed, set `"status": "failed"`, explain in `"notes"`, and do not push.
