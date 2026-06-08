package com.github._255_ping.rpg.holograms;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class RpgHologramsPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

    private HologramManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new HologramManager(this);
        manager.loadAll();
        var cmd = Objects.requireNonNull(getCommand("holograms"), "command 'holograms' missing");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
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
            sender.sendMessage("§7Usage: §e/holograms <create|delete|list|info|tp|move|line|set|reload>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list"   -> handleList(sender);
            case "info"   -> handleInfo(sender, args);
            case "tp"     -> handleTp(sender, args);
            case "move"   -> handleMove(sender, args);
            case "line"   -> handleLine(sender, args);
            case "set"    -> handleSet(sender, args);
            default -> sender.sendMessage("§cUnknown subcommand: " + args[0]);
        }
        return true;
    }

    // ── Subcommand handlers ────────────────────────────────────────────────────

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
        sender.sendMessage("§aHologram '§e" + id + "§a' created.");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.delete")) {
            sender.sendMessage("§cNo permission."); return;
        }
        if (args.length < 2) { sender.sendMessage("§7/holograms delete <id>"); return; }
        boolean ok = manager.delete(args[1]);
        sender.sendMessage(ok ? "§aDeleted." : "§cNot found.");
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("rpg.holograms.admin.list")) {
            sender.sendMessage("§cNo permission."); return;
        }
        sender.sendMessage("§7Holograms: §e" + manager.all().size());
        for (HologramDef def : manager.all()) {
            String animTag = def.animated() ? " §b[anim " + def.frameInterval() + "t]" : "";
            sender.sendMessage("§7- §e" + def.id() + animTag + " §8("
                    + def.worldName() + " " + (int) def.x() + "," + (int) def.y() + "," + (int) def.z()
                    + ", " + def.lines().size() + " lines)");
        }
    }

    /** Shows id, location, line/frame count, and animated status for one hologram. */
    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.list")) {
            sender.sendMessage("§cNo permission."); return;
        }
        if (args.length < 2) { sender.sendMessage("§7/holograms info <id>"); return; }
        Optional<HologramDef> opt = manager.get(args[1]);
        if (opt.isEmpty()) { sender.sendMessage("§cNot found."); return; }
        HologramDef def = opt.get();
        sender.sendMessage("§6=== Hologram: §e" + def.id() + " §6===");
        sender.sendMessage("§7World: §f" + def.worldName()
                + "  §7Pos: §f" + String.format("%.2f, %.2f, %.2f", def.x(), def.y(), def.z()));
        sender.sendMessage("§7Animated: §f" + (def.animated() ? "§atrue§f (interval " + def.frameInterval() + " ticks)" : "§cfalse"));
        sender.sendMessage("§7" + (def.animated() ? "Frames" : "Lines") + ": §f" + def.lines().size());
        for (int i = 0; i < def.lines().size(); i++) {
            sender.sendMessage("§8  [" + i + "] §r" + def.lines().get(i));
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.tp")) {
            sender.sendMessage("§cNo permission."); return;
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return; }
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
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return; }
        if (args.length < 2) { sender.sendMessage("§7/holograms move <id>"); return; }
        Optional<HologramDef> opt = manager.get(args[1]);
        if (opt.isEmpty()) { sender.sendMessage("§cNot found."); return; }
        manager.move(opt.get(), p.getLocation());
        sender.sendMessage("§aMoved hologram '§e" + args[1] + "§a' here.");
    }

    private void handleLine(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.edit")) {
            sender.sendMessage("§cNo permission."); return;
        }
        if (args.length < 3) {
            sender.sendMessage("§7/holograms line <add|set|remove|list> <id> [index] [text...]");
            return;
        }
        String op = args[1].toLowerCase(Locale.ROOT);
        String id = args[2];
        Optional<HologramDef> opt = manager.get(id);
        if (opt.isEmpty()) { sender.sendMessage("§cNot found."); return; }
        HologramDef def = opt.get();

        switch (op) {
            case "list" -> {
                // Print all lines / frames with their indices
                String label = def.animated() ? "frames" : "lines";
                sender.sendMessage("§7Hologram '§e" + def.id() + "§7' " + label + " (" + def.lines().size() + "):");
                for (int i = 0; i < def.lines().size(); i++) {
                    sender.sendMessage("§8  [" + i + "] §r" + def.lines().get(i));
                }
            }
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

    /**
     * Sets a property on an existing hologram.
     * Currently supported properties: {@code animated}, {@code frameinterval}.
     */
    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.holograms.admin.edit")) {
            sender.sendMessage("§cNo permission."); return;
        }
        if (args.length < 4) {
            sender.sendMessage("§7/holograms set <id> <animated|frameinterval> <value>"); return;
        }
        Optional<HologramDef> opt = manager.get(args[1]);
        if (opt.isEmpty()) { sender.sendMessage("§cNot found."); return; }
        HologramDef def = opt.get();
        String prop  = args[2].toLowerCase(Locale.ROOT);
        String value = args[3];
        switch (prop) {
            case "animated" -> {
                def.setAnimated(Boolean.parseBoolean(value));
                manager.rebuild(def);
                sender.sendMessage("§aSet animated = §e" + def.animated()
                        + " §aon '§e" + def.id() + "§a'.");
            }
            case "frameinterval", "interval" -> {
                try {
                    def.setFrameInterval(Integer.parseInt(value));
                    manager.saveAll();
                    sender.sendMessage("§aSet frameInterval = §e" + def.frameInterval()
                            + "t §aon '§e" + def.id() + "§a'.");
                } catch (NumberFormatException ex) {
                    sender.sendMessage("§cBad number: " + value);
                }
            }
            default -> sender.sendMessage("§cUnknown property '§7" + prop
                    + "§c'. Valid: animated, frameinterval");
        }
    }

    // ── Tab completion ──────────────────────────────────────────────────────────

    private static final List<String> SUBCOMMANDS =
            List.of("create", "delete", "list", "info", "tp", "move", "line", "set", "reload");
    private static final List<String> LINE_OPS = List.of("add", "set", "remove", "list");
    private static final List<String> SET_PROPS = List.of("animated", "frameinterval");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return filterPrefix(SUBCOMMANDS, args[0]);

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            return switch (sub) {
                case "delete", "tp", "move", "info" -> hologramIds(args[1]);
                case "line"                          -> filterPrefix(LINE_OPS, args[1]);
                case "set"                           -> hologramIds(args[1]);
                default                              -> List.of();
            };
        }

        // /holograms line <op> <id> [index] [text...]
        if (sub.equals("line")) {
            String op = args[1].toLowerCase(Locale.ROOT);
            if (args.length == 3 && LINE_OPS.contains(op)) return hologramIds(args[2]);
            if (args.length == 4 && (op.equals("set") || op.equals("remove"))) {
                return manager.get(args[2]).map(def -> {
                    List<String> indices = new ArrayList<>();
                    for (int i = 0; i < def.lines().size(); i++) indices.add(String.valueOf(i));
                    return filterPrefix(indices, args[3]);
                }).orElse(List.of());
            }
        }

        // /holograms set <id> <prop> [value]
        if (sub.equals("set")) {
            if (args.length == 3) return filterPrefix(SET_PROPS, args[2]);
            if (args.length == 4 && args[2].equalsIgnoreCase("animated")) {
                return filterPrefix(List.of("true", "false"), args[3]);
            }
        }

        return List.of();
    }

    /** Returns IDs of all registered holograms that start with {@code partial}. */
    private List<String> hologramIds(String partial) {
        return filterPrefix(
                manager.all().stream().map(HologramDef::id).collect(Collectors.toList()),
                partial);
    }

    /** Case-insensitive prefix filter. Returns the full list unchanged when {@code partial} is empty. */
    private static List<String> filterPrefix(List<String> candidates, String partial) {
        if (partial.isEmpty()) return candidates;
        String lower = partial.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
