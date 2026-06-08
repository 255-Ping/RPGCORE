package com.github._255_ping.rpg.parties;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.gui.GuiConfig;
import com.github._255_ping.rpg.api.parties.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 54-slot inventory GUI for {@code /party}.
 *
 * <h3>Layout — in-party mode</h3>
 * <pre>
 * Row 0: [BG][BG][BG][BG][Party Info][BG][BG][BG][Invite]
 * Row 1: [BG][M0 ][M1 ][M2 ][M3 ][M4 ][M5 ][M6 ][BG]   slots 10–16
 * Row 2: [BG][M7 ][M8 ][M9 ][M10][M11][M12][M13][BG]   slots 19–25
 * Row 3: [BG][M14][M15][M16][M17][M18][M19][M20][BG]   slots 28–34
 * Row 4: [BG][BG ][BG ][BG ][BG ][BG ][BG ][BG ][BG]   (empty filler)
 * Row 5: [BG][BG ][BG ][BG ][Close][BG][BG ][BG ][Leave/Disband]
 * </pre>
 *
 * <h3>Member card interactions</h3>
 * <ul>
 *   <li><b>Left-click</b> (owner only, non-self, non-owner target, must be online):
 *       toggles Moderator ↔ Member role.</li>
 *   <li><b>Right-click</b> (owner or mod, online non-owner non-self target):
 *       opens the kick confirmation overlay.</li>
 * </ul>
 *
 * <h3>Confirmation overlay</h3>
 * Replaces all content with a three-slot prompt: slot 20 = ✔ Confirm,
 * slot 22 = ⚠ warning item, slot 24 = ✖ Cancel.
 * Used for kick, leave, and disband.
 */
