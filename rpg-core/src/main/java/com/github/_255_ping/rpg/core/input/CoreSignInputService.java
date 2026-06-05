package com.github._255_ping.rpg.core.input;

import com.github._255_ping.rpg.api.input.SignInputService;
import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sign-backed text input using Paper's virtual sign API.
 *
 * <p>The sign is sent to the prompting player's client only via
 * {@link Player#sendBlockChange} + {@link Player#sendBlockUpdate}; no real block is
 * placed and other players see nothing. The server reads the typed text from
 * {@link UncheckedSignChangeEvent} when the player clicks Done.
 *
 * <p>Must be registered as a Bukkit listener (done in {@code RpgCorePlugin.onEnable}).
 */
public final class CoreSignInputService implements SignInputService, Listener {

    /** Seconds before the sign prompt auto-cancels. */
    private static final long TIMEOUT_TICKS = 20L * 60L; // 60 seconds

    private final Plugin plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public CoreSignInputService(Plugin plugin) {
        this.plugin = plugin;
    }

    // ── API ───────────────────────────────────────────────────────────────────

    @Override
    public void ask(Player player, String label, Consumer<String> callback) {
        UUID id = player.getUniqueId();
        cancelSilently(id); // cancel any existing prompt without null-callback side-effects

        // The sign must be within the client's interaction distance to stay open.
        // Placing it at the player's feet block is always in range.
        Location loc = player.getLocation().getBlock().getLocation();
        World world = loc.getWorld();
        if (world == null) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            return;
        }

        // Build the virtual sign state with the prompt label.
        BlockData signData = Material.OAK_SIGN.createBlockData();
        BlockState bs = signData.createBlockState();
        if (!(bs instanceof Sign sign)) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            return;
        }

        SignSide front = sign.getSide(Side.FRONT);
        front.line(0, Component.empty()); // line 0 is where the player types
        front.line(1, Component.text("^^^^^^^^^^^^^^^", NamedTextColor.DARK_GRAY));
        front.line(2, Component.text(truncate(label, 15), NamedTextColor.GOLD));
        front.line(3, Component.text("(press Done)", NamedTextColor.GRAY));

        BlockData original = world.getBlockAt(loc).getBlockData();

        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Pending p = pending.remove(id);
            if (p == null) return;
            Player online = Bukkit.getPlayer(id);
            if (online != null) {
                online.sendBlockChange(p.location(), p.originalData());
                online.sendActionBar(Component.text("Prompt timed out.", NamedTextColor.RED));
            }
            p.callback().accept(null);
        }, TIMEOUT_TICKS);

        pending.put(id, new Pending(callback, loc.clone(), original, timeout));

        // Send the fake block + tile entity, then open the sign editor next tick
        // so the client has time to process the block update first.
        player.sendBlockChange(loc, signData);
        player.sendBlockUpdate(loc, sign);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            Position pos = Position.block(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            player.openVirtualSign(pos, Side.FRONT);
        });
    }

    @Override
    public void cancel(UUID id) {
        Pending p = pending.remove(id);
        if (p == null) return;
        p.timeout().cancel();
        Player online = Bukkit.getPlayer(id);
        if (online != null) online.sendBlockChange(p.location(), p.originalData());
        p.callback().accept(null);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(UncheckedSignChangeEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Pending p = pending.remove(id);
        if (p == null) return;

        p.timeout().cancel();

        // Read line 0 — the editable line in the sign editor.
        List<Component> lines = event.lines();
        String input = lines.isEmpty() ? ""
                : PlainTextComponentSerializer.plainText().serialize(lines.get(0)).trim();

        // Cancel the event so the server doesn't try to update the real block.
        event.setCancelled(true);

        // Restore whatever block was actually at that location in the player's view.
        event.getPlayer().sendBlockChange(p.location(), p.originalData());

        String result = input.isEmpty() ? null : input;
        Bukkit.getScheduler().runTask(plugin, () -> p.callback().accept(result));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Pending p = pending.remove(event.getPlayer().getUniqueId());
        if (p == null) return;
        // Player disconnected — cancel the timeout, no block restore needed,
        // no callback (the GUI that opened the prompt is gone too).
        p.timeout().cancel();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /** Like {@link #cancel} but does NOT fire the callback. Used before re-prompting. */
    private void cancelSilently(UUID id) {
        Pending p = pending.remove(id);
        if (p == null) return;
        p.timeout().cancel();
        Player online = Bukkit.getPlayer(id);
        if (online != null) online.sendBlockChange(p.location(), p.originalData());
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record Pending(
            Consumer<String> callback,
            Location location,
            BlockData originalData,
            BukkitTask timeout
    ) {}
}
