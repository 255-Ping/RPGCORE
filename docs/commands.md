# Master command reference

> **Status:** Working (synced with plugin.ymls as of suite 21 — updated with holograms 0.0.5, core 1.10.18)

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
| `/rpg items` | `rpg.core.items.browse` | op | Open the Item Browser GUI (paginated; filter by type / rarity / search; shift-click to give ×64) |
| `/rpg mob spawn <id> [count]` | `rpg.core.mob.spawn` | op | Spawn a custom mob at your location |
| `/rpg ability cast <id>` | `rpg.core.ability.cast` | op | Debug-cast a custom ability |
| `/rpg block give <id> [amount]` | `rpg.core.block.give` | op | Give a placeable form of a custom block |
| `/rpg block convert <radius> <fromMaterial> <toBlockId>` | `rpg.core.block.convert` | op | Bulk-convert vanilla blocks in radius to a custom block |
| `/rpg wand [mode]` | `rpg.core.wand` | op | Give selection wand; `mode` ∈ `dungeon`, `spawner`, `entrance`, `region`, `hologram` |
| `/rpg fix [player]` | `rpg.core.fix` | op | Remove orphaned movement-speed modifiers, clear in-memory status effects, and force a full attribute resync — use when a player reports permanent slowness |
| `/rpg status apply <effect> [player] [duration]` | `rpg.core.status.apply` | op | Apply a status effect |
| `/rpg status clear [player] [effect]` | `rpg.core.status.clear` | op | Clear status effects |
| `/rpg skill set <skill> <level\|xp> <amount> [player]` | `rpg.core.skill.set` | op | Adjust skill state |
| `/stats [player]` | `rpg.core.stats` / `rpg.core.stats.other` | true / op | Open 54-slot inventory stats GUI (rpg-core 1.7.0+; replaces old chat output) |
| `/skill [player]` | `rpg.core.skill` / `rpg.core.skill.other` | true / op | Open skills GUI |
| `/skill pin <skill>` | `rpg.core.skill.pin` | true | Pin a skill to the vanilla XP bar |
| `/effects` | `rpg.core.effects` | true | Open active status effects GUI |
| `/menu` (alias `/m`) | `rpg.core.menu` | true | Open the Main Menu hub GUI (also opened by right-clicking the persistent menu item) |
| `/achievements [player]` | `rpg.core.achievements` / `rpg.core.achievements.others` | true / op | Open the Achievements GUI |
| `/profile [player]` | `rpg.profile.view` / `rpg.profile.view.others` | true / op | Open 54-slot profile GUI (skill levels, balance, recent achievements, stats link) |
| `/adventure` (alias `/adv`) | `rpg.adventure.view` | true | Open the Adventure hub GUI (quests, economy, achievements) |
| `/social` | `rpg.social.view` | true | Open the Social hub GUI (friends, party, guild, mail) |
| `/settings` (aliases `/preferences`, `/prefs`) | `rpg.settings.view` | true | Open the player Settings GUI |

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
| `/region define <id>` | `rpg.regions.admin.define` | op | Define a region from current wand selection |
| `/region delete <id>` | `rpg.regions.admin.delete` | op | Delete region |
| `/region list` | `rpg.regions.admin.list` | op | List regions |
| `/region flag <id> <flag> <value>` | `rpg.regions.admin.flag` | op | Set a region flag |
| `/region info [id]` | `rpg.regions.admin.info` | op | Show region info / region at location |

## Dungeons (`rpg-dungeons`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/dungeon create <id>` | `rpg.dungeons.admin.create` | op | Register a new dungeon template |
| `/dungeon delete <id>` | `rpg.dungeons.admin.delete` | op | Delete a dungeon template |
| `/dungeon list` | `rpg.dungeons.admin.list` | op | List registered dungeons |
| `/dungeon setentrance <id>` | `rpg.dungeons.admin.set` | op | Set dungeon entrance at current location |
| `/dungeon setexit <id>` | `rpg.dungeons.admin.set` | op | Set dungeon exit at current location |
| `/dungeon setspawn <id>` | `rpg.dungeons.admin.set` | op | Set player spawn inside the dungeon |
| `/dungeon enter <id>` | `rpg.dungeons.use.enter` | true | Enter a dungeon (solo or with party) |
| `/dungeon leave` | `rpg.dungeons.use.leave` | true | Exit current dungeon |
| `/dungeon reload` | `rpg.dungeons.admin.reload` | op | Reload dungeon YAMLs |

