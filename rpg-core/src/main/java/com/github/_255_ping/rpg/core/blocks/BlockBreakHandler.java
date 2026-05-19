package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.Block;
import com.github._255_ping.rpg.api.blocks.RequiredToolType;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * v1 block-break handler: instant break on click (no hold-to-break progress bar yet).
 * Checks BREAKING_POWER and RequiredToolType gates, rolls drops, schedules respawn.
 * Progress-bar UX arrives in a polish slice.
 */
public final class BlockBreakHandler implements Listener {

    private final RpgCorePlugin plugin;
    private final CoreBlockRegistry registry;

    public BlockBreakHandler(RpgCorePlugin plugin, CoreBlockRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        Optional<Block> opt = registry.at(loc);
        if (opt.isEmpty()) return;
        Block block = opt.get();

        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE
                && plugin.getConfig().getBoolean("creative-mode-bypass.break-time", true)) {
            registry.untagLocation(loc);
            return;
        }

        double breakingPower = RpgServices.player(player).get(BuiltinStat.BREAKING_POWER);
        if (block.requiredPower() > breakingPower) {
            event.setCancelled(true);
            player.sendActionBar(plugin.messages().component("block.power-too-low",
                    java.util.Map.of("required", block.requiredPower())));
            return;
        }

        if (!toolMatches(block.requiredToolType(), player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            player.sendActionBar(plugin.messages().component("block.wrong-tool",
                    java.util.Map.of("tool", block.requiredToolType().name().toLowerCase())));
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);
        registry.untagLocation(loc);

        rollDrops(block, loc);

        if (block.respawnTicks() > 0) {
            event.getBlock().setType(block.respawnPlaceholder());
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (loc.getWorld() == null) return;
                loc.getBlock().setType(block.material());
                registry.tagLocation(loc, block.id());
            }, block.respawnTicks());
        }
    }

    private static boolean toolMatches(RequiredToolType required, ItemStack hand) {
        if (required == RequiredToolType.ANY) return true;
        if (required == RequiredToolType.NONE) return hand == null || hand.getType().isAir();
        if (hand == null) return false;
        String typeName = hand.getType().name();
        return switch (required) {
            case PICKAXE -> typeName.endsWith("_PICKAXE");
            case AXE -> typeName.endsWith("_AXE") && !typeName.endsWith("_PICKAXE");
            case SHOVEL -> typeName.endsWith("_SHOVEL");
            case HOE -> typeName.endsWith("_HOE");
            default -> true;
        };
    }

    private void rollDrops(Block block, Location loc) {
        if (loc.getWorld() == null) return;
        for (String spec : block.dropSpecs()) {
            try {
                ItemStack drop = parseDrop(spec);
                if (drop != null) {
                    loc.getWorld().dropItemNaturally(loc, drop);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Bad drop spec '" + spec + "' on block " + block.id() + ": " + ex.getMessage());
            }
        }
    }

    /** Parses {@code "<itemId> <count>"} or {@code "<itemId> <min>-<max>"} (optional leading file token). */
    private static ItemStack parseDrop(String spec) {
        String[] parts = spec.trim().split("\\s+");
        if (parts.length < 2) throw new IllegalArgumentException("expected at least 2 tokens, got: " + spec);

        // If we have 3 tokens, the first is the file/category and we drop it.
        int idIdx = parts.length >= 3 ? 1 : 0;
        int rangeIdx = idIdx + 1;
        String itemId = parts[idIdx];
        String range = parts[rangeIdx];

        int min, max;
        if (range.contains("-")) {
            String[] r = range.split("-", 2);
            min = Integer.parseInt(r[0]);
            max = Integer.parseInt(r[1]);
        } else {
            min = max = Integer.parseInt(range);
        }
        int amount = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        if (amount <= 0) return null;

        String cleanId = itemId;
        if (cleanId.startsWith("vanilla:")) cleanId = cleanId.substring("vanilla:".length());
        else if (cleanId.startsWith("minecraft:")) cleanId = cleanId.substring("minecraft:".length());

        Optional<RpgItem> custom = RpgServices.items().get(cleanId);
        if (custom.isPresent()) {
            ItemStack stack = custom.get().toItemStack();
            stack.setAmount(amount);
            return stack;
        }
        Material mat = Material.matchMaterial(cleanId);
        if (mat == null) throw new IllegalArgumentException("unknown item or material: " + itemId);
        return new ItemStack(mat, amount);
    }
}
