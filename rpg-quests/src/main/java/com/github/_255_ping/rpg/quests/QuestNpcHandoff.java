package com.github._255_ping.rpg.quests;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Service object registered to Bukkit's ServicesManager under the class name
 * {@code com.github._255_ping.rpg.npcs.QuestHandoffBridge}. rpg-npcs picks this up via that
 * fully-qualified name; we don't import npcs here so we keep that as a soft-dep only.
 *
 * <p>This class implements that interface reflectively via {@code java.lang.reflect.Proxy} since
 * we can't import the type directly without depending on rpg-npcs at compile time.
 */
public final class QuestNpcHandoff {

    private final QuestManager manager;

    public QuestNpcHandoff(QuestManager manager) {
        this.manager = manager;
    }

    public void register(org.bukkit.plugin.java.JavaPlugin plugin) {
        try {
            Class<?> iface = Class.forName("com.github._255_ping.rpg.npcs.QuestHandoffBridge");
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                    plugin.getClass().getClassLoader(),
                    new Class<?>[]{iface},
                    (p, method, args) -> {
                        if (method.getName().equals("handoff") && args != null && args.length == 2) {
                            handoff((Player) args[0], String.valueOf(args[1]));
                            return null;
                        }
                        return null;
                    });
            plugin.getServer().getServicesManager().register(
                    (Class<Object>) iface, proxy, plugin, org.bukkit.plugin.ServicePriority.Normal);
            plugin.getLogger().info("Registered quest NPC hand-off bridge.");
        } catch (ClassNotFoundException ex) {
            // rpg-npcs not loaded — bridge unused. Fine.
        }
    }

    public void handoff(Player player, String questId) {
        Optional<QuestDef> def = manager.registry().get(questId);
        if (def.isEmpty()) return;
        // If quest is already active, treat as a "turn-in" attempt: complete only if all objectives
        // are done. Otherwise auto-accept.
        boolean alreadyActive = manager.load(player.getUniqueId()).active.stream()
                .anyMatch(a -> a.questId.equals(def.get().id()));
        if (alreadyActive) {
            // Hand off also triggers the TALK_NPC objective tick for any active quest pointing at
            // this quest's id (used as a "talk to elder" trigger).
            manager.progressFor(player, QuestObjective.Type.TALK_NPC, questId);
        } else {
            manager.accept(player, questId);
        }
    }
}
