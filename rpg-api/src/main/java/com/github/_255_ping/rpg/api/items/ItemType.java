package com.github._255_ping.rpg.api.items;

public sealed interface ItemType permits BuiltinItemType, CustomItemType {
    String id();
}
