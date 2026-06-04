# Suite 16 — Dev Log (Archived)

← [Back to changelog index](../changelog.md)

!!! note "Archived format"
    This is the granular per-change development log from Suite 16.
    The new changelog format is a concise summary written once at suite transition — see the [changelog index](../changelog.md).

---

### rpg-core `0.1.1`
- Initial shipping build. Damage pipeline, stat registry, skills framework, block break handler, health display, content loaders (items/mobs/abilities/blocks/recipes), loot tables, admin spawners, natural spawning.

### rpg-mining `0.0.2`
- `MINING_FORTUNE` drop multiplier in `BlockBreakHandler.rollDrops()`.
- `MINING_SPEED` hold-to-break ticking live.
- `BREAKING_POWER` and `RequiredToolType` gates enforced.

### rpg-combat `0.0.1`
- Initial ship: damage XP from `PostDamageEvent` proportional to damage dealt. `COMBAT_WISDOM` scaling.

### rpg-guilds `0.1.0`
- Persistent guilds via DataStore. Guild XP from member skill gains (`SkillXpAwardEvent`). Guild level curve + per-stat perk injection (`StatRecalcEvent`). `/guild create|invite|accept|kick|promote|demote|leave|disband|info|list|deposit|withdraw`.

### rpg-foraging `0.0.1` / rpg-farming `0.0.1`
- XP on log/crop harvest. `FORAGING_WISDOM` / `FARMING_WISDOM` scaling.

### rpg-enchanting `0.0.2`
- Shipped: custom enchants, reforges, item upgrades, anvil GUI.

### rpg-hud `0.1.0`
- Shipped: scoreboard, tablist, action bar, `TextDisplay` nametags above player head.

### rpg-dungeons, rpg-npcs, rpg-quests, rpg-economy, rpg-chat, rpg-accessories, rpg-holograms, rpg-regions, rpg-parties
- Initial ships — see individual addon pages for feature status.
