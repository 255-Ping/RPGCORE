package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.core.mobs.OwnedMobTracker;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Spawns one or more registered custom mobs near the caster or its current target.
 *
 * <h3>Parameters</h3>
 * <ul>
 *   <li>{@code id} — (required) mob ID from the mob registry</li>
 *   <li>{@code count=1} — number of mobs to spawn</li>
 *   <li>{@code at=caster} — spawn anchor: {@code "caster"}, {@code "target"}, or {@code "point"}</li>
 *   <li>{@code radius=0.0} — random XZ spread radius around the anchor (0 = exact location)</li>
 *   <li>{@code offset_y=0.0} — Y offset from the anchor location</li>
 *   <li>{@code owned=false} — if {@code true}, the mob is tagged as belonging to the caster:
 *       it will not attack its owner, despawns when the caster logs out, and counts toward the
 *       per-caster cap set in {@code abilities.spawn-mob.max-per-caster} config</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * # Boss spawns adds when hurt
 * - "spawn_mob{id=golem_shard,count=2,radius=4.0} ~onHurt"
 *
 * # Phase transition: at 25% HP, spawn adds + shield (one-shot via if_not_flag)
 * - "if_health_below{percent=25} if_not_flag{name=phase2} set_flag{name=phase2} spawn_mob{id=golem_shard,count=3,radius=5.0} shield{amount=500,duration=200,target=caster} ~onTimer:20"
 *
 * # Player summoner item: right-click to summon a temporary ally
 * - "mana_cost{amount=50} cooldown{ticks=400} spawn_mob{id=summoned_skeleton,count=1,at=caster,radius=2.0,owned=true} right_click"
 * }</pre>
 */
public final class SpawnMobEffect implements AbilityEffect {

    private static final Random RANDOM = new Random();

    private final Plugin plugin;
    private final String mobId;
    private final int count;
    private final String at;        // "caster" | "target" | "point"
    private final double radius;
    private final double offsetY;
    private final boolean owned;

    public SpawnMobEffect(Plugin plugin, Map<String, String> params) {
        this.plugin  = plugin;
        this.mobId   = params.getOrDefault("id", "").trim();
        this.count   = Math.max(1, AbilityDsl.intParam(params, "count", 1));
        this.at      = params.getOrDefault("at", "caster").toLowerCase();
        this.radius  = AbilityDsl.doubleParam(params, "radius", 0.0);
        this.offsetY = AbilityDsl.doubleParam(params, "offset_y", 0.0);
        this.owned   = Boolean.parseBoolean(params.getOrDefault("owned", "false"));
    }

    @Override public String name() { return "spawn_mob"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.isBlocked()) return CompletableFuture.completedFuture(ctx);
        if (mobId.isEmpty()) return CompletableFuture.completedFuture(ctx);

        Optional<RpgMob> mobOpt = RpgServices.mobs().get(mobId);
        if (mobOpt.isEmpty()) return CompletableFuture.completedFuture(ctx);
        RpgMob mob = mobOpt.get();

        Location anchor = resolveAnchor(ctx);
        if (anchor == null || anchor.getWorld() == null) return CompletableFuture.completedFuture(ctx);

        // Per-caster cap check for owned mobs
        OwnedMobTracker tracker = OwnedMobTracker.get();
        if (owned && tracker != null) {
            int max = plugin.getConfig().getInt("abilities.spawn-mob.max-per-caster", 10);
            int current = tracker.countOwned(ctx.caster().getUniqueId());
            if (current >= max) return CompletableFuture.completedFuture(ctx);
        }

        for (int i = 0; i < count; i++) {
            Location spawnLoc = spreadLocation(anchor.clone());
            LivingEntity spawned = mob.spawn(spawnLoc);
            if (spawned == null) continue;

            if (owned && tracker != null) {
                // Tag the entity with its owner's UUID so DamagePipeline can check it later
                spawned.getPersistentDataContainer().set(
                        tracker.ownerKey(),
                        PersistentDataType.STRING,
                        ctx.caster().getUniqueId().toString());
                tracker.track(ctx.caster().getUniqueId(), spawned.getUniqueId());
            }
        }

        return CompletableFuture.completedFuture(ctx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Location resolveAnchor(AbilityContext ctx) {
        return switch (at) {
            case "target" -> ctx.target() != null ? ctx.target().getLocation() : ctx.caster().getLocation();
            case "point"  -> ctx.point()  != null ? ctx.point()                : ctx.caster().getLocation();
            default       -> ctx.caster().getLocation();
        };
    }

    private Location spreadLocation(Location base) {
        base.add(0, offsetY, 0);
        if (radius > 0) {
            double angle  = RANDOM.nextDouble() * 2 * Math.PI;
            double dist   = RANDOM.nextDouble() * radius;
            base.add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        }
        return base;
    }
}
