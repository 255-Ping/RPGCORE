# Cookbook — Copy-Paste Examples

> One complete, working example of every content type. Drop any of these into your plugins folder, reload, and test. No invented syntax — every field maps directly to the schema docs.

---

## Items

### Starter Sword

```yaml
# plugins/rpg-core/items/starter.yml
training_blade:
  MinecraftItem: wooden_sword
  Type: SWORD
  DisplayName: "&fTraining Blade"
  Rarity: "&f&lCOMMON"
  Lore:
  - "&7A worn sword issued to new adventurers."
  Stats:
    damage: 40
    strength: 10
    crit_chance: 5
```

---

### Starter Bow (with ammo item)

```yaml
recruit_bow:
  MinecraftItem: bow
  Type: BOW
  DisplayName: "&fRecruit Bow"
  Rarity: "&f&lCOMMON"
  Lore:
  - "&7Reliable and easy to aim."
  Stats:
    damage: 35
    crit_chance: 8
    crit_damage: 20
  AmmoType: iron_arrow           # requires the item below in inventory to fire
  InfiniteAmmo: false

iron_arrow:
  MinecraftItem: arrow
  Type: MATERIAL
  DisplayName: "&7Iron Arrow"
  Rarity: "&f&lCOMMON"
  Lore:
  - "&7Standard ammunition."
```

---

### Wand with Ability

```yaml
apprentice_wand:
  MinecraftItem: blaze_rod
  Type: WAND
  DisplayName: "&aApprentice Wand"
  Rarity: "&a&lUNCOMMON"
  Lore:
  - "&7A wand used by novice mages."
  Stats:
    damage: 30
    intelligence: 25
    max_mana: 50
  Abilities:
  - mana_bolt{}
```

```yaml
# plugins/rpg-core/abilities/spells.yml
mana_bolt:
  Name: "Mana Bolt"
  Description:
  - "&7Fire a bolt of arcane energy."
  Cooldown: 20
  ManaCost: 15
  AbilitySequence:
  - projectile{speed=2.0, gravity=0.0, damage_multiplier=1.2, particle=CRIT}
  - damage{type=magic}
```

---

### Full Armor Set

```yaml
# All four pieces — wear together for +27 defense, +50 max_health
novice_helmet:
  MinecraftItem: leather_helmet
  Type: ARMOR
  ArmorSlot: HELMET
  DisplayName: "&fNovice Helmet"
  Rarity: "&f&lCOMMON"
  Stats:
    defense: 5
    max_health: 10

novice_chestplate:
  MinecraftItem: leather_chestplate
  Type: ARMOR
  ArmorSlot: CHEST
  DisplayName: "&fNovice Chestplate"
  Rarity: "&f&lCOMMON"
  Stats:
    defense: 10
    max_health: 20

novice_leggings:
  MinecraftItem: leather_leggings
  Type: ARMOR
  ArmorSlot: LEGS
  DisplayName: "&fNovice Leggings"
  Rarity: "&f&lCOMMON"
  Stats:
    defense: 8
    max_health: 15

novice_boots:
  MinecraftItem: leather_boots
  Type: ARMOR
  ArmorSlot: BOOTS
  DisplayName: "&fNovice Boots"
  Rarity: "&f&lCOMMON"
  Stats:
    defense: 4
    max_health: 5
```

---

### Consumable (Healing Potion)

```yaml
healing_potion:
  MinecraftItem: potion
  Type: CONSUMABLE
  DisplayName: "&cHealing Potion"
  Rarity: "&f&lCOMMON"
  Lore:
  - "&7Restores health over a short time."
  CustomModelData: 10101
  OnConsume:
    Effects:
    - { effect: regen, level: 2, duration: 100 }
```

---

### Upgrade Book

```yaml
# Applied via the custom anvil GUI — adds stats to a weapon, up to MaxStacks times
hot_potato_book:
  MinecraftItem: book
  Type: UPGRADE
  DisplayName: "&6Hot Potato Book"
  Rarity: "&6&lRARE"
  Lore:
  - "&7Slightly improves a weapon or armor piece."
  Upgrade:
    AppliesTo: [SWORD, BOW, WAND, ARMOR]
    MaxStacks: 10
    Effect:
      stats:
        strength: 2
        max_health: 4
    LoreAdd: "&7(+%stacks% Hot Potato)"
```

---

### Accessory

```yaml
zombie_talisman:
  MinecraftItem: zombie_head
  Type: ACCESSORY
  DisplayName: "&aZombie Talisman"
  Rarity: "&a&lUNCOMMON"
  Lore:
  - "&7Grants vitality drawn from the undead."
  Stats:
    max_health: 20
    defense: 5
  Accessory:
    Family: zombie_talisman      # only the highest copy of this family counts
```

---

## Mobs

### Basic Hostile Mob

```yaml
# plugins/rpg-core/mobs/forest.yml
forest_goblin:
  MinecraftMob: zombie
  DisplayName: "&2Forest Goblin"
  Health: 120
  Damage: 8
  Armor: 0
  AI:
    profile: aggressive
    aggression-range: 14
    attack-range: 2
    leash-range: 30
    leash-action: return
  Equipment:
  - HAND training_blade
  Abilities:
  - apply_status{id=poison, level=1, duration=80} ~onHit
  LootTable:
    attribution: weighted-by-damage
    roll-mode: per-player
    coin-drop: { min: 5, max: 20 }
    rolls:
    - { item: goblin_ear, chance: 35.0, min: 1, max: 1 }
    - { item: training_blade, chance: 5.0, min: 1, max: 1 }
```

