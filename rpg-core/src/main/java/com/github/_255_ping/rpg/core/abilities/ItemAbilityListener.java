package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityPipeline;
import com.github._255_ping.rpg.api.abilities.ItemAbilityBinding;
import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import com.github._255_ping.rpg.api.items.BuiltinItemType;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import com.github._255_ping.rpg.core.abilities.effects.ManaCostEffect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

/**
 * Fires active-click ability bindings ({@link PlayerAbilityTrigger#RIGHT_CLICK},
 * {@link PlayerAbilityTrigger#LEFT_CLICK}, {@link PlayerAbilityTrigger#SHIFT_RIGHT_CLICK},
 * {@link PlayerAbilityTrigger#SHIFT_LEFT_CLICK}) from the held item on interact events.
 *
 * <p>Both air-click and block-click variants are handled.  Block-rightclick cancels the
 * placement for weapon-type items so the cast doesn't also place a block.
 * Only MAIN_HAND interactions are processed to prevent double-firing.
 */
public final class ItemAbilityListener implements Listener {

    private final RpgCorePlugin plugin;
    private final CoreAbilityRegistry registry;

    public ItemAbilityListener(RpgCorePlugin plugin, CoreAbilityRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean leftClick  = action == Action.LEFT_CLICK_AIR  || action == Action.LEFT_CLICK_BLOCK;
        if (!rightClick && !leftClick) return;

        Player player = event.getPlayer();
        boolean sneaking = player.isSneaking();

        PlayerAbilityTrigger trigger;
        if (rightClick) {
            trigger = sneaking ? PlayerAbilityTrigger.SHIFT_RIGHT_CLICK : PlayerAbilityTrigger.RIGHT_CLICK;
        } else {
            trigger = sneaking ? PlayerAbilityTrigger.SHIFT_LEFT_CLICK : PlayerAbilityTrigger.LEFT_CLICK;
        }

        ItemStack stack = event.getItem();
        if (stack == null || stack.getType().isAir()) {
            stack = player.getInventory().getItemInMainHand();
        }
        if (stack == null || stack.getType().isAir()) return;

        Optional<RpgItem> opt = RpgServices.items().from(stack);
        if (opt.isEmpty()) return;
        RpgItem item = opt.get();

        List<ItemAbilityBinding> bindings = item.triggeredAbilities().stream()
                .filter(b -> b.trigger() == trigger)
                .toList();
        if (bindings.isEmpty()) return;

        // Cancel block-place for weapon-type items so the right-click cast doesn't also place a block.
        if (action == Action.RIGHT_CLICK_BLOCK && isWeaponType(item)) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }

        // Item-level cooldown check (prevents rapid re-use of wands/consumables).
        int itemCd = item.itemCooldownTicks();
        if (itemCd > 0) {
            String cdKey = "item_use:" + item.id();
            if (RpgServices.cooldowns().isOnCooldown(player.getUniqueId(), cdKey)) {
                try {
                    RpgServices.actionBar().send(player,
                            net.kyori.adventure.text.Component.text("§cItem on cooldown!"), 15);
                } catch (IllegalStateException ignored) {}
                return;
            }
            RpgServices.cooldowns().set(player.getUniqueId(), cdKey, itemCd);
        }

