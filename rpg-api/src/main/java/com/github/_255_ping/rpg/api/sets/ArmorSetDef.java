package com.github._255_ping.rpg.api.sets;

import java.util.List;
import java.util.Map;

/**
 * Definition of an armor set — a named group of item IDs that grant stat bonuses and
 * abilities when enough pieces are worn simultaneously.
 *
 * <p>Defined in {@code plugins/rpg-core/sets/*.yml}.  Each set has a map of
 * <em>thresholds</em>: wearing {@code k} or more matching pieces activates that tier's bonus.
 * Only the highest satisfied threshold is active at a time.
 *
 * <p>Stats in a bonus are always passive — they activate the moment the tier is met and
 * deactivate on unequip. No trigger or proc is needed for stat bonuses. The {@code Abilities:}
 * block is optional and only needed for event-driven effects (on_hit, on_hurt, etc.).
 *
 * <pre>
 * berserker_set:
 *   Name: "Berserker's Set"
 *   Pieces:
 *     - berserker_helmet
 *     - berserker_chestplate
 *     - berserker_leggings
 *     - berserker_boots
 *   Bonuses:
 *     2:
 *       Stats:                      # purely passive — just wear the armor
 *         ferocity: 25
 *     4:
 *       Stats:
 *         ferocity: 75
 *         damage: 50
 *       Abilities:                  # optional event-driven bonus
 *         - "~on_hit drain{amount=10, leech=1.0}"
 * </pre>
 *
 * <p>Alternatively, use {@code Scale} to derive a weaker tier automatically.  With
 * {@code Scale: 0.5} at tier 2, all numeric effect parameters are halved at load time — so
 * {@code drain{amount=10}} becomes {@code drain{amount=5.0}}.  This is a convenience for when
 * both tiers share the same ability shape.  If you want entirely different effects per tier,
 * omit {@code Scale} and write the effects explicitly.
 */
public interface ArmorSetDef {

    /** Unique identifier matching the YAML key, e.g. {@code "berserker_set"}. */
    String id();

    /** Display name shown in item lore and set-status UI, e.g. {@code "Berserker's Set"}. */
    String name();

    /**
     * Ordered list of item IDs that count as pieces of this set.  An item ID may only belong to
     * one set; duplicates in the list count once per armor slot.
     */
    List<String> pieces();

    /**
     * Maps piece-count thresholds to their bonuses.  Key = minimum pieces worn to activate.
     * Only the highest satisfied key is active.
     */
    Map<Integer, SetBonus> bonuses();

    /** Total number of pieces that make up this set (i.e. {@link #pieces()}.size()). */
    default int totalPieces() {
        return pieces().size();
    }

    /**
     * Returns the active bonus for {@code piecesWorn}, or {@code null} if no threshold is met.
     * Highest satisfied threshold wins.
     */
    default SetBonus activeBonus(int piecesWorn) {
        return bonuses().entrySet().stream()
                .filter(e -> e.getKey() <= piecesWorn)
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(null);
    }
}
