# Master command reference

> **Status:** Working (synced with plugin.ymls as of suite 21)

Every command across every module. Permissions follow `rpg.<module>.<command>[.<sub>]`. Self-use commands default to true; admin commands default to op; moderation commands declared per-command.

## Conventions

- `<arg>` = required, `[arg]` = optional, `<a|b|c>` = pick one
- Commands marked **admin** are op-default; **self** are true-default; **mod** noted explicitly
- Self-targeting versions of admin commands often have a `.other` permission for targeting other players

---

## Core (`rpg-core`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/rpg version` | `rpg.core.version` | true | List loaded modules and versions |
| `/rpg reloadall` | `rpg.core.reload-all` | op | Reload every plugin in the suite |
| `/rpg item give <id> [player] [amount]` | `rpg.core.item.give` | op | Spawn a custom item |
| `/rpg mob spawn <id> [count]` | `rpg.core.mob.spawn` | op | Spawn a custom mob at your location |
| `/rpg ability cast <id>` | `rpg.core.ability.cast` | op | Debug-cast a custom ability |
| `/rpg block give <id> [amount]` | `rpg.core.block.give` | op | Give a placeable form of a custom block |
| `/rpg block convert <radius> <fromMaterial> <toBlockId>` | `rpg.core.block.convert` | op | Bulk-convert vanilla blocks in radius to a custom block |
| `/rpg wand [mode]` | `rpg.core.wand` | op | Give selection wand; `mode` ∈ `dungeon`, `spawner`, `entrance`, `region`, `hologram` |
| `/rpg status apply <effect> [player] [duration]` | `rpg.core.status.apply` | op | Apply a status effect |
| `/rpg status clear [player] [effect]` | `rpg.core.status.clear` | op | Clear status effects |
| `/rpg skill set <skill> <level\|xp> <amount> [player]` | `rpg.core.skill.set` | op | Adjust skill state |
| `/stats [player]` | `rpg.core.stats` / `rpg.core.stats.other` | true / op | Open 54-slot inventory stats GUI (rpg-core 1.7.0+; replaces old chat output) |
| `/skill [player]` | `rpg.core.skill` / `rpg.core.skill.other` | true / op | Open skills GUI |
| `/skill pin <skill>` | `rpg.core.skill.pin` | true | Pin a skill to the vanilla XP bar |
| `/effects` | `rpg.core.effects` | true | Open active status effects GUI |

## Economy (`rpg-economy`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/balance [player]` | `rpg.economy.balance` / `rpg.economy.balance.other` | true / op | View balance |
| `/pay <player> <amount>` | `rpg.economy.pay` | true | Send currency to another player |
| `/eco set <player> <amount>` | `rpg.economy.admin.set` | op | Set a player's balance |
| `/eco add <player> <amount>` | `rpg.economy.admin.add` | op | Add to a player's balance |
| `/eco remove <player> <amount>` | `rpg.economy.admin.remove` | op | Subtract from a player's balance |
| `/eco reset <player>` | `rpg.economy.admin.reset` | op | Reset balance to starting amount |
| `/baltop [page]` | `rpg.economy.baltop` | true | View richest players |

## Trade (`rpg-trade`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/trade <player>` | `rpg.trade.use` | true | Send a trade request (30 s expiry) |
| `/trade accept` | `rpg.trade.use` | true | Accept incoming trade request |
| `/trade deny` | `rpg.trade.use` | true | Decline incoming trade request |
| `/trade cancel` | `rpg.trade.use` | true | Cancel an in-progress trade |

## Chat (`rpg-chat`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/chat <global\|party\|guild>` | `rpg.chat.use.<channel>` | per-channel | Switch active chat channel |
| `/chat reload` | `rpg.chat.admin.reload` | op | Reload rpg-chat config |
| `/msg <player> <msg>` (`/tell`, `/w`) | `rpg.chat.msg` | true | Direct message |
| `/reply <msg>` (`/r`) | `rpg.chat.reply` | true | Reply to last DM |
| `/clearchat` (`/cc`) | `rpg.chat.clearchat` | op | Clear chat for everyone |
| `/mutechat` (`/mc`) | `rpg.chat.mutechat` | op | Toggle global chat mute on/off |