public final class PartyGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // ── Fixed slots ───────────────────────────────────────────────────────────
    private static final int INFO_SLOT          = 4;    // row 0, centre — party summary
    private static final int INVITE_SLOT        = 8;    // row 0, right  — invite button
    private static final int LEAVE_DISBAND_SLOT = 53;   // row 5, right  — leave/disband
    private static final int CREATE_PARTY_SLOT  = 22;   // centre — used in no-party mode only

    // Member card slots — rows 1-3, inner columns (21 slots, supports up to 21 members)
    private static final int[] MEMBER_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,   // row 1
        19, 20, 21, 22, 23, 24, 25,   // row 2
        28, 29, 30, 31, 32, 33, 34,   // row 3
    };

    // Confirmation overlay (replaces all content)
    private static final int CONFIRM_YES_SLOT  = 20;
    private static final int CONFIRM_WARN_SLOT = 22;
    private static final int CONFIRM_NO_SLOT   = 24;

    // ── Per-viewer state ──────────────────────────────────────────────────────
    private final RpgPartiesPlugin plugin;
    private final PartyManager manager;

    private final Map<UUID, Inventory>        openInvs      = new HashMap<>();
    private final Map<UUID, Map<Integer, UUID>> slotToMember = new HashMap<>();
    private final Map<UUID, PendingConfirm>   pendingConfirm = new HashMap<>();
    /** UUIDs whose GUI was intentionally closed to open the sign-entry invite prompt. */
    private final Set<UUID>                   pendingInvite = new HashSet<>();

    private enum ConfirmAction { KICK, LEAVE, DISBAND }
    private record PendingConfirm(ConfirmAction action, UUID targetId) {}

    public PartyGui(RpgPartiesPlugin plugin, PartyManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    // ── Open ──────────────────────────────────────────────────────────────────

    /** Opens (or re-opens) the Party GUI for {@code player}. */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54,
                Component.text("⚔ Party").color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));
        RpgServices.guiConfig().fillAll(inv);
        openInvs.put(player.getUniqueId(), inv);
        slotToMember.put(player.getUniqueId(), new HashMap<>());
        pendingConfirm.remove(player.getUniqueId());
        populateMain(player, inv);
        player.openInventory(inv);
    }

    // ── Populate ──────────────────────────────────────────────────────────────

    private void populateMain(Player viewer, Inventory inv) {
        GuiConfig gui = RpgServices.guiConfig();
        gui.fillAll(inv);
        Map<Integer, UUID> mapping = slotToMember.computeIfAbsent(
                viewer.getUniqueId(), k -> new HashMap<>());
        mapping.clear();
        gui.placeNavBar(inv);   // border row + close at 49

        Optional<Party> opt = manager.partyOf(viewer);

        // ── No-party state ────────────────────────────────────────────────────
        if (opt.isEmpty()) {
            inv.setItem(INFO_SLOT, simple(Material.GRAY_DYE, "&8No Party",
                    List.of("&7You are not currently in a party.")));
            inv.setItem(CREATE_PARTY_SLOT, simple(Material.LIME_DYE, "&aCreate Party",
                    List.of("&7Start a new party and invite friends.", "",
                            "&8▶ &7Click to create")));
            return;
        }

        CoreParty party = (CoreParty) opt.get();

        // ── Party summary ─────────────────────────────────────────────────────
        long onlineCount = party.memberIds().stream()
                .filter(id -> Bukkit.getPlayer(id) != null).count();
        int totalCount = party.memberIds().size();
        inv.setItem(INFO_SLOT, simple(Material.NETHER_STAR, "&6&lParty",
                List.of("&7Members: &e" + onlineCount + " online &8/ &e" + totalCount + " total",
                        "&7Max size: &e" + manager.maxSize(),
                        "",
                        "&8Party: &7" + party.id().toString().substring(0, 8) + "…")));

        // ── Invite button ─────────────────────────────────────────────────────
        if (party.isOwner(viewer) || party.isModerator(viewer)) {
            int available = manager.maxSize() - totalCount;
            if (available > 0) {
                inv.setItem(INVITE_SLOT, simple(Material.LIME_DYE, "&aInvite Player",
                        List.of("&7Invite an online player.", "&7Slots available: &e" + available,
                                "", "&8▶ &7Click to invite")));
            } else {
                inv.setItem(INVITE_SLOT, simple(Material.GRAY_DYE, "&8Party Full",
                        List.of("&7No more slots available.")));
            }
        }

        // ── Leave / Disband ───────────────────────────────────────────────────
        if (party.isOwner(viewer)) {
            inv.setItem(LEAVE_DISBAND_SLOT, simple(Material.RED_DYE, "&cDisband Party",
                    List.of("&7Permanently disbands the party.", "&7All members are removed.",
                            "", "&8▶ &7Click to disband")));
        } else {
            inv.setItem(LEAVE_DISBAND_SLOT, simple(Material.YELLOW_DYE, "&eLeave Party",
                    List.of("&7Leave this party.", "", "&8▶ &7Click to leave")));
        }

        // ── Member cards ──────────────────────────────────────────────────────
        int slotIdx = 0;
        for (UUID memberId : party.memberIds()) {
            if (slotIdx >= MEMBER_SLOTS.length) break;
            int slot = MEMBER_SLOTS[slotIdx++];
            mapping.put(slot, memberId);
            inv.setItem(slot, buildMemberCard(viewer, party, memberId));
        }
    }

    private void populateConfirm(Inventory inv, PendingConfirm confirm, String targetName) {
        GuiConfig gui = RpgServices.guiConfig();
        gui.fillAll(inv);
        gui.placeNavBar(inv);

        String warnDesc;
        Material warnMat;
        switch (confirm.action()) {
            case KICK    -> {
                String nm = targetName != null ? targetName : "Unknown";
                warnDesc = "Kick " + nm + " from the party?";
                warnMat  = Material.RED_DYE;
            }
            case LEAVE   -> { warnDesc = "Leave the party?";                       warnMat = Material.YELLOW_DYE; }
            case DISBAND -> { warnDesc = "Disband the party permanently?";          warnMat = Material.RED_DYE; }
            default      -> { warnDesc = "Are you sure?";                          warnMat = Material.RED_DYE; }
        }

        inv.setItem(CONFIRM_WARN_SLOT, simple(warnMat, "&c⚠ Confirm Action",
                List.of("&7" + warnDesc, "", "&7This action cannot be undone.")));
        inv.setItem(CONFIRM_YES_SLOT, simple(Material.LIME_DYE, "&a✔ Confirm",
                List.of("&8▶ &7Click to confirm")));
        inv.setItem(CONFIRM_NO_SLOT, simple(Material.RED_DYE, "&c✖ Cancel",
                List.of("&8▶ &7Click to go back")));
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory tracked = openInvs.get(p.getUniqueId());
        if (tracked == null) return;

        Inventory top = e.getView().getTopInventory();
        if (!tracked.equals(top)) return;

        int raw = e.getRawSlot();

        // Bottom-half clicks — only block shift-clicks that would fill the GUI
        if (raw < 0 || raw >= top.getSize()) {
            if (e.isShiftClick()) e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        GuiConfig gui = RpgServices.guiConfig();

        // Close button
        if (gui.isCloseButton(top.getItem(raw))) {
            p.closeInventory();
            return;
        }

        // Confirmation overlay
        PendingConfirm confirm = pendingConfirm.get(p.getUniqueId());
        if (confirm != null) {
            if (raw == CONFIRM_YES_SLOT) executeConfirm(p, confirm, top);
            else if (raw == CONFIRM_NO_SLOT) cancelConfirm(p, top);
            return;
        }

        Optional<Party> opt = manager.partyOf(p);

        // ── No-party state ────────────────────────────────────────────────────
        if (opt.isEmpty()) {
            if (raw == CREATE_PARTY_SLOT) {
                CoreParty created = manager.create(p);
                if (created != null) {
                    p.sendMessage(msg("&aParty created! Use the invite button to add members."));
                    populateMain(p, top);
                } else {
                    p.sendMessage(msg("&cYou're already in a party."));
                }
            }
            return;
        }

        CoreParty party = (CoreParty) opt.get();

        // ── Invite ────────────────────────────────────────────────────────────
        if (raw == INVITE_SLOT) {
            if (!party.isOwner(p) && !party.isModerator(p)) return;
            if (party.memberIds().size() >= manager.maxSize()) return;
            triggerInvite(p, party);
            return;
        }

        // ── Leave / Disband ───────────────────────────────────────────────────
        if (raw == LEAVE_DISBAND_SLOT) {
            if (party.isOwner(p)) {
                pendingConfirm.put(p.getUniqueId(),
                        new PendingConfirm(ConfirmAction.DISBAND, null));
            } else {
                pendingConfirm.put(p.getUniqueId(),
                        new PendingConfirm(ConfirmAction.LEAVE, null));
            }
            populateConfirm(top, pendingConfirm.get(p.getUniqueId()), null);
            return;
        }

        // ── Member card ───────────────────────────────────────────────────────
        Map<Integer, UUID> mapping = slotToMember.getOrDefault(p.getUniqueId(), Map.of());
        UUID targetId = mapping.get(raw);
        if (targetId == null) return;

        Player     targetPlayer = Bukkit.getPlayer(targetId);
        OfflinePlayer targetOp  = Bukkit.getOfflinePlayer(targetId);
        String targetName       = targetOp.getName() != null ? targetOp.getName() : "Unknown";

        ClickType click = e.getClick();
        boolean isLeft  = click == ClickType.LEFT  || click == ClickType.SHIFT_LEFT;
        boolean isRight = click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT;

        if (isLeft) {
            // Promote / Demote — owner only
            if (!party.isOwner(p)) return;
            if (p.getUniqueId().equals(targetId)) return;          // can't act on self
            if (party.ownerIdRaw().equals(targetId)) return;       // can't demote the owner
            if (targetPlayer == null) {
                p.sendMessage(msg("&cThat player is offline.")); return;
            }
            if (party.isModerator(targetPlayer)) {
                manager.demote(party, targetPlayer);
                for (Player m : party.members())
                    m.sendMessage(msg("&e" + fmt(targetPlayer) + " &7is no longer a moderator."));
            } else {
                manager.promote(party, targetPlayer);
                for (Player m : party.members())
                    m.sendMessage(msg("&e" + fmt(targetPlayer) + " &7is now a moderator."));
            }
            populateMain(p, top);

        } else if (isRight) {
            // Kick confirm — owner or moderator
            if (!party.isOwner(p) && !party.isModerator(p)) return;
            if (p.getUniqueId().equals(targetId)) return;
            if (party.ownerIdRaw().equals(targetId)) {
                p.sendMessage(msg("&cYou can't kick the party owner.")); return;
            }
            if (targetPlayer == null) {
                p.sendMessage(msg("&cOffline players cannot be kicked here.")); return;
            }
            pendingConfirm.put(p.getUniqueId(),
                    new PendingConfirm(ConfirmAction.KICK, targetId));
            populateConfirm(top, pendingConfirm.get(p.getUniqueId()), targetName);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        // If the player closed the GUI to open sign-entry, preserve state
        if (pendingInvite.contains(p.getUniqueId())) return;
        Inventory tracked = openInvs.remove(p.getUniqueId());
        if (tracked == null) return;
        slotToMember.remove(p.getUniqueId());
        pendingConfirm.remove(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        openInvs.remove(id);
        slotToMember.remove(id);
        pendingConfirm.remove(id);
        pendingInvite.remove(id);
    }

    // ── Confirmation logic ────────────────────────────────────────────────────

    private void executeConfirm(Player p, PendingConfirm confirm, Inventory top) {
        pendingConfirm.remove(p.getUniqueId());
        Optional<Party> opt = manager.partyOf(p);
        if (opt.isEmpty()) { p.closeInventory(); return; }
        CoreParty party = (CoreParty) opt.get();

        switch (confirm.action()) {
            case KICK -> {
                UUID targetId = confirm.targetId();
                if (targetId == null) break;
                Player target = Bukkit.getPlayer(targetId);
                if (target == null) {
                    p.sendMessage(msg("&cThat player went offline."));
                    populateMain(p, top);
                    break;
                }
                if (!party.isMember(target)) {
                    p.sendMessage(msg("&cThat player is no longer in the party."));
                    populateMain(p, top);
                    break;
                }
                manager.removeMember(party, target);
                target.sendMessage(msg("&cYou were kicked from the party."));
                for (Player m : party.members())
                    m.sendMessage(msg("&e" + fmt(target) + " &7was kicked from the party."));
                populateMain(p, top);
            }
            case LEAVE -> {
                p.closeInventory();
                manager.removeMember(party, p);
                p.sendMessage(msg("&7You left the party."));
                for (Player m : party.members())
                    m.sendMessage(msg("&e" + fmt(p) + " &7left the party."));
            }
            case DISBAND -> {
                p.closeInventory();
                for (Player m : party.members())
                    m.sendMessage(msg("&cThe party was disbanded."));
                manager.disband(party);
            }
        }
    }

    private void cancelConfirm(Player p, Inventory top) {
        pendingConfirm.remove(p.getUniqueId());
        populateMain(p, top);
    }

    // ── Invite ────────────────────────────────────────────────────────────────

    private void triggerInvite(Player p, CoreParty party) {
        pendingInvite.add(p.getUniqueId());
        p.closeInventory();
        // Inventory must be fully closed before a sign editor can be opened
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pendingInvite.remove(p.getUniqueId());
            if (!p.isOnline()) {
                openInvs.remove(p.getUniqueId());
                slotToMember.remove(p.getUniqueId());
                return;
            }
            try {
                RpgServices.signInput().ask(p, "Invite player:", input -> {
                    // Callback runs on main thread
                    if (input != null && !input.isBlank()) {
                        processInvite(p, party, input.trim());
                    }
                    if (p.isOnline()) open(p);
                });
            } catch (IllegalStateException ex) {
                // Sign-input service not loaded (shouldn't happen in normal operation)
                p.sendMessage(msg("&7Use &e/party invite <player> &7from chat instead."));
                if (p.isOnline()) open(p);
            }
        });
    }

    private void processInvite(Player inviter, CoreParty party, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            inviter.sendMessage(msg("&cPlayer not found: &7" + targetName)); return;
        }
        if (target.getUniqueId().equals(inviter.getUniqueId())) {
            inviter.sendMessage(msg("&cYou can't invite yourself.")); return;
        }
        if (!manager.invite(party, target)) {
            inviter.sendMessage(msg(
                    "&cCould not invite &e" + fmt(target)
                    + "&c (party full, already a member, or invite pending).")); return;
        }
        inviter.sendMessage(msg("&aInvited &e" + fmt(target) + "&a to the party."));
        target.sendMessage(msg("&e" + fmt(inviter)
                + " &7invited you to a party. Type &e/party accept &7to join."));
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildMemberCard(Player viewer, CoreParty party, UUID memberId) {
        OfflinePlayer op  = Bukkit.getOfflinePlayer(memberId);
        Player member     = Bukkit.getPlayer(memberId);
        boolean online    = member != null;
        String name       = op.getName() != null
                ? op.getName() : memberId.toString().substring(0, 8) + "…";

        boolean isOwner  = party.ownerIdRaw().equals(memberId);
        boolean isMod    = party.moderatorIds().contains(memberId);
        boolean isViewer = viewer.getUniqueId().equals(memberId);

        String roleColor = isOwner ? "&6" : isMod ? "&a" : "&7";
        String roleName  = isOwner ? "Owner" : isMod ? "Moderator" : "Member";

        List<String> loreLines = new ArrayList<>();
        loreLines.add("&7Role: " + roleColor + roleName);
        loreLines.add("&7Status: " + (online ? "&a● Online" : "&8● Offline"));

        if (online) {
            double hp = member.getHealth();
            AttributeInstance maxAttr = member.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = maxAttr != null ? maxAttr.getValue() : 20.0;
            int pct = maxHp > 0 ? (int) Math.round(hp / maxHp * 100) : 0;
            loreLines.add("&7HP: &c" + String.format("%.1f", hp)
                    + " &7/ &c" + String.format("%.1f", maxHp)
                    + " &8(&c" + pct + "%&8)");
        }

        loreLines.add("");

        boolean viewerIsOwner = party.isOwner(viewer);
        boolean viewerIsMod   = party.isModerator(viewer);

        if (isViewer) {
            loreLines.add("&7(You)");
        } else if (isOwner) {
            loreLines.add("&6(Party Owner)");
        } else {
            if (viewerIsOwner) {
                loreLines.add(isMod ? "&7Left-click to &cdemote" : "&7Left-click to &apromote");
                loreLines.add(online ? "&7Right-click to &ckick"
                                     : "&8Right-click to kick &8(offline)");
            } else if (viewerIsMod && !isMod) {
                loreLines.add(online ? "&7Right-click to &ckick"
                                     : "&8Right-click to kick &8(offline)");
            }
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(op);
            meta.displayName(ni(LEGACY.deserialize(
                    roleColor + (isViewer ? "&l" : "") + name)));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) lore.add(ni(LEGACY.deserialize(line)));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private static ItemStack simple(Material mat, String legacyName, List<String> loreLegacy) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta  meta  = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(ni(LEGACY.deserialize(legacyName)));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLegacy) lore.add(ni(LEGACY.deserialize(line)));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Component ni(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static Component msg(String legacy) {
        return LEGACY.deserialize(legacy);
    }

    private static String fmt(Player p) {
        try { return RpgServices.nameFormatter().format(p); }
        catch (IllegalStateException ex) { return p.getName(); }
    }
}
