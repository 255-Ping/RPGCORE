package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.core.player.PlayerPreferencesService;
import com.github._255_ping.rpg.core.player.PlayerPreferencesService.PlayerPreferences;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 54-slot player-preferences GUI opened by the crafting-nav Settings button or {@code /settings}.
 *
 * <h3>Layout</h3>
 * <pre>
 * Slot  4: ⚙ Settings  (COMPARATOR, decorative title)
 * Slot 10: Party HUD       toggle  (GOLDEN_APPLE)
 * Slot 11: Sound Effects   toggle  (NOTE_BLOCK)
 * Slot 12: Damage Numbers  toggle  (PAPER)
 * Slot 13: Show on Leaderboard toggle (COMPASS)
 * Slot 49: ❌ Close
 * </pre>
 *
 * <p>Clicking a toggle item flips the flag immediately, updates the icon in place, and the
 * change is flushed to disk the next time {@link com.github._255_ping.rpg.core.player.PlayerLifecycleListener}
 * processes the player's quit.
 *
 * <p><b>Integration notes</b> (each toggle still needs to be wired):
 * <ul>
 *   <li>Party HUD → {@code rpg-parties} PartyHudTask should check this flag via PreferencesService.</li>
 *   <li>Sound Effects → RPG sound-play sites should check this flag before playing.</li>
 *   <li>Damage Numbers → {@code DamageIndicatorListener} should check this flag before spawning holograms.</li>
 *   <li>Show on Leaderboard → Leaderboard query should filter out players with this off.</li>
 * </ul>
 */
public final class SettingsGui implements Listener {

    private static final int SLOT_TITLE       =  4;
    private static final int SLOT_PARTY_HUD   = 10;
    private static final int SLOT_SOUNDS      = 11;
    private static final int SLOT_DMG_NUMBERS = 12;
    private static final int SLOT_LEADERBOARD = 13;

    private final PlayerPreferencesService prefsService;

    private final Map<UUID, Boolean>   viewers     = new HashMap<>();
    private final Map<Inventory, UUID> invToViewer = new HashMap<>();

    public SettingsGui(PlayerPreferencesService prefsService) {
        this.prefsService = prefsService;
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("⚙ Settings", NamedTextColor.GRAY));

        PlayerPreferences prefs = prefsService.get(viewer.getUniqueId());

        inv.setItem(SLOT_TITLE,       buildTitle());
        inv.setItem(SLOT_PARTY_HUD,   buildToggle(Material.GOLDEN_APPLE, "Party HUD",
                "Shows party members' HP in your action bar.", prefs.partyHudEnabled));
        inv.setItem(SLOT_SOUNDS,      buildToggle(Material.NOTE_BLOCK, "Sound Effects",
                "Plays RPG ability and UI sounds.", prefs.soundEffectsEnabled));
        inv.setItem(SLOT_DMG_NUMBERS, buildToggle(Material.PAPER, "Damage Numbers",
                "Shows floating damage numbers when entities are hit.", prefs.damageNumbersEnabled));
        inv.setItem(SLOT_LEADERBOARD, buildToggle(Material.COMPASS, "Show on Leaderboard",
                "Whether your name appears on public leaderboards.", prefs.showOnLeaderboard));

        RpgServices.guiConfig().fillBackground(inv);
        RpgServices.guiConfig().placeNavBar(inv);

        UUID id = viewer.getUniqueId();
        viewers.put(id, true);
        invToViewer.put(inv, id);
        viewer.openInventory(inv);
    }

    // ── Events ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID id = viewer.getUniqueId();
        if (!viewers.containsKey(id)) return;
        if (!invToViewer.containsKey(event.getInventory())) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (RpgServices.guiConfig().isCloseButton(clicked)) {
            viewer.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        PlayerPreferences prefs = prefsService.get(id);

        switch (slot) {
            case SLOT_PARTY_HUD -> {
                prefs.partyHudEnabled = !prefs.partyHudEnabled;
                event.getInventory().setItem(slot, buildToggle(Material.GOLDEN_APPLE, "Party HUD",
                        "Shows party members' HP in your action bar.", prefs.partyHudEnabled));
            }
            case SLOT_SOUNDS -> {
                prefs.soundEffectsEnabled = !prefs.soundEffectsEnabled;
                event.getInventory().setItem(slot, buildToggle(Material.NOTE_BLOCK, "Sound Effects",
                        "Plays RPG ability and UI sounds.", prefs.soundEffectsEnabled));
            }
            case SLOT_DMG_NUMBERS -> {
                prefs.damageNumbersEnabled = !prefs.damageNumbersEnabled;
                event.getInventory().setItem(slot, buildToggle(Material.PAPER, "Damage Numbers",
                        "Shows floating damage numbers when entities are hit.", prefs.damageNumbersEnabled));
            }
            case SLOT_LEADERBOARD -> {
                prefs.showOnLeaderboard = !prefs.showOnLeaderboard;
                event.getInventory().setItem(slot, buildToggle(Material.COMPASS, "Show on Leaderboard",
                        "Whether your name appears on public leaderboards.", prefs.showOnLeaderboard));
            }
            default -> { }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        UUID id = viewer.getUniqueId();
        if (!viewers.containsKey(id)) return;
        Inventory inv = event.getInventory();
        if (!invToViewer.containsKey(inv)) return;
        viewers.remove(id);
        invToViewer.remove(inv);
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private static ItemStack buildTitle() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text("⚙ Settings", NamedTextColor.GRAY, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click a setting below to toggle it.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildToggle(Material mat, String name, String description, boolean enabled) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        NamedTextColor nameColor = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;
        String statusLabel = enabled ? "✔ Enabled" : "✘ Disabled";
        NamedTextColor statusColor = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;

        meta.displayName(Component.text(name, nameColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(description, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Status: ", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(statusLabel, statusColor)
                                .decoration(TextDecoration.ITALIC, false)),
                Component.text("Click to toggle.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
