# Master permission reference

> **Status:** Working (synced with plugin.ymls as of suite 21)

Permission nodes follow `rpg.<module>.<verb>[.<qualifier>]`. Self-use defaults to true; admin/op nodes default to op.

For the full command list, see [commands.md](commands.md).

---

## Conventions

- `.other` suffix = same command targeting another player (always op-default).
- `.admin.*` group = administrative actions for that module (always op-default).
- Player-use commands (`/balance`, `/stats`, `/effects`, `/party *`, `/guild *`, etc.) default-true so players can use them without explicit LuckPerms setup.

---

## Core (`rpg-core`)

| Node | Default | Description |
|---|---|---|
| `rpg.core.version` | true | List loaded modules and versions |
| `rpg.core.reload-all` | op | Reload every plugin |
| `rpg.core.item.give` | op | Give a custom item to a player |
| `rpg.core.mob.spawn` | op | Spawn a custom mob |
| `rpg.core.block.give` | op | Give a placeable custom block |
| `rpg.core.block.convert` | op | Bulk-convert vanilla blocks to a custom block |
| `rpg.core.wand` | op | Get the selection wand |
| `rpg.core.loot-chest` | op | Manage loot chest bindings |
| `rpg.core.particle` | op | Spawn debug particle effects (`/rpg particle`) |
| `rpg.core.stats` | true | View own stats |
| `rpg.core.stats.other` | op | View another player's stats |
| `rpg.core.skill` | true | View own skill levels |
| `rpg.core.skill.other` | op | View another player's skill levels |
| `rpg.core.effects` | true | View own active status effects |
| `rpg.core.effects.other` | op | View another player's effects |

## Admin (`rpg-admin`)

| Node | Default | Description |
|---|---|---|
| `rpg.admin.*` | op | All rpg-admin permissions (wildcard) |
| `rpg.admin.gamemode` | op | `/gmc`, `/gms`, `/gma`, `/gmsp` |
| `rpg.admin.fly` | op | `/fly [player]` |
| `rpg.admin.god` | op | `/god [player]` |
| `rpg.admin.tp` | op | `/tp`, `/tphere` |
| `rpg.admin.heal` | op | `/heal` (self) |
| `rpg.admin.heal.others` | op | `/heal <player>` |
| `rpg.admin.feed` | op | `/feed` (self) |
| `rpg.admin.feed.others` | op | `/feed <player>` |
| `rpg.admin.speed` | op | `/speed` |
| `rpg.admin.clear` | op | `/clear` (self) |
| `rpg.admin.clear.others` | op | `/clear <player>` |
| `rpg.admin.broadcast` | op | `/broadcast` |
| `rpg.admin.sudo` | op | `/sudo` |

## Spawners

| Node | Default | Description |
|---|---|---|
| `rpg.spawners.admin.create` | op | Create a spawner |
| `rpg.spawners.admin.delete` | op | Delete a spawner |
| `rpg.spawners.admin.edit` | op | Edit spawner settings |
| `rpg.spawners.admin.list` | op | List spawners |
| `rpg.spawners.admin.tp` | op | Teleport to a spawner |

## Death tier examples (configurable)

| Node | Description |
|---|---|
| `rpg.death.default` | Default death rule |
| `rpg.death.vip` | VIP death rule (keep everything) |

