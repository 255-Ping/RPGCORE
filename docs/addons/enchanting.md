# Enchanting (`rpg-enchanting`)

> **Status:** v0.0.2 — shipped. Custom enchants, reforges, item upgrades all functional. GUIs open from custom enchanting/anvil blocks (admins must copy `blocks-example.yml` into `plugins/rpg-core/blocks/`). Vanilla enchanting-table + anvil + smithing-table outputs suppressed.

One addon bundling four related "improve your gear" features. Each sub-feature is individually toggleable.

1. **Enchanting skill** — XP from applying enchants
2. **Enchanting mechanic** — custom enchants applied at a custom enchanting block
3. **Reforge mechanic** — apply a reforge to an item for stat bundles (random or via consumable stones)
4. **Item upgrades** — apply `UPGRADE`-type items via a custom anvil block

Vanilla enchanting tables and anvils are cancelled (per [vanilla suppression](../core/vanilla-suppression.md)).

## Config

`plugins/rpg-enchanting/config.yml`:

```yaml
# Per-feature toggles.
features:
  enchanting: true
  reforges: true
  upgrades: true

# Station block IDs to listen on (legacy — prefer setting StationType in block YAML instead).
stations:
  enchanting-block: rpg_enchanting_table
  anvil-block: rpg_custom_anvil

# Whether to also intercept the vanilla enchanting table / anvil (default true).
intercept-vanilla-enchanting: true
intercept-vanilla-anvil: true

# Skill XP awarded on successful operation.
xp:
  per-enchant: 25
  per-reforge: 15
  per-upgrade: 40

# Whether enchants/reforges/upgrades require currency in addition to materials.
charge-currency: true

# Mirror of rpg-core vanilla-suppression flags (convenience overrides).
suppress:
  enchanting-table: true
  anvil: true
```

## Custom enchants

Files under `plugins/rpg-enchanting/enchants/<file>.yml`:

```yaml
sharpness:
  display: "&7Sharpness"
  applies-to: [SWORD]
  max-level: 7
  levels:
    1: { stats: { strength: 5 } }
    2: { stats: { strength: 12 } }
    3: { stats: { strength: 20 } }
  apply-requirements:
    enchanting-level: 0          # min skill level to apply this enchant
  conflicts: []                  # mutually exclusive with other enchants
```

## Reforges

Files under `plugins/rpg-enchanting/reforges/<file>.yml`:

```yaml
sharp:
  display: "&7Sharp"
  applies-to: [SWORD]
  modes:                         # which application modes are supported
    pay-currency-random: true
    stone: true
  stone-item-id: sharp_reforge_stone   # the consumable that applies this reforge
  effect:
    stats:
      crit_chance: 5
      crit_damage: 10
```

## Upgrades

`UPGRADE`-type items, defined in normal `items/` YAML. See [items.md](../content/items.md#upgrade).

Applied via the anvil GUI: drop the target item + the upgrade item, click to consume the upgrade and apply its effect. `MaxStacks` per-target enforced.

## Custom station blocks

Both the enchanting station and anvil are custom blocks (define them in `blocks/`):

```yaml
custom_enchanting_table:
  MinecraftBlock: enchanting_table
  Toughness: 200
  Interactable: true
  StationType: enchanting
  Drops:
  - vanilla:enchanting_table 1

custom_anvil:
  MinecraftBlock: anvil
  Toughness: 300
  Interactable: true
  StationType: anvil
  Drops:
  - vanilla:anvil 1
```

## Commands

| Command | Permission |
|---|---|
| `/enchanting reload` | `rpg.enchanting.admin.reload` |
| `/enchanting list` | `rpg.enchanting.admin.list` |
| `/enchanting give <id>` | `rpg.enchanting.admin.give` |

Station blocks open the enchanting/anvil GUIs on right-click. The commands are admin utilities.

## Stats

- `enchanting_wisdom` — XP bonus
- `enchanting_luck` — quality of random reforge / upgrade outcomes

## Related

- [Skills framework](../core/skills.md)
- [Items (UPGRADE)](../content/items.md#upgrade)
- [Blocks](../content/blocks.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
