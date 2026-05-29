package com.github._255_ping.rpg.core.status;

import com.github._255_ping.rpg.api.status.StackingStrategy;
import com.github._255_ping.rpg.api.status.StatusEffect;

import java.util.List;

public final class CoreStatusEffect implements StatusEffect {

    private final String id;
    private final String displayName;
    private final Category category;
    private final StackingStrategy stacking;
    private final boolean hidden;
    private final List<StatModifier> statModifiers;
    private final TickSpec tickSpec;
    private final HookSpec onApply;
    private final HookSpec onExpire;

    public CoreStatusEffect(String id, String displayName, Category category,
                            StackingStrategy stacking, boolean hidden,
                            List<StatModifier> statModifiers, TickSpec tickSpec,
                            HookSpec onApply, HookSpec onExpire) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.stacking = stacking;
        this.hidden = hidden;
        this.statModifiers = List.copyOf(statModifiers);
        this.tickSpec = tickSpec;
        this.onApply = onApply;
        this.onExpire = onExpire;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public Category category() { return category; }
    @Override public StackingStrategy stacking() { return stacking; }
    @Override public boolean hidden() { return hidden; }

    public List<StatModifier> statModifiers() { return statModifiers; }
    public TickSpec tickSpec() { return tickSpec; }
    /** {@code null} means no hook. */
    public HookSpec onApply() { return onApply; }
    /** {@code null} means no hook. */
    public HookSpec onExpire() { return onExpire; }

    /** {@code null} {@link #action()} means no periodic action. */
    public record TickSpec(long intervalTicks, String action, double amount, String source) {}

    /**
     * Sound + particle hook fired when the effect is applied or expires.
     * Any field may be {@code null} to skip that part of the hook.
     */
    public record HookSpec(String sound, float volume, float pitch, String particle, int particleCount) {}
}
