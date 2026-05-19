package com.github._255_ping.rpg.core.mobs;

import java.util.Locale;

public sealed interface MobAbilityTrigger {

    record OnTimer(long intervalTicks) implements MobAbilityTrigger {}
    record OnHit() implements MobAbilityTrigger {}
    record OnHurt() implements MobAbilityTrigger {}
    record OnSpawn() implements MobAbilityTrigger {}
    record OnDeath() implements MobAbilityTrigger {}

    /** Parses the suffix portion (after {@code ~}) of a mob ability spec. */
    static MobAbilityTrigger parse(String suffix) {
        String t = suffix.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("ontimer")) {
            int colon = t.indexOf(':');
            if (colon < 0) throw new IllegalArgumentException("~onTimer requires :ticks (e.g., ~onTimer:100)");
            return new OnTimer(Long.parseLong(t.substring(colon + 1).trim()));
        }
        return switch (t) {
            case "onhit" -> new OnHit();
            case "onhurt" -> new OnHurt();
            case "onspawn" -> new OnSpawn();
            case "ondeath" -> new OnDeath();
            default -> throw new IllegalArgumentException("unknown trigger: ~" + suffix);
        };
    }
}
