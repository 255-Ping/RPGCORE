package com.github._255_ping.rpg.api.regions;

import org.bukkit.Location;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegionService {

    /** Highest-priority region at this location, or empty. */
    Optional<Region> regionAt(Location location);

    /** All regions that contain this location, sorted descending by priority. */
    List<Region> regionsAt(Location location);

    /** Reads a boolean flag at this location with priority order. Returns {@code defaultValue}
     *  if no region overrides the flag. */
    boolean flag(Location location, String key, boolean defaultValue);

    /** Reads a String flag at this location with priority order. Returns {@code defaultValue}
     *  if no region overrides the flag with a String value. */
    default String flagString(Location location, String key, String defaultValue) {
        return regionsAt(location).stream()
                .map(r -> r.flags().get(key))
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .findFirst()
                .orElse(defaultValue);
    }

    Optional<Region> get(String id);

    Collection<Region> all();
}
