package com.github._255_ping.rpg.core.damage;

import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

public final class DamageIndicatorListener implements Listener {

    private final JavaPlugin plugin;

    public DamageIndicatorListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(PostDamageEvent event) {
        if (!plugin.getConfig().getBoolean("damage-indicators.enabled", true)) return;

        double damage = event.dealtDamage();
        boolean isCrit = event.context().critMultiplier() > 1.0;

        String text = damage == Math.floor(damage) ? String.valueOf((long) damage) : String.format("%.1f", damage);

        Location loc = event.context().victim().getLocation().add(
                ThreadLocalRandom.current().nextDouble(-0.4, 0.4),
                event.context().victim().getHeight() * 0.9,
                ThreadLocalRandom.current().nextDouble(-0.4, 0.4)
        );

        Component label = Component.text(text, isCrit ? NamedTextColor.GOLD : NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, isCrit);

        TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.text(label);
        td.setBillboard(Display.Billboard.CENTER);
        td.setDefaultBackground(false);
        td.setShadowed(true);
        td.setVisibleByDefault(true);

        int durationTicks = plugin.getConfig().getInt("damage-indicators.duration-ticks", 25);
        float riseBlocks = (float) plugin.getConfig().getDouble("damage-indicators.rise-blocks", 1.2);

        // Use TextDisplay interpolation: client animates the rise + shrink with no per-tick task.
        td.setInterpolationDelay(0);
        td.setInterpolationDuration(durationTicks);
        AxisAngle4f noRot = new AxisAngle4f(0f, 0f, 1f, 0f);
        td.setTransformation(new Transformation(
                new Vector3f(0f, riseBlocks, 0f),
                noRot,
                new Vector3f(0.01f, 0.01f, 0.01f),
                noRot
        ));

        plugin.getServer().getScheduler().runTaskLater(plugin, td::remove, durationTicks + 5L);
    }
}
