package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class CooldownEffect implements AbilityEffect {

    private final long ticks;
    private final String key;

    public CooldownEffect(Map<String, String> params) {
        this.ticks = AbilityDsl.intParam(params, "ticks", 20);
        this.key = params.getOrDefault("key", "ability");
    }

    @Override public String name() { return "cooldown"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.caster() instanceof Player p && ticks > 0) {
            RpgServices.cooldowns().set(p.getUniqueId(), "cooldown:" + key, ticks);
        }
        return CompletableFuture.completedFuture(ctx);
    }
}
