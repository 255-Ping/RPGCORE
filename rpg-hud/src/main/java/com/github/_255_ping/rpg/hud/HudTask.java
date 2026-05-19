package com.github._255_ping.rpg.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player HUD updater. One Scoreboard instance is created per player; lines are
 * refreshed at the configured cadence. Tablist + action bar are pushed via the standard
 * Adventure APIs at their own cadences.
 */
public final class HudTask implements Runnable {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();
    private final Set<UUID> disabledScoreboard = new HashSet<>();
    private final Set<UUID> disabledTablist = new HashSet<>();
    private final Set<UUID> disabledActionBar = new HashSet<>();

    private long tickCounter = 0;

    public HudTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        tickCounter++;
        long sbEvery = Math.max(1, plugin.getConfig().getLong("scoreboard.update-ticks", 10));
        long tlEvery = Math.max(1, plugin.getConfig().getLong("tablist.update-ticks", 40));
        long abEvery = Math.max(1, plugin.getConfig().getLong("action-bar.update-ticks", 5));

        boolean sbOn = plugin.getConfig().getBoolean("scoreboard.enabled", true);
        boolean tlOn = plugin.getConfig().getBoolean("tablist.enabled", true);
        boolean abOn = plugin.getConfig().getBoolean("action-bar.enabled", true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (sbOn && !disabledScoreboard.contains(id) && tickCounter % sbEvery == 0) updateScoreboard(p);
            if (tlOn && !disabledTablist.contains(id) && tickCounter % tlEvery == 0) updateTablist(p);
            if (abOn && !disabledActionBar.contains(id) && tickCounter % abEvery == 0) updateActionBar(p);
        }
    }

    public void onJoin(Player p) {
        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) updateScoreboard(p);
        if (plugin.getConfig().getBoolean("tablist.enabled", true)) updateTablist(p);
    }

    public void onQuit(Player p) {
        boards.remove(p.getUniqueId());
    }

    public boolean toggleScoreboard(Player p) {
        boolean newState = disabledScoreboard.contains(p.getUniqueId());
        if (newState) {
            disabledScoreboard.remove(p.getUniqueId());
            updateScoreboard(p);
        } else {
            disabledScoreboard.add(p.getUniqueId());
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        return newState;   // true = now enabled
    }

    public boolean toggleTablist(Player p) {
        boolean newState = disabledTablist.contains(p.getUniqueId());
        if (newState) {
            disabledTablist.remove(p.getUniqueId());
            updateTablist(p);
        } else {
            disabledTablist.add(p.getUniqueId());
            p.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        }
        return newState;
    }

    public boolean toggleActionBar(Player p) {
        boolean newState = disabledActionBar.contains(p.getUniqueId());
        if (newState) disabledActionBar.remove(p.getUniqueId());
        else disabledActionBar.add(p.getUniqueId());
        return newState;
    }

    private void updateScoreboard(Player player) {
        Scoreboard sb = boards.computeIfAbsent(player.getUniqueId(), k -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective obj = sb.getObjective("rpg");
        if (obj == null) {
            obj = sb.registerNewObjective("rpg", Criteria.DUMMY, LEGACY.deserialize(plugin.getConfig().getString("scoreboard.title", "RPG")));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.displayName(LEGACY.deserialize(plugin.getConfig().getString("scoreboard.title", "RPG")));
        }

        // Clear prior scores by un-registering all entries.
        for (String entry : new ArrayList<>(sb.getEntries())) {
            sb.resetScores(entry);
        }

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        // Bukkit ranks scores high → low; assign descending so they render in declared order.
        int score = lines.size();
        Set<String> used = new HashSet<>();
        for (String raw : lines) {
            String resolved = PlaceholderResolver.resolve(player, raw);
            String entry = uniquify(resolved, used);
            Score s = obj.getScore(entry);
            s.setScore(score--);
        }
        player.setScoreboard(sb);
    }

    private void updateTablist(Player player) {
        List<String> header = plugin.getConfig().getStringList("tablist.header");
        List<String> footer = plugin.getConfig().getStringList("tablist.footer");
        Component h = joinLegacy(player, header);
        Component f = joinLegacy(player, footer);
        player.sendPlayerListHeaderAndFooter(h, f);
    }

    private void updateActionBar(Player player) {
        String fmt = plugin.getConfig().getString("action-bar.idle-format", "");
        if (fmt.isEmpty()) return;
        player.sendActionBar(LEGACY.deserialize(PlaceholderResolver.resolve(player, fmt)));
    }

    private static Component joinLegacy(Player p, List<String> lines) {
        if (lines == null || lines.isEmpty()) return Component.empty();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(PlaceholderResolver.resolve(p, lines.get(i)));
        }
        return LEGACY.deserialize(sb.toString());
    }

    /** Scoreboard entries must be unique. Suffix invisible color codes to dedupe. */
    private static String uniquify(String s, Set<String> used) {
        if (used.add(s)) return s;
        for (char c = '0'; c <= '9'; c++) {
            String candidate = s + "§" + c;
            if (used.add(candidate)) return candidate;
        }
        for (char c = 'a'; c <= 'f'; c++) {
            String candidate = s + "§" + c;
            if (used.add(candidate)) return candidate;
        }
        // Pathological many-duplicates: chain two
        String candidate = s + "§r§0";
        used.add(candidate);
        return candidate;
    }
}
