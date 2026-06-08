package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.core.spawners.SpawnerDef;
import com.github._255_ping.rpg.core.spawners.SpawnerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 54-slot admin GUI for editing spawner properties. Opened via {@code /spawner edit <id>}.
 *
 * <p>Layout:
 * <pre>
 *   Slot  4: Title (SPAWNER) — shows spawner id and mob.
 *   Slot 10: Max Alive       — click → sign entry → update.
 *   Slot 12: Cooldown Ticks  — click → sign entry → update.
 *   Slot 13: Spawn Radius    — click → sign entry → update.
 *   Slot 16: Continuous      — click → toggle.
 *   Slot 21: Min Level       — click → sign entry → update.
 *   Slot 23: Max Level       — click → sign entry → update.
 *   Slot 24: Location        — read-only info.
 *   Slot 49: Close button.
 * </pre>
 *
 * <p>All changes are persisted via {@link SpawnerManager#saveOne} immediately after each edit.
 */
public final class SpawnerGui implements Listener {

    // Slot assignments
    private static final int SLOT_TITLE      =  4;
    private static final int SLOT_MAX_ALIVE  = 10;
    private static final int SLOT_COOLDOWN   = 12;
    private static final int SLOT_RADIUS     = 13;
    private static final int SLOT_CONTINUOUS = 16;
    private static final int SLOT_MIN_LEVEL  = 21;
    private static final int SLOT_MAX_LEVEL  = 23;
    private static final int SLOT_LOCATION   = 24;
    private static final int SLOT_CLOSE      = 49;

    private final JavaPlugin   plugin;
    private final SpawnerManager manager;

    /** Maps viewer UUID → spawner id they have open. */
    private final Map<UUID, String>    viewerToSpawner = new HashMap<>();
    /** Maps inventory instance → viewer UUID (so we can match close events). */
    private final Map<Inventory, UUID> invToViewer     = new HashMap<>();

    public SpawnerGui(JavaPlugin plugin, SpawnerManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    // ── Open ─────────────────────────────────────────────────────────────

    public void open(Player player, SpawnerDef def) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("⚙ Spawner: " + def.id(), NamedTextColor.GOLD));

        // Fill background and close button via GuiConfig if available, else raw glass
        try {
            RpgServices.guiConfig().fillBackground(inv);
            inv.setItem(SLOT_CLOSE, closeBtnItem());
        } catch (Exception ignored) {
            // guiConfig may not provide fillBackground on all API versions
            ItemStack bg = bgPane();
            for (int i = 0; i < 54; i++) inv.setItem(i, bg);
            inv.setItem(SLOT_CLOSE, closeBtnItem());
        }

        populateSlots(inv, def);

        UUID id = player.getUniqueId();
        viewerToSpawner.put(id, def.id());
        invToViewer.put(inv, id);
        player.openInventory(inv);
    }

    /** Rebuilds all field items in-place (called after editing a field). */
    private void populateSlots(Inventory inv, SpawnerDef def) {
        inv.setItem(SLOT_TITLE, titleItem(def));
        inv.setItem(SLOT_MAX_ALIVE, fieldItem(Material.COMPARATOR, "Max Alive",
                String.valueOf(def.maxAlive()),
                List.of("§7Max mobs alive from this spawner.", "§eClick to edit.")));
        inv.setItem(SLOT_COOLDOWN, fieldItem(Material.CLOCK, "Cooldown Ticks",
                def.cooldownTicks() + "t (" + (def.cooldownTicks() / 20) + "s)",
                List.of("§7Ticks between spawn attempts.", "§eClick to edit (ticks).")));
        inv.setItem(SLOT_RADIUS, fieldItem(Material.COMPASS, "Spawn Radius",
                String.valueOf(def.spawnRadius()),
                List.of("§7Radius (blocks) around the spawner.", "§eClick to edit.")));
        inv.setItem(SLOT_CONTINUOUS, toggleItem(def.continuous()));
        inv.setItem(SLOT_MIN_LEVEL, fieldItem(Material.LIME_DYE, "Min Level",
                String.valueOf(def.minLevel()),
                List.of("§7Minimum mob level (≥1).", "§eClick to edit.")));
        inv.setItem(SLOT_MAX_LEVEL, fieldItem(Material.RED_DYE, "Max Level",
                String.valueOf(def.maxLevel()),
                List.of("§7Maximum mob level (≥ min level).", "§eClick to edit.")));
        inv.setItem(SLOT_LOCATION, fieldItem(Material.GRASS_BLOCK, "Location",
                def.worldName() + "  " + def.x() + ", " + def.y() + ", " + def.z(),
                List.of("§8Read-only.", "§8Delete and recreate to move.")));
    }

    // ── Events ────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID id = player.getUniqueId();
        if (!viewerToSpawner.containsKey(id)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (!invToViewer.containsKey(topInv)) return;

        // Block bottom-inventory interaction
        if (event.getRawSlot() >= topInv.getSize()) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        String spawnerId = viewerToSpawner.get(id);
        SpawnerDef def = manager.get(spawnerId).orElse(null);
        if (def == null) { player.closeInventory(); return; }

        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_CLOSE -> player.closeInventory();
            case SLOT_CONTINUOUS -> {
                def.setContinuous(!def.continuous());
                manager.saveOne(spawnerId);
                topInv.setItem(SLOT_CONTINUOUS, toggleItem(def.continuous()));
                player.sendMessage(Component.text("Continuous → " + def.continuous(),
                        NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            }
            case SLOT_MAX_ALIVE  -> promptInt(player, def, topInv, "Max Alive", 1, Integer.MAX_VALUE,
                    val -> def.setMaxAlive(val),   SLOT_MAX_ALIVE);
            case SLOT_COOLDOWN   -> promptInt(player, def, topInv, "Cooldown (ticks)", 1, Integer.MAX_VALUE,
                    val -> def.setCooldownTicks(val), SLOT_COOLDOWN);
            case SLOT_RADIUS     -> promptInt(player, def, topInv, "Spawn Radius", 0, 128,
                    val -> def.setSpawnRadius(val),  SLOT_RADIUS);
            case SLOT_MIN_LEVEL  -> promptInt(player, def, topInv, "Min Level", 1, Integer.MAX_VALUE,
                    val -> def.setMinLevel(val),     SLOT_MIN_LEVEL);
            case SLOT_MAX_LEVEL  -> promptInt(player, def, topInv, "Max Level", 1, Integer.MAX_VALUE,
                    val -> def.setMaxLevel(val),     SLOT_MAX_LEVEL);
            default -> { /* decorative slots — no action */ }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID id = player.getUniqueId();
        if (!viewerToSpawner.containsKey(id)) return;
        Inventory inv = event.getInventory();
        if (!invToViewer.containsKey(inv)) return;
        viewerToSpawner.remove(id);
        invToViewer.remove(inv);
    }

    // ── Sign-input helper ─────────────────────────────────────────────────

    private void promptInt(Player player, SpawnerDef def, Inventory inv,
                           String label, int min, int max,
                           Consumer<Integer> apply, int refreshSlot) {
        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                RpgServices.signInput().ask(player, label, input -> {
                    if (input == null) {
                        // Cancelled — reopen on main thread
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> open(player, def));
                        return;
                    }
                    try {
                        int value = Integer.parseInt(input.trim());
                        if (value < min || value > max) {
                            player.sendMessage(Component.text(
                                    "Value must be between " + min + " and " + max + ".",
                                    NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                        } else {
                            apply.accept(value);
                            manager.saveOne(def.id());
                            player.sendMessage(Component.text(
                                    "Set " + label + " → " + value + ".",
                                    NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                        }
                    } catch (NumberFormatException ex) {
                        player.sendMessage(Component.text("Invalid number: " + input,
                                NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                    }
                    // Reopen the GUI after sign closes
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> open(player, def));
                }), 1L);
    }

    // ── Item builders ─────────────────────────────────────────────────────

    private static ItemStack titleItem(SpawnerDef def) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("⚙ Spawner: " + def.id(), NamedTextColor.GOLD,
                    TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Mob: " + def.mobId(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click fields below to edit.", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack fieldItem(Material mat, String name, String value, List<String> desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Value: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(value, NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            for (String d : desc) {
                lore.add(Component.text(stripLegacy(d), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack toggleItem(boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Continuous", enabled ? NamedTextColor.GREEN : NamedTextColor.RED,
                    TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Spawner re-triggers on cooldown.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Status: ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(enabled ? "✔ Enabled" : "✘ Disabled",
                                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Click to toggle.", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack closeBtnItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Close", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack bgPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Strips leading §x color/format codes from a legacy-style string for Component.text(). */
    private static String stripLegacy(String s) {
        return s.replaceAll("(?i)§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }
}
