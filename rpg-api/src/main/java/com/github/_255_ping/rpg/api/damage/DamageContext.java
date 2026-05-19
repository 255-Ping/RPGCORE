package com.github._255_ping.rpg.api.damage;

import org.bukkit.entity.LivingEntity;

public final class DamageContext {

    private final LivingEntity attacker;
    private final LivingEntity victim;
    private double baseDamage;
    private double critMultiplier;
    private boolean trueDamage;
    private String source;

    public DamageContext(LivingEntity attacker, LivingEntity victim, double baseDamage, String source) {
        this.attacker = attacker;
        this.victim = victim;
        this.baseDamage = baseDamage;
        this.critMultiplier = 1.0;
        this.trueDamage = false;
        this.source = source;
    }

    public LivingEntity attacker() { return attacker; }
    public LivingEntity victim() { return victim; }
    public double baseDamage() { return baseDamage; }
    public double critMultiplier() { return critMultiplier; }
    public boolean trueDamage() { return trueDamage; }
    public String source() { return source; }

    public DamageContext setBaseDamage(double d) { this.baseDamage = d; return this; }
    public DamageContext setCritMultiplier(double m) { this.critMultiplier = m; return this; }
    public DamageContext setTrueDamage(boolean t) { this.trueDamage = t; return this; }
    public DamageContext setSource(String s) { this.source = s; return this; }
}
