package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.BuiltinItemType;
import com.github._255_ping.rpg.api.items.RpgItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Paginated 54-slot GUI for browsing all registered RPG items.
 *
 * <p>Layout:
 * <ul>
 *   <li>Row 0 (slots 0-8): control bar — Prev, Type, Rarity, Summary, Search, Next.</li>
 *   <li>Rows 1-4 (slots 9-44): up to 36 item cards per page.</li>
 *   <li>Row 5 (slots 45-53): standard nav bar (Close at 49).</li>
 * </ul>
 *
 * <p>Clicking an item card gives 1 (or 64 on shift-click) to the viewer's inventory
 * if they have {@code rpg.core.item.give}. Type and Rarity buttons cycle their filter
 * on left/right-click. Search opens a sign-entry prompt.
 *
 * <p>State is kept across control-button clicks by repopulating the same {@link Inventory}
 * object in-place so there's no close/reopen flicker.
 */
public final class ItemBrowserGui implements Listener {

    // ── Slots ──────────────────────────────────────────────────────────────────
    private static final int SLOT_PREV    = 0;
    private static final int SLOT_TYPE    = 2;
    private static final int SLOT_RARITY  = 3;
    private static final int SLOT_SUMMARY = 4;
    private static final int SLOT_SEARCH  = 5;
    private static final int SLOT_NEXT    = 8;

    private static final int CONTENT_START = 9;
    private static final int CONTENT_END   = 44;  // inclusive
    private static final int PAGE_SIZE     = CONTENT_END - CONTENT_START + 1; // 36

    /**
     * PDC key added to each displayed item card so {@code onClick} can map a click
     * back to the originating RPG item id without maintaining a separate slot→id map.
     */
    private static final NamespacedKey BROWSE_ITEM_KEY =
            new NamespacedKey("rpg", "browse_item_id");

    /** PDC key on control-bar buttons to identify their action. */
    private static final NamespacedKey CONTROL_KEY =
            new NamespacedKey("rpg", "browse_control");

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private static final BuiltinItemType[] TYPES = BuiltinItemType.values();

    static final String PERM_BROWSE = "rpg.core.items.browse";
    private static final String PERM_GIVE = "rpg.core.item.give";

    // ── Per-viewer state ───────────────────────────────────────────────────────

    private static final class ViewState {
        int typeIndex   = -1;   // -1 = all types, 0..TYPES.length-1 = specific type
        String rarityId = null; // null = all rarities
        String search   = null; // null = no filter; matched against id + displayName
        int page        = 0;    // 0-based
    }

    private final Map<UUID, ViewState> states       = new HashMap<>();
    private final Map<UUID, Inventory> openInvs     = new HashMap<>();
    private final Map<Inventory, UUID> invToViewer  = new HashMap<>();

    /**
     * Players who clicked Search and are in the sign-input flow.
     * Their state is preserved through the inventory-close that the sign editor triggers.
     */
    private final Set<UUID> pendingSearch = new HashSet<>();

    private final JavaPlugin plugin;

