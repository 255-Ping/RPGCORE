package com.github._255_ping.rpg.npcs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class NpcCommand implements CommandExecutor, TabCompleter {

    private final RpgNpcsPlugin plugin;

    public NpcCommand(RpgNpcsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/npc <create|delete|move|list|reload|setbehavior>")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "move" -> handleMove(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "setbehavior" -> handleSetBehavior(sender, args);
            default -> sender.sendMessage(Component.text("Unknown: " + args[0]).color(NamedTextColor.YELLOW));
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.create")) {
            sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Players only.").color(NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("/npc create <id> [displayName]").color(NamedTextColor.YELLOW));
            return;
        }
        String id = args[1];
        if (plugin.manager().get(id).isPresent()) {
            sender.sendMessage(Component.text("NPC '" + id + "' already exists.").color(NamedTextColor.RED));
            return;
        }
        String name = args.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : id;
        plugin.manager().create(id, p.getLocation(), name);
        sender.sendMessage(Component.text("Created NPC '" + id + "'.").color(NamedTextColor.GREEN));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.delete")) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("/npc delete <id>").color(NamedTextColor.YELLOW));
            return;
        }
        boolean ok = plugin.manager().delete(args[1]);
        sender.sendMessage(Component.text(ok ? "Deleted." : "Not found.")
                .color(ok ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.move")) return;
        if (!(sender instanceof Player p)) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("/npc move <id>").color(NamedTextColor.YELLOW));
            return;
        }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) {
            sender.sendMessage(Component.text("Not found.").color(NamedTextColor.RED));
            return;
        }
        plugin.manager().move(opt.get(), p.getLocation());
        sender.sendMessage(Component.text("Moved.").color(NamedTextColor.GREEN));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("rpg.npcs.admin.list")) return;
        sender.sendMessage(Component.text(plugin.manager().all().size() + " NPCs:")
                .color(NamedTextColor.AQUA));
        for (NpcDef def : plugin.manager().all()) {
            sender.sendMessage(Component.text(" - " + def.id() + " (" + def.behaviorType() + ")"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpg.npcs.admin.reload")) return;
        plugin.reloadAll();
        sender.sendMessage(Component.text("rpg-npcs reloaded.").color(NamedTextColor.GREEN));
    }

    private void handleSetBehavior(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.npcs.admin.setbehavior")) return;
        if (args.length < 3) {
            sender.sendMessage(Component.text("/npc setbehavior <id> <dialogue|shop|quest|banker> [arg...]")
                    .color(NamedTextColor.YELLOW));
            return;
        }
        Optional<NpcDef> opt = plugin.manager().get(args[1]);
        if (opt.isEmpty()) {
            sender.sendMessage(Component.text("Not found.").color(NamedTextColor.RED));
            return;
        }
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
                if (args.length >= 4) {
                    lines.add(String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)));
                }
                def.setBehavior(NpcDef.BehaviorType.DIALOGUE, def.shopItems(), lines, null, null);
            }
            case QUEST -> {
                String quest = args.length >= 4 ? args[3] : "";
                def.setBehavior(NpcDef.BehaviorType.QUEST, def.shopItems(), def.dialogueLines(), quest, null);
            }
            case SHOP -> {
                def.setBehavior(NpcDef.BehaviorType.SHOP, def.shopItems(), def.dialogueLines(), null, null);
                sender.sendMessage(Component.text("Shop behavior set. Edit npcs/all.yml to add items.")
                        .color(NamedTextColor.YELLOW));
            }
            case BANKER -> {
                String bankName = args.length >= 4
                    ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length))
                    : def.displayName();
                def.setBehavior(NpcDef.BehaviorType.BANKER, def.shopItems(), def.dialogueLines(), null,
                    new NpcDef.BankerData(bankName, 0.5));
                sender.sendMessage(Component.text("Banker behavior set. Bank: " + bankName).color(NamedTextColor.GREEN));
            }
        }
        plugin.manager().rebuild(def);
        sender.sendMessage(Component.text("Behavior updated.").color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("create", "delete", "move", "list", "reload", "setbehavior");
        if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            return plugin.manager().all().stream().map(NpcDef::id).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setbehavior")) {
            return List.of("dialogue", "shop", "quest", "banker");
        }
        return List.of();
    }
}
