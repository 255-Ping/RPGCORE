package com.github._255_ping.rpg.core.mobs;

import java.util.Locale;

public sealed interface MobAbilityTrigger {

    record OnTimer(long intervalTicks) implements MobAbilityTrigger {}
    record OnHit() implements MobAbilityTrigger {}
    record OnHurt() implements MobAbilityTrigger {}
    record OnSpawn() implements MobAbilityTrigger {}
    record OnDeath() implements MobAbilityTrigger {}

    /**
     * Fires when the mob initiates a melee or projectile attack — before the damage pipeline
     * resolves. Fires even if the attack is later cancelled. Contrast with {@code OnHit} which
     * fires only after the RPG damage pipeline confirms the hit.
     */
    record OnAttack() implements MobAbilityTrigger {}

    /** Fires once when this mob lands the killing blow on another entity. */
    record OnKill() implements MobAbilityTrigger {}

    /** Fires each time the mob jumps (requires Paper's EntityJumpEvent). */
    record OnJump() implements MobAbilityTrigger {}

    /** Parses the suffix portion (after {@code ~}) of a mob ability spec. */
    static MobAbilityTrigger parse(String suffix) {
        String t = suffix.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("ontimer")) {
            int colon = t.indexOf(':');
            if (colon < 0) throw new IllegalArgumentException("~onTimer requires :ticks (e.g., ~onTimer:100)");
            return new OnTimer(Long.parseLong(t.substring(colon + 1).trim()));
        }
        return switch (t) {
            case "onhit"    -> new OnHit();
            case "onhurt"   -> new OnHurt();
            case "onspawn"  -> new OnSpawn();
            case "ondeath"  -> new OnDeath();
            case "onattack" -> new OnAttack();
            case "onkill"   -> new OnKill();
            case "onjump"   -> new OnJump();
            default -> throw new IllegalArgumentException("unknown trigger: ~" + suffix);
        };
    }
}
