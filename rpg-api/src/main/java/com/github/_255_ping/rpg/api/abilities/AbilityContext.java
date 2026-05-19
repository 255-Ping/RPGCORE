package com.github._255_ping.rpg.api.abilities;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public final class AbilityContext {

    private final LivingEntity caster;
    private Location point;
    private LivingEntity target;
    private double carriedDamage;
    private int pierceRemaining;
    private final Map<String, Object> bag;

    public AbilityContext(LivingEntity caster, double baseDamage) {
        this.caster = caster;
        this.carriedDamage = baseDamage;
        this.pierceRemaining = 1;
        this.bag = new HashMap<>();
    }

    public LivingEntity caster() { return caster; }
    public Location point() { return point; }
    public LivingEntity target() { return target; }
    public double carriedDamage() { return carriedDamage; }
    public int pierceRemaining() { return pierceRemaining; }
    public Map<String, Object> bag() { return bag; }

    public AbilityContext setPoint(Location p) { this.point = p; return this; }
    public AbilityContext setTarget(LivingEntity t) { this.target = t; return this; }
    public AbilityContext setCarriedDamage(double d) { this.carriedDamage = d; return this; }
    public AbilityContext setPierceRemaining(int n) { this.pierceRemaining = n; return this; }
}
