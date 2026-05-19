package com.github._255_ping.rpg.api.formatting;

import net.kyori.adventure.text.Component;

import java.util.Map;

public interface MessageFormatter {
    String format(String key);
    String format(String key, Map<String, ?> placeholders);
    Component component(String key);
    Component component(String key, Map<String, ?> placeholders);
}
