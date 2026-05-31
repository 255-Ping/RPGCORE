# Content Creator Learning Path

A recommended reading order for new content authors. Each step builds on the previous — follow it top to bottom and you'll understand how the whole system fits together before you start layering complexity.

---

## Path overview

```
Quick Start → Items → Abilities → Status Effects → Mobs
    → Loot Tables → Spawning → Quests → Patterns → Cookbook
```

---

## 1. [Quick Start](quickstart.md)

**Read first — always.** Walks you through creating a weapon, a mob that drops it, an ability on that mob, a spawner, and a kill quest. By the end you've touched every major content type at least once, even if you don't fully understand it yet.

*Prerequisites: rpg-core installed, Paper server running.*

---

## 2. [Items](items.md)

Everything starts with items. Learn the full `Type` list (SWORD, WAND, BOW, ARMOR, MATERIAL, CONSUMABLE, UPGRADE, ACCESSORY), how stats go on items, how `Abilities:` are attached, and what `Rarity` and `Tradeable` do. Items are referenced everywhere else — mobs equip them, loot tables drop them, quests reward them.

*Before moving on: create one item of each type so you see how lore renders.*

---

## 3. [Abilities](abilities.md)

Understand the DSL (`effect{k=v} effect{k=v}`), the custom ability YAML format, and how `AbilityContext` carries state between effects. Abilities attach to items and mobs alike — the system is the same for both.

After the overview, read the [Effects Reference](ability-effects.md) to see what every built-in effect reads, writes, and expects from context.

*Before moving on: write an ability that chains at least three effects (e.g. `beam → damage → apply_status`).*

---

## 4. [Status Effects](../core/status-effects.md)

Status effects are applied by abilities and block/item interactions. Learn the YAML format for defining them, the `StackingStrategy` options, and how `~onHurt`/`~onExpire` hooks work. Understanding this before mobs matters because mob abilities frequently apply statuses.

---

## 5. [Mobs](mobs.md)

Custom mobs reference items (equipment), abilities (with trigger syntax), and loot tables (inline or external). Now that you know how all three work, the mob YAML will make complete sense. The ability trigger section (`~onTimer:N`, `~onHit`, `~onHurt`, `~onSpawn`, `~onDeath`) is what makes mobs feel alive.

*Before moving on: create a mob that equips a custom weapon, fires a timed AoE ability, and drops custom loot.*

---

## 6. [Loot Tables](loot-tables.md)

Inline loot in mobs is fine for simple cases. External loot tables let multiple mobs share the same drop pool, support attribution modes (solo vs. shared), and can be referenced from dungeon chests and loot-chest blocks. Learn the roll modes (`FIRST`, `ALL`, `WEIGHTED`), `Guaranteed:` drops, and the new `currency-rolls:` section.

---

## 7. [Spawning](spawning.md)

Two systems: **admin spawners** (`/spawner create`) for hand-placed spawn points, and **natural-spawn rules** for replacing vanilla mob spawns with custom ones. Natural spawning is how you populate an entire biome without placing a spawner per chunk.

---

## 8. [Quests](../addons/quests.md)

Quests consume everything above — kill objectives reference mob IDs, item objectives reference item IDs, reward blocks reference items and currency amounts. Learn the objective types (`kill_mob`, `collect_item`, `reach_location`, `interact_npc`) and how `RequiredQuests:` chains them into progressions.

*Requires `rpg-quests` installed.*

---

## 9. [Common Patterns](patterns.md)

A library of named, reusable ability recipes — fireball, life steal, blink, AoE slam, proxy mine, beam burst. Once you understand the effects reference, these show you how to combine primitives efficiently. Use them as starting points rather than building from scratch.

---

## 10. [Cookbook](cookbook.md)

Full worked examples for every content type. Each entry is a ready-to-paste YAML block with every field filled in and annotated. Useful when you want a complete reference rather than a tutorial explanation.

---

## After the path

Once you're comfortable with all of the above, the natural next destinations are:

| Goal | Where to go |
|---|---|
| Gear progression (tiers, stat budgets) | [Progression Guide](progression-guide.md) |
| Dungeons (instanced rooms, entry requirements) | [Dungeons](../addons/dungeons.md) |
| NPC shops, dialogue, banker | [NPCs](../addons/npcs.md) |
| Enchantments, reforges, upgrades | [Enchanting](../addons/enchanting.md) |
| Writing a Java addon against the API | [Developer Guide](../development.md) |
