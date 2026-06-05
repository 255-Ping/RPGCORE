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
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a short-lived TextDisplay at the victim showing the damage number.
 * Visible to all nearby players. Animates via server-side teleport + scale update
 * each tick (avoids TextDisplay client interpolation packet-timing issues entirely).
 *
 * <p><b>Animation:</b> the indicator follows a sine-arc — it rises to {@code rise-blocks}
 * at the midpoint and drifts back down to the start height by the end. Scale shrinks
 * linearly from 1.0 → 0.0 over the full duration so it fades out as it rises.
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

        Location startLoc = victim.getLocation().add(
                ThreadLocalRandom.current().nextDouble(-0.4, 0.4),
                victim.getHeight() * 0.9,
                ThreadLocalRandom.current().nextDouble(-0.4, 0.4));

        Component label = Component.text(text, isCrit ? NamedTextColor.GOLD : NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, isCrit);

        int    durationTicks = plugin.getConfig().getInt("damage-indicators.duration-ticks", 30);
        double riseBlocks    = plugin.getConfig().getDouble("damage-indicators.rise-blocks", 1.5);

        TextDisplay td = (TextDisplay) victim.getWorld().spawnEntity(startLoc, EntityType.TEXT_DISPLAY);
        td.text(label);
        td.setBillboard(Display.Billboard.CENTER);
        td.setDefaultBackground(false);
        td.setShadowed(true);
        // Set initial scale to 1.0 explicitly so the first tick update is clean
        td.setTransformation(new Transformation(
                new Vector3f(), new Quaternionf(),
                new Vector3f(1f, 1f, 1f), new Quaternionf()));

        // Animate by teleporting each tick (avoids client-side interpolation start-state issue).
        // Y follows sin(t·π): rises to riseBlocks at t=0.5, returns to 0 at t=1.
        // Scale shrinks linearly 1→0 so the number fades as it arcs.
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!td.isValid() || ticks >= durationTicks) {
                    if (td.isValid()) td.remove();
                    return;
                }
                float t = (float) ticks / durationTicks;
                ticks++;

                double yOffset = riseBlocks * Math.sin(t * Math.PI);
                td.teleport(startLoc.clone().add(0, yOffset, 0));

                float scale = 1.0f - t;
                td.setTransformation(new Transformation(
                        new Vector3f(), new Quaternionf(),
                        new Vector3f(scale, scale, scale), new Quaternionf()));
            }
        }, 1L, 1L);
    }
}