    public ItemBrowserGui(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public open ────────────────────────────────────────────────────────────

    public void open(Player viewer) {
        if (!viewer.hasPermission(PERM_BROWSE)) {
            viewer.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        UUID id = viewer.getUniqueId();
        ViewState state = states.computeIfAbsent(id, k -> new ViewState());

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("✦ Item Browser", NamedTextColor.GOLD));
        populateInventory(inv, state);
        openInvs.put(id, inv);
        invToViewer.put(inv, id);
        viewer.openInventory(inv);
    }

    // ── Inventory population ───────────────────────────────────────────────────

    /**
     * Fills {@code inv} from scratch. Called on first open and on every control-button
     * click that changes the state (type/rarity/search/page). Reuses the same
     * {@link Inventory} object when called from {@link #reopen}, so the client never
     * sees a window close/reopen.
     */
    private void populateInventory(Inventory inv, ViewState state) {
        inv.clear();

        List<RpgItem> filtered = buildFilteredList(state);
        int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        // Clamp page index in case a filter change shrunk the result set
        state.page = Math.max(0, Math.min(state.page, totalPages - 1));

        int pageStart  = state.page * PAGE_SIZE;
        List<RpgItem> pageItems = filtered.subList(
                pageStart, Math.min(pageStart + PAGE_SIZE, filtered.size()));

        placeControls(inv, state, filtered.size(), totalPages);
        for (int i = 0; i < pageItems.size(); i++) {
            inv.setItem(CONTENT_START + i, buildCard(pageItems.get(i)));
        }
        RpgServices.guiConfig().fillBackground(inv);
        RpgServices.guiConfig().placeNavBar(inv);
    }

    /**
     * Repopulates the player's currently-open inventory in-place.
     * Falls back to a full close+open if somehow the inventory is no longer tracked.
     */
    private void reopen(Player viewer) {
        UUID id = viewer.getUniqueId();
        Inventory inv = openInvs.get(id);
        if (inv == null) { open(viewer); return; }
        ViewState state = states.computeIfAbsent(id, k -> new ViewState());
        populateInventory(inv, state);
        // The player is still looking at the same Inventory object — Bukkit propagates
        // content updates to the client automatically, so no openInventory call needed.
    }

    // ── Filter + sort ──────────────────────────────────────────────────────────

    private List<RpgItem> buildFilteredList(ViewState state) {
        return RpgServices.items().all().stream()
                .filter(item -> {
                    if (state.typeIndex >= 0) {
                        if (!(item.type() instanceof BuiltinItemType bt)
                                || bt != TYPES[state.typeIndex]) return false;
                    }
                    if (state.rarityId != null
                            && !state.rarityId.equals(item.rarity().id())) return false;
                    if (state.search != null) {
                        String q = state.search.toLowerCase(Locale.ROOT);
                        return item.id().toLowerCase(Locale.ROOT).contains(q)
                                || item.displayName().toLowerCase(Locale.ROOT).contains(q);
                    }
                    return true;
                })
                .sorted(Comparator
                        .<RpgItem, String>comparing(i -> i.type() != null ? i.type().id() : "",
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(i -> i.rarity() != null ? i.rarity().id() : "",
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(i -> i.displayName() != null ? i.displayName() : "",
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(i -> i.id() != null ? i.id() : "",
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // ── Control bar ────────────────────────────────────────────────────────────

    private void placeControls(Inventory inv, ViewState state, int totalItems, int totalPages) {
        boolean hasPrev = state.page > 0;
        boolean hasNext = state.page < totalPages - 1;

        inv.setItem(SLOT_PREV, buildControl(
                hasPrev ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE,
                hasPrev ? "&a← Previous Page" : "&8← Previous Page",
                "prev"));

        inv.setItem(SLOT_NEXT, buildControl(
                hasNext ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE,
                hasNext ? "&a→ Next Page" : "&8→ Next Page",
                "next"));

        // Type filter — cycles through ALL + each BuiltinItemType
        String typeLabel = state.typeIndex < 0
                ? "&fAll" : "&f" + capitalize(TYPES[state.typeIndex].id());
        inv.setItem(SLOT_TYPE, buildControlWithLore(Material.ENCHANTED_BOOK,
                "&6Type: " + typeLabel, "type",
                List.of("&7Left-click: cycle →", "&7Right-click: cycle ←")));

        // Rarity filter — cycles through ALL + each distinct rarity in the registry
        String rarityLabel = buildRarityLabel(state.rarityId);
        inv.setItem(SLOT_RARITY, buildControlWithLore(Material.NETHER_STAR,
                "&6Rarity: " + rarityLabel, "rarity",
                List.of("&7Left-click: cycle →", "&7Right-click: cycle ←")));

        // Summary
        inv.setItem(SLOT_SUMMARY, buildSummary(state, totalItems, totalPages));

        // Search
        boolean hasSearch = state.search != null;
        String searchName = hasSearch ? "&bSearch: &f\"" + state.search + "\"" : "&bSearch...";
        List<String> searchLore = hasSearch
                ? List.of("&7Left-click: new search", "&7Right-click: clear")
                : List.of("&7Left-click: search by name or ID");
        inv.setItem(SLOT_SEARCH, buildControlWithLore(Material.COMPASS, searchName, "search", searchLore));
    }

    private String buildRarityLabel(String rarityId) {
        if (rarityId == null) return "&fAll";
        return RpgServices.items().all().stream()
                .filter(i -> i.rarity().id().equals(rarityId))
                .map(i -> i.rarity().coloredDisplay())
                .findFirst()
                .orElse("&f" + rarityId);
    }

    private ItemStack buildSummary(ViewState state, int totalItems, int totalPages) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✦ Item Browser", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Showing: " + totalItems + " item" + (totalItems == 1 ? "" : "s"),
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Page: " + (state.page + 1) + " / " + totalPages,
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        boolean filtered = state.typeIndex >= 0 || state.rarityId != null || state.search != null;
        if (filtered) {
            lore.add(Component.empty());
            lore.add(Component.text("Active filters:", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            if (state.typeIndex >= 0)
                lore.add(Component.text("  Type: " + capitalize(TYPES[state.typeIndex].id()),
                        NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            if (state.rarityId != null)
                lore.add(Component.text("  Rarity: " + state.rarityId,
                        NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            if (state.search != null)
                lore.add(Component.text("  Search: \"" + state.search + "\"",
                        NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildControl(Material mat, String legacyName, String action) {
        return buildControlWithLore(mat, legacyName, action, List.of());
    }

    private ItemStack buildControlWithLore(Material mat, String legacyName,
                                           String action, List<String> loreLegacy) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(legacyName).decoration(TextDecoration.ITALIC, false));
        if (!loreLegacy.isEmpty()) {
            meta.lore(loreLegacy.stream()
                    .map(l -> LEGACY.deserialize(l).decoration(TextDecoration.ITALIC, false))
                    .collect(Collectors.toList()));
        }
        meta.getPersistentDataContainer().set(CONTROL_KEY, PersistentDataType.STRING, action);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    // ── Item card ──────────────────────────────────────────────────────────────

    /**
     * Clones the real item stack produced by {@link RpgItem#toItemStack()}, tags it with
     * the RPG item id (invisible to players), and appends admin-hint lore lines.
     * When the card is clicked, the tag is read back to look up and give the item.
     */
    private ItemStack buildCard(RpgItem rpgItem) {
        ItemStack card = rpgItem.toItemStack().clone();
        ItemMeta meta = card.getItemMeta();
        if (meta == null) return card;

        meta.getPersistentDataContainer()
                .set(BROWSE_ITEM_KEY, PersistentDataType.STRING, rpgItem.id());

        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("ID: " + rpgItem.id(), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("L-click: give ×1  ·  Shift: give ×64", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        card.setItemMeta(meta);
        return card;
    }

    // ── Events ─────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (!invToViewer.containsKey(topInv)) return;

        // Cancel all clicks while our GUI is open (prevents item movement from either pane)
        event.setCancelled(true);

        // Only process clicks on the GUI pane itself, not the player's bottom hotbar
        if (!topInv.equals(event.getClickedInventory())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        UUID id = viewer.getUniqueId();
        ItemMeta meta = clicked.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        // ── Close button ──────────────────────────────────────────────────────
        if (RpgServices.guiConfig().isCloseButton(clicked)) {
            viewer.closeInventory();
            return;
        }

        // ── Item card (click-to-give) ─────────────────────────────────────────
        String browseId = pdc.get(BROWSE_ITEM_KEY, PersistentDataType.STRING);
        if (browseId != null) {
            handleCardClick(viewer, browseId, event.getClick());
            return;
        }

        // ── Control button ────────────────────────────────────────────────────
        String control = pdc.get(CONTROL_KEY, PersistentDataType.STRING);
        if (control == null) return;

        ViewState state = states.computeIfAbsent(id, k -> new ViewState());
        boolean rightClick = event.getClick() == ClickType.RIGHT
                || event.getClick() == ClickType.SHIFT_RIGHT;

        switch (control) {
            case "prev" -> {
                if (state.page > 0) { state.page--; reopen(viewer); }
            }
            case "next" -> {
                List<RpgItem> filtered = buildFilteredList(state);
                int pages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
                if (state.page < pages - 1) { state.page++; reopen(viewer); }
            }
            case "type" -> {
                state.typeIndex = rightClick
                        ? (state.typeIndex <= -1 ? TYPES.length - 1 : state.typeIndex - 1)
                        : (state.typeIndex >= TYPES.length - 1 ? -1 : state.typeIndex + 1);
                state.page = 0;
                reopen(viewer);
            }
            case "rarity" -> {
                List<String> rarityIds = RpgServices.items().all().stream()
                        .map(i -> i.rarity().id()).distinct().sorted()
                        .collect(Collectors.toList());
                if (!rarityIds.isEmpty()) {
                    int cur = state.rarityId == null ? -1 : rarityIds.indexOf(state.rarityId);
                    cur = rightClick
                            ? (cur <= -1 ? rarityIds.size() - 1 : cur - 1)
                            : (cur >= rarityIds.size() - 1 ? -1 : cur + 1);
                    state.rarityId = cur < 0 ? null : rarityIds.get(cur);
                    state.page = 0;
                    reopen(viewer);
                }
            }
            case "search" -> {
                if (rightClick && state.search != null) {
                    // Right-click clears an active search
                    state.search = null;
                    state.page   = 0;
                    reopen(viewer);
                } else if (!rightClick) {
                    // Left-click opens sign-entry prompt.
                    // Mark as pending so onClose doesn't wipe the ViewState.
                    pendingSearch.add(id);
                    viewer.closeInventory();
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            RpgServices.signInput().ask(viewer, "Search items:", text -> {
                                pendingSearch.remove(id);
                                ViewState s = states.computeIfAbsent(id, k -> new ViewState());
                                s.search = (text == null || text.isBlank()) ? null : text.trim();
                                s.page   = 0;
                                open(viewer);
                            }));
                }
            }
        }
    }

    private void handleCardClick(Player viewer, String browseId, ClickType click) {
        if (!viewer.hasPermission(PERM_GIVE)) {
            viewer.sendMessage(Component.text("You need ", NamedTextColor.RED)
                    .append(Component.text(PERM_GIVE, NamedTextColor.YELLOW))
                    .append(Component.text(" to give items.", NamedTextColor.RED)));
            return;
        }
        Optional<RpgItem> opt = RpgServices.items().get(browseId);
        if (opt.isEmpty()) {
            viewer.sendMessage(Component.text("Item no longer registered: " + browseId,
                    NamedTextColor.RED));
            return;
        }
        RpgItem rpgItem = opt.get();
        int amount = (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) ? 64 : 1;
        ItemStack give = rpgItem.toItemStack();
        give.setAmount(amount);
        viewer.getInventory().addItem(give);
        viewer.sendMessage(Component.text("Gave ", NamedTextColor.GREEN)
                .append(Component.text("×" + amount + " ", NamedTextColor.YELLOW))
                .append(LEGACY.deserialize(rpgItem.rarity().coloredDisplay() + rpgItem.displayName()))
                .append(Component.text(".", NamedTextColor.GREEN)));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        UUID id = viewer.getUniqueId();
        Inventory inv = event.getInventory();
        if (!invToViewer.containsKey(inv)) return;

        openInvs.remove(id);
        invToViewer.remove(inv);
        // Keep ViewState alive if the player is mid-search so the filter survives the
        // sign-editor's implicit inventory close and can be restored when they reopen.
        if (!pendingSearch.contains(id)) {
            states.remove(id);
        }
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
