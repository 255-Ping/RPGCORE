package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.Block;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import com.github._255_ping.rpg.core.blocks.CoreBlockRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RpgCommand implements CommandExecutor, TabCompleter {

    private final RpgCorePlugin plugin;

    public RpgCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg <version|reload|item|mob|block>")));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help", "?" -> handleHelp(sender);
            case "version" -> handleVersion(sender);
            case "reload", "reloadall" -> handleReload(sender);
            case "item" -> handleItem(sender, args);
            case "mob" -> handleMob(sender, args);
            case "block" -> handleBlock(sender, args);
            case "wand" -> handleWand(sender, args);
            case "loot-chest", "lootchest" -> handleLootChest(sender, args);
            case "effects" -> handleEffects(sender, args);
            case "particle" -> handleParticle(sender, args);
            case "fix" -> handleFix(sender, args);
            default -> sender.sendMessage(plugin.messages().component("command.unknown",
                    Map.of("sub", args[0])));
        }
        return true;
    }

    private void handleLootChest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().component("command.player-only"));
            return;
        }
        if (!sender.hasPermission("rpg.core.loot-chest")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "/rpg loot-chest <define|delete|count> [args]"));
            return;
        }
        String sub = args[1].toLowerCase();
        if (sub.equals("define")) {
            if (args.length < 3) {
                sender.sendMessage(net.kyori.adventure.text.Component.text(
                        "/rpg loot-chest define <lootTableId>  (uses your wand selection)"));
                return;
            }
            String tableId = args[2];
            if (RpgServices.lootTables().get(tableId).isEmpty()) {
                sender.sendMessage(net.kyori.adventure.text.Component.text(
                        "Unknown loot table: " + tableId));
                return;
            }
            java.util.Optional<com.github._255_ping.rpg.api.wand.WandSelection> sel;
            try { sel = RpgServices.wands().selectionOf(player); }
            catch (IllegalStateException ex) { sel = java.util.Optional.empty(); }
            if (sel.isEmpty()) {
                sender.sendMessage(net.kyori.adventure.text.Component.text(
                        "No wand selection — set both corners first."));
                return;
            }
            int bound = bindChestsIn(sel.get(), tableId);
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "Bound " + bound + " chest(s) to loot table '" + tableId + "'."));
        } else if (sub.equals("delete")) {
            org.bukkit.block.Block targeted = player.getTargetBlockExact(8);
            if (targeted == null) {
                sender.sendMessage(net.kyori.adventure.text.Component.text("No targeted block."));
                return;
            }
            boolean removed = plugin.lootChestRegistry().unbind(targeted.getLocation());
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    removed ? "Loot-chest binding removed." : "No binding at that block."));
        } else if (sub.equals("count")) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "Tracked loot chests: " + plugin.lootChestRegistry().count()));
        } else {
            sender.sendMessage(net.kyori.adventure.text.Component.text("Unknown sub: " + sub));
        }
    }

    private int bindChestsIn(com.github._255_ping.rpg.api.wand.WandSelection sel, String tableId) {
        org.bukkit.util.Vector min = sel.min();
        org.bukkit.util.Vector max = sel.max();
        org.bukkit.World world = sel.corner1().getWorld();
        int count = 0;
        for (int y = (int) min.getY(); y <= (int) max.getY(); y++) {
            for (int z = (int) min.getZ(); z <= (int) max.getZ(); z++) {
                for (int x = (int) min.getX(); x <= (int) max.getX(); x++) {
                    org.bukkit.block.Block b = world.getBlockAt(x, y, z);
                    if (b.getState() instanceof org.bukkit.block.Container) {
                        plugin.lootChestRegistry().bind(b.getLocation(), tableId);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void handleWand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().component("command.player-only"));
            return;
        }
        if (!sender.hasPermission("rpg.core.wand")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "/rpg wand <give|mode> [region|loot-chest|dungeon|spawner|entrance]"));
            return;
        }
        String sub = args[1].toLowerCase();
        if (sub.equals("give")) {
            player.getInventory().addItem(plugin.wandListener().newWand());
            player.sendMessage(net.kyori.adventure.text.Component.text("Selection wand granted."));
            return;
        }
        if (sub.equals("mode")) {
            if (args.length < 3) {
                player.sendMessage(net.kyori.adventure.text.Component.text(
                        "Current mode: " + plugin.wandService().modeOf(player)));
                return;
            }
            String mode = args[2].toLowerCase();
            if (!java.util.Set.of("region", "loot-chest", "dungeon", "spawner", "entrance").contains(mode)) {
                player.sendMessage(net.kyori.adventure.text.Component.text("Unknown mode: " + mode));
                return;
            }
            plugin.wandService().setMode(player, mode);
            player.sendMessage(net.kyori.adventure.text.Component.text("§aWand mode set to: §e" + mode));
            // Action bar notification (2 seconds) so it's visible while holding the wand.
            try {
                RpgServices.actionBar().send(player,
                        net.kyori.adventure.text.Component.text("§6[Wand] §eMode: §b" + mode), 40);
            } catch (IllegalStateException ignored) {}
            // Update wand item lore to show current mode.
            updateWandLore(player, mode);
            return;
        }
        sender.sendMessage(net.kyori.adventure.text.Component.text("Unknown wand subcommand."));
    }

    /** Updates the wand item's lore in the player's hand to show the current mode. */
    private void updateWandLore(org.bukkit.entity.Player player, String mode) {
        org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
        if (!plugin.wandListener().isWand(hand)) return;
        org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
        if (meta == null) return;
        var lore = new java.util.ArrayList<>(java.util.List.of(
                net.kyori.adventure.text.Component.text("§bMode: §e" + mode)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                net.kyori.adventure.text.Component.text("§7L-click block: corner 1")
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                net.kyori.adventure.text.Component.text("§7R-click block: corner 2")
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                net.kyori.adventure.text.Component.text("§8/rpg wand <mode> to switch")
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
        ));
        meta.lore(lore);
        hand.setItemMeta(meta);
    }

    private void handleBlock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg block <give|convert> ...")));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "give" -> handleBlockGive(sender, args);
            case "convert" -> handleBlockConvert(sender, args);
            default -> sender.sendMessage(plugin.messages().component("command.unknown",
                    Map.of("sub", "block " + args[1])));
        }
    }

    private void handleBlockGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.core.block.give")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg block give <id> [amount]")));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().component("command.player-only"));
            return;
        }
        String id = args[2];
        Optional<Block> opt = RpgServices.blocks().get(id);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.messages().component("block.not-found", Map.of("id", id)));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Integer.parseInt(args[3])); }
            catch (NumberFormatException ex) { amount = 1; }
        }
        Block block = opt.get();
        ItemStack stack = new ItemStack(block.material(), amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("[RPG] " + block.id(), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Block ID: " + block.id(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Place to auto-register as custom block (admin+creative required)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            // Tag with block id so BlockPlaceListener can auto-register on placement.
            meta.getPersistentDataContainer().set(
                    plugin.blockItemKey(), org.bukkit.persistence.PersistentDataType.STRING, block.id());
            stack.setItemMeta(meta);
        }
        player.getInventory().addItem(stack);
    }

    private void handleBlockConvert(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.core.block.convert")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        if (args.length < 5) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg block convert <radius> <fromMaterial> <toBlockId>")));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().component("command.player-only"));
            return;
        }
        int radius;
        try { radius = Math.max(1, Math.min(64, Integer.parseInt(args[2]))); }
        catch (NumberFormatException ex) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg block convert <radius> <fromMaterial> <toBlockId>")));
            return;
        }
        Material from = Material.matchMaterial(args[3]);
        if (from == null || !from.isBlock()) {
            sender.sendMessage(plugin.messages().component("block.not-found", Map.of("id", args[3])));
            return;
        }
        String toId = args[4];
        Optional<Block> opt = RpgServices.blocks().get(toId);
        if (opt.isEmpty()) {
            sender.sendMessage(plugin.messages().component("block.not-found", Map.of("id", toId)));
            return;
        }
        Block to = opt.get();
        if (!(RpgServices.blocks() instanceof CoreBlockRegistry coreReg)) {
            return;
        }

        org.bukkit.Location center = player.getLocation();
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    org.bukkit.Location loc = center.clone().add(dx, dy, dz);
                    if (loc.getBlock().getType() != from) continue;
                    if (to.material() != from) {
                        loc.getBlock().setType(to.material());
                    }
                    coreReg.tagLocation(loc, to.id());
                    count++;
                }
            }
        }
        sender.sendMessage(plugin.messages().component("block.converted",
                Map.of("count", count, "radius", radius, "id", toId)));
    }

    private void handleParticle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.core.particle")) {
            sender.sendMessage(net.kyori.adventure.text.Component.text("§cNo permission."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                "§7Usage: §e/rpg particle <create|delete|list|move> [args]"));
            return;
        }
        var pm = plugin.particleManager();
        if (pm == null) { sender.sendMessage(net.kyori.adventure.text.Component.text("§cParticle system not ready.")); return; }

        switch (args[1].toLowerCase()) {
            case "create" -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) { sender.sendMessage(net.kyori.adventure.text.Component.text("§cPlayers only.")); return; }
                if (args.length < 3) { sender.sendMessage(net.kyori.adventure.text.Component.text("§7Usage: §e/rpg particle create <id> [type=FLAME] [count=5] [spread=0.3] [pattern=POINT|CIRCLE|SPIRAL]")); return; }
                String id = args[2];
                String type = args.length > 3 ? args[3] : "FLAME";
                int count = args.length > 4 ? parseInt(args[4], 5) : 5;
                double spread = args.length > 5 ? parseDouble(args[5], 0.3) : 0.3;
                com.github._255_ping.rpg.core.particles.ParticleManager.Pattern pat =
                    com.github._255_ping.rpg.core.particles.ParticleManager.parsePattern(args.length > 6 ? args[6] : "POINT");
                if (!pm.create(id, p.getLocation(), type, count, spread, pat)) {
                    sender.sendMessage(net.kyori.adventure.text.Component.text("§cID '" + id + "' already exists."));
                } else {
                    sender.sendMessage(net.kyori.adventure.text.Component.text("§aCreated particle §e" + id + "§a (" + type + " ×" + count + " " + pat + ")"));
                }
            }
            case "delete" -> {
                if (args.length < 3) { sender.sendMessage(net.kyori.adventure.text.Component.text("§7Usage: §e/rpg particle delete <id>")); return; }
                sender.sendMessage(pm.delete(args[2])
                    ? net.kyori.adventure.text.Component.text("§aDeleted §e" + args[2])
                    : net.kyori.adventure.text.Component.text("§cNot found: " + args[2]));
            }
            case "list" -> {
                var all = pm.all();
                sender.sendMessage(net.kyori.adventure.text.Component.text("§6Particles (" + all.size() + "):"));
                for (var entry : all) {
                    sender.sendMessage(net.kyori.adventure.text.Component.text(
                        "§e" + entry.id() + " §7" + entry.particleType() + " ×" + entry.count() +
                        " " + entry.pattern() + " @" + entry.worldName() +
                        " " + (int)entry.x() + "," + (int)entry.y() + "," + (int)entry.z()));
                }
            }
            case "move" -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) { sender.sendMessage(net.kyori.adventure.text.Component.text("§cPlayers only.")); return; }
                if (args.length < 3) { sender.sendMessage(net.kyori.adventure.text.Component.text("§7Usage: §e/rpg particle move <id>")); return; }
                sender.sendMessage(pm.move(args[2], p.getLocation())
                    ? net.kyori.adventure.text.Component.text("§aMoved §e" + args[2] + "§a to your location.")
                    : net.kyori.adventure.text.Component.text("§cNot found: " + args[2]));
            }
            default -> sender.sendMessage(net.kyori.adventure.text.Component.text("§cUnknown: " + args[1]));
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
    }

    private void handleEffects(CommandSender sender, String[] args) {
        org.bukkit.entity.Player target;
        if (args.length >= 2 && sender.hasPermission("rpg.core.effects.other")) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(net.kyori.adventure.text.Component.text("§cPlayer not found: " + args[1]));
                return;
            }
        } else if (sender instanceof org.bukkit.entity.Player p) {
            target = p;
        } else {
            sender.sendMessage(net.kyori.adventure.text.Component.text("§cSpecify a player name."));
            return;
        }

        var active = RpgServices.statusEffects().active(target);
        if (active.isEmpty()) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "§7" + target.getName() + " has no active status effects."));
            return;
        }
        sender.sendMessage(net.kyori.adventure.text.Component.text(
                "§6=== Active Effects: §e" + target.getName() + " §6==="));
        for (var effect : active) {
            double secs = effect.remainingTicks() / 20.0;
            String timeStr = secs > 0
                    ? String.format("§7%.1fs remaining", secs)
                    : "§8permanent";
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "§d" + effect.effectId() + " §8Lv." + effect.level() + " §8— " + timeStr));
        }
    }

    /**
     * /rpg fix [player] — clears stuck vanilla Slowness + orphaned MOVEMENT_SPEED modifiers,
     * wipes all RPG status effects, then forces a full attribute resync.
     * Use this when a player has permanent slowness with no visible potion icon.
     */
    private void handleFix(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.core.fix")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.messages().component("player.not-found",
                        Map.of("name", args[1])));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "§cSpecify a player name when running from console."));
            return;
        }

        // 1. Remove the vanilla SLOWNESS effect (the most common stuck-state culprit).
        target.removePotionEffect(PotionEffectType.SLOWNESS);

        // 2. Clear any orphaned MOVEMENT_SPEED attribute modifiers. These are the
        //    invisible part — they survive a server restart even after the potion
        //    expires, causing permanent slowdown with no visible icon.
        AttributeInstance movespeed = target.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movespeed != null) {
            for (AttributeModifier mod : List.copyOf(movespeed.getModifiers())) {
                movespeed.removeModifier(mod);
            }
        }

        // 3. Clear all in-memory RPG status effects (e.g. stuck slow debuff).
        RpgServices.statusEffects().clearAll(target);

        // 4. Force a full stat + attribute recalc so movement speed, attack speed,
        //    and swing range are all recomputed from current gear + (now empty) effects.
        plugin.equipmentListener().resync(target);

        sender.sendMessage(net.kyori.adventure.text.Component.text(
                "§aFixed §e" + target.getName() + "§a — cleared stuck effects and resynced stats."));
        if (!target.equals(sender)) {
            target.sendMessage(net.kyori.adventure.text.Component.text(
                    "§aAn admin cleared your stuck effects and resynced your stats."));
        }
    }

    private void handleHelp(CommandSender sender) {
        sender.sendMessage(net.kyori.adventure.text.Component.text("§6§l=== RPG Plugin Commands ==="));
        String[] lines = {
            "§e/rpg help §7— Show this help page",
            "§e/rpg version §7— List all loaded module versions",
            "§e/rpg reload §7— Hot-reload all content (items/mobs/abilities/blocks/effects)",
            "§e/rpg item give <id> [player] [amount] §7— Give a custom item",
            "§e/rpg mob spawn <id> [count] §7— Spawn a custom mob at your location",
            "§e/rpg block give <id> §7— Get a placeable custom block in hand",
            "§e/rpg block convert <radius> <from> <to> §7— Bulk-convert blocks",
            "§e/rpg wand §7— Get the selection wand",
            "§e/rpg wand region §7— Switch wand to region-selection mode",
            "§e/rpg wand dungeon §7— Switch wand to dungeon-selection mode",
            "§e/spawner create <mobId> §7— Place a mob spawner at your location",
            "§e/spawner show §7— Toggle spawner particle markers",
            "§e/region create <id> §7— Create region from wand selection",
            "§e/region flag <id> <flag> <value> §7— Set a region flag",
            "§e/region info [id] §7— Show region info at your location",
            "§e/region list §7— List all regions",
            "§e/rpg status apply <id> [player] §7— Apply a status effect",
            "§e/rpg skill set <skill> level <n> [player] §7— Set skill level",
            "§e/rpg ability cast <id> §7— Debug-cast an ability",
            "§e/rpg effects [player] §7— Show active status effects",
            "§e/rpg fix [player] §7— Clear stuck movement-speed debuffs and resync stats",
            "§e/rpg particle create <id> [type] [count] [spread] [pattern] §7— Place world particle effect",
            "§e/rpg particle delete/list/move §7— Manage particle effects",
            "§e/region global flag <key> <value> §7— Set server-wide default region flag",
        };
        for (String line : lines) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(line));
        }
    }

    private void handleVersion(CommandSender sender) {
        if (!sender.hasPermission("rpg.core.version")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        sender.sendMessage(plugin.messages().component("version.header"));
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (p.getName().startsWith("rpg-")) {
                sender.sendMessage(plugin.messages().component("version.entry",
                        Map.of("module", p.getName(),
                                "version", p.getPluginMeta().getVersion())));
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpg.core.reload-all")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        sender.sendMessage(plugin.messages().component("reload.starting"));
        plugin.reloadAll();
        sender.sendMessage(plugin.messages().component("reload.success"));
    }

    private void handleItem(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg item give <id> [player] [amount]")));
            return;
        }
        if (!sender.hasPermission("rpg.core.item.give")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg item give <id> [player] [amount]")));
            return;
        }
        String id = args[2];
        Optional<RpgItem> item = RpgServices.items().get(id);
        if (item.isEmpty()) {
            sender.sendMessage(plugin.messages().component("item.not-found", Map.of("id", id)));
            return;
        }
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                sender.sendMessage(plugin.messages().component("player.not-found",
                        Map.of("name", args[3])));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(plugin.messages().component("command.player-only"));
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            try { amount = Math.max(1, Integer.parseInt(args[4])); }
            catch (NumberFormatException ex) { amount = 1; }
        }
        var stack = item.get().toItemStack();
        stack.setAmount(amount);
        target.getInventory().addItem(stack);
        sender.sendMessage(plugin.messages().component("item.given",
                Map.of("id", id, "amount", amount, "player", target.getName())));
    }

    private void handleMob(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("spawn")) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg mob spawn <id> [count]")));
            return;
        }
        if (!sender.hasPermission("rpg.core.mob.spawn")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().component("command.player-only"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg mob spawn <id> [count]")));
            return;
        }
        String id = args[2];
        Optional<RpgMob> mob = RpgServices.mobs().get(id);
        if (mob.isEmpty()) {
            sender.sendMessage(plugin.messages().component("mob.not-found", Map.of("id", id)));
            return;
        }
        int count = 1;
        if (args.length >= 4) {
            try { count = Math.max(1, Math.min(50, Integer.parseInt(args[3]))); }
            catch (NumberFormatException ex) { count = 1; }
        }
        for (int i = 0; i < count; i++) {
            LivingEntity spawned = mob.get().spawn(player.getLocation());
            if (spawned == null) {
                sender.sendMessage(plugin.messages().component("mob.spawn-failed", Map.of("id", id)));
                return;
            }
        }
        sender.sendMessage(plugin.messages().component("mob.spawned",
                Map.of("id", id, "count", count)));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filtered(args[0], List.of(
                    "help", "version", "reload",
                    "item", "mob", "block",
                    "wand", "loot-chest", "effects", "fix", "particle"));
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            return switch (sub) {
                case "item" -> filtered(args[1], List.of("give"));
                case "mob"  -> filtered(args[1], List.of("spawn"));
                case "block" -> filtered(args[1], List.of("give", "convert"));
                case "wand" -> filtered(args[1], List.of("give", "mode"));
                case "loot-chest" -> filtered(args[1], List.of("define", "delete", "count"));
                case "effects" -> filtered(args[1], onlinePlayerNames());
                case "fix"    -> filtered(args[1], onlinePlayerNames());
                case "particle" -> filtered(args[1], List.of("create", "delete", "list", "move"));
                default -> List.of();
            };
        }
        if (args.length == 3) {
            if (sub.equals("block") && args[1].equalsIgnoreCase("give")) {
                return filtered(args[2], RpgServices.blocks().all().stream().map(Block::id).toList());
            }
            if (sub.equals("item") && args[1].equalsIgnoreCase("give")) {
                return filtered(args[2], RpgServices.items().all().stream().map(RpgItem::id).toList());
            }
            if (sub.equals("mob") && args[1].equalsIgnoreCase("spawn")) {
                return filtered(args[2], RpgServices.mobs().all().stream().map(RpgMob::id).toList());
            }
            if (sub.equals("wand") && args[1].equalsIgnoreCase("mode")) {
                return filtered(args[2], List.of("region", "loot-chest", "dungeon", "spawner", "entrance"));
            }
            if (sub.equals("loot-chest") && args[1].equalsIgnoreCase("define")) {
                return filtered(args[2], RpgServices.lootTables().all().stream()
                        .map(com.github._255_ping.rpg.api.loot.LootTable::id).toList());
            }
            if (sub.equals("particle") && List.of("delete", "move").contains(args[1].toLowerCase())) {
                var pm = plugin.particleManager();
                if (pm != null) return filtered(args[2],
                        pm.all().stream()
                          .map(com.github._255_ping.rpg.core.particles.ParticleManager.ParticleEntry::id)
                          .toList());
            }
        }
        return List.of();
    }

    private static List<String> onlinePlayerNames() {
        return org.bukkit.Bukkit.getOnlinePlayers().stream()
                .map(org.bukkit.entity.Player::getName).toList();
    }

    private static List<String> filtered(String prefix, List<String> candidates) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String c : candidates) {
            if (c.toLowerCase().startsWith(lower)) out.add(c);
        }
        return out;
    }
}
