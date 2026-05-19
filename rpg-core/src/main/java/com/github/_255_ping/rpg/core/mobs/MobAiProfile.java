package com.github._255_ping.rpg.core.mobs;

/**
 * Mob AI override profile. Drives target acquisition + retreat behavior on top of
 * vanilla pathfinding. Parsed from the {@code AI:} block in mob YAML.
 *
 * <p>v1 profile semantics:
 * <ul>
 *   <li><b>aggressive</b> — periodically scans nearby players within {@code aggressionRange},
 *       sets the nearest as Mob.target. Vanilla pathfinding chases.</li>
 *   <li><b>passive</b> — vanilla AI disabled on spawn; mob does nothing.</li>
 *   <li><b>defensive</b> — vanilla auto-aggro disabled; targets the last attacker (driven by
 *       OnHurt-style logic in MobAbilityEventListener).</li>
 *   <li><b>stationary</b> — same as passive for v1.</li>
 *   <li><b>ranged_kiter</b>, <b>boss</b>, <b>swarming</b>, <b>pack-hunter</b>, <b>flying</b> —
 *       fall back to aggressive for v1; full implementations come later.</li>
 * </ul>
 */
public record MobAiProfile(
        Kind kind,
        double aggressionRange,
        double attackRange,
        double leashRange,
        boolean immuneToKnockback
) {

    public static final MobAiProfile DEFAULT = new MobAiProfile(Kind.AGGRESSIVE, 16, 2, 32, false);

    public enum Kind {
        AGGRESSIVE,
        PASSIVE,
        DEFENSIVE,
        STATIONARY,
        RANGED_KITER,
        BOSS,
        SWARMING,
        PACK_HUNTER,
        FLYING
    }

    public static Kind parseKind(String s) {
        if (s == null) return Kind.AGGRESSIVE;
        String n = s.trim().toLowerCase().replace('_', '-').replace(' ', '-');
        return switch (n) {
            case "aggressive" -> Kind.AGGRESSIVE;
            case "passive" -> Kind.PASSIVE;
            case "defensive" -> Kind.DEFENSIVE;
            case "stationary" -> Kind.STATIONARY;
            case "ranged-kiter" -> Kind.RANGED_KITER;
            case "boss" -> Kind.BOSS;
            case "swarming" -> Kind.SWARMING;
            case "pack-hunter" -> Kind.PACK_HUNTER;
            case "flying" -> Kind.FLYING;
            default -> Kind.AGGRESSIVE;
        };
    }
}
