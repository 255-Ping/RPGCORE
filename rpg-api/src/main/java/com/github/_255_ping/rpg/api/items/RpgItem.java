package com.github._255_ping.rpg.api.items;

import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public interface RpgItem {
    String id();
    String displayName();
    ItemType type();
    Rarity rarity();
    Material material();
    int customModelData();
    Map<Stat, Double> stats();
    List<AbilityInvocation> abilities();
    List<String> extraLore();

    ItemStack toItemStack();

    // ── Attack cooldown ────────────────────────────────────────────────────────

    /**
     * Ticks between full-charge attacks. Sets the {@code generic.attack_speed} attribute
     * when the item is equipped. {@code 0} = use Minecraft's default (4 attacks/second).
     * Set via {@code AttackCooldown: N} in item YAML.
     */
    default int attackCooldownTicks() { return 0; }

    /**
     * Item-level use cooldown in ticks — prevents rapid re-use of wands/bows/consumables.
     * Separate from ability-specific cooldowns. {@code 0} = no item cooldown.
     * Set via {@code ItemCooldown: N} in item YAML.
     */
    default int itemCooldownTicks() { return 0; }

    // ── Ranged / bow fields ────────────────────────────────────────────────────

    /**
     * For BOW / CROSSBOW items: RPG item id of the required ammo. Players need this item
     * in their inventory to fire. {@code null} or empty = use vanilla arrows (or infinite if
     * {@link #infiniteAmmo()} is true).
     */
    default String ammoType() { return null; }

    /**
     * If {@code true}, this bow fires without consuming any ammo item.
     */
    default boolean infiniteAmmo() { return false; }

    /**
     * Bukkit {@code EntityType} name of the projectile this bow fires.
     * Valid values: {@code ARROW}, {@code SPECTRAL_ARROW}, {@code SNOWBALL}, {@code EGG},
     * {@code SMALL_FIREBALL}, {@code TRIDENT}. Defaults to {@code ARROW}.
     */
    default String projectileType() { return "ARROW"; }
}
