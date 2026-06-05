package com.github._255_ping.rpg.kits;

import java.util.List;

/**
 * Immutable definition of a claimable kit, loaded from YAML.
 *
 * @param id             Lowercase kit ID (the YAML key)
 * @param displayName    Player-visible name (§-colour-coded)
 * @param description    Lore lines shown in /kit list
 * @param items          Items granted on claim
 * @param permission     Permission node required to claim; {@code null} = any player with rpg.kits.use
 * @param oneTime        If {@code true}, a player may claim it only once and it can never be re-claimed
 * @param cooldownSeconds Seconds between claims; ignored when {@code oneTime} is {@code true}
 */
public record KitDef(
        String id,
        String displayName,
        List<String> description,
        List<ItemEntry> items,
        String permission,
        boolean oneTime,
        int cooldownSeconds
) {

    /** A single item grant inside a kit. */
    public record ItemEntry(String itemId, int amount) {}
}
