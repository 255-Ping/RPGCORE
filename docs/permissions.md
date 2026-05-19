# Master permission reference

> **Status:** Planned

Permission nodes follow `rpg.<module>.<command>[.<sub>]`. Self-use defaults to true; admin defaults to op; moderator-tier nodes are noted explicitly.

For the full command list, see [commands.md](commands.md).

---

## Conventions

- `.other` suffix on a node = same command but targeting another player (always op-default).
- `.admin.*` group = administrative actions for that module (always op-default).
- Self-use commands (`/balance`, `/stats`, `/effects`, `/party *`, `/guild *`, `/chat *`, `/msg`) default-true so players can use them without explicit LuckPerms setup.

## Core

| Node | Default | Description |
|---|---|---|
| `rpg.core.version` | true | List loaded modules |
| `rpg.core.reload-all` | op | Reload every plugin |
| `rpg.core.reload` | op | Reload only core |
| `rpg.core.item.give` | op | Give a custom item |
| `rpg.core.mob.spawn` | op | Spawn a custom mob |
| `rpg.core.ability.cast` | op | Debug-cast a custom ability |
| `rpg.core.block.give` | op | Give a placeable custom block |
| `rpg.core.block.convert` | op | Bulk-convert vanilla blocks |
| `rpg.core.wand` | op | Get the selection wand |
| `rpg.core.status.apply` | op | Apply a status effect |
| `rpg.core.status.clear` | op | Clear a status effect |
| `rpg.core.skill.set` | op | Adjust skill state |
| `rpg.core.stats` | true | Open own stats GUI |
| `rpg.core.stats.other` | op | Open another player's stats GUI |
| `rpg.core.skill` | true | Open own skill GUI |
| `rpg.core.skill.other` | op | Open another player's skill GUI |
| `rpg.core.skill.pin` | true | Pin a skill to the vanilla XP bar |
| `rpg.core.effects` | true | Open effects GUI |

## Death tier examples (configurable)

| Node | Description |
|---|---|
| `rpg.death.default` | Default death rule |
| `rpg.death.keep-all` | Keep everything on death |
| `rpg.death.partial` | Partial drop (e.g., 60%) |
| `rpg.death.full` | Drop everything |

These names are illustrative — actual node names come from your `core/config.yml` `death-rules.groups[*].permission`. See [core/damage](core/damage.md).

## Economy

| Node | Default | Description |
|---|---|---|
| `rpg.economy.balance` | true | Check own balance |
| `rpg.economy.balance.other` | op | Check another player's balance |
| `rpg.economy.pay` | true | Send currency |
| `rpg.economy.baltop` | true | Leaderboard |
| `rpg.economy.admin.set` | op | Set balance |
| `rpg.economy.admin.add` | op | Add balance |
| `rpg.economy.admin.remove` | op | Remove balance |
| `rpg.economy.admin.reset` | op | Reset balance |

## Chat

| Node | Default | Description |
|---|---|---|
| `rpg.chat.use.global` | true | Use global channel |
| `rpg.chat.use.staff` | op | Use staff channel |
| `rpg.chat.use.party` | true | Use party channel |
| `rpg.chat.use.guild` | true | Use guild channel |
| `rpg.chat.msg` | true | Direct message |
| `rpg.chat.reply` | true | Reply |
| `rpg.chat.clearchat` | op | Clear chat |
| `rpg.chat.mutechat` | op | Toggle global chat |
| `rpg.chat.mute` | op | Mute a player |
| `rpg.chat.unmute` | op | Unmute |
| `rpg.chat.mute.bypass` | op | Bypass mutechat |
| `rpg.chat.slowmode` | op | Slowmode toggle |
| `rpg.chat.socialspy` | op | View DMs |

## HUD

| Node | Default | Description |
|---|---|---|
| `rpg.hud.toggle` | true | Toggle HUD elements |
| `rpg.hud.reload` | op | Reload HUD config |

## Parties

| Node | Default | Description |
|---|---|---|
| `rpg.parties.create` | true | Create party |
| `rpg.parties.invite` | true | Invite |
| `rpg.parties.accept` | true | Accept |
| `rpg.parties.kick` | true | Kick (rank-gated at runtime) |
| `rpg.parties.promote` | true | Promote (owner only at runtime) |
| `rpg.parties.demote` | true | Demote (owner only at runtime) |
| `rpg.parties.leave` | true | Leave |
| `rpg.parties.disband` | true | Disband (owner only at runtime) |
| `rpg.parties.list` | true | List members |

