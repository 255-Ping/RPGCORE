package com.github._255_ping.rpg.api.items;

public enum BuiltinItemType implements ItemType {
    SWORD,
    WAND,
    BOW,
    ARMOR,
    MATERIAL,
    QUEST,
    CONSUMABLE,
    UPGRADE,
    ACCESSORY;

    @Override
    public String id() {
        return name().toLowerCase();
    }
}
