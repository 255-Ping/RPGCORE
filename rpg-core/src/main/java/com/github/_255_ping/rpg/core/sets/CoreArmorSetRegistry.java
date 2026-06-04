package com.github._255_ping.rpg.core.sets;

import com.github._255_ping.rpg.api.sets.ArmorSetDef;
import com.github._255_ping.rpg.api.sets.ArmorSetRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CoreArmorSetRegistry implements ArmorSetRegistry {

    private final Map<String, ArmorSetDef> sets = new LinkedHashMap<>();

    public void register(ArmorSetDef def) {
        sets.put(def.id(), def);
    }

    public void clear() {
        sets.clear();
    }

    @Override
    public Optional<ArmorSetDef> get(String id) {
        return Optional.ofNullable(sets.get(id));
    }

    @Override
    public Collection<ArmorSetDef> all() {
        return Collections.unmodifiableCollection(sets.values());
    }
}
