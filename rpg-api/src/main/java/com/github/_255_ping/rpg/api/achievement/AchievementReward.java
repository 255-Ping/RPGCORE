package com.github._255_ping.rpg.api.achievement;

import java.math.BigDecimal;

/**
 * Reward granted when an achievement is first unlocked.
 *
 * @param money  currency deposited directly into the player's balance (may be zero)
 * @param xp     skill XP awarded to the "combat" skill (may be zero)
 */
public record AchievementReward(BigDecimal money, long xp) {

    public static final AchievementReward NONE = new AchievementReward(BigDecimal.ZERO, 0L);

    public static AchievementReward money(BigDecimal amount) {
        return new AchievementReward(amount, 0L);
    }

    public static AchievementReward xp(long amount) {
        return new AchievementReward(BigDecimal.ZERO, amount);
    }

    public static AchievementReward of(BigDecimal money, long xp) {
        return new AchievementReward(money, xp);
    }

    public boolean isEmpty() {
        return money.compareTo(BigDecimal.ZERO) <= 0 && xp <= 0;
    }
}
