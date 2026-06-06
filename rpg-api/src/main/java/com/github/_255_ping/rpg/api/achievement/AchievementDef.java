package com.github._255_ping.rpg.api.achievement;

/**
 * Immutable definition of a single achievement, loaded from YAML.
 *
 * <p>Trigger types:
 * <ul>
 *   <li>{@code MANUAL}  — unlocked explicitly via {@link AchievementService#grant}</li>
 *   <li>{@code COUNTER} — unlocked when the named counter reaches {@code target}</li>
 * </ul>
 *
 * @param id          unique identifier, e.g. {@code "first_kill"}
 * @param title       short display name shown in the GUI
 * @param description one-line flavour / how-to-unlock hint
 * @param category    grouping label (e.g. "Combat", "Economy", "Exploration")
 * @param icon        Material name for the GUI icon item (e.g. "DIAMOND_SWORD")
 * @param triggerType {@code "MANUAL"} or {@code "COUNTER"}
 * @param counterKey  name of the counter this achievement listens to (null for MANUAL)
 * @param target      counter value required to unlock (ignored for MANUAL)
 * @param reward      reward granted on first unlock
 */
public record AchievementDef(
        String id,
        String title,
        String description,
        String category,
        String icon,
        String triggerType,
        String counterKey,
        long target,
        AchievementReward reward
) {
    public boolean isCounter() {
        return "COUNTER".equalsIgnoreCase(triggerType);
    }
}
