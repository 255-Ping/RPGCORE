package com.github._255_ping.rpg.hud;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.skills.SkillsService;
import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    private PlaceholderResolver() {}

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

    private static String lookup(Player player, String key) {
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

    private static String coinsBalance(Player player) {
        try {
            BigDecimal bal = RpgServices.economy().balance(player);
            return bal.stripTrailingZeros().toPlainString();
        } catch (IllegalStateException ex) {
            // rpg-economy not loaded — placeholder is empty
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
}
