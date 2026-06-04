package com.github._255_ping.rpg.holograms;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class RpgHologramsPlugin extends JavaPlugin implements CommandExecutor {

    private HologramManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new HologramManager(this);
        manager.loadAll();
        Objects.requireNonNull(getCommand("holograms"), "command 'holograms' missing").setExecutor(this);
        getLogger().info("rpg-holograms v" + getPluginMeta().getVersion()
                + " enabled with " + manager.all().size() + " holograms.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.saveAll();
            manager.despawnAll();
        }
        getLogger().info("rpg-holograms disabled.");
    }

    public HologramManager manager() { return manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usage: §e/holograms <create|delete|list|tp|move|line|reload>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "tp" -> handleTp(sender, args);
            case "move" -> handleMove(sender, args);
            case "line" -> handleLine(sender, args);
            default -> sender.sendMessage("§cUnknown: " + args[0]);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpg.holograms.admin.reload")) {
            sender.sendMessage("§cNo permission."); return;
        }
        reloadConfig();
        manager.loadAll();
        sender.sendMessage("§arpg-holograms reloaded.");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.create")) {
            sender.sendMessage("§cNo permission."); return;
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return; }
        if (args.length < 3) {
            sender.sendMessage("§7Usage: §e/holograms create <id> <line>");
            return;
        }
        String id = args[1];
        if (manager.get(id).isPresent()) {
            sender.sendMessage("§cAlready exists."); return;
        }
        String line = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        manager.create(id, p.getLocation(), List.of(line));
        sender.sendMessage("§aHologram '" + id + "' created.");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.delete")) return;
        if (args.length < 2) { sender.sendMessage("§7/holograms delete <id>"); return; }
        boolean ok = manager.delete(args[1]);
        sender.sendMessage(ok ? "§aDeleted." : "§cNot found.");
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("rpg.holograms.admin.list")) return;
        sender.sendMessage("§7Holograms: §e" + manager.all().size());
        for (HologramDef def : manager.all()) {
            sender.sendMessage("§7- §e" + def.id() + " §8(" + def.worldName() + " "
                    + (int) def.x() + "," + (int) def.y() + "," + (int) def.z() + ")");
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.tp")) return;
        if (!(sender instanceof Player p)) return;
        if (args.length < 2) { sender.sendMessage("§7/holograms tp <id>"); return; }
        Optional<HologramDef> opt = manager.get(args[1]);
        if (opt.isEmpty()) { sender.sendMessage("§cNot found."); return; }
        var loc = opt.get().location();
        if (loc == null) { sender.sendMessage("§cWorld not loaded."); return; }
        p.teleport(loc);
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.move")) {
            sender.sendMessage("§cNo permission."); return;
        }
        if (!(sender instanceof Player p)) return;
        if (args.length < 2) { sender.sendMessage("§7/holograms move <id>"); return; }
        Optional<HologramDef> opt = manager.get(args[1]);
        if (opt.isEmpty()) { sender.sendMessage("§cNot found."); return; }
        manager.move(opt.get(), p.getLocation());
        sender.sendMessage("§aMoved hologram '" + args[1] + "' here.");
    }

    private void handleLine(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.edit")) {
            sender.sendMessage("§cNo permission."); return;
        }
        if (args.length < 3) {
            sender.sendMessage("§7/holograms line <add|set|remove> <id> [index] [text...]");
            return;
        }
        String op = args[1].toLowerCase(Locale.ROOT);
        String id = args[2];
        Optional<HologramDef> opt = manager.get(id);
        if (opt.isEmpty()) { sender.sendMessage("§cNot found."); return; }
        HologramDef def = opt.get();
        switch (op) {
            case "add" -> {
                if (args.length < 4) { sender.sendMessage("§7/holograms line add <id> <text...>"); return; }
                String text = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                def.lines().add(text);
                manager.rebuild(def);
                sender.sendMessage("§aLine added.");
            }
            case "set" -> {
                if (args.length < 5) { sender.sendMessage("§7/holograms line set <id> <index> <text...>"); return; }
                int idx;
                try { idx = Integer.parseInt(args[3]); } catch (NumberFormatException ex) {
                    sender.sendMessage("§cBad index."); return;
                }
                if (idx < 0 || idx >= def.lines().size()) { sender.sendMessage("§cIndex out of range."); return; }
                String text = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                def.lines().set(idx, text);
                manager.rebuild(def);
                sender.sendMessage("§aLine set.");
            }
            case "remove" -> {
                if (args.length < 4) { sender.sendMessage("§7/holograms line remove <id> <index>"); return; }
                int idx;
                try { idx = Integer.parseInt(args[3]); } catch (NumberFormatException ex) {
                    sender.sendMessage("§cBad index."); return;
                }
                if (idx < 0 || idx >= def.lines().size()) { sender.sendMessage("§cIndex out of range."); return; }
                def.lines().remove(idx);
                manager.rebuild(def);
                sender.sendMessage("§aLine removed.");
            }
            default -> sender.sendMessage("§cUnknown line op: " + op);
        }
    }
}
