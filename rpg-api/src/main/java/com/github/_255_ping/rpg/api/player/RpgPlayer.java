package com.github._255_ping.rpg.api.player;

import com.github._255_ping.rpg.api.stats.StatHolder;
import org.bukkit.entity.Player;

public interface RpgPlayer extends StatHolder {
    Player bukkit();
    double mana();
    double maxMana();
    void setMana(double mana);
    void recalculateStats();
}
