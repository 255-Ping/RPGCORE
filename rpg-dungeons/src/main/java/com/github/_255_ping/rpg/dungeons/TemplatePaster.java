package com.github._255_ping.rpg.dungeons;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

/**
 * Copies blocks from a template region to a destination origin in budgeted slices, then captures
 * the BlockState (tile-entity NBT) of any block that has one — chests, signs, furnaces, command
 * blocks, banners, etc.
 *
 * <p>The paste is driven by a Bukkit scheduler task that walks the cuboid in row-major order and
 * processes {@code blocks-per-tick} blocks per tick. The {@code onDone} callback fires on the
 * main thread once every block (and its state snapshot, when applicable) has been written.
 */
public final class TemplatePaster {

    private final JavaPlugin plugin;
    private final int blocksPerTick;

    public TemplatePaster(JavaPlugin plugin, int blocksPerTick) {
        this.plugin = plugin;
        this.blocksPerTick = Math.max(64, blocksPerTick);
    }

    /**
     * Schedule a paste. Returns immediately; {@code onDone} fires on completion. {@code min} and
     * {@code max} are inclusive corners; {@code destOrigin} is where the min corner lands.
     */
    public void pasteAsync(World templateWorld, Vector min, Vector max,
                           World destWorld, Vector destOrigin, Consumer<Integer> onDone) {
        if (templateWorld == null || destWorld == null) {
            onDone.accept(0);
            return;
        }
        int minX = Math.min((int) min.getX(), (int) max.getX());
        int minY = Math.min((int) min.getY(), (int) max.getY());
        int minZ = Math.min((int) min.getZ(), (int) max.getZ());
        int maxX = Math.max((int) min.getX(), (int) max.getX());
        int maxY = Math.max((int) min.getY(), (int) max.getY());
        int maxZ = Math.max((int) min.getZ(), (int) max.getZ());
        int oX = (int) destOrigin.getX();
        int oY = (int) destOrigin.getY();
        int oZ = (int) destOrigin.getZ();

        final int xMin = minX, xMax = maxX, zMin = minZ, zMax = maxZ, yMax = maxY;

        new BukkitRunnable() {
            int x = xMin;
            int y = minY;
            int z = zMin;
            int copied = 0;

            @Override
            public void run() {
                int budget = blocksPerTick;
                while (budget-- > 0 && y <= yMax) {
                    Block src = templateWorld.getBlockAt(x, y, z);
                    Block dst = destWorld.getBlockAt(oX + (x - xMin), oY + (y - minY), oZ + (z - zMin));
                    BlockData data = src.getBlockData();
                    dst.setBlockData(data, false);

                    if (hasTileEntity(src.getType())) {
                        try {
                            BlockState srcState = src.getState(false);
                            // copy() lets us retarget the snapshot to the new location, then
                            // update() writes the NBT (inventory, sign lines, etc.) into the dest.
                            BlockState dstSnapshot = srcState.copy(dst.getLocation());
                            dstSnapshot.update(true, false);
                        } catch (Exception ex) {
                            plugin.getLogger().fine("Block-state copy failed at "
                                    + x + "," + y + "," + z + ": " + ex.getMessage());
                        }
                    }
                    copied++;
                    advance();
                }
                if (y > yMax) {
                    cancel();
                    onDone.accept(copied);
                }
            }

            private void advance() {
                x++;
                if (x > xMax) {
                    x = xMin;
                    z++;
                    if (z > zMax) {
                        z = zMin;
                        y++;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Wipes the destination instance area back to air (synchronous; runs on small areas). */
    public void clear(World destWorld, Vector destOrigin, Vector min, Vector max) {
        if (destWorld == null) return;
        int sizeX = (int) Math.abs(max.getX() - min.getX()) + 1;
        int sizeY = (int) Math.abs(max.getY() - min.getY()) + 1;
        int sizeZ = (int) Math.abs(max.getZ() - min.getZ()) + 1;
        int oX = (int) destOrigin.getX();
        int oY = (int) destOrigin.getY();
        int oZ = (int) destOrigin.getZ();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    Block dst = destWorld.getBlockAt(oX + x, oY + y, oZ + z);
                    if (dst.getType() != Material.AIR) dst.setType(Material.AIR, false);
                }
            }
        }
    }

    private static boolean hasTileEntity(Material mat) {
        String n = mat.name();
        return n.endsWith("_SIGN") || n.endsWith("_HANGING_SIGN")
                || n.endsWith("_BANNER") || n.endsWith("_HEAD") || n.endsWith("_SKULL")
                || n.equals("CHEST") || n.equals("TRAPPED_CHEST") || n.equals("BARREL")
                || n.equals("FURNACE") || n.equals("BLAST_FURNACE") || n.equals("SMOKER")
                || n.equals("BREWING_STAND") || n.equals("ENCHANTING_TABLE")
                || n.equals("DISPENSER") || n.equals("DROPPER") || n.equals("HOPPER")
                || n.equals("COMMAND_BLOCK") || n.equals("CHAIN_COMMAND_BLOCK") || n.equals("REPEATING_COMMAND_BLOCK")
                || n.equals("LECTERN") || n.equals("JUKEBOX") || n.equals("BEACON")
                || n.equals("STRUCTURE_BLOCK") || n.equals("JIGSAW")
                || n.equals("SPAWNER") || n.equals("TRIAL_SPAWNER") || n.equals("VAULT")
                || n.equals("DECORATED_POT") || n.equals("END_GATEWAY") || n.equals("END_PORTAL");
    }
}
