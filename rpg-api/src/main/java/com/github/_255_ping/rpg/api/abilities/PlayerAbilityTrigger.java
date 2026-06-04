package com.github._255_ping.rpg.api.abilities;

import java.util.Locale;

/**
 * Describes when a player-side ability fires.
 *
 * <p>In item YAML, prefix an {@code Abilities:} entry with {@code ~trigger_name } to bind it to
 * a specific trigger.  Lines without a prefix default to {@link #RIGHT_CLICK}.
 *
 * <pre>
 * Abilities:
 *   - "mana_cost{amount=50} fireball{}"            # implicit right_click
 *   - "~shift_right_click mana_cost{amount=100} beam{range=30}"
 *   - "~on_hit drain{amount=5}"                    # proc, no mana gate by default
 *   - "~passive heal{amount=2}"                    # ticks every N ticks while held/equipped
 * </pre>
 *
 * <p>Passive and proc triggers ({@link #ON_HIT}, {@link #ON_HURT}, {@link #ON_JUMP},
 * {@link #PASSIVE}) do NOT auto-apply a mana cost — add {@code mana_cost{}} explicitly if needed.
 */
public enum PlayerAbilityTrigger {

    /** Right-click (air or block). Default when no {@code ~} prefix is present. */
    RIGHT_CLICK,

    /** Left-click (air or block). */
    LEFT_CLICK,

    /** Right-click while sneaking. */
    SHIFT_RIGHT_CLICK,

    /** Left-click while sneaking. */
    SHIFT_LEFT_CLICK,

    /** Fires once when the player deals damage (melee hit or projectile landing). */
    ON_HIT,

    /** Fires once when the player receives damage. */
    ON_HURT,

    /** Fires once when the player jumps. */
    ON_JUMP,

    /**
     * Ticking passive — fires on a repeating schedule while the item is held or equipped.
     * The interval (ticks) is controlled by {@code abilities.passive-interval-ticks} in
     * {@code config.yml}; defaults to 20.
     */
    PASSIVE;

    /** @throws IllegalArgumentException if the string does not match any trigger. */
    public static PlayerAbilityTrigger parse(String s) {
        return switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "right_click"        -> RIGHT_CLICK;
            case "left_click"         -> LEFT_CLICK;
            case "shift_right_click"  -> SHIFT_RIGHT_CLICK;
            case "shift_left_click"   -> SHIFT_LEFT_CLICK;
            case "on_hit"             -> ON_HIT;
            case "on_hurt"            -> ON_HURT;
            case "on_jump"            -> ON_JUMP;
            case "passive"            -> PASSIVE;
            default -> throw new IllegalArgumentException("unknown player ability trigger: '" + s
                    + "'. Valid values: right_click, left_click, shift_right_click, "
                    + "shift_left_click, on_hit, on_hurt, on_jump, passive");
        };
    }

    /** Human-readable label used in item lore. */
    public String loreHint() {
        return switch (this) {
            case RIGHT_CLICK        -> "Right-click";
            case LEFT_CLICK         -> "Left-click";
            case SHIFT_RIGHT_CLICK  -> "Shift + Right-click";
            case SHIFT_LEFT_CLICK   -> "Shift + Left-click";
            case ON_HIT             -> "On Hit";
            case ON_HURT            -> "On Hurt";
            case ON_JUMP            -> "On Jump";
            case PASSIVE            -> "Passive";
        };
    }

    /** Returns true for triggers that fire from a player action (click). False for proc/passive. */
    public boolean isActive() {
        return switch (this) {
            case RIGHT_CLICK, LEFT_CLICK, SHIFT_RIGHT_CLICK, SHIFT_LEFT_CLICK -> true;
            default -> false;
        };
    }
}