```yaml
# The drop item — a quest/crafting material
goblin_ear:
  MinecraftItem: rabbit_foot
  Type: MATERIAL
  DisplayName: "&2Goblin Ear"
  Rarity: "&f&lCOMMON"
  Lore:
  - "&7Smells terrible. Might be useful."
```

---

### Ranged Mob

```yaml
arcane_apprentice:
  MinecraftMob: skeleton
  DisplayName: "&dArcane Apprentice"
  Health: 80
  Damage: 6
  Armor: 0
  AI:
    profile: ranged_kiter
    aggression-range: 20
    attack-range: 12
  Equipment:
  - HAND apprentice_wand
  Abilities:
  - mana_bolt{} ~onTimer:60
  LootTable:
    attribution: last-hit
    roll-mode: per-player
    coin-drop: { min: 8, max: 25 }
    rolls:
    - { item: mana_dust, chance: 50.0, min: 1, max: 2 }
    - { item: apprentice_wand, chance: 3.0, min: 1, max: 1 }
```

---

## Abilities

### Projectile → Damage chain

```yaml
# plugins/rpg-core/abilities/spells.yml
fireball:
  Name: "Fireball"
  Description:
  - "&7Hurl a ball of fire at your target."
  Cooldown: 30
  ManaCost: 20
  AbilitySequence:
  - projectile{speed=1.5, gravity=0.02, damage_multiplier=1.0, particle=FLAME}
  - explode{radius=2.5, damage_multiplier=0.5, particle=FLAME}
```

### AoE slam

```yaml
ground_slam:
  Name: "Ground Slam"
  Description:
  - "&7Slam the ground, damaging all nearby enemies."
  Cooldown: 60
  AbilitySequence:
  - particles{type=BLOCK_CRACK, count=30, spread=2.0}
  - sound{key=entity.generic.explode, volume=1.0, pitch=0.8}
  - aoe{radius=5.0, damage_multiplier=1.2}
```

### Heal + teleport blink

```yaml
phase_step:
  Name: "Phase Step"
  Description:
  - "&7Blink forward and heal yourself."
  Cooldown: 100
  ManaCost: 25
  AbilitySequence:
  - teleport{distance=8, mode=eyeline}
  - heal{amount=30, target=caster}
  - particles{type=PORTAL, count=20, spread=0.5}
```

---

## Status Effects

```yaml
# plugins/rpg-core/status-effects/custom.yml

# Damage-over-time debuff
burning:
  display: "&cBurning"
  category: debuff
  stacking: refresh
  hidden: false
  tick:
    interval-ticks: 20
    action: damage
    amount: 8
    source: burning
  on-apply:
    particles:
      type: FLAME
      count: 10
      spread: 0.3

# Stat-modifier buff
haste:
  display: "&eHaste"
  category: buff
  stacking: take-max
  hidden: false
  stat-modifiers:
    attack_speed:
      kind: percent
      value: 25
  on-apply:
    sound:
      key: entity.experience_orb.pickup
      volume: 0.8
      pitch: 1.4
```

---

## Enchants

### Stat enchant

```yaml
# plugins/rpg-enchanting/enchants/weapons.yml
sharpness:
  display: "&7Sharpness"
  applies-to: [SWORD, AXE]
  max-level: 5
  levels:
    1: { stats: { strength: 8 } }
    2: { stats: { strength: 18 } }
    3: { stats: { strength: 30 } }
    4: { stats: { strength: 44 } }
    5: { stats: { strength: 60 } }
  conflicts: []
```

### Defensive stat enchant

```yaml
protection:
  display: "&9Protection"
  applies-to: [ARMOR]
  max-level: 5
  levels:
    1: { stats: { defense: 5 } }
    2: { stats: { defense: 12 } }
    3: { stats: { defense: 20 } }
    4: { stats: { defense: 30 } }
    5: { stats: { defense: 42 } }
  conflicts: []
```

---

## Reforges

```yaml
# plugins/rpg-enchanting/reforges/basic.yml

sharp:
  display: "&7Sharp"
  applies-to: [SWORD, AXE]
  modes:
    pay-currency-random: true
    stone: true
  stone-item-id: sharp_reforge_stone
  effect:
    stats:
      crit_chance: 8
      crit_damage: 15

heavy:
  display: "&cHeavy"
  applies-to: [SWORD, AXE]
  modes:
    pay-currency-random: true
    stone: true
  stone-item-id: heavy_reforge_stone
  effect:
    stats:
      strength: 20
      crit_chance: -3

fortified:
  display: "&6Fortified"
  applies-to: [ARMOR]
  modes:
    pay-currency-random: true
    stone: true
  stone-item-id: fortified_reforge_stone
  effect:
    stats:
      defense: 25
      max_health: 60
```

---

## Related

- [Items schema](items.md)
- [Mobs schema](mobs.md)
- [Abilities reference](abilities.md)
- [Status effects](../core/status-effects.md)
- [Enchanting](../addons/enchanting.md)
- [Stats reference](../stats.md)
- [End-to-end walkthrough](first-mob.md)
