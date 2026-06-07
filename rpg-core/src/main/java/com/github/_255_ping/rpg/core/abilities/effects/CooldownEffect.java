package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        if (!(ctx.caster() instanceof Player p) || ticks <= 0) {
            return CompletableFuture.completedFuture(ctx);
        }
        String cdKey = "cooldown:" + key;
        if (RpgServices.cooldowns().isOnCooldown(p.getUniqueId(), cdKey)) {
            long remaining = RpgServices.cooldowns().remainingTicks(p.getUniqueId(), cdKey);
            double secs = remaining / 20.0;
            String timeStr = (secs == Math.floor(secs))
                    ? (int) secs + "s"
                    : String.format("%.1fs", secs);
            p.sendActionBar(Component.text("Ability on cooldown — ", NamedTextColor.RED)
                    .append(Component.text(timeStr + " remaining", NamedTextColor.YELLOW)));
            ctx.setBlocked(true);
            return CompletableFuture.completedFuture(ctx);
        }
        RpgServices.cooldowns().set(p.getUniqueId(), cdKey, ticks);
        return CompletableFuture.completedFuture(ctx);
    }
}
