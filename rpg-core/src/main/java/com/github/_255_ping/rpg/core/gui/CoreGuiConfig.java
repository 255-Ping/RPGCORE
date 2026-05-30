package com.github._255_ping.rpg.core.gui;

import com.github._255_ping.rpg.api.gui.GuiConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

/**
 * Reads GUI material/name config from rpg-core's config.yml {@code gui:} block.
 * Instantiated and registered in {@code RpgCorePlugin.onEnable}.
 */
public final class CoreGuiConfig implements GuiConfig {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final ItemStack backgroundItem;
    private final ItemStack borderItem;

    public CoreGuiConfig(FileConfiguration config) {
        Material bgMat   = parseMat(config, "gui.background-material", Material.GRAY_STAINED_GLASS_PANE);
        Material brdMat  = parseMat(config, "gui.border-material",     Material.BLACK_STAINED_GLASS_PANE);
        String   bgName  = config.getString("gui.background-name", " ");
        String   brdName = config.getString("gui.border-name",     " ");
        this.backgroundItem = buildPane(bgMat,  bgName);
        this.borderItem     = buildPane(brdMat, brdName);
    }

    // ── Interface ──────────────────────────────────────────────────────────────

    @Override
    public ItemStack backgroundItem() { return backgroundItem.clone(); }

    @Override
    public ItemStack borderItem() { return borderItem.clone(); }

    @Override
    public void fillBackground(Inventory inv) {
        ItemStack bg = backgroundItem.clone();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, bg.clone());
            }
        }
    }

    @Override
    public void fillAll(Inventory inv) {
        ItemStack bg = backgroundItem.clone();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, bg.clone());
        }
    }

    @Override
    public void fillBottomRow(Inventory inv) {
        int rows  = inv.getSize() / 9;
        int start = (rows - 1) * 9;
        ItemStack brd = borderItem.clone();
        for (int i = start; i < start + 9; i++) {
            inv.setItem(i, brd.clone());
        }
    }

    @Override
    public void fillRow(Inventory inv, int row) {
        int start = row * 9;
        int end   = Math.min(start + 9, inv.getSize());
        ItemStack brd = borderItem.clone();
        for (int i = start; i < end; i++) {
            inv.setItem(i, brd.clone());
        }
    }

    @Override
    public void fillSlots(Inventory inv, int... slots) {
        ItemStack bg = backgroundItem.clone();
        for (int slot : slots) {
            if (slot >= 0 && slot < inv.getSize() && inv.getItem(slot) == null) {
                inv.setItem(slot, bg.clone());
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static Material parseMat(FileConfiguration config, String key, Material fallback) {
        String val = config.getString(key);
        if (val == null) return fallback;
        try {
            return Material.valueOf(val.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static ItemStack buildPane(Material mat, String legacyName) {
        ItemStack is   = new ItemStack(mat);
        ItemMeta  meta = is.getItemMeta();
        Component name = LEGACY.deserialize(legacyName)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);
        is.setItemMeta(meta);
        return is;
    }
}
