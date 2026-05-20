package com.github._255_ping.rpg.dungeons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class DungeonCommand implements CommandExecutor, TabCompleter {

    private final RpgDungeonsPlugin plugin;

    public DungeonCommand(RpgDungeonsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/dungeon <list|create|delete|enter|leave|setentrance|setexit|setspawn|reload>")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> handleList(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "enter" -> handleEnter(sender, args);
            case "leave" -> handleLeave(sender);
            case "setentrance" -> handleSet(sender, args, SetTarget.ENTRANCE);
            case "setexit" -> handleSet(sender, args, SetTarget.EXIT);
            case "setspawn" -> handleSet(sender, args, SetTarget.SPAWN);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(Component.text("Unknown: " + args[0]).color(NamedTextColor.YELLOW));
        }
        return true;
    }

    private enum SetTarget { ENTRANCE, EXIT, SPAWN }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("rpg.dungeons.admin.list")) return;
        sender.sendMessage(Component.text("Dungeons:").color(NamedTextColor.AQUA));
        for (DungeonDef def : plugin.registry().all()) {
            sender.sendMessage(Component.text(" - " + def.id() + " (" + def.displayName() + ")"));
        }
        sender.sendMessage(Component.text("Active instances: " + plugin.manager().activeInstances().size())
                .color(NamedTextColor.GRAY));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.dungeons.admin.create")) return;
        if (!(sender instanceof Player p)) return;
        if (args.length < 5) {
            sender.sendMessage(Component.text("/dungeon create <id> <minX,Y,Z> <maxX,Y,Z> <displayName>")
                    .color(NamedTextColor.YELLOW));
            return;
        }
        String id = args[1];
        Vector min = parseVec(args[2]);
        Vector max = parseVec(args[3]);
        if (min == null || max == null) {
            sender.sendMessage(Component.text("Bad coordinates.").color(NamedTextColor.RED));
            return;
        }
        String displayName = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
        if (plugin.registry().get(id).isPresent()) {
            sender.sendMessage(Component.text("Already exists.").color(NamedTextColor.RED));
            return;
        }
        String world = p.getWorld().getName();
        Vector spawn = new Vector(
                (max.getX() - min.getX()) / 2,
                1,
                (max.getZ() - min.getZ()) / 2);
        Vector ent = p.getLocation().toVector();
        DungeonDef def = new DungeonDef(id.toLowerCase(Locale.ROOT), displayName,
                world, min, max, spawn, world, ent, world, ent, 4, 0);
        plugin.registry().put(def);
        plugin.registry().saveAll();
        sender.sendMessage(Component.text("Created dungeon '" + id + "'.").color(NamedTextColor.GREEN));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.dungeons.admin.delete")) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("/dungeon delete <id>").color(NamedTextColor.YELLOW));
            return;
        }
        boolean ok = plugin.registry().remove(args[1]);
        if (ok) plugin.registry().saveAll();
        sender.sendMessage(Component.text(ok ? "Deleted." : "Not found.")
                .color(ok ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void handleEnter(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        if (!p.hasPermission("rpg.dungeons.use.enter")) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("/dungeon enter <id>").color(NamedTextColor.YELLOW));
            return;
        }
        plugin.manager().enter(p, args[1]);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player p)) return;
        if (!p.hasPermission("rpg.dungeons.use.leave")) return;
        plugin.manager().leave(p);
    }

    private void handleSet(CommandSender sender, String[] args, SetTarget target) {
        if (!sender.hasPermission("rpg.dungeons.admin.set")) return;
        if (!(sender instanceof Player p)) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("/dungeon set... <id>").color(NamedTextColor.YELLOW));
            return;
        }
        Optional<DungeonDef> opt = plugin.registry().get(args[1]);
        if (opt.isEmpty()) {
            sender.sendMessage(Component.text("Not found.").color(NamedTextColor.RED));
            return;
        }
        DungeonDef d = opt.get();
        Vector at = p.getLocation().toVector();
        String world = p.getWorld().getName();
        DungeonDef updated = switch (target) {
            case ENTRANCE -> new DungeonDef(d.id(), d.displayName(), d.templateWorld(), d.min(), d.max(),
                    d.spawnOffset(), world, at, d.exitWorld(), d.exit(), d.maxPlayers(), d.requiredLevel());
            case EXIT -> new DungeonDef(d.id(), d.displayName(), d.templateWorld(), d.min(), d.max(),
                    d.spawnOffset(), d.entranceWorld(), d.entrance(), world, at, d.maxPlayers(), d.requiredLevel());
            case SPAWN -> new DungeonDef(d.id(), d.displayName(), d.templateWorld(), d.min(), d.max(),
                    at.clone().subtract(d.min()), d.entranceWorld(), d.entrance(), d.exitWorld(), d.exit(),
                    d.maxPlayers(), d.requiredLevel());
        };
        plugin.registry().put(updated);
        plugin.registry().saveAll();
        sender.sendMessage(Component.text(target.name().toLowerCase() + " set.").color(NamedTextColor.GREEN));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpg.dungeons.admin.reload")) return;
        plugin.reloadAll();
        sender.sendMessage(Component.text("rpg-dungeons reloaded.").color(NamedTextColor.GREEN));
    }

    private static Vector parseVec(String s) {
        try {
            String[] parts = s.split(",");
            return new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        } catch (Exception ex) { return null; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("list", "create", "delete", "enter", "leave",
                "setentrance", "setexit", "setspawn", "reload");
        if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            return plugin.registry().all().stream().map(DungeonDef::id).toList();
        }
        return List.of();
    }
}
