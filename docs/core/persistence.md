# Persistence

> **Status:** In progress — YAML backend works; MySQL backend not yet implemented (config key reserved, currently no-op)

`rpg-core` provides a `DataStore` service that every addon must go through to read or write persistent data. The backend is selectable in core config: **YAML (default)** or **MySQL**.

## Config

`plugins/rpg-core/config.yml`:

```yaml
persistence:
  backend: yaml                  # yaml | mysql
  mysql:
    host: 127.0.0.1
    port: 3306
    database: rpg
    user: rpg
    password: ""
    pool-size: 8
    use-ssl: false
```

If `backend: mysql` and the connection fails on startup, core falls back to YAML with a loud warning. Fix credentials and run `/rpg reloadall`.

## Where data lives

| Backend | Layout |
|---|---|
| YAML | One file per record under `plugins/<module>/data/`. Example: `plugins/rpg-core/data/players/<uuid>.yml` |
| MySQL | One table per record type, schema versioned per [schema versioning](#schema-versioning) |

## What gets persisted

- **Player files** (rpg-core) — base stats, current mana, skill XP+levels, active status effects, cooldown timestamps, pinned skill, accessory bag contents, accessory bag tier
- **Economy balances** (rpg-economy) — currency amounts per (player, currency)
- **Guild data** (rpg-guilds) — guild metadata, members, ranks, XP/level, bank contents, audit log
- **Region data** (rpg-regions) — region definitions, flags
- **Dungeon templates** (rpg-dungeons) — captured volumes, conditions, win conditions, loot pools
- **Spawner placements** (rpg-core) — admin-placed spawner definitions
- **NPC placements** (rpg-npcs) — NPC definitions and locations
- **Hologram placements** (rpg-holograms)
- **Mute records** (rpg-chat)
- **Quest progress** (rpg-quests) — per-player

## Schema versioning

Every persistent record carries a `schema-version: <int>` field. On read, the migration runner upgrades old records by running a sequence of migrations.

- YAML: migration mutates the YAML in-place, writes the new version.
- MySQL: migrations are versioned SQL files in `rpg-core` resources; a `_schema_versions` table tracks applied migrations per addon.

When a server admin downgrades a plugin (rare), they must restore from backup — we don't auto-downgrade schemas.

## API surface (developer-facing)

`DataStore` exposes named `Repository` instances. Records are stored as untyped `Map<String, Object>` — each addon is responsible for serializing its own types to/from maps. This avoids depending on a heavyweight ORM and matches Bukkit's YamlConfiguration shape natively.

```java
DataStore store = RpgServices.dataStore();
DataStore.Repository players = store.repository("players");

// Read
Optional<Map<String, Object>> rec = players.get(uuid.toString());

// Write (async)
players.save(uuid.toString(), Map.of(
    "schema-version", 1,
    "stats", Map.of("max_health", 100, "strength", 5),
    "skills", Map.of("combat", Map.of("level", 1, "totalXp", 0L))
)).join();

// Delete
players.delete(uuid.toString()).join();

// Listing
Collection<String> all = players.keys();
```

Repository names map to a folder under the calling plugin's data folder (or a table in MySQL once that backend lands). Keys are sanitized (`[^a-zA-Z0-9_.-]` → `_`) to be filesystem-safe. Writes are queued on the common ForkJoinPool via `CompletableFuture.runAsync`; reads are synchronous.

## Concurrency

Reads can happen on any thread. Writes are queued onto a single I/O thread per store, serialized in order. Players' read-modify-write cycles use a per-player lock to prevent torn writes.

## Backup

YAML is trivially backed-up by copying `plugins/`. MySQL: standard `mysqldump`. We don't ship a backup tool; admins handle it.

## Related

- [Vanilla suppression](vanilla-suppression.md) (death rules etc. are persisted as player flags)
- [Configuration overview](../configuration.md)
