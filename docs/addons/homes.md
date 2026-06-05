# Homes & Warps (`rpg-homes`)

> **Status:** Working — per-player homes (DataStore-backed) and admin-defined server warps (warps.yml).

## Config

`plugins/rpg-homes/config.yml`:

```yaml
max-homes: 3   # maximum homes per player

messages:
  home-set: "&aHome '{name}' set."
  home-deleted: "&aHome '{name}' deleted."
  home-not-found: "&cNo home named '{name}'."
  home-limit: "&cYou can only have {max} homes. Delete one first."
  # ... (all messages configurable here)
```

## Commands

### Player commands

| Command | Description |
|---|---|
| `/home` | Teleport to your home named "home" |
| `/home <name>` | Teleport to a named home |
| `/home set [name]` | Set a home at your location (default name: `home`) |
| `/home delete <name>` | Delete a home |
| `/home list` | List all your saved homes |
| `/warp <name>` | Teleport to a server warp point |
| `/warps` | List all server warps |

### Admin commands

| Command | Permission | Description |
|---|---|---|
| `/setwarp <name>` | `rpg.homes.admin` | Create/overwrite a warp at your location |
| `/delwarp <name>` | `rpg.homes.admin` | Delete a warp |

## Permissions

| Node | Default | Description |
|---|---|---|
| `rpg.homes.use` | `true` | Use `/home` and `/warp` |
| `rpg.homes.admin` | `op` | Use `/setwarp` and `/delwarp` |
| `rpg.homes.reload` | `op` | Reload config |

## Storage

- **Homes** — stored per-player in the DataStore under repository `homes`, keyed by UUID. Location fields: world, x, y, z, yaw, pitch. Loaded lazily on first access, evicted from memory on quit.
- **Warps** — stored in `plugins/rpg-homes/warps.yml`. Written on every `/setwarp` and `/delwarp`. Safe to edit by hand while the server is stopped.

## Notes

- Home names are case-insensitive and normalised to lowercase.
- The `max-homes` limit counts per player. Admins can exceed the limit via `/setwarp`.
- Tab completion on home names draws from the player's current list; warp tab completion draws from the server warp list.

## Related

- [Persistence](../core/persistence.md)
