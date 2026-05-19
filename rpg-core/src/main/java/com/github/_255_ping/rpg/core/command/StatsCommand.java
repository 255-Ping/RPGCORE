package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StatsCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static final List<String> GROUP_ORDER = List.of(
            "combat", "survival", "caster", "mobility", "loot", "gathering",
            "mining", "foraging", "farming", "fishing", "wisdom", "enchanting", "pets"
    );

    private final RpgCorePlugin plugin;

    public StatsCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(plugin.messages().component("command.player-only"));
                return true;
            }
            if (!sender.hasPermission("rpg.core.stats")) {
                sender.sendMessage(plugin.messages().component("command.no-permission"));
                return true;
            }
            target = p;
        } else {
            if (!sender.hasPermission("rpg.core.stats.other")) {
                sender.sendMessage(plugin.messages().component("command.no-permission"));
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.messages().component("player.not-found", Map.of("name", args[0])));
                return true;
            }
        }

        RpgPlayer rp = RpgServices.player(target);
        Map<Stat, Double> snap = rp.snapshot();

        // Group by stat.group(), preserve GROUP_ORDER, then within each group sort by id.
        Map<String, java.util.SortedMap<String, Double>> byGroup = new LinkedHashMap<>();
        for (String g : GROUP_ORDER) byGroup.put(g, new java.util.TreeMap<>());
        java.util.SortedMap<String, Double> other = new java.util.TreeMap<>();

        for (Map.Entry<Stat, Double> e : snap.entrySet()) {
            if (e.getValue() == 0.0) continue;
            String g = e.getKey().group();
            byGroup.computeIfAbsent(g, k -> new java.util.TreeMap<>()).put(e.getKey().id(), e.getValue());
        }

        String header = "&6&l=== Stats: &e" + target.getName() + " &6&l===";
        sender.sendMessage(LEGACY.deserialize(header));
        for (Map.Entry<String, java.util.SortedMap<String, Double>> ge : byGroup.entrySet()) {
            if (ge.getValue().isEmpty()) continue;
            sender.sendMessage(LEGACY.deserialize("&7&n" + capitalize(ge.getKey())));
            for (Map.Entry<String, Double> se : ge.getValue().entrySet()) {
                Stat s = RpgServices.stats().get(se.getKey()).orElse(null);
                if (s == null) continue;
                sender.sendMessage(LEGACY.deserialize("  " + s.colorCode() + s.displayName()
                        + " &7: &f" + format(se.getValue(), s.percent())));
            }
        }
        return true;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String format(double v, boolean percent) {
        String num;
        if (v == Math.floor(v) && !Double.isInfinite(v)) num = Long.toString((long) v);
        else num = String.format("%.1f", v);
        return num + (percent ? "%" : "");
    }
}
