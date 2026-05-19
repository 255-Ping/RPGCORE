package com.github._255_ping.rpg.api.loot;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;

public record LootContext(
        LivingEntity victim,
        Map<Player, Double> damagers,
        Player magicFindLoadout
) {}
