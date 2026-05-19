package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class SoundEffect implements AbilityEffect {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SoundEffect(Map<String, String> params) {
        this.sound = parseSound(params.getOrDefault("key", "entity.experience_orb.pickup"));
        this.volume = (float) AbilityDsl.doubleParam(params, "volume", 1.0);
        this.pitch = (float) AbilityDsl.doubleParam(params, "pitch", 1.0);
    }

    @Override public String name() { return "sound"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.caster() == null || ctx.caster().getWorld() == null) {
            return CompletableFuture.completedFuture(ctx);
        }
        Location at = ctx.point() != null ? ctx.point() : ctx.caster().getLocation();
        at.getWorld().playSound(at, sound, volume, pitch);
        return CompletableFuture.completedFuture(ctx);
    }

    @SuppressWarnings("removal")
    private static Sound parseSound(String name) {
        // Sound.valueOf is deprecated for removal as the Sound type transitions to a
        // Registry. For now we accept dotted/colon names and translate; once Registry
        // becomes the only API, swap this method out.
        String upper = name.toUpperCase(Locale.ROOT).replace('.', '_').replace(':', '_').replace('-', '_');
        try {
            return Sound.valueOf(upper);
        } catch (IllegalArgumentException ex) {
            return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
    }
}
