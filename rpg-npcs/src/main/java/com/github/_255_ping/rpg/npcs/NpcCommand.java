package com.github._255_ping.rpg.npcs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class NpcCommand implements CommandExecutor, TabCompleter {

    private final RpgNpcsPlugin plugin;

    public NpcCommand(RpgNpcsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create"        -> handleCreate(sender, args);
            case "delete"        -> handleDelete(sender, args);
            case "move"          -> handleMove(sender, args);
            case "list"          -> handleList(sender);
            case "reload"        -> handleReload(sender);
            case "setbehavior"   -> handleSetBehavior(sender, args);
            case "setentitytype" -> handleSetEntityType(sender, args);
            case "setstyle"      -> handleSetStyle(sender, args);
            case "setskin"       -> handleSetSkin(sender, args);
            case "dialogue"      -> handleDialogue(sender, args);
            case "shop"          -> handleShop(sender, args);
            case "setlook"       -> handleSetLook(sender, args);
            case "info"          -> handleInfo(sender, args);
            default -> sender.sendMessage(Component.text("Unknown subcommand: " + args[0]
                    + ". Type /npc for help.").color(NamedTextColor.YELLOW));
        }
        return true;
    }

    // ---- help ----

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== /npc subcommands ===").color(NamedTextColor.GOLD));
        help(sender, "create <id> [name]",                "Spawn an NPC at your location");
        help(sender, "delete <id>",                       "Remove an NPC permanently");
        help(sender, "move <id>",                         "Teleport NPC to your location");
        help(sender, "list",                              "List all NPCs");
        help(sender, "info <id>",                         "Show all settings for an NPC");
        help(sender, "reload",                            "Reload all NPCs from disk");
        help(sender, "setbehavior <id> <type> [arg]",     "Set dialogue/shop/quest/banker");
        help(sender, "setentitytype <id> <TYPE>",         "Set the entity type (ENTITY style)");
        help(sender, "setstyle <id> <entity|player>",     "Switch body style");
        help(sender, "setskin <id> <name|raw v sig>",     "Apply a player skin (PLAYER style)");
        help(sender, "setlook <id> <true|false>",         "Toggle look-at-nearest-player");
        help(sender, "dialogue <sub> <id> [args]",        "add/set/remove/clear/list dialogue");
        help(sender, "shop <sub> <id> [args]",            "add/remove/list/clear shop items");
    }

    private void help(CommandSender s, String usage, String desc) {
        s.sendMessage(Component.text("  /" + "npc " + usage + " ").color(NamedTextColor.YELLOW)
                .append(Component.text("— " + desc).color(NamedTextColor.GRAY)));
    }

    // ---- create / delete / move / list / reload ----

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.create")) { noPerms(sender); return; }
        if (!(sender instanceof Player p)) { playerOnly(sender); return; }
        if (args.length < 2) { usage(sender, "/npc create <id> [displayName]"); return; }
        String id = args[1];
        if (plugin.manager().get(id).isPresent()) {
            sender.sendMessage(Component.text("NPC '" + id + "' already exists.").color(NamedTextColor.RED));
            return;
        }
        String name = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : id;
        plugin.manager().create(id, p.getLocation(), name);
        sender.sendMessage(Component.text("Created NPC '" + id + "'.").color(NamedTextColor.GREEN));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.delete")) { noPerms(sender); return; }
        if (args.length < 2) { usage(sender, "/npc delete <id>"); return; }
        boolean ok = plugin.manager().delete(args[1]);
        sender.sendMessage(Component.text(ok ? "Deleted '" + args[1] + "'." : "NPC not found: " + args[1])
                .color(ok ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.move")) { noPerms(sender); return; }
        if (!(sender instanceof Player p)) { playerOnly(sender); return; }
        if (args.length < 2) { usage(sender, "/npc move <id>"); return; }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) { notFound(sender, args[1]); return; }
        plugin.manager().move(opt.get(), p.getLocation());
        sender.sendMessage(Component.text("Moved '" + args[1] + "' to your location.").color(NamedTextColor.GREEN));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("rpg.npcs.admin.list")) { noPerms(sender); return; }
        Collection<NpcDef> all = plugin.manager().all();
        sender.sendMessage(Component.text(all.size() + " NPC(s):").color(NamedTextColor.AQUA));
        for (NpcDef def : all) {
            sender.sendMessage(Component.text("  " + def.id()
                    + " [" + def.behaviorType().name().toLowerCase() + "]"
                    + " (" + def.entityStyle().name().toLowerCase() + ")"
                    + " @ " + def.worldName()
                    + " " + String.format("%.0f,%.0f,%.0f", def.x(), def.y(), def.z()))
                    .color(NamedTextColor.GRAY));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpg.npcs.admin.reload")) { noPerms(sender); return; }
        plugin.reloadAll();
        sender.sendMessage(Component.text("rpg-npcs reloaded.").color(NamedTextColor.GREEN));
    }

    // ---- setbehavior ----

    private void handleSetBehavior(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.setbehavior")) { noPerms(sender); return; }
        if (args.length < 3) {
            usage(sender, "/npc setbehavior <id> <dialogue|shop|quest|banker> [arg]");
            return;
        }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) { notFound(sender, args[1]); return; }
        NpcDef def = opt.get();
        NpcDef.BehaviorType type;
        try {
            type = NpcDef.BehaviorType.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown behavior: " + args[2]).color(NamedTextColor.RED));
            return;
        }
        switch (type) {
            case DIALOGUE -> {
                List<String> lines = new ArrayList<>();
                if (args.length >= 4) lines.add(String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
                def.setBehavior(NpcDef.BehaviorType.DIALOGUE, def.shopItems(), lines, null, null);
            }
            case QUEST -> {
                String quest = args.length >= 4 ? args[3] : "";
                def.setBehavior(NpcDef.BehaviorType.QUEST, def.shopItems(), def.dialogueLines(), quest, null);
            }
            case SHOP -> {
                def.setBehavior(NpcDef.BehaviorType.SHOP, def.shopItems(), def.dialogueLines(), null, null);
                sender.sendMessage(Component.text("Shop behavior set. Use /npc shop add to add items.")
                        .color(NamedTextColor.YELLOW));
            }
            case BANKER -> {
                String bankName = args.length >= 4
                        ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                        : def.displayName();
                def.setBehavior(NpcDef.BehaviorType.BANKER, def.shopItems(), def.dialogueLines(), null,
                        new NpcDef.BankerData(bankName, 0.5));
                sender.sendMessage(Component.text("Banker behavior set. Bank: " + bankName)
                        .color(NamedTextColor.GREEN));
            }
        }
        plugin.manager().rebuild(def);
        sender.sendMessage(Component.text("Behavior updated.").color(NamedTextColor.GREEN));
    }

    // ---- setentitytype ----

    private void handleSetEntityType(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.setentitytype")) { noPerms(sender); return; }
        if (args.length < 3) { usage(sender, "/npc setentitytype <id> <ENTITY_TYPE>"); return; }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) { notFound(sender, args[1]); return; }
        NpcDef def = opt.get();
        if (def.entityStyle() != NpcDef.EntityStyle.ENTITY) {
            sender.sendMessage(Component.text("This NPC uses PLAYER style — entity type doesn't apply.")
                    .color(NamedTextColor.YELLOW));
            return;
        }
        String typeName = args[2].toUpperCase(Locale.ROOT);
        try {
            EntityType.valueOf(typeName);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown entity type: " + typeName).color(NamedTextColor.RED));
            return;
        }
        def.setEntityType(typeName);
        plugin.manager().rebuild(def);
        sender.sendMessage(Component.text("Entity type set to " + typeName + ".").color(NamedTextColor.GREEN));
    }

    // ---- setstyle ----

    private void handleSetStyle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.setstyle")) { noPerms(sender); return; }
        if (args.length < 3) { usage(sender, "/npc setstyle <id> <entity|player>"); return; }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) { notFound(sender, args[1]); return; }
        NpcDef def = opt.get();
        NpcDef.EntityStyle style;
        try {
            style = NpcDef.EntityStyle.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown style: " + args[2] + ". Use entity or player.")
                    .color(NamedTextColor.RED));
            return;
        }
        def.setEntityStyle(style);
        plugin.manager().rebuild(def);
        sender.sendMessage(Component.text("Style set to " + style.name().toLowerCase() + ".")
                .color(NamedTextColor.GREEN));
    }

    // ---- setskin ----

    private void handleSetSkin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.setskin")) { noPerms(sender); return; }
        if (args.length < 3) {
            usage(sender, "/npc setskin <id> <playerName>  OR  /npc setskin <id> raw <value> <signature>");
            return;
        }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) { notFound(sender, args[1]); return; }
        NpcDef def = opt.get();

        if (args[2].equalsIgnoreCase("raw")) {
            if (args.length < 5) {
                usage(sender, "/npc setskin <id> raw <value> <signature>");
                return;
            }
            def.setSkin(new NpcDef.SkinDef(null, args[3], args[4]));
            plugin.manager().rebuild(def);
            sender.sendMessage(Component.text("Raw skin applied.").color(NamedTextColor.GREEN));
        } else {
            String playerName = args[2];
            // Store player name only — NpcManager will fetch the texture on next spawn.
            def.setSkin(new NpcDef.SkinDef(playerName, null, null));
            // Switch to PLAYER style automatically if not already.
            if (def.entityStyle() != NpcDef.EntityStyle.PLAYER) {
                def.setEntityStyle(NpcDef.EntityStyle.PLAYER);
                sender.sendMessage(Component.text("Style switched to player automatically.")
                        .color(NamedTextColor.YELLOW));
            }
            plugin.manager().rebuild(def);
            sender.sendMessage(Component.text("Skin fetch queued for '" + playerName + "'. Respawn to apply.")
                    .color(NamedTextColor.GREEN));
        }
    }

    // ---- dialogue ----

    private void handleDialogue(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.dialogue")) { noPerms(sender); return; }
        // /npc dialogue <sub> <id> [args]
        if (args.length < 3) {
            usage(sender, "/npc dialogue <add|set|remove|clear|list> <id> [args]");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        Optional<NpcDef> opt = plugin.manager().get(args[2]);
        if (opt.isEmpty()) { notFound(sender, args[2]); return; }
        NpcDef def = opt.get();
        List<String> lines = def.dialogueLines();

        switch (sub) {
            case "add" -> {
                if (args.length < 4) { usage(sender, "/npc dialogue add <id> <line...>"); return; }
                String line = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                lines.add(line);
                plugin.manager().saveOnly();
                sender.sendMessage(Component.text("Added dialogue line " + (lines.size() - 1) + ".")
                        .color(NamedTextColor.GREEN));
            }
            case "set" -> {
                if (args.length < 5) { usage(sender, "/npc dialogue set <id> <index> <line...>"); return; }
                int idx = parseIndex(sender, args[3], lines.size()); if (idx < 0) return;
                String line = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                lines.set(idx, line);
                plugin.manager().saveOnly();
                sender.sendMessage(Component.text("Updated line " + idx + ".").color(NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (args.length < 4) { usage(sender, "/npc dialogue remove <id> <index>"); return; }
                int idx = parseIndex(sender, args[3], lines.size()); if (idx < 0) return;
                lines.remove(idx);
                plugin.manager().saveOnly();
                sender.sendMessage(Component.text("Removed line " + idx + ".").color(NamedTextColor.GREEN));
            }
            case "clear" -> {
                lines.clear();
                plugin.manager().saveOnly();
                sender.sendMessage(Component.text("Cleared all dialogue.").color(NamedTextColor.GREEN));
            }
            case "list" -> {
                if (lines.isEmpty()) {
                    sender.sendMessage(Component.text("No dialogue lines.").color(NamedTextColor.GRAY));
                } else {
                    sender.sendMessage(Component.text("Dialogue for '" + def.id() + "':").color(NamedTextColor.AQUA));
                    for (int i = 0; i < lines.size(); i++) {
                        sender.sendMessage(Component.text("  [" + i + "] " + lines.get(i)).color(NamedTextColor.GRAY));
                    }
                }
            }
            default -> usage(sender, "/npc dialogue <add|set|remove|clear|list> <id> [args]");
        }
    }

    // ---- shop ----

    private void handleShop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.shop")) { noPerms(sender); return; }
        // /npc shop <sub> <id> [args]
        if (args.length < 3) {
            usage(sender, "/npc shop <add|remove|list|clear> <id> [args]");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        Optional<NpcDef> opt = plugin.manager().get(args[2]);
        if (opt.isEmpty()) { notFound(sender, args[2]); return; }
        NpcDef def = opt.get();
        List<NpcDef.ShopEntry> items = def.shopItems();

        switch (sub) {
            case "add" -> {
                // /npc shop add <id> <itemId> <buyPrice> <sellPrice>
                if (args.length < 6) { usage(sender, "/npc shop add <id> <itemId> <buyPrice> <sellPrice>"); return; }
                double buy = parsePrice(sender, args[4]); if (buy < 0) return;
                double sell = parsePrice(sender, args[5]); if (sell < 0) return;
                items.add(new NpcDef.ShopEntry(args[3], buy, sell));
                plugin.manager().saveOnly();
                sender.sendMessage(Component.text("Added " + args[3] + " to shop (slot " + (items.size()-1) + ").")
                        .color(NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (args.length < 4) { usage(sender, "/npc shop remove <id> <index>"); return; }
                int idx = parseIndex(sender, args[3], items.size()); if (idx < 0) return;
                NpcDef.ShopEntry removed = items.remove(idx);
                plugin.manager().saveOnly();
                sender.sendMessage(Component.text("Removed " + removed.itemId() + " (slot " + idx + ").")
                        .color(NamedTextColor.GREEN));
            }
            case "list" -> {
                if (items.isEmpty()) {
                    sender.sendMessage(Component.text("No shop items.").color(NamedTextColor.GRAY));
                } else {
                    sender.sendMessage(Component.text("Shop for '" + def.id() + "':").color(NamedTextColor.AQUA));
                    for (int i = 0; i < items.size(); i++) {
                        NpcDef.ShopEntry e = items.get(i);
                        sender.sendMessage(Component.text(
                                "  [" + i + "] " + e.itemId()
                                + " buy=" + e.buy() + " sell=" + e.sell())
                                .color(NamedTextColor.GRAY));
                    }
                }
            }
            case "clear" -> {
                items.clear();
                plugin.manager().saveOnly();
                sender.sendMessage(Component.text("Cleared all shop items.").color(NamedTextColor.GREEN));
            }
            default -> usage(sender, "/npc shop <add|remove|list|clear> <id> [args]");
        }
    }

    // ---- setlook ----

    private void handleSetLook(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.setlook")) { noPerms(sender); return; }
        if (args.length < 3) { usage(sender, "/npc setlook <id> <true|false>"); return; }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) { notFound(sender, args[1]); return; }
        NpcDef def = opt.get();
        boolean enabled = Boolean.parseBoolean(args[2]);
        def.setLookAtPlayers(enabled);
        plugin.manager().saveOnly();
        sender.sendMessage(Component.text("Look-at-player " + (enabled ? "enabled" : "disabled")
                + " for '" + def.id() + "'.").color(NamedTextColor.GREEN));
    }

    // ---- info ----

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.list")) { noPerms(sender); return; }
        if (args.length < 2) { usage(sender, "/npc info <id>"); return; }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) { notFound(sender, args[1]); return; }
        NpcDef def = opt.get();

        sender.sendMessage(Component.text("=== NPC: " + def.id() + " ===").color(NamedTextColor.GOLD));
        infoLine(sender, "Display name", def.displayName());
        infoLine(sender, "World",
                def.worldName() + " @ " + String.format("%.1f, %.1f, %.1f", def.x(), def.y(), def.z()));
        infoLine(sender, "Yaw / Pitch", String.format("%.1f / %.1f", def.yaw(), def.pitch()));
        infoLine(sender, "Style", def.entityStyle().name().toLowerCase());
        if (def.entityStyle() == NpcDef.EntityStyle.ENTITY) {
            infoLine(sender, "Entity type",
                    def.entityType() != null ? def.entityType() : "(global default)");
        } else {
            NpcDef.SkinDef skin = def.skin();
            infoLine(sender, "Skin",
                    skin == null ? "none"
                    : skin.playerName() != null ? skin.playerName()
                    : skin.value() != null ? "raw (" + skin.value().substring(0, Math.min(12, skin.value().length())) + "…)" : "none");
        }
        infoLine(sender, "Behavior", def.behaviorType().name().toLowerCase());
        switch (def.behaviorType()) {
            case DIALOGUE -> infoLine(sender, "Dialogue lines", String.valueOf(def.dialogueLines().size()));
            case SHOP     -> infoLine(sender, "Shop items", String.valueOf(def.shopItems().size()));
            case QUEST    -> infoLine(sender, "Quest ID", def.questId() != null ? def.questId() : "none");
            case BANKER   -> {
                if (def.bankerData() != null) {
                    infoLine(sender, "Bank name", def.bankerData().bankName());
                    infoLine(sender, "Interest", def.bankerData().dailyInterestPercent() + "%/day");
                }
            }
        }
        infoLine(sender, "Look at players", def.lookAtPlayers() + " (radius: " + def.lookRadius() + " blocks)");
    }

    private void infoLine(CommandSender s, String key, String value) {
        s.sendMessage(Component.text("  " + key + ": ").color(NamedTextColor.YELLOW)
                .append(Component.text(value).color(NamedTextColor.WHITE)));
    }

    // ---- tab-complete ----

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String arg0 = args[0].toLowerCase(Locale.ROOT);
        List<String> npcIds = plugin.manager().all().stream().map(NpcDef::id).toList();

        if (args.length == 1) {
            return List.of("create", "delete", "move", "list", "reload",
                    "setbehavior", "setentitytype", "setstyle", "setskin",
                    "dialogue", "shop", "setlook", "info");
        }

        if (args.length == 2) {
            return switch (arg0) {
                case "dialogue" -> List.of("add", "set", "remove", "clear", "list");
                case "shop"     -> List.of("add", "remove", "list", "clear");
                case "create"   -> List.of();
                default         -> npcIds;
            };
        }

        if (args.length == 3) {
            return switch (arg0) {
                case "setbehavior"   -> List.of("dialogue", "shop", "quest", "banker");
                case "setstyle"      -> List.of("entity", "player");
                case "setlook"       -> List.of("true", "false");
                case "setentitytype" -> livingEntityTypes();
                // For dialogue/shop: arg[2] is the NPC id
                case "dialogue", "shop" -> npcIds;
                default -> List.of();
            };
        }

        if (args.length == 4) {
            // /npc setbehavior <id> quest → suggest quest IDs
            if (arg0.equals("setbehavior") && args[2].equalsIgnoreCase("quest")) {
                return getQuestIds();
            }
            // /npc dialogue set|remove <id> → suggest indices
            if (arg0.equals("dialogue")) {
                String sub = args[1].toLowerCase(Locale.ROOT);
                if (sub.equals("set") || sub.equals("remove")) {
                    return plugin.manager().get(args[2].toLowerCase(Locale.ROOT))
                            .map(def -> indexList(def.dialogueLines().size()))
                            .orElse(List.of());
                }
            }
            // /npc shop remove <id> → suggest indices
            if (arg0.equals("shop") && args[1].equalsIgnoreCase("remove")) {
                return plugin.manager().get(args[2].toLowerCase(Locale.ROOT))
                        .map(def -> indexList(def.shopItems().size()))
                        .orElse(List.of());
            }
        }

        return List.of();
    }

    // ---- helpers ----

    private static List<String> livingEntityTypes() {
        return Arrays.stream(EntityType.values())
                .filter(t -> t.getEntityClass() != null
                        && LivingEntity.class.isAssignableFrom(t.getEntityClass())
                        && t != EntityType.PLAYER)
                .map(t -> t.name().toLowerCase(Locale.ROOT))
                .sorted()
                .toList();
    }

    private static List<String> indexList(int size) {
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) out.add(String.valueOf(i));
        return out;
    }

    /** Soft-dep: reflect into rpg-quests to get quest IDs for tab-complete. */
    private static List<String> getQuestIds() {
        try {
            var qPlugin = Bukkit.getPluginManager().getPlugin("rpg-quests");
            if (qPlugin == null) return List.of();
            var registryMethod = qPlugin.getClass().getMethod("registry");
            var registry = registryMethod.invoke(qPlugin);
            var allMethod = registry.getClass().getMethod("all");
            Collection<?> defs = (Collection<?>) allMethod.invoke(registry);
            return defs.stream()
                    .map(d -> {
                        try { return (String) d.getClass().getMethod("id").invoke(d); }
                        catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private int parseIndex(CommandSender sender, String raw, int size) {
        try {
            int idx = Integer.parseInt(raw);
            if (idx < 0 || idx >= size) {
                sender.sendMessage(Component.text("Index out of range: " + idx
                        + " (valid: 0–" + (size - 1) + ")").color(NamedTextColor.RED));
                return -1;
            }
            return idx;
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text("Not a number: " + raw).color(NamedTextColor.RED));
            return -1;
        }
    }

    private double parsePrice(CommandSender sender, String raw) {
        try {
            double v = Double.parseDouble(raw);
            if (v < 0) { sender.sendMessage(Component.text("Price cannot be negative.").color(NamedTextColor.RED)); return -1; }
            return v;
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text("Invalid price: " + raw).color(NamedTextColor.RED));
            return -1;
        }
    }

    private void noPerms(CommandSender s) {
        s.sendMessage(Component.text("You don't have permission.").color(NamedTextColor.RED));
    }

    private void playerOnly(CommandSender s) {
        s.sendMessage(Component.text("This command requires a player.").color(NamedTextColor.RED));
    }

    private void notFound(CommandSender s, String id) {
        s.sendMessage(Component.text("NPC not found: " + id).color(NamedTextColor.RED));
    }

    private void usage(CommandSender s, String msg) {
        s.sendMessage(Component.text("Usage: " + msg).color(NamedTextColor.YELLOW));
    }
}
