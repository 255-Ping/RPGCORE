package com.github._255_ping.rpg.core.damage;

import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a short-lived TextDisplay at the victim showing the damage number.
 * Uses client-side interpolation for a smooth rise + shrink — no per-tick task needed.
 *
 * <p>Audience is configurable: show to the attacker, the victim (if a player),
 * and/or all bystanders. Defaults to attacker-only so other players aren't
 * spammed by nearby combat.
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
                .decoration(TextDecoration.BOLD, isCrit)
                .decoration(TextDecoration.ITALIC, false);

        TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.text(label);
        td.setBillboard(Display.Billboard.CENTER);
        td.setDefaultBackground(false);
        td.setShadowed(true);

        // Determine who should see this indicator, then hide it from everyone else.
        // We use hideEntity (not setVisibleByDefault=false + showEntity) because
        // showEntity on a freshly-spawned entity is unreliable — the tracker hasn't
        // sent the spawn packet to clients yet, so the show call may be a no-op.
        Set<Player> audience = buildAudience(event);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!audience.contains(p)) p.hideEntity(plugin, td);
        }

        int durationTicks = plugin.getConfig().getInt("damage-indicators.duration-ticks", 25);
        float riseBlocks = (float) plugin.getConfig().getDouble("damage-indicators.rise-blocks", 1.2);

        // Client-side interpolation: the TextDisplay rises and shrinks without any per-tick task.
        td.setInterpolationDelay(0);
        td.setInterpolationDuration(durationTicks);
        AxisAngle4f noRot = new AxisAngle4f(0f, 0f, 1f, 0f);
        td.setTransformation(new Transformation(
                new Vector3f(0f, riseBlocks, 0f),
                noRot,
                new Vector3f(0.01f, 0.01f, 0.01f),
                noRot));

        plugin.getServer().getScheduler().runTaskLater(plugin, td::remove, durationTicks + 5L);
    }

    private Set<Player> buildAudience(PostDamageEvent event) {
        Set<Player> audience = new HashSet<>();
        boolean toAttacker    = plugin.getConfig().getBoolean("damage-indicators.show-to.attacker", true);
        boolean toVictim      = plugin.getConfig().getBoolean("damage-indicators.show-to.victim-if-player", true);
        boolean toBystanders  = plugin.getConfig().getBoolean("damage-indicators.show-to.bystanders", false);

        if (toBystanders) {
            audience.addAll(plugin.getServer().getOnlinePlayers());
            return audience;
        }
        if (toAttacker && event.context().attacker() instanceof Player p) audience.add(p);
        if (toVictim && event.context().victim() instanceof Player p) audience.add(p);
        return audience;
    }
}
