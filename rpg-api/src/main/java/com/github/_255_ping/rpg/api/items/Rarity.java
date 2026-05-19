package com.github._255_ping.rpg.api.items;

public record Rarity(String id, String coloredDisplay) {
    public static Rarity of(String id, String coloredDisplay) {
        return new Rarity(id, coloredDisplay);
    }
}
