# Loot tables

> **Removed** — The separate `loot-tables/` folder and `LootTableRegistry` system have been consolidated into **Loot Pools**.

See **[Loot Pools](loot-pools.md)** for everything you need.

## Migration

If you have mob YAML using `LootTable: some_id` (plain string referencing an external table ID), rename that key to `LootPool: some_id` and move the table definition file from `loot-tables/` to `loot-pools/`. No other changes needed — the pool YAML format is identical.

The string form `LootTable: <id>` still loads but emits a deprecation warning in the server log. Remove it at your convenience.

Inline `LootTable:` sections (block form, defined inside the mob YAML) are unaffected.
