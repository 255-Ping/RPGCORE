package com.github._255_ping.rpg.fishing;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles all fishing gameplay hooks: catch table loot, XP awards,
 * fishing-speed wait-time reduction, and catch notifications.
 */
public final class FishingListener implements Listener {

    private final RpgFishingPlugin plugin;

    // --- Config cache (refreshed on reload) ---
    private long xpPerCatch;
    private String activeTableId;
    private int baseMinWait;
    private int baseMaxWait;
    private int floorMinWait;
    private int floorMaxWait;
    private double speedScale;
    private double fortuneScale;
    private double fortuneMaxMult;

    private Map<String, CatchTable> catchTables;

    public FishingListener(RpgFishingPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Refreshes config and reloads all catch table YAML files. */
    public void reload() {
        var cfg = plugin.getConfig();
        xpPerCatch     = cfg.getLong("xp-per-catch", 10);
        activeTableId  = cfg.getString("active-table", "default");
        baseMinWait    = cfg.getInt("fishing-speed.base-min-ticks", 100);
        baseMaxWait    = cfg.getInt("fishing-speed.base-max-ticks", 600);
        floorMinWait   = cfg.getInt("fishing-speed.floor-min-ticks", 20);
        floorMaxWait   = cfg.getInt("fishing-speed.floor-max-ticks", 40);
        speedScale     = cfg.getDouble("fishing-speed.scale", 300.0);
        fortuneScale   = cfg.getDouble("fishing-fortune.scale", 100.0);
        fortuneMaxMult = cfg.getDouble("fishing-fortune.max-multiplier", 5.0);

        File tablesDir = new File(plugin.getDataFolder(), "catch-tables");
        catchTables = new CatchTableLoader(tablesDir, plugin.getLogger()).loadAll();

        if (!catchTables.containsKey(activeTableId)) {
            plugin.getLogger().warning("[rpg-fishing] active-table '" + activeTableId
                    + "' not found — no custom loot will be given until a table with that name is loaded.");
        }
    }

    // -------------------------------------------------------------------------
    // FISHING_SPEED — shorten bobber wait time when the rod is cast
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBobberThrown(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING) return;
        if (!(event.getHook() instanceof FishHook hook)) return;

        double speed = statSafe(event.getPlayer(), BuiltinStat.FISHING_SPEED);
        if (speed <= 0 || speedScale <= 0) return;

        // waitMultiplier ∈ [0.1, 1.0]: each speedScale points of FISHING_SPEED = 1× reduction.
        double mult = Math.max(0.1, 1.0 - (speed / speedScale));
        int newMin = Math.max(floorMinWait, (int) (baseMinWait * mult));
        int newMax = Math.max(floorMaxWait, (int) (baseMaxWait * mult));
        // Ensure max ≥ min + a small gap so the hook has a valid range.
        if (newMax <= newMin) newMax = newMin + 20;

        hook.setMinWaitTime(newMin);
        hook.setMaxWaitTime(newMax);
    }

    // -------------------------------------------------------------------------
    // CAUGHT_FISH — roll custom loot, optionally suppress vanilla
    // -------------------------------------------------------------------------

    /**
     * High priority + ignoreCancelled=false so we fire even when rpg-core's
     * vanilla fishing suppression cancels the event at LOWEST priority.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onCatch(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();

        // Always award fishing XP — even if vanilla suppression is on.
        awardXp(player);

        CatchTable table = catchTables.get(activeTableId);
        if (table == null) return;

        if (table.suppressVanilla()) {
            // Cancel vanilla pull-to-player and remove the item entity.
            event.setCancelled(true);
            if (event.getCaught() != null) {
                event.getCaught().remove();
            }
        }

        // Roll custom items.
        double fortune = statSafe(player, BuiltinStat.FISHING_FORTUNE);
        List<ItemStack> drops = rollTable(table, fortune);

        if (drops.isEmpty()) return;

        // Give to player; overflow drops at feet.
        for (ItemStack item : drops) {
            player.getInventory().addItem(item).values()
                    .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        }

        sendCatchMessage(player, drops);
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void awardXp(Player player) {
        if (xpPerCatch <= 0) return;
        double wisdom = statSafe(player, BuiltinStat.FISHING_WISDOM);
        long amount = Math.round(xpPerCatch * (1.0 + wisdom / 100.0));
        if (amount > 0) {
            RpgServices.skills().awardXp(player, BuiltinSkill.FISHING.id(), amount);
        }
    }

    private List<ItemStack> rollTable(CatchTable table, double fortune) {
        double fortMult = fortuneScale > 0
                ? Math.min(fortuneMaxMult, 1.0 + (fortune / fortuneScale))
                : 1.0;

        List<ItemStack> results = new ArrayList<>();
        for (CatchEntry entry : table.entries()) {
            double effective = entry.fortuneAffected()
                    ? Math.min(100.0, entry.chance() * fortMult)
                    : entry.chance();
            if (ThreadLocalRandom.current().nextDouble(100.0) >= effective) continue;

            int amount = entry.min() >= entry.max() ? entry.min()
                    : ThreadLocalRandom.current().nextInt(entry.min(), entry.max() + 1);
            ItemStack stack = resolveItem(entry.itemId(), amount);
            if (stack != null) results.add(stack);
        }
        return results;
    }

    /** Resolves an RPG item id or vanilla material name to an ItemStack. */
    private static ItemStack resolveItem(String itemId, int amount) {
        if (amount <= 0) return null;
        // Strip common namespace prefixes so "vanilla:cod" and "minecraft:cod" both work.
        String cleanId = itemId;
        if (cleanId.startsWith("vanilla:")) cleanId = cleanId.substring(8);
        else if (cleanId.startsWith("minecraft:")) cleanId = cleanId.substring(10);

        Optional<RpgItem> custom = RpgServices.items().get(cleanId);
        if (custom.isPresent()) {
            ItemStack stack = custom.get().toItemStack();
            stack.setAmount(amount);
            return stack;
        }
        Material mat = Material.matchMaterial(cleanId);
        if (mat == null || mat == Material.AIR) return null;
        return new ItemStack(mat, amount);
    }

    private static void sendCatchMessage(Player player, List<ItemStack> drops) {
        StringBuilder sb = new StringBuilder("§3✦ §fYou caught: §b");
        for (int i = 0; i < drops.size(); i++) {
            if (i > 0) sb.append("§f, §b");
            ItemStack item = drops.get(i);
            ItemMeta meta = item.getItemMeta();
            String name = (meta != null && meta.hasDisplayName())
                    ? meta.getDisplayName()
                    : toTitleCase(item.getType().name());
            if (item.getAmount() > 1) sb.append(item.getAmount()).append("× ");
            sb.append(name);
        }
        sb.append("§f!");
        player.sendMessage(sb.toString());
    }

    /** "TROPICAL_FISH" → "Tropical Fish" */
    private static String toTitleCase(String s) {
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static double statSafe(Player player, BuiltinStat stat) {
        try {
            return RpgServices.player(player).get(stat);
        } catch (Exception ex) {
            return 0.0;
        }
    }
}
