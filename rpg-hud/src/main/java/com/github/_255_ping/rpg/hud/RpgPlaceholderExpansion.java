package com.github._255_ping.rpg.hud;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes all rpg-hud placeholders via PlaceholderAPI as {@code %rpg_<identifier>%}.
 *
 * <p>Examples:
 * <pre>
 *   %rpg_health%           → current HP
 *   %rpg_mana%             → current mana
 *   %rpg_tps%              → server TPS (color-coded)
 *   %rpg_ping%             → player's ping in ms
 *   %rpg_skill_combat_level%   → Combat skill level
 *   %rpg_skill_mining_to_next% → XP to next Mining level
 * </pre>
 *
 * <p>Skill placeholders translate underscores back to colons:
 * {@code skill_<id>_level} → {@code skill:<id>:level}, and similarly
 * for {@code _total_xp} and {@code _to_next} suffixes.
 *
 * <p>Multi-line placeholders (e.g. {@code party_members}) join lines with {@code ", "}.
 */
public final class RpgPlaceholderExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;

    public RpgPlaceholderExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "rpg"; }
    @Override public @NotNull String getAuthor()     { return "255Ping"; }
    @Override public @NotNull String getVersion()    { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";
        String internalKey = papiToInternal(identifier);
        String raw = PlaceholderResolver.lookup(player, internalKey);
        // Flatten multi-line values (e.g. party_members) for PAPI consumers.
        return raw.replace('\n', ',').replace(", ,", ",").trim();
    }

    /**
     * Converts a PAPI identifier (underscores only) to the internal key format.
     * Skill placeholders use colons internally: {@code skill:id:prop}.
     */
    private static String papiToInternal(String identifier) {
        if (identifier.startsWith("skill_")) {
            String rest = identifier.substring(6);
            if (rest.endsWith("_level")) {
                return "skill:" + rest.substring(0, rest.length() - 6) + ":level";
            } else if (rest.endsWith("_total_xp")) {
                return "skill:" + rest.substring(0, rest.length() - 9) + ":total_xp";
            } else if (rest.endsWith("_to_next")) {
                return "skill:" + rest.substring(0, rest.length() - 8) + ":to_next";
            }
        }
        return identifier;
    }
}
