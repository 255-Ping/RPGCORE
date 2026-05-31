package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.Block;
import com.github._255_ping.rpg.api.blocks.RequiredToolType;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import com.github._255_ping.rpg.core.drops.DropManager;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hold-to-break for custom blocks.
 *
 * <p>Vanilla break-time is replaced. When a player starts damaging a tagged block, we
 * start a per-tick task that drains {@code MINING_SPEED} HP/sec from a per-block-instance
 * counter initialized to {@code Toughness}. We push the {@link Player#sendBlockDamage}
 * packet each tick to draw the 0-9 break stage. On 0, the break flow runs (drops + respawn).
 *
 * <p>BlockBreakEvent on a tagged block always cancels — only this handler can complete
 * the break. PlayerInteractEvent left-click on air clears progress, as does
 * {@code BlockDamageAbortEvent} and quitting / swapping items.
 */
public final class BlockBreakHandler implements Listener {

    private final RpgCorePlugin plugin;
    private final CoreBlockRegistry registry;
    private final DropManager dropManager;
    private final BlockHologramService hologramService;
    private final Map<UUID, BlockBreakProgress> active = new HashMap<>();
    private BukkitTask tickTask;

    public BlockBreakHandler(RpgCorePlugin plugin, CoreBlockRegistry registry,
                             DropManager dropManager, BlockHologramService hologramService) {
        this.plugin = plugin;
        this.registry = registry;
        this.dropManager = dropManager;
        this.hologramService = hologramService;
    }

    public void start() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

    public void stop() {
        if (tickTask != null) tickTask.cancel();
    }

    // ----- Event entry points -----

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(BlockDamageEvent event) {
        Location loc = event.getBlock().getLocation();
        Optional<Block> opt = registry.at(loc);
        if (opt.isEmpty()) return;
        Block block = opt.get();
        Player player = event.getPlayer();

        // Creative bypass: instant-break the tagged block, no progress.
        // Only admins (rpg.admin) in creative mode can break custom blocks.
        if (player.getGameMode() == GameMode.CREATIVE
                && player.hasPermission("rpg.admin")
                && plugin.getConfig().getBoolean("creative-mode-bypass.break-time", true)) {
            event.setInstaBreak(true);
            return;
        }

        event.setCancelled(true);

        if (!gatesPass(player, block)) return;

        BlockBreakProgress existing = active.get(player.getUniqueId());
        if (existing != null && existing.location.equals(loc)) {
            // Continuing on the same block — refresh the click timestamp only.
            existing.lastClickMs = System.currentTimeMillis();
        } else {
            // New block — start fresh progress.
            BlockBreakProgress progress = new BlockBreakProgress(loc, block);
            progress.lastClickMs = System.currentTimeMillis();
            active.put(player.getUniqueId(), progress);
        }
    }

    @EventHandler
    public void onAbort(BlockDamageAbortEvent event) {
        clearProgress(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!registry.hasTag(loc)) return;

        // Creative bypass — let the break go through, but un-tag.
        // Only admins (rpg.admin) in creative mode can break custom blocks.
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE
                && event.getPlayer().hasPermission("rpg.admin")
                && plugin.getConfig().getBoolean("creative-mode-bypass.break-time", true)) {
            hologramService.despawnAt(loc);
            registry.untagLocation(loc);
            event.setDropItems(false);
            return;
        }

        // Survival: cancel — only our tick can complete the break.
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearProgress(event.getPlayer());
    }

    @EventHandler
    public void onSlotSwitch(PlayerItemHeldEvent event) {
        clearProgress(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR) {
            clearProgress(event.getPlayer());
        }
    }

    // Some setups use BlockDestroyEvent (e.g., piston pushes) — keep our state coherent.
    @EventHandler
    public void onDestroy(BlockDestroyEvent event) {
        Location loc = event.getBlock().getLocation();
        if (registry.hasTag(loc)) {
            hologramService.despawnAt(loc);
            registry.untagLocation(loc);
        }
    }

    // ----- Tick loop -----

    private void tickAll() {
        if (active.isEmpty()) return;
        active.entrySet().removeIf(entry -> {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            BlockBreakProgress progress = entry.getValue();
            if (player == null || !player.isOnline()) return true;
            if (!registry.hasTag(progress.location)) return true;

            // Require the player to be holding left-click: if no BlockDamageEvent arrived in
            // the last 400 ms, the player released the mouse button — cancel progress.
            if (System.currentTimeMillis() - progress.lastClickMs > 400) {
                player.sendBlockDamage(progress.location, 0f);
                return true;
            }

            // Distance check: player must be within 6 blocks of the target.
            Location blockCenter = progress.location.clone().add(0.5, 0.5, 0.5);
            if (player.getWorld() != progress.location.getWorld() ||
                    player.getLocation().distanceSquared(blockCenter) > 36) {
                player.sendBlockDamage(progress.location, 0f);
                return true;
            }

            // Still aiming at the same block?
            org.bukkit.block.Block targeted = player.getTargetBlockExact(8);
            if (targeted == null || !targeted.getLocation().equals(progress.location)) {
                player.sendBlockDamage(progress.location, 0f);
                return true;
            }

            double miningSpeed = RpgServices.player(player).get(BuiltinStat.MINING_SPEED);
            if (miningSpeed <= 0) miningSpeed = 1.0;     // 1 HP/sec floor so plain hands still progress
            double perTick = miningSpeed / 20.0;
            progress.remainingHp -= perTick;

            if (progress.remainingHp <= 0) {
                completeBreak(player, progress);
                return true;
            }

            // Update the 0-9 break stage packet.
            double pct = 1.0 - (progress.remainingHp / progress.definition.toughness());
            int stage = Math.max(0, Math.min(9, (int) Math.floor(pct * 10)));
            if (stage != progress.lastStage) {
                progress.lastStage = stage;
                player.sendBlockDamage(progress.location, (float) Math.min(0.99, pct));
            }
            return false;
        });
    }

    private void completeBreak(Player player, BlockBreakProgress progress) {
        Block block = progress.definition;
        Location loc = progress.location;
        player.sendBlockDamage(loc, 0f);    // clear the break-stage overlay

        com.github._255_ping.rpg.api.blocks.RpgBlockBreakEvent rpgEvent =
                new com.github._255_ping.rpg.api.blocks.RpgBlockBreakEvent(player, block, loc);
        plugin.getServer().getPluginManager().callEvent(rpgEvent);
        if (rpgEvent.isCancelled()) return;

        hologramService.despawnAt(loc);
        registry.untagLocation(loc);

        // Break the world block — set to air, then roll our drops.
        if (loc.getWorld() != null) {
            loc.getBlock().setType(Material.AIR);
        }

        rollDrops(block, loc, player);

        if (block.respawnTicks() > 0 && loc.getWorld() != null) {
            loc.getBlock().setType(block.respawnPlaceholder());
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (loc.getWorld() == null) return;
                loc.getBlock().setType(block.material());
                registry.tagLocation(loc, block.id());
                hologramService.spawnAt(loc, block);   // re-spawn hologram after block respawns
            }, block.respawnTicks());
        }
    }

    // ----- Helpers -----

    private boolean gatesPass(Player player, Block block) {
        double breakingPower = RpgServices.player(player).get(BuiltinStat.BREAKING_POWER);
        if (block.requiredPower() > breakingPower) {
            sendPriorityActionBar(player, plugin.messages().component("block.power-too-low",
                    java.util.Map.of("required", block.requiredPower())));
            return false;
        }
        if (!toolMatches(block.requiredToolType(), player.getInventory().getItemInMainHand())) {
            sendPriorityActionBar(player, plugin.messages().component("block.wrong-tool",
                    java.util.Map.of("tool", block.requiredToolType().name().toLowerCase())));
            return false;
        }
        return true;
    }

    /** Sends an action bar message via the priority service (1 second) so it isn't instantly overridden by the idle HUD. */
    private static void sendPriorityActionBar(Player player, net.kyori.adventure.text.Component msg) {
        try {
            RpgServices.actionBar().send(player, msg, 20);
        } catch (IllegalStateException ignored) {
            player.sendActionBar(msg);
        }
    }

    private void clearProgress(Player player) {
        BlockBreakProgress progress = active.remove(player.getUniqueId());
        if (progress != null) player.sendBlockDamage(progress.location, 0f);
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

    /**
     * Rolls drops for a broken block, applying the player's {@code MINING_FORTUNE} stat.
     *
     * <p>Fortune formula: every 100 points = +1 guaranteed extra drop multiplier. The
     * fractional part is a probabilistic extra (50 fortune → 50 % chance of an extra
     * multiply). Total multiplier = 1 + floor(fortune/100) + chanceRoll.
     */
    private void rollDrops(Block block, Location loc, Player player) {
        if (loc.getWorld() == null) return;

        double fortune = RpgServices.player(player).get(BuiltinStat.MINING_FORTUNE);
        int guaranteed = (int) (fortune / 100.0);
        double chance   = (fortune % 100.0) / 100.0;
        int fortuneMult = 1 + guaranteed
                + (ThreadLocalRandom.current().nextDouble() < chance ? 1 : 0);

        for (String spec : block.dropSpecs()) {
            try {
                ItemStack drop = parseDrop(spec);
                if (drop == null) continue;
                if (fortuneMult > 1) {
                    drop.setAmount(Math.min(drop.getAmount() * fortuneMult,
                            drop.getType().getMaxStackSize()));
                }
                org.bukkit.entity.Item dropped = loc.getWorld().dropItemNaturally(loc, drop);
                dropManager.register(dropped, player);
            } catch (Exception ex) {
                plugin.getLogger().warning(
                        "Bad drop spec '" + spec + "' on block " + block.id() + ": " + ex.getMessage());
            }
        }
    }

    /** Parses {@code "[file] <itemId> <min>[-<max>]"}. The file token is optional. */
    private static ItemStack parseDrop(String spec) {
        String[] parts = spec.trim().split("\\s+");
        if (parts.length < 2) throw new IllegalArgumentException("expected at least 2 tokens, got: " + spec);

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
