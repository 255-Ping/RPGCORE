package com.github._255_ping.rpg.core.player;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.ManaService;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import org.bukkit.entity.Player;

public final class CoreManaService implements ManaService {

    @Override
    public boolean consume(Player player, double amount) {
        RpgPlayer rp = RpgServices.player(player);
        if (rp.mana() < amount) return false;
        rp.setMana(rp.mana() - amount);
        return true;
    }

    @Override
    public void restore(Player player, double amount) {
        RpgPlayer rp = RpgServices.player(player);
        rp.setMana(rp.mana() + amount);
    }

    @Override
    public double get(Player player) {
        return RpgServices.player(player).mana();
    }

    @Override
    public void setRegenRate(Player player, double perSecond) {
        // v1: regen rate is driven by the MANA_REGEN stat; no per-player override.
        // A future API revision can layer transient overrides here if needed.
    }
}