## HUD (`rpg-hud`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/hud toggle <scoreboard\|tablist\|actionbar>` | `rpg.hud.toggle` | true | Toggle a HUD element for yourself |
| `/hud reload` | `rpg.hud.reload` | op | Reload HUD config |

## Parties (`rpg-parties`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/party create` | `rpg.parties.create` | true | Create a party |
| `/party invite <player>` | `rpg.parties.invite` | true | Invite a player |
| `/party accept` | `rpg.parties.accept` | true | Accept invitation |
| `/party kick <player>` | `rpg.parties.kick` | true (owner/mod only) | Kick from party |
| `/party promote <player>` | `rpg.parties.promote` | true (owner only) | Promote to moderator |
| `/party demote <player>` | `rpg.parties.demote` | true (owner only) | Demote moderator |
| `/party leave` | `rpg.parties.leave` | true | Leave party |
| `/party disband` | `rpg.parties.disband` | true (owner only) | Disband |
| `/party list` | `rpg.parties.list` | true | Show member list |
| `/party reload` | `rpg.parties.admin.reload` | op | Reload rpg-parties config |
| `/chat party` | `rpg.chat.use.party` | true | Switch to party channel |

## Guilds (`rpg-guilds`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/guild create <name>` | `rpg.guilds.create` | true | Create a guild (requirements apply) |
| `/guild invite <player>` | `rpg.guilds.invite` | true | Invite |
| `/guild kick <player>` | `rpg.guilds.kick` | true (rank-gated) | Kick |
| `/guild promote <player>` | `rpg.guilds.promote` | true (rank-gated) | Promote |
| `/guild demote <player>` | `rpg.guilds.demote` | true (rank-gated) | Demote |
| `/guild leave` | `rpg.guilds.leave` | true | Leave |
| `/guild disband` | `rpg.guilds.disband` | true (owner) | Disband |
| `/guild info [guild]` | `rpg.guilds.info` | true | Show info |
| `/guild list` | `rpg.guilds.list` | true | List guilds |
| `/guild bank` | `rpg.guilds.bank` | true | Open guild bank GUI |
| `/guild bank upgrade` | `rpg.guilds.bank.upgrade` | true (rank-gated) | Upgrade bank tier |
| `/guild ranks` | `rpg.guilds.ranks` | true (owner) | Rename guild's rank slots |
| `/chat guild` | `rpg.chat.use.guild` | true | Switch to guild channel |

## Regions (`rpg-regions`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/region create <id>` | `rpg.regions.admin.create` | op | Create region from selection |
| `/region edit <id>` | `rpg.regions.admin.edit` | op | Open region editor GUI |
| `/region delete <id>` | `rpg.regions.admin.delete` | op | Delete region |
| `/region list` | `rpg.regions.admin.list` | op | List regions |
| `/region flag <id> <flag> <value>` | `rpg.regions.admin.flag` | op | Set a region flag |
| `/region info [id]` | `rpg.regions.admin.info` | op | Show region info / region at location |

## Dungeons (`rpg-dungeons`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/dungeon create <id>` | `rpg.dungeons.admin.create` | op | Begin dungeon authoring |
| `/dungeon save <id>` | `rpg.dungeons.admin.save` | op | Capture dungeon volume |
| `/dungeon edit <id>` | `rpg.dungeons.admin.edit` | op | Open dungeon editor GUI |
| `/dungeon delete <id>` | `rpg.dungeons.admin.delete` | op | Delete a dungeon template |
| `/dungeon list` | `rpg.dungeons.list` | true | List available dungeons |
| `/dungeon join <id>` | `rpg.dungeons.join` | true | Enter (solo or with party) |
| `/dungeon leave` | `rpg.dungeons.leave` | true | Exit current dungeon |
| `/dungeon admin abort <instance>` | `rpg.dungeons.admin.abort` | op | Force-end a running instance |