## Spawners (`rpg-core`)

| Command | Permission | Default | Description |
|---|---|---|---|
| `/spawner create <id> <mobId>` | `rpg.spawners.admin.create` | op | Create a spawner at your location |
| `/spawner edit <id>` | `rpg.spawners.admin.edit` | op | Open 54-slot GUI editor for all spawner fields |
| `/spawner set <id> <field> <value>` | `rpg.spawners.admin.edit` | op | Set a spawner field directly (`max-alive`, `cooldown-ticks`, `spawn-radius`, `continuous`, `min-level`, `max-level`) |
| `/spawner delete <id>` | `rpg.spawners.admin.delete` | op | Delete a spawner |
| `/spawner list` | `rpg.spawners.admin.list` | op | List all spawners |
| `/spawner tp <id>` | `rpg.spawners.admin.tp` | op | Teleport to a spawner |

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
| `/npc create <id> [name]` | `rpg.npcs.admin.create` | op | Create NPC at your location |
| `/npc delete <id>` | `rpg.npcs.admin.delete` | op | Delete an NPC |
| `/npc move <id>` | `rpg.npcs.admin.move` | op | Move NPC to your location |
| `/npc list` | `rpg.npcs.admin.list` | op | List all NPCs |
| `/npc info <id>` | `rpg.npcs.admin.list` | op | Show all settings for an NPC |
| `/npc reload` | `rpg.npcs.admin.reload` | op | Reload NPC YAMLs |
| `/npc setbehavior <id> <dialogue\|shop\|quest\|banker>` | `rpg.npcs.admin.setbehavior` | op | Change NPC behavior type |
| `/npc setentitytype <id> <type>` | `rpg.npcs.admin.setentitytype` | op | Change the vanilla EntityType |
| `/npc setstyle <id> <entity\|player>` | `rpg.npcs.admin.setstyle` | op | Switch body style |
| `/npc setskin <id> <playerName>` | `rpg.npcs.admin.setskin` | op | Apply player skin (fetched from Mojang) |
| `/npc setlook <id> <true\|false>` | `rpg.npcs.admin.setlook` | op | Toggle look-at-player behavior |
| `/npc dialogue <id> <add\|set\|remove\|clear\|list> [args]` | `rpg.npcs.admin.dialogue` | op | Edit dialogue lines in-game |
| `/npc shop <id> <add\|remove\|list\|clear> [args]` | `rpg.npcs.admin.shop` | op | Edit shop entries in-game |

## Holograms (`rpg-holograms`)

Alias: `/holo` works for all subcommands.

| Command | Permission | Default | Description |
|---|---|---|---|
| `/holograms create <id> <text>` | `rpg.holograms.admin.create` | op | Create at your location with initial text |
| `/holograms delete <id>` | `rpg.holograms.admin.delete` | op | Delete |
| `/holograms list` | `rpg.holograms.admin.list` | op | List all holograms |
| `/holograms info <id>` | `rpg.holograms.admin.edit` | op | Print all properties of a hologram |
| `/holograms tp <id>` | `rpg.holograms.admin.tp` | op | TP to hologram |
| `/holograms move <id>` | `rpg.holograms.admin.move` | op | Move hologram to your location |
| `/holograms line add <id> <text>` | `rpg.holograms.admin.edit` | op | Append a line (or animation frame) |
| `/holograms line set <id> <index> <text>` | `rpg.holograms.admin.edit` | op | Replace a specific line |
| `/holograms line remove <id> <index>` | `rpg.holograms.admin.edit` | op | Remove a line by index |
| `/holograms line list <id>` | `rpg.holograms.admin.edit` | op | List all lines with their index numbers |
| `/holograms set <id> animated <true\|false>` | `rpg.holograms.admin.edit` | op | Enable/disable frame animation |
| `/holograms set <id> frameinterval <ticks>` | `rpg.holograms.admin.edit` | op | Ticks between animation frame advances |
| `/holograms reload` | `rpg.holograms.admin.reload` | op | Reload all hologram YAMLs |

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
