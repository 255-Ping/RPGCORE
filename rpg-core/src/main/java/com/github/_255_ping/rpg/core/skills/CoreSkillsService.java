package com.github._255_ping.rpg.core.skills;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.formula.ExpressionEvaluator;
import com.github._255_ping.rpg.api.skills.SkillXpAwardEvent;
import com.github._255_ping.rpg.api.skills.SkillsService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks per-player skill XP and computes level from configured curves at read time.
 *
 * <p>Curves are expressions parsed by {@link ExpressionEvaluator}. The curve gives the XP
 * cost to advance from {@code level} to {@code level+1}; cumulative cost is integrated.
 * For long curves this can be slow on every level lookup, so we cache the cumulative
 * thresholds per skill until the curve or max-level change.
 */
public final class CoreSkillsService implements SkillsService {

    private final JavaPlugin plugin;
    private final ConcurrentMap<UUID, PlayerSkillState> states = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> pinned = new ConcurrentHashMap<>();
    private final Map<String, long[]> curveCache = new HashMap<>();
    private final ExpressionEvaluator.Compiled[] cachedCurves = new ExpressionEvaluator.Compiled[0];

    public CoreSkillsService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerSkillState getOrCreate(UUID id) {
        return states.computeIfAbsent(id, k -> new PlayerSkillState());
    }

    public void remove(UUID id) {
        states.remove(id);
        pinned.remove(id);
    }

    public void onReload() {
        curveCache.clear();
    }

    @Override
    public int level(Player player, String skillId) {
        long total = totalXp(player, skillId);
        return levelForTotal(skillId, total);
    }

    @Override
    public long totalXp(Player player, String skillId) {
        PlayerSkillState s = states.get(player.getUniqueId());
        return s == null ? 0L : s.totalXp(skillId);
    }

    @Override
    public long xpToNext(Player player, String skillId) {
        long total = totalXp(player, skillId);
        int level = levelForTotal(skillId, total);
        long[] thresholds = thresholds(skillId);
        if (level >= thresholds.length - 1) return 0L;
        return Math.max(0L, thresholds[level + 1] - total);
    }

    @Override
    public void awardXp(Player player, String skillId, long amount) {
        if (amount <= 0) return;

        SkillXpAwardEvent ev = new SkillXpAwardEvent(player, skillId, amount);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled() || ev.amount() <= 0) return;

        PlayerSkillState state = getOrCreate(player.getUniqueId());
        long before = state.totalXp(skillId);
        int beforeLevel = levelForTotal(skillId, before);
        state.addTotalXp(skillId, ev.amount());
        long after = state.totalXp(skillId);
        int afterLevel = levelForTotal(skillId, after);

        if (afterLevel > beforeLevel) {
            // TODO: fire a SkillLevelUpEvent + grant milestone rewards once the milestones
            // system lands. For now we just log it through the message formatter.
            plugin.getLogger().info(player.getName() + " " + skillId + " " + beforeLevel + " -> " + afterLevel);
        }

        pinned.put(player.getUniqueId(), skillId);
    }

    @Override
    public void setLevel(Player player, String skillId, int level) {
        long[] thresholds = thresholds(skillId);
        int clamped = Math.max(1, Math.min(level, thresholds.length - 1));
        setTotalXp(player, skillId, thresholds[clamped]);
    }

    @Override
    public void setTotalXp(Player player, String skillId, long totalXp) {
        getOrCreate(player.getUniqueId()).setTotalXp(skillId, totalXp);
    }

    @Override
    public int maxLevel(String skillId) {
        return readMaxLevel(skillId);
    }

    @Override
    public String pinnedSkill(Player player) {
        return pinned.get(player.getUniqueId());
    }

    @Override
    public void pinSkill(Player player, String skillId) {
        if (skillId == null) pinned.remove(player.getUniqueId());
        else pinned.put(player.getUniqueId(), skillId);
    }

    /** Returns the level for the player's total XP, given the current cached thresholds. */
    private int levelForTotal(String skillId, long totalXp) {
        long[] thresholds = thresholds(skillId);
        // thresholds[i] = total XP required to reach level i (thresholds[0] = 0)
        // Find the largest i such that thresholds[i] <= totalXp.
        int lo = 0;
        int hi = thresholds.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (thresholds[mid] <= totalXp) lo = mid;
            else hi = mid - 1;
        }
        return Math.max(1, lo);
    }

    private long[] thresholds(String skillId) {
        return curveCache.computeIfAbsent(skillId, this::buildThresholds);
    }

    private long[] buildThresholds(String skillId) {
        int maxLevel = readMaxLevel(skillId);
        String curve = readCurve(skillId);
        ExpressionEvaluator evalSvc = RpgServices.expressions();
        ExpressionEvaluator.Compiled compiled = evalSvc.compile(curve);

        long[] arr = new long[maxLevel + 1];
        arr[0] = 0;
        arr[1] = 0;
        long running = 0;
        for (int lvl = 1; lvl < maxLevel; lvl++) {
            Map<String, Double> vars = new HashMap<>();
            vars.put("level", (double) lvl);
            vars.put("prev_xp_total", (double) running);
            double cost = compiled.evaluate(vars);
            running += Math.max(0, Math.round(cost));
            arr[lvl + 1] = running;
        }
        return arr;
    }

    private int readMaxLevel(String skillId) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("skills." + skillId);
        int defaultMax = plugin.getConfig().getInt("skills.default-max-level", 50);
        if (sec == null) return defaultMax;
        return sec.getInt("max-level", defaultMax);
    }

    private String readCurve(String skillId) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("skills." + skillId);
        String defaultCurve = plugin.getConfig().getString("skills.default-curve", "100 * level ^ 1.5");
        if (sec == null) return defaultCurve;
        return sec.getString("curve", defaultCurve);
    }
}
