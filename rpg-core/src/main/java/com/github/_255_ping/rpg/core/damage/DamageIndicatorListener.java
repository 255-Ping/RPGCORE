package com.github._255_ping.rpg.core.damage;

import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a short-lived TextDisplay at the victim showing the damage number.
 * Visible to all nearby players. Animates upward via server-side teleport each tick
 * (avoids TextDisplay client interpolation packet-timing issues entirely).
 */
public final class DamageIndicatorListener implements Listener {

    private final JavaPlugin plugin;

    public DamageIndicatorListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(PostDamageEvent event) {
        if (!plugin.getConfig().getBoolean("damage-indicators.enabled", true)) return;

        double damage = event.dealtDamage();
        if (damage <= 0) return;

        LivingEntity victim = event.context().victim();
        if (victim == null || victim.getWorld() == null) return;

        boolean isCrit = event.context().critMultiplier() > 1.0;
        String text = damage == Math.floor(damage)
                ? String.valueOf((long) damage)
                : String.format("%.1f", damage);

        Location loc = victim.getLocation().add(
                ThreadLocalRandom.current().nextDouble(-0.4, 0.4),
                victim.getHeight() * 0.9,
                ThreadLocalRandom.current().nextDouble(-0.4, 0.4));

        Component label = Component.text(text, isCrit ? NamedTextColor.GOLD : NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, isCrit);

        int durationTicks = plugin.getConfig().getInt("damage-indicators.duration-ticks", 25);
        float riseBlocks = (float) plugin.getConfig().getDouble("damage-indicators.rise-blocks", 1.2);
        double risePerTick = riseBlocks / (double) durationTicks;

        TextDisplay td = (TextDisplay) victim.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.text(label);
        td.setBillboard(Display.Billboard.CENTER);
        td.setDefaultBackground(false);
        td.setShadowed(true);

        // Animate by teleporting upward each tick. We deliberately avoid setTransformation /
        // client-side interpolation because Paper bundles entity metadata into the spawn packet,
        // causing the entity to arrive at its end-state scale before any animation can start.
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!td.isValid() || ticks++ >= durationTicks) {
                    if (td.isValid()) td.remove();
                    return;
                }
                td.teleport(td.getLocation().add(0, risePerTick, 0));
            }
        }, 1L, 1L);
    }
}