        double baseDamage = RpgServices.player(player).get(BuiltinStat.DAMAGE);
        for (ItemAbilityBinding binding : bindings) {
            AbilityContext ctx = new AbilityContext(player, baseDamage);
            ctx.setPoint(player.getEyeLocation());
            AbilityPipeline pipeline = new AbilityPipeline(binding.invocations());
            pipeline.cast(ctx, registry).exceptionally(err -> {
                Throwable root = err instanceof CompletionException ce && ce.getCause() != null
                        ? ce.getCause() : err;
                if (root instanceof ManaCostEffect.InsufficientMana) {
                    player.sendActionBar(plugin.messages().component("ability.no-mana"));
                } else {
                    plugin.getLogger().warning("Ability chain failed: " + root.getMessage());
                }
                return ctx;
            });
        }
    }

    private static boolean isWeaponType(RpgItem item) {
        if (!(item.type() instanceof BuiltinItemType bt)) return false;
        return switch (bt) {
            case SWORD, BOW, CROSSBOW, WAND, CONSUMABLE, ACCESSORY, UPGRADE, QUEST -> true;
            case ARMOR, MATERIAL -> false;
        };
    }

    /**
     * Register all built-in effects. Called once on plugin enable.
     * Also wires up the zone tick task and shield/zone cleanup listeners.
     */
    public static void registerBuiltins(CoreAbilityRegistry registry, RpgCorePlugin plugin) {
        // ── Existing effects ──────────────────────────────────────────────────
        registry.register("damage",       com.github._255_ping.rpg.core.abilities.effects.DamageEffect::new);
        registry.register("heal",         com.github._255_ping.rpg.core.abilities.effects.HealEffect::new);
        registry.register("beam",         com.github._255_ping.rpg.core.abilities.effects.BeamEffect::new);
        registry.register("explode",      com.github._255_ping.rpg.core.abilities.effects.ExplodeEffect::new);
        registry.register("aoe",          com.github._255_ping.rpg.core.abilities.effects.ExplodeEffect::new);
        registry.register("particles",    com.github._255_ping.rpg.core.abilities.effects.ParticlesEffect::new);
        registry.register("sound",        com.github._255_ping.rpg.core.abilities.effects.SoundEffect::new);
        registry.register("delay",        com.github._255_ping.rpg.core.abilities.effects.DelayEffect::new);
        registry.register("apply_status", com.github._255_ping.rpg.core.abilities.effects.ApplyStatusEffect::new);
        registry.register("mana_cost",    com.github._255_ping.rpg.core.abilities.effects.ManaCostEffect::new);
        registry.register("cooldown",     com.github._255_ping.rpg.core.abilities.effects.CooldownEffect::new);

        // ── New effects ───────────────────────────────────────────────────────
        registry.register("knockback",    com.github._255_ping.rpg.core.abilities.effects.KnockbackEffect::new);
        registry.register("launch",       com.github._255_ping.rpg.core.abilities.effects.LaunchEffect::new);
        registry.register("blink",        com.github._255_ping.rpg.core.abilities.effects.BlinkEffect::new);
        registry.register("drain",        com.github._255_ping.rpg.core.abilities.effects.DrainEffect::new);
        registry.register("restore_mana", com.github._255_ping.rpg.core.abilities.effects.RestoreManaEffect::new);
        registry.register("freeze",       com.github._255_ping.rpg.core.abilities.effects.FreezeEffect::new);
        registry.register("chain",        com.github._255_ping.rpg.core.abilities.effects.ChainEffect::new);
        registry.register("zone",         com.github._255_ping.rpg.core.abilities.effects.ZoneEffect::new);
        registry.register("shield",       com.github._255_ping.rpg.core.abilities.effects.ShieldEffect::new);
        registry.register("mark",         com.github._255_ping.rpg.core.abilities.effects.MarkEffect::new);
        registry.register("chance",       com.github._255_ping.rpg.core.abilities.effects.ChanceEffect::new);

        // ── #24 Target selection ──────────────────────────────────────────────
        registry.register("nearest_enemy",  p -> new com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect(com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect.Mode.NEAREST_ENEMY, p));
        registry.register("farthest_enemy", p -> new com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect(com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect.Mode.FARTHEST_ENEMY, p));
        registry.register("nearest_ally",   p -> new com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect(com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect.Mode.NEAREST_ALLY, p));
        registry.register("random_enemy",   p -> new com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect(com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect.Mode.RANDOM_ENEMY, p));
        registry.register("self",           p -> new com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect(com.github._255_ping.rpg.core.abilities.effects.TargetSelectEffect.Mode.SELF, p));

        // ── #25 Conditional flow ──────────────────────────────────────────────
        registry.register("if_health_below",      p -> new com.github._255_ping.rpg.core.abilities.effects.IfHealthEffect(com.github._255_ping.rpg.core.abilities.effects.IfHealthEffect.Mode.BELOW, p));
        registry.register("if_health_above",      p -> new com.github._255_ping.rpg.core.abilities.effects.IfHealthEffect(com.github._255_ping.rpg.core.abilities.effects.IfHealthEffect.Mode.ABOVE, p));
        registry.register("if_mana_below",        p -> new com.github._255_ping.rpg.core.abilities.effects.IfManaEffect(com.github._255_ping.rpg.core.abilities.effects.IfManaEffect.Mode.BELOW, p));
        registry.register("if_mana_above",        p -> new com.github._255_ping.rpg.core.abilities.effects.IfManaEffect(com.github._255_ping.rpg.core.abilities.effects.IfManaEffect.Mode.ABOVE, p));
        registry.register("if_marked",            com.github._255_ping.rpg.core.abilities.effects.IfMarkedEffect::new);
        registry.register("if_target_has_status", com.github._255_ping.rpg.core.abilities.effects.IfTargetHasStatusEffect::new);
        registry.register("if_flag",              p -> new com.github._255_ping.rpg.core.abilities.effects.FlagEffect(com.github._255_ping.rpg.core.abilities.effects.FlagEffect.Mode.IF_FLAG, p));
        registry.register("if_not_flag",          p -> new com.github._255_ping.rpg.core.abilities.effects.FlagEffect(com.github._255_ping.rpg.core.abilities.effects.FlagEffect.Mode.IF_NOT_FLAG, p));
        registry.register("set_flag",             p -> new com.github._255_ping.rpg.core.abilities.effects.FlagEffect(com.github._255_ping.rpg.core.abilities.effects.FlagEffect.Mode.SET_FLAG, p));
        registry.register("clear_flag",           p -> new com.github._255_ping.rpg.core.abilities.effects.FlagEffect(com.github._255_ping.rpg.core.abilities.effects.FlagEffect.Mode.CLEAR_FLAG, p));

        // ── #42 Mob spawning ──────────────────────────────────────────────────
        registry.register("spawn_mob", p -> new com.github._255_ping.rpg.core.abilities.effects.SpawnMobEffect(plugin, p));

        // ── Zone infrastructure ───────────────────────────────────────────────
        int zoneMax = plugin.getConfig().getInt("abilities.zone.max-active", 50);
        com.github._255_ping.rpg.core.abilities.effects.ZoneEffect.init(zoneMax);
        plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                com.github._255_ping.rpg.core.abilities.effects.ZoneEffect::tickAll,
                1L, 1L);
        plugin.getServer().getPluginManager().registerEvents(
                new com.github._255_ping.rpg.core.abilities.effects.ZoneEffect.ZoneCleanupListener(),
                plugin);

        // ── Shield cleanup ────────────────────────────────────────────────────
        plugin.getServer().getPluginManager().registerEvents(
                new com.github._255_ping.rpg.core.abilities.effects.ShieldEffect.ShieldCleanupListener(),
                plugin);
    }
}
