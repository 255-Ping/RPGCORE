package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RegionCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final CoreRegionService regions;

    public RegionCommand(CoreRegionService regions) {
        this.regions = regions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(msg("&7Usage: &e/region <define|delete|list|info|flag>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "define" -> handleDefine(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender);
            case "flag" -> handleFlag(sender, args);
            default -> sender.sendMessage(msg("&cUnknown subcommand: " + args[0]));
        }
        return true;
    }

    private void handleDefine(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.regions.admin.define")) { sender.sendMessage(msg("&cNo permission.")); return; }
        if (!(sender instanceof Player p)) { sender.sendMessage(msg("&cPlayers only.")); return; }
        if (args.length < 3) {
            sender.sendMessage(msg("&7Usage: &e/region define <id> <radius>")); return;
        }
        String id = args[1];
        int radius;
        try { radius = Math.max(1, Math.min(256, Integer.parseInt(args[2]))); }
        catch (NumberFormatException ex) { sender.sendMessage(msg("&cBad radius.")); return; }
        Location loc = p.getLocation();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        CoreRegion region = new CoreRegion(id, loc.getWorld().getName(),
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius,
                0, new HashMap<>());
        regions.put(region);
        sender.sendMessage(msg("&aDefined region &e" + id + " &awith radius &e" + radius + "&a."));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.regions.admin.delete")) { sender.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { sender.sendMessage(msg("&7Usage: &e/region delete <id>")); return; }
        if (regions.remove(args[1])) {
            sender.sendMessage(msg("&aDeleted region &e" + args[1] + "&a."));
        } else {
            sender.sendMessage(msg("&cNo region with id &7" + args[1] + "&c."));
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("rpg.regions.admin.list")) { sender.sendMessage(msg("&cNo permission.")); return; }
        var all = regions.all();
        sender.sendMessage(msg("&6&l=== Regions (" + all.size() + ") ==="));
        for (Region r : all) {
            sender.sendMessage(msg("&7- &e" + r.id()
                    + " &7(" + r.world().getName() + " "
                    + r.minX() + "," + r.minY() + "," + r.minZ() + " → "
                    + r.maxX() + "," + r.maxY() + "," + r.maxZ()
                    + ", priority " + r.priority() + ")"));
        }
    }

    private void handleInfo(CommandSender sender) {
        if (!sender.hasPermission("rpg.regions.admin.info")) { sender.sendMessage(msg("&cNo permission.")); return; }
        if (!(sender instanceof Player p)) { sender.sendMessage(msg("&cPlayers only.")); return; }
        List<Region> here = regions.regionsAt(p.getLocation());
        if (here.isEmpty()) {
            sender.sendMessage(msg("&7No regions at your location."));
            return;
        }
        sender.sendMessage(msg("&6&l=== Regions here ==="));
        for (Region r : here) {
            sender.sendMessage(msg("&7- &e" + r.id() + " &7(priority " + r.priority() + ")"));
            for (Map.Entry<String, Object> e : r.flags().entrySet()) {
                sender.sendMessage(msg("    &8" + e.getKey() + ": &f" + e.getValue()));
            }
        }
    }

    private void handleFlag(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.regions.admin.flag")) { sender.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 4) {
            sender.sendMessage(msg("&7Usage: &e/region flag <id> <flag> <value>")); return;
        }
        var opt = regions.get(args[1]);
        if (opt.isEmpty()) { sender.sendMessage(msg("&cNo region with id &7" + args[1])); return; }
        CoreRegion region = (CoreRegion) opt.get();
        String flag = args[2].toLowerCase(Locale.ROOT);
        String rawValue = args[3];
        Object value;
        if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false")) {
            value = Boolean.parseBoolean(rawValue);
        } else {
            try { value = Integer.parseInt(rawValue); }
            catch (NumberFormatException ex) { value = rawValue; }
        }
        region.setFlag(flag, value);
        regions.saveOne(region.id());
        sender.sendMessage(msg("&aSet &e" + region.id() + "." + flag + " &7= &f" + value));
    }

    private static Component msg(String legacy) { return LEGACY.deserialize(legacy); }
}
