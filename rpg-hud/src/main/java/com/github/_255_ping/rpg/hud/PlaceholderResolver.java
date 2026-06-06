package com.github._255_ping.rpg.hud;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.skills.SkillsService;
import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    private static volatile RecentXpTracker tracker;

    private PlaceholderResolver() {}

    public static void setTracker(RecentXpTracker t) {
        tracker = t;
    }

    public static String resolve(Player player, String template) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = lookup(player, m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String lookup(Player player, String key) {
        if (key.startsWith("skill:")) {
            String[] parts = key.split(":");
            if (parts.length < 3) return "";
            String skillId = parts[1];
            String prop = parts[2];
            SkillsService svc = RpgServices.skills();
            return switch (prop) {
                case "level" -> Integer.toString(svc.level(player, skillId));
                case "total_xp" -> Long.toString(svc.totalXp(player, skillId));
                case "to_next" -> Long.toString(svc.xpToNext(player, skillId));
                default -> "";
            };
        }
        return switch (key) {
            case "name" -> player.getName();
            case "prefix" -> safePrefix(player);
            case "suffix" -> safeSuffix(player);
            case "online" -> Integer.toString(Bukkit.getOnlinePlayers().size());
            case "world" -> player.getWorld().getName();
            case "health" -> formatNumber(RpgServices.health().currentHp(player));
            case "max_health" -> formatNumber(RpgServices.health().maxHp(player));
            case "mana" -> formatNumber(RpgServices.player(player).mana());
            case "max_mana" -> formatNumber(RpgServices.player(player).maxMana());
            case "coins" -> coinsBalance(player);
            case "effects" -> activeEffects(player);
            case "tps" -> serverTps();
            case "ram_used" -> ramUsedMb() + "MB";
            case "ram_max" -> ramMaxMb() + "MB";
            case "ping" -> player.getPing() + "ms";
            case "party_members" -> partyMembers(player);
            case "cooldowns" -> activeCooldowns(player);
            case "recent_xp" -> tracker != null ? tracker.get(player.getUniqueId()) : "";
            default -> statValue(player, key);
        };
    }

    private static String statValue(Player player, String statId) {
        Optional<Stat> stat = RpgServices.stats().get(statId);
        if (stat.isEmpty()) return "";
        RpgPlayer rp = RpgServices.player(player);
        double v = rp.get(stat.get());
        return formatNumber(v) + (stat.get().percent() ? "%" : "");
    }

    /**
     * Returns a compact comma-separated list of active status effects for the player,
     * for use in tablist footer: {@code §dStrength I §8(30s)  §cPoison II §8(5s)}.
     * Returns an empty string if no effects are active or the service isn't loaded.
     */
    private static String activeEffects(Player player) {
        try {
            var effects = RpgServices.statusEffects().active(player);
            if (effects.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (var eff : effects) {
                if (sb.length() > 0) sb.append("  ");
                double secs = eff.remainingTicks() / 20.0;
                sb.append("§d").append(eff.effectId()).append(" ").append(toRoman(eff.level()))
                  .append(" §8(").append(secs > 0 ? String.format("%.0fs", secs) : "∞").append(")");
            }
            return sb.toString();
        } catch (IllegalStateException ex) {
            return "";
        }
    }

    private static String toRoman(int n) {
        return switch (Math.min(n, 5)) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    private static String coinsBalance(Player player) {
        try {
            BigDecimal bal = RpgServices.economy().balance(player);
            return RpgServices.currencies().primary()
                    .map(c -> c.format(bal))
                    .orElse(bal.stripTrailingZeros().toPlainString());
        } catch (IllegalStateException ex) {
            // rpg-economy or currency registry not loaded
            return "0";
        }
    }

    private static String safePrefix(Player player) {
        try { return RpgServices.nameFormatter().prefix(player); }
        catch (Exception ex) { return ""; }
    }

    private static String safeSuffix(Player player) {
        try { return RpgServices.nameFormatter().suffix(player); }
        catch (Exception ex) { return ""; }
    }

    private static String formatNumber(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return Long.toString((long) v);
        return String.format("%.1f", v);
    }

    private static String serverTps() {
        double[] tps = Bukkit.getServer().getTPS();
        double t = Math.min(20.0, tps.length > 0 ? tps[0] : 20.0);
        String color = t >= 19.0 ? "§a" : t >= 15.0 ? "§e" : "§c";
        return color + String.format("%.1f", t);
    }

    private static long ramUsedMb() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
    }

    private static long ramMaxMb() {
        return Runtime.getRuntime().maxMemory() / (1024L * 1024L);
    }

    /**
     * Returns a newline-joined list of party member health lines for use in the scoreboard.
     * Each line: {@code §7Name §c/§a current/max HP}.
     * Returns {@code ""} when the player is not in a party so the scoreboard line is suppressed.
     */
    private static String partyMembers(Player player) {
        try {
            var partyOpt = RpgServices.parties().partyOf(player);
            if (partyOpt.isEmpty()) return "";
            var party = partyOpt.get();
            StringBuilder sb = new StringBuilder();
            for (Player m : party.members()) {
                if (!sb.isEmpty()) sb.append('\n');
                double hp = RpgServices.health().currentHp(m);
                double maxHp = RpgServices.health().maxHp(m);
                double ratio = maxHp > 0 ? hp / maxHp : 1.0;
                String hpColor = ratio >= 0.5 ? "§a" : ratio >= 0.25 ? "§e" : "§c";
                sb.append("§7").append(m.getName()).append(" ")
                  .append(hpColor).append(formatNumber(hp))
                  .append("§7/§a").append(formatNumber(maxHp));
            }
            return sb.toString();
        } catch (IllegalStateException ex) {
            return "";
        }
    }

    /**
     * Returns a compact list of active ability cooldowns, e.g. {@code §7Ability §c(10s)}.
     * Filters to keys with the {@code "cooldown:"} prefix used by CooldownEffect.
     * Returns {@code ""} if no cooldowns are active.
     */
    private static String activeCooldowns(Player player) {
        try {
            Map<String, Long> active = RpgServices.cooldowns().active(player.getUniqueId());
            if (active.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Long> entry : active.entrySet()) {
                String k = entry.getKey();
                if (!k.startsWith("cooldown:")) continue;
                String label = capitalize(k.substring("cooldown:".length()));
                long secs = Math.max(1L, entry.getValue() / 20L);
                if (!sb.isEmpty()) sb.append("  ");
                sb.append("§7").append(label).append(" §c(").append(secs).append("s)");
            }
            return sb.toString();
        } catch (IllegalStateException ex) {
            return "";
        }
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
