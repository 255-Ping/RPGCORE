package com.github._255_ping.rpg.core.wand;

import com.github._255_ping.rpg.api.wand.WandSelection;
import com.github._255_ping.rpg.api.wand.WandService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CoreWandService implements WandService {

    public static final String DEFAULT_MODE = "region";

    private final Map<UUID, Selection> selections = new HashMap<>();
    private final Map<UUID, String> modes = new HashMap<>();

    @Override
    public Optional<WandSelection> selectionOf(Player player) {
        Selection s = selections.get(player.getUniqueId());
        if (s == null || s.c1 == null || s.c2 == null) return Optional.empty();
        return Optional.of(new WandSelection(s.c1, s.c2));
    }

    @Override
    public String modeOf(Player player) {
        return modes.getOrDefault(player.getUniqueId(), DEFAULT_MODE);
    }

    @Override
    public void setMode(Player player, String mode) {
        modes.put(player.getUniqueId(), mode);
    }

    @Override
    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }

    public void setCorner1(Player player, Location loc) {
        selections.computeIfAbsent(player.getUniqueId(), k -> new Selection()).c1 = loc;
    }

    public void setCorner2(Player player, Location loc) {
        selections.computeIfAbsent(player.getUniqueId(), k -> new Selection()).c2 = loc;
    }

    private static final class Selection {
        Location c1;
        Location c2;
    }
}
