package com.github._255_ping.rpg.core.sets;

import com.github._255_ping.rpg.api.sets.ArmorSetDef;
import com.github._255_ping.rpg.api.sets.SetBonus;

import java.util.List;
import java.util.Map;

public record ArmorSetDefImpl(
        String id,
        String name,
        List<String> pieces,
        Map<Integer, SetBonus> bonuses
) implements ArmorSetDef {

    public ArmorSetDefImpl {
        pieces  = List.copyOf(pieces);
        bonuses = Map.copyOf(bonuses);
    }
}
