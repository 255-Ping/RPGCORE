package com.github._255_ping.rpg.core.health;

final class HealthState {

    double currentHp;
    double maxHp;

    HealthState(double currentHp, double maxHp) {
        this.currentHp = currentHp;
        this.maxHp = maxHp;
    }
}
