package com.github._255_ping.rpg.api.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Central GUI style configuration, loaded from rpg-core's config.yml {@code gui:} block.
 *
 * <p>Access via {@code RpgServices.guiConfig()}. Never hardcode pane materials or display
 * names in GUI code — always read from this service so admins can restyle all GUIs at once.
 *
 * <p>Design convention:
 * <ul>
 *   <li>Gray background panes fill all empty/non-content slots.
 *   <li>Black border panes fill the bottom row when the GUI has unused vertical space.
 *   <li>Action/navigation buttons sit in the last meaningful content row, just above any
 *       black border row.
 * </ul>
 */
public interface GuiConfig {

    // ── Navigation bar constants ──────────────────────────────────────────────

    /** Slot for the Close button in top-level (non-nested) 54-slot GUIs — centre of the bottom row. */
    int CLOSE_SLOT        = 49;
    /** Slot for the Back button in nested 54-slot GUIs — far-left of the bottom row. */
    int BACK_SLOT         = 45;
    /** Slot for the Close button in nested 54-slot GUIs — far-right of the bottom row. */
    int NESTED_CLOSE_SLOT = 53;

    /**
     * PDC key used to identify nav-bar buttons.
     * Value is {@code "close"} for Close buttons, {@code "back"} for Back buttons.
     */
    NamespacedKey NAV_ACTION_KEY = new NamespacedKey("rpg", "nav_action");

    // ── Navigation bar population ─────────────────────────────────────────────

    /**
     * Fill the bottom row of a <b>top-level</b> 54-slot GUI with border panes and place a
     * ❌ Close button at slot {@value #CLOSE_SLOT}. Call this after all other content is placed.
     */
    void placeNavBar(Inventory inv);

    /**
     * Fill the bottom row of a <b>nested</b> 54-slot GUI with border panes and place a
     * ← Back button at slot {@value #BACK_SLOT} and a ❌ Close button at slot
     * {@value #NESTED_CLOSE_SLOT}. The Back button should reopen the parent GUI.
     */
    void placeNavBarNested(Inventory inv);

    // ── Navigation bar detection ──────────────────────────────────────────────

    /**
     * Returns {@code true} if {@code item} is a Close button placed by {@link #placeNavBar} or
     * {@link #placeNavBarNested}. Use in onClick handlers: if this returns true, call
     * {@code player.closeInventory()}.
     */
    default boolean isCloseButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "close".equals(item.getItemMeta().getPersistentDataContainer()
                .get(NAV_ACTION_KEY, PersistentDataType.STRING));
    }

    /**
     * Returns {@code true} if {@code item} is a Back button placed by {@link #placeNavBarNested}.
     * Use in onClick handlers: if this returns true, reopen the parent GUI.
     */
    default boolean isBackButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "back".equals(item.getItemMeta().getPersistentDataContainer()
                .get(NAV_ACTION_KEY, PersistentDataType.STRING));
    }

    /** ItemStack to use for background slots (default: GRAY_STAINED_GLASS_PANE, name " "). */
    ItemStack backgroundItem();

    /** ItemStack to use for border slots, typically the bottom row (default: BLACK_STAINED_GLASS_PANE, name " "). */
    ItemStack borderItem();

    /**
     * Fill all currently-empty slots in {@code inv} with the background item.
     * Call this last, after placing all real content, so it only fills leftover slots.
     */
    void fillBackground(Inventory inv);

    /**
     * Set every slot in {@code inv} to the background item, overwriting existing content.
     * Use only on a freshly-created inventory before content is placed.
     */
    void fillAll(Inventory inv);

    /**
     * Fill the bottom row of {@code inv} with border items.
     * Use this when the GUI has extra vertical space below the content area.
     */
    void fillBottomRow(Inventory inv);

    /**
     * Fill an entire row (0-indexed) with border items.
     *
     * @param inv the inventory
     * @param row 0 = top row, (size/9 - 1) = bottom row
     */
    void fillRow(Inventory inv, int row);

    /**
     * Fill specific slot indices with background items (does not overwrite non-null items).
     */
    void fillSlots(Inventory inv, int... slots);

    /**
     * Returns the slot index of the first slot in a given row.
     * Convenience: {@code rowStart(4)} for a 5-row GUI gives slot 36.
     */
    static int rowStart(int row) { return row * 9; }

    /**
     * Returns the slot indices for a complete row.
     */
    static int[] rowSlots(int row) {
        int start = row * 9;
        int[] slots = new int[9];
        for (int i = 0; i < 9; i++) slots[i] = start + i;
        return slots;
    }
}