## Spawners (`rpg-core`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/spawner create <mobId>` | `rpg.spawners.admin.create` | op | Create a spawner at your location |
| `/spawner edit <id>` | `rpg.spawners.admin.edit` | op | Open spawner editor GUI |
| `/spawner delete <id>` | `rpg.spawners.admin.delete` | op | Delete |
| `/spawner list [near\|world]` | `rpg.spawners.admin.list` | op | List |
| `/spawner tp <id>` | `rpg.spawners.admin.tp` | op | Teleport to spawner |
| `/spawner show` | `rpg.spawners.admin.show` | op | Toggle particle markers on nearby spawners |

## Accessories (`rpg-accessories`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/accessories` (`/bag`) | `rpg.accessories.open` | true | Open your accessory bag |
| `/accessories upgrade` | `rpg.accessories.upgrade` | true | Upgrade bag tier |
| `/accessories reload` | `rpg.accessories.admin.reload` | op | Reload rpg-accessories config |

## Enchanting (`rpg-enchanting`)

Admin commands only — the station blocks open GUIs on right-click without a command.

| Command | Permission | Default | Description |
|---|---|---|---|
| `/enchanting reload` | `rpg.enchanting.admin.reload` | op | Reload all enchant/reforge/upgrade YAML |
| `/enchanting list` | `rpg.enchanting.admin.list` | op | List registered enchant/reforge IDs |
| `/enchanting give enchant <id>` | `rpg.enchanting.admin.give` | op | Give an enchant book |
| `/enchanting give reforge <id>` | `rpg.enchanting.admin.give` | op | Give the reforge stone item |
| `/enchanting give upgrade <id>` | `rpg.enchanting.admin.give` | op | Give the upgrade book item |

## NPCs (`rpg-npcs`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/npc create <id>` | `rpg.npcs.admin.create` | op | Create NPC at your location |
| `/npc delete <id>` | `rpg.npcs.admin.delete` | op | Delete an NPC |
| `/npc move <id>` | `rpg.npcs.admin.move` | op | Move NPC to your location |
| `/npc list` | `rpg.npcs.admin.list` | op | List all NPCs |
| `/npc reload` | `rpg.npcs.admin.reload` | op | Reload NPC YAMLs |
| `/npc setbehavior <id> <type>` | `rpg.npcs.admin.setbehavior` | op | Change NPC behavior type (`dialogue`/`shop`/`quest`/`banker`) |

## Holograms (`rpg-holograms`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/hologram create <id>` | `rpg.holograms.admin.create` | op | Create at your location |
| `/hologram edit <id>` | `rpg.holograms.admin.edit` | op | Edit GUI |
| `/hologram addline <id> <text>` | `rpg.holograms.admin.edit` | op | Add a line |
| `/hologram delete <id>` | `rpg.holograms.admin.delete` | op | Delete |
| `/hologram list [near\|world]` | `rpg.holograms.admin.list` | op | List holograms |
| `/hologram tp <id>` | `rpg.holograms.admin.tp` | op | TP to hologram |

## Quests (`rpg-quests`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/quest list` | `rpg.quests.use.list` | true | List available and active quests |
| `/quest accept <id>` | `rpg.quests.use.accept` | true | Accept a quest |
| `/quest abandon <id>` | `rpg.quests.use.abandon` | true | Abandon an active quest |
| `/quest progress` | `rpg.quests.use.progress` | true | Show progress on active quests |
| `/quest complete <questId> [player]` | `rpg.quests.admin.complete` | op | Force-complete a quest |
| `/quest reload` | `rpg.quests.admin.reload` | op | Reload quest YAMLs |

## Skill addons

Each skill addon adds:

| Command | Permission | Default | Description |
|---|---|---|---|
| `/<skill> reload` | `rpg.<skill>.reload` | op | Reload that skill's config |
| `/<skill> give <item>` | `rpg.<skill>.admin.give` | op | Give a content item from this addon |

Plus skill-specific admin commands documented in each addon's page.

## Related

- [Master permission reference](permissions.md)
- [Configuration overview](configuration.md)
