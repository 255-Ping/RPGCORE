package com.github._255_ping.rpg.holograms;

import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns short-lived TextDisplay entities at the victim's hit location showing the
 * damage number. Format per source type. Visibility filtered to a configured audience.
 */
public final class DamageIndicatorListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;

    public DamageIndicatorListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(PostDamageEvent event) {
        if (!plugin.getConfig().getBoolean("damage-indicators.enabled", true)) return;
        LivingEntity victim = event.context().victim();
        if (victim == null || victim.getWorld() == null) return;
        double amount = event.dealtDamage();
        if (amount <= 0) return;

        String kind = pickKind(event);
        String template = plugin.getConfig().getString(
                "damage-indicators.formats." + kind,
                plugin.getConfig().getString("damage-indicators.formats.normal", "&f{amount}"));
        String text = template.replace("{amount}", formatAmount(amount));
        Component component = LEGACY.deserialize(text).decoration(TextDecoration.ITALIC, false);

        Location at = victim.getLocation().add(
                jitter(), 1.5 + jitter() * 0.5, jitter());
        TextDisplay display = (TextDisplay) victim.getWorld().spawnEntity(at, org.bukkit.entity.EntityType.TEXT_DISPLAY);
        display.text(component);
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setSeeThrough(false);
        display.setShadowed(true);

        Set<Player> audience = pickAudience(event);
        // Visibility scoped via packet hide for everyone not in audience.
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!audience.contains(p)) p.hideEntity(plugin, display);
        }

        long duration = plugin.getConfig().getLong("damage-indicators.duration-ticks", 25);
        double rise = plugin.getConfig().getDouble("damage-indicators.rise-blocks", 0.8);
        long fmtDuration = duration;
        Vector perTick = new Vector(0, rise / fmtDuration, 0);
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            long ticks = 0;
            @Override public void run() {
                if (!display.isValid() || ticks++ >= fmtDuration) {
                    if (display.isValid()) display.remove();
                    return;
                }
                display.teleport(display.getLocation().add(perTick));
            }
        }, 1L, 1L);
    }

    private String pickKind(PostDamageEvent event) {
        String source = event.context().source();
        if (source == null) return "normal";
        if (event.context().trueDamage()) return "true";
        if (event.context().critMultiplier() > 1.0) return "crit";
        if (source.startsWith("ability")) return "normal";
        return "normal";
    }

    private Set<Player> pickAudience(PostDamageEvent event) {
        Set<Player> audience = new HashSet<>();
        boolean toAttacker = plugin.getConfig().getBoolean("damage-indicators.show-to.attacker", true);
        boolean toVictim = plugin.getConfig().getBoolean("damage-indicators.show-to.victim-if-player", true);
        boolean toBystanders = plugin.getConfig().getBoolean("damage-indicators.show-to.bystanders", false);
        if (toBystanders) {
            audience.addAll(plugin.getServer().getOnlinePlayers());
            return audience;
        }
        if (toAttacker && event.context().attacker() instanceof Player p) audience.add(p);
        if (toVictim && event.context().victim() instanceof Player p) audience.add(p);
        return audience;
    }

    private static double jitter() {
        return ThreadLocalRandom.current().nextDouble(-0.4, 0.4);
    }

    private static String formatAmount(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return Long.toString((long) amount);
        }
        return String.format("%.1f", amount);
    }

}
