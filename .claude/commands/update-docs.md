---
description: Audit and sync all docs/ pages that are touched by recent code changes. Fixes drift, updates todo items, and stamps a marker so the pre-push hook knows docs are current.
---

You are running the **update-docs** routine for RPGCORE. Execute every step in order without skipping. Zero narration during the process — output only the final JSON block.

## Step 1 — Identify changed files

Run:
```
git diff --name-only HEAD
git status --porcelain
```

Collect every file that is new, modified, or staged. If there are no git changes, also check whether the user mentioned specific files or modules in their last message and use those.

## Step 2 — Map changes to doc pages

Using the routing table below, determine which doc pages need reviewing. Flag **every** match — err on the side of checking too many rather than too few.

| If a changed file is in / is named... | Check these doc pages |
|---|---|
| `rpg-core/` or `rpg-api/` (any) | `docs/development.md`, `docs/core/README.md`, affected subsystem page |
| `*/RpgServices.java` | `docs/development.md` — services list |
| `*/config.yml` for any module | The addon's own doc page in `docs/addons/<name>.md` |
| `*/messages.yml` | `docs/formatting.md` |
| `rpg-<name>/` (addon) | `docs/addons/<name>.md` |
| Any `*/migrations/` SQL file | `docs/core/persistence.md` |
| `gradle.properties` | `docs/changelog.md`, `CLAUDE.md` versions table |
| Any status-effect schema change | `docs/core/status-effects.md` |
| Any skill XP or curve change | `docs/core/skills.md` |
| Any damage pipeline change | `docs/core/damage.md` |
| Any command or permission | `docs/commands.md`, `docs/permissions.md` |
| Any content YAML (items, mobs, abilities, blocks) | `docs/content/<type>.md` |
| Any recipe YAML | `docs/content/recipes.md` |

If `gradle.properties` changed (version bumps), `docs/changelog.md` is always required.

## Step 3 — For each flagged doc page

For every doc page identified in Step 2:

1. **Read the actual source of truth first**: for an addon, read its `config.yml` and key Java source (plugin main class, loaders, listeners, commands). Do not rely on memory.
2. **Read the doc page.**
3. **Find and fix every drift**:
   - Config keys, types, or defaults shown in the doc that don't match the actual `config.yml`
   - Commands or permissions in the doc that don't exist in the code (check `plugin.yml`)
   - YAML schema examples (items, recipes, blocks, etc.) whose field names or structure don't match the loader
   - Status text (`Working` / `In Progress` / `Planned`) that doesn't match the actual implementation state
   - Numbers (XP values, costs, rates) that differ from the actual config defaults or code constants
   - Broken links to other doc pages
4. **While the page is open, also fix unrelated inconsistencies**: formatting, duplicate sections, placeholder text, or other obvious errors you notice even if they weren't caused by the current change.
5. Write the corrected doc in a single Edit.

## Step 4 — Update changelog.md

If `gradle.properties` was changed or any plugin version was bumped during this session:
- Open `docs/changelog.md`
- Find or create the correct `## Suite <N>` heading and `### rpg-<name> X.Y.Z` block
- Ensure every bumped plugin has a matching entry with at least one bullet describing what changed

If changelog is already up to date, skip this step.

## Step 5 — Update CLAUDE.md versions table

If `gradle.properties` changed, read the current versions and update the **Current versions** table in `CLAUDE.md` to match exactly.

## Step 6 — Resolve completed todo items

1. Run `git log --oneline -5` to see recent commit messages.
2. Read `docs/planned/todo.md` and all five sub-pages: `todo-bugs.md`, `todo-features.md`, `todo-improvements.md`, `todo-gui.md`, `todo-docs.md`.
3. For each todo item, check if it's done based on evidence from the diff, changed files, or git log. An item is **done** when:
   - The feature it describes now exists in the modified or new code files
   - The bug it describes is clearly fixed based on the diff
   - A doc page it called for was written in Step 3
   - The commit message or recent git log explicitly names it as complete
4. For each **clearly** completed item:
   - Remove it from its sub-page (or strikethrough it if it's in a table)
   - Add a line to the `## ✅ Recently Completed` section in `docs/planned/todo.md`: `- <brief description>`
   - Do **not** touch items that are only partially done or where you are uncertain
5. If no items are clearly done based on the available evidence, skip the edits and note it.

## Step 7 — Stamp the docs-checked marker

Run:
```
git rev-parse HEAD > .claude/last-docs-check
```

This tells the pre-push hook that docs are up to date at this exact commit. The hook will skip its reminder if this file matches the current HEAD.

## Step 8 — Output

After all edits are written, output this JSON and nothing else:

```json
{
  "status": "done | failed",
  "docs_checked": ["relative/path/to/doc.md"],
  "docs_updated": ["relative/path/to/doc.md"],
  "fixes": ["short description of each fix made"],
  "todo_items_resolved": ["brief description of each item moved to Recently Completed"],
  "changelog_updated": true,
  "notes": "anything non-obvious the user should know"
}
```

If a doc page was checked and needed no changes, include it in `docs_checked` but not `docs_updated`.