## Guilds

| Node | Default | Description |
|---|---|---|
| `rpg.guilds.create` | true | Create (requirements apply) |
| `rpg.guilds.invite` | true | Invite (rank-gated) |
| `rpg.guilds.kick` | true | Kick (rank-gated) |
| `rpg.guilds.promote` | true | Promote (rank-gated) |
| `rpg.guilds.demote` | true | Demote (rank-gated) |
| `rpg.guilds.leave` | true | Leave |
| `rpg.guilds.disband` | true | Disband (owner only) |
| `rpg.guilds.info` | true | Info |
| `rpg.guilds.list` | true | List |
| `rpg.guilds.bank` | true | Open bank (rank-gated for deposit/withdraw at runtime) |
| `rpg.guilds.bank.upgrade` | true | Upgrade bank tier (rank-gated) |
| `rpg.guilds.ranks` | true | Rename rank slots (owner only) |

## Regions

| Node | Default | Description |
|---|---|---|
| `rpg.regions.admin.create` | op | Create region |
| `rpg.regions.admin.edit` | op | Edit region |
| `rpg.regions.admin.delete` | op | Delete region |
| `rpg.regions.admin.list` | op | List regions |
| `rpg.regions.admin.flag` | op | Set region flag |
| `rpg.regions.admin.info` | op | Region info |

## Dungeons

| Node | Default | Description |
|---|---|---|
| `rpg.dungeons.admin.create` | op | Create dungeon template |
| `rpg.dungeons.admin.save` | op | Save dungeon |
| `rpg.dungeons.admin.edit` | op | Edit dungeon |
| `rpg.dungeons.admin.delete` | op | Delete dungeon |
| `rpg.dungeons.admin.abort` | op | Force-end an instance |
| `rpg.dungeons.list` | true | List dungeons |
| `rpg.dungeons.join` | true | Join a dungeon |
| `rpg.dungeons.leave` | true | Leave |

## Spawners

| Node | Default | Description |
|---|---|---|
| `rpg.spawners.admin.create` | op | Create |
| `rpg.spawners.admin.edit` | op | Edit |
| `rpg.spawners.admin.delete` | op | Delete |
| `rpg.spawners.admin.list` | op | List |
| `rpg.spawners.admin.tp` | op | TP |
| `rpg.spawners.admin.show` | op | Toggle particle markers |

## Accessories

| Node | Default | Description |
|---|---|---|
| `rpg.accessories.open` | true | Open bag |
| `rpg.accessories.upgrade` | true | Upgrade tier |

## Enchanting

| Node | Default | Description |
|---|---|---|
| `rpg.enchanting.open` | true | Open enchant GUI |
| `rpg.enchanting.reforge` | true | Reforge GUI |
| `rpg.enchanting.anvil` | true | Anvil GUI |

## NPCs

| Node | Default | Description |
|---|---|---|
| `rpg.npcs.admin.create` | op | Create NPC |
| `rpg.npcs.admin.edit` | op | Edit NPC |
| `rpg.npcs.admin.delete` | op | Delete |
| `rpg.npcs.admin.list` | op | List |
| `rpg.npcs.admin.tp` | op | TP |
| `rpg.npcs.admin.move` | op | Move |

## Holograms

| Node | Default | Description |
|---|---|---|
| `rpg.holograms.admin.create` | op | Create |
| `rpg.holograms.admin.edit` | op | Edit (incl. add lines) |
| `rpg.holograms.admin.delete` | op | Delete |
| `rpg.holograms.admin.list` | op | List |
| `rpg.holograms.admin.tp` | op | TP |

## Quests

| Node | Default | Description |
|---|---|---|
| `rpg.quests.open` | true | Open quest log |
| `rpg.quests.admin.give` | op | Force-give |
| `rpg.quests.admin.reset` | op | Reset |
| `rpg.quests.admin.complete` | op | Force-complete |

## Skill addons

Each skill addon registers these:

| Pattern | Default | Description |
|---|---|---|
| `rpg.<skill>.reload` | op | Reload that addon |
| `rpg.<skill>.admin.give` | op | Give content from that addon |

Skill-specific permissions are listed in [addons/skills.md](addons/skills.md).