Actual node names come from `death-rules.tiers[*].permission` in core `config.yml`. See [damage pipeline](core/damage.md#death-rules).

## Economy (`rpg-economy`)

| Node | Default | Description |
|---|---|---|
| `rpg.economy.balance` | true | Check own balance |
| `rpg.economy.balance.other` | op | Check another player's balance |
| `rpg.economy.pay` | true | Send currency |
| `rpg.economy.baltop` | true | Leaderboard |
| `rpg.economy.admin.set` | op | Set balance |
| `rpg.economy.admin.add` | op | Add balance |
| `rpg.economy.admin.remove` | op | Subtract balance |
| `rpg.economy.admin.reset` | op | Reset balance |
| `rpg.economy.admin.reload` | op | Reload config |

## Trade (`rpg-trade`)

| Node | Default | Description |
|---|---|---|
| `rpg.trade.use` | true | Send and receive trade requests |
| `rpg.trade.admin.reload` | op | Reload config |

## Chat (`rpg-chat`)

| Node | Default | Description |
|---|---|---|
| `rpg.chat.use.global` | true | Use global channel |
| `rpg.chat.use.party` | true | Use party channel |
| `rpg.chat.use.guild` | true | Use guild channel |
| `rpg.chat.msg` | true | Direct message |
| `rpg.chat.reply` | true | Reply to DM |
| `rpg.chat.clearchat` | op | Clear chat for everyone |
| `rpg.chat.mutechat` | op | Toggle global chat mute |
| `rpg.chat.mute.bypass` | op | Bypass mutechat |
| `rpg.chat.admin.reload` | op | Reload config |

## HUD (`rpg-hud`)

| Node | Default | Description |
|---|---|---|
| `rpg.hud.toggle` | true | Toggle HUD elements |
| `rpg.hud.reload` | op | Reload config |

## Parties (`rpg-parties`)

| Node | Default | Description |
|---|---|---|
| `rpg.parties.create` | true | Create a party |
| `rpg.parties.invite` | true | Invite |
| `rpg.parties.accept` | true | Accept invite |
| `rpg.parties.kick` | true | Kick (rank-gated at runtime) |
| `rpg.parties.promote` | true | Promote (owner-only at runtime) |
| `rpg.parties.demote` | true | Demote (owner-only at runtime) |
| `rpg.parties.leave` | true | Leave |
| `rpg.parties.disband` | true | Disband (owner-only at runtime) |
| `rpg.parties.list` | true | List members |
| `rpg.parties.admin.reload` | op | Reload config |

## Guilds (`rpg-guilds`)

| Node | Default | Description |
|---|---|---|
| `rpg.guilds.create` | true | Create a guild |
| `rpg.guilds.invite` | true | Invite |
| `rpg.guilds.accept` | true | Accept invite |
| `rpg.guilds.kick` | true | Kick (rank-gated) |
| `rpg.guilds.promote` | true | Promote |
| `rpg.guilds.demote` | true | Demote |
| `rpg.guilds.leave` | true | Leave |
| `rpg.guilds.disband` | true | Disband (owner-only) |
| `rpg.guilds.info` | true | Guild info |
| `rpg.guilds.list` | true | List all guilds |
| `rpg.guilds.bank` | true | Open bank (rank-gated for deposit/withdraw at runtime) |
| `rpg.guilds.admin.reload` | op | Reload config |

## Regions (`rpg-regions`)

| Node | Default | Description |
|---|---|---|
| `rpg.regions.admin.define` | op | Define a region |
| `rpg.regions.admin.delete` | op | Delete a region |
| `rpg.regions.admin.list` | op | List all regions |
| `rpg.regions.admin.info` | op | Region info at a location |
| `rpg.regions.admin.flag` | op | Set a region flag |
| `rpg.regions.admin.global` | op | Edit the global region flags |
| `rpg.regions.admin.reload` | op | Reload config |

## Dungeons (`rpg-dungeons`)

| Node | Default | Description |
|---|---|---|
| `rpg.dungeons.admin.create` | op | Create a dungeon template |
| `rpg.dungeons.admin.delete` | op | Delete a template |
| `rpg.dungeons.admin.set` | op | Set entrance/exit/spawn for a dungeon |
| `rpg.dungeons.admin.list` | true | List dungeons |
| `rpg.dungeons.admin.reload` | op | Reload dungeon YAMLs |
| `rpg.dungeons.use.enter` | true | Enter a dungeon |
| `rpg.dungeons.use.leave` | true | Leave a dungeon |

## Accessories (`rpg-accessories`)

| Node | Default | Description |
|---|---|---|
| `rpg.accessories.open` | true | Open accessory bag |
| `rpg.accessories.upgrade` | true | Upgrade bag tier |
| `rpg.accessories.admin.reload` | op | Reload config |

## Enchanting (`rpg-enchanting`)

| Node | Default | Description |
|---|---|---|
| `rpg.enchanting.use.enchant` | true | Use the enchanting station block |
| `rpg.enchanting.use.anvil` | true | Use the anvil block |
| `rpg.enchanting.admin.reload` | op | Reload enchant/reforge/upgrade YAMLs |
| `rpg.enchanting.admin.list` | op | List loaded enchants/reforges/upgrades |
| `rpg.enchanting.admin.give` | op | Give enchant book / reforge stone / upgrade scroll |

## NPCs (`rpg-npcs`)

| Node | Default | Description |
|---|---|---|
| `rpg.npcs.use` | true | Click NPCs to interact |
| `rpg.npcs.admin.create` | op | Create NPC |
| `rpg.npcs.admin.delete` | op | Delete |
| `rpg.npcs.admin.move` | op | Move to player location |
| `rpg.npcs.admin.list` | op | List / info |
| `rpg.npcs.admin.reload` | op | Reload YAMLs |
| `rpg.npcs.admin.setbehavior` | op | Set behavior type |
| `rpg.npcs.admin.setentitytype` | op | Set entity type |
| `rpg.npcs.admin.setstyle` | op | Switch body style |
| `rpg.npcs.admin.setskin` | op | Apply player skin |
| `rpg.npcs.admin.dialogue` | op | Edit dialogue in-game |
| `rpg.npcs.admin.shop` | op | Edit shop items in-game |
| `rpg.npcs.admin.setlook` | op | Toggle look-at-player |

## Holograms (`rpg-holograms`)

| Node | Default | Description |
|---|---|---|
| `rpg.holograms.admin.create` | op | Create |
| `rpg.holograms.admin.delete` | op | Delete |
| `rpg.holograms.admin.list` | op | List |
| `rpg.holograms.admin.tp` | op | Teleport to |
| `rpg.holograms.admin.move` | op | Move |
| `rpg.holograms.admin.edit` | op | Edit lines |
| `rpg.holograms.admin.reload` | op | Reload config |

## Quests (`rpg-quests`)

| Node | Default | Description |
|---|---|---|
| `rpg.quests.use.list` | true | List available + active quests |
| `rpg.quests.use.accept` | true | Accept a quest |
| `rpg.quests.use.abandon` | true | Abandon an active quest |
| `rpg.quests.use.progress` | true | Show quest progress |
| `rpg.quests.admin.complete` | op | Force-complete for a player |
| `rpg.quests.admin.reload` | op | Reload quest YAMLs |

## Homes (`rpg-homes`)

| Node | Default | Description |
|---|---|---|
| `rpg.homes.use` | true | Use `/home` and `/warp` |
| `rpg.homes.admin` | op | Use `/setwarp` and `/delwarp` |
| `rpg.homes.reload` | op | Reload config |

## Kits (`rpg-kits`)

| Node | Default | Description |
|---|---|---|
| `rpg.kits.use` | true | Claim kits via `/kit` |
| `rpg.kits.admin` | op | `/givenkit` and `/kitreset` |
| `rpg.kits.reload` | op | Reload kit YAMLs |

## Cooking (`rpg-cooking`)

| Node | Default | Description |
|---|---|---|
| `rpg.cooking.use` | true | Use the cooking station |
| `rpg.cooking.admin.reload` | op | Reload config and recipes |
| `rpg.cooking.admin.list` | op | List loaded recipes |

## Alchemy (`rpg-alchemy`)

| Node | Default | Description |
|---|---|---|
| `rpg.alchemy.use.brew` | true | Use the brewing station |
| `rpg.alchemy.use.drink` | true | Drink custom potions |
| `rpg.alchemy.admin.reload` | op | Reload config and recipes |
| `rpg.alchemy.admin.list` | op | List loaded potions |

## Crafting (`rpg-crafting`)

| Node | Default | Description |
|---|---|---|
| `rpg.crafting.admin.reload` | op | Reload recipes |
| `rpg.crafting.admin.list` | op | List loaded recipes |

## Smelting (`rpg-smelting`)

| Node | Default | Description |
|---|---|---|
| `rpg.smelting.use` | true | Use the smelting station |
| `rpg.smelting.admin.reload` | op | Reload config and recipes |
| `rpg.smelting.admin.list` | op | List loaded recipes |

## Skill addons (Mining / Combat / Foraging / Farming / Fishing)

Each skill addon registers:

| Pattern | Default | Description |
|---|---|---|
| `rpg.<skill>.admin.reload` | op | Reload that addon's config |

Specific nodes:

| Node | Default | Description |
|---|---|---|
| `rpg.mining.admin.reload` | op | Reload rpg-mining |
| `rpg.combat.admin.reload` | op | Reload rpg-combat |
| `rpg.foraging.admin.reload` | op | Reload rpg-foraging |
| `rpg.farming.admin.reload` | op | Reload rpg-farming |
| `rpg.fishing.admin.reload` | op | Reload rpg-fishing |
