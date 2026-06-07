package com.github._255_ping.rpg.fishing;

import java.util.List;

/**
 * A named fishing catch table: a list of {@link CatchEntry entries} rolled independently
 * on each successful catch.
 *
 * <p>When {@code suppressVanilla} is {@code true}, rpg-fishing cancels the vanilla
 * {@code PlayerFishEvent} and removes the vanilla item entity, replacing it entirely
 * with the items rolled from this table. When {@code false}, custom items are added
 * on top of whatever vanilla would have given.
 */
public record CatchTable(
        String id,
        boolean suppressVanilla,
        List<CatchEntry> entries
) {}
