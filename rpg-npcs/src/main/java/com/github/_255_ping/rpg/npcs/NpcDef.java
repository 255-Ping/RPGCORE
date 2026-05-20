package com.github._255_ping.rpg.npcs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;

public final class NpcDef {

    public enum BehaviorType { SHOP, DIALOGUE, QUEST }

    public record ShopEntry(String itemId, double buy, double sell) {}

    private final String id;
    private String displayName;
    private String worldName;
    private double x, y, z;
    private float yaw, pitch;
    private BehaviorType behaviorType;
    private List<ShopEntry> shopItems;
    private List<String> dialogueLines;
    private String questId;

    public NpcDef(String id, String displayName, String worldName,
                  double x, double y, double z, float yaw, float pitch,
                  BehaviorType behaviorType, List<ShopEntry> shopItems,
                  List<String> dialogueLines, String questId) {
        this.id = id;
        this.displayName = displayName;
        this.worldName = worldName;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.behaviorType = behaviorType;
        this.shopItems = shopItems;
        this.dialogueLines = dialogueLines;
        this.questId = questId;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String worldName() { return worldName; }
    public double x() { return x; } public double y() { return y; } public double z() { return z; }
    public float yaw() { return yaw; } public float pitch() { return pitch; }
    public BehaviorType behaviorType() { return behaviorType; }
    public List<ShopEntry> shopItems() { return shopItems; }
    public List<String> dialogueLines() { return dialogueLines; }
    public String questId() { return questId; }

    public Location location() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public void moveTo(Location loc) {
        if (loc.getWorld() == null) return;
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX(); this.y = loc.getY(); this.z = loc.getZ();
        this.yaw = loc.getYaw(); this.pitch = loc.getPitch();
    }

    public void setBehavior(BehaviorType type, List<ShopEntry> shop, List<String> lines, String quest) {
        this.behaviorType = type;
        this.shopItems = shop;
        this.dialogueLines = lines;
        this.questId = quest;
    }

    public void setDisplayName(String name) { this.displayName = name; }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("DisplayName", displayName);
        out.put("World", worldName);
        out.put("X", x); out.put("Y", y); out.put("Z", z);
        out.put("Yaw", yaw); out.put("Pitch", pitch);
        Map<String, Object> beh = new java.util.LinkedHashMap<>();
        beh.put("Type", behaviorType.name().toLowerCase());
        if (shopItems != null && !shopItems.isEmpty()) {
            List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (ShopEntry e : shopItems) {
                items.add(Map.of("Item", e.itemId(), "Buy", e.buy(), "Sell", e.sell()));
            }
            beh.put("Items", items);
        }
        if (dialogueLines != null && !dialogueLines.isEmpty()) {
            beh.put("Lines", dialogueLines);
        }
        if (questId != null && !questId.isEmpty()) {
            beh.put("Quest", questId);
        }
        out.put("Behavior", beh);
        return out;
    }
}
