package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RegionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> KNOWN_FLAGS = List.of(
            // ── Block / break ─────────────────────────────────────────────────
            "no-break", "no-break-vanilla", "no-place",
            // ── Combat / damage ───────────────────────────────────────────────
            "pvp", "no-damage", "damage-multiplier",
            // ── Spawning ──────────────────────────────────────────────────────
            "no-mob-spawn",
            // ── Player movement / state ───────────────────────────────────────
            "fly", "keep-inventory", "no-item-drop",
            // ── Messages ──────────────────────────────────────────────────────
            "enter-message", "leave-message",
            // ── Misc / legacy ─────────────────────────────────────────────────
            "no-ability-use", "health-regen-multiplier",
            "no-dungeon-entry", "apply-status");


    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final CoreRegionService regions;
    private final RpgRegionsPlugin plugin;

    public RegionCommand(CoreRegionService regions) {
        this.regions = regions;
        this.plugin = null;
    }

    public RegionCommand(CoreRegionService regions, RpgRegionsPlugin plugin) {
        this.regions = regions;
        this.plugin = plugin;
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
            case "global" -> handleGlobal(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(msg("&cUnknown subcommand: " + args[0]));
        }
        return true;
    }

    private void handleGlobal(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.regions.admin.global")) {
            sender.sendMessage(msg("&cNo permission.")); return;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("info")) {
            // Show current global flags.
            var flags = regions.globalFlags();
            sender.sendMessage(msg("&6&l=== Global Region Flags (" + flags.size() + ") ==="));
            if (flags.isEmpty()) {
                sender.sendMessage(msg("&7No global flags set. Use &e/region global flag <key> <value>&7."));
            } else {
                for (Map.Entry<String, Object> e : flags.entrySet()) {
                    sender.sendMessage(msg("  &8" + e.getKey() + ": &f" + e.getValue()));
                }
            }
            sender.sendMessage(msg("&7These apply everywhere no region overrides them."));
            return;
        }
        if (args[1].equalsIgnoreCase("flag")) {
            if (args.length < 4) {
                sender.sendMessage(msg("&7Usage: &e/region global flag <flag> <value|clear>")); return;
            }
            String flag = args[2].toLowerCase(Locale.ROOT);
            String rawValue = args[3];
            if (rawValue.equalsIgnoreCase("clear") || rawValue.equalsIgnoreCase("remove")) {
                regions.removeGlobalFlag(flag);
                sender.sendMessage(msg("&aCleared global flag &e" + flag + "&a."));
            } else {
                Object value;
                if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false")) {
                    value = Boolean.parseBoolean(rawValue);
                } else {
                    try { value = Integer.parseInt(rawValue); }
                    catch (NumberFormatException ex) { value = rawValue; }
                }
                regions.setGlobalFlag(flag, value);
                sender.sendMessage(msg("&aSet global &e" + flag + " &7= &f" + value));
            }
            return;
        }
        sender.sendMessage(msg("&7Usage: &e/region global [info | flag <key> <value>]"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpg.regions.admin.reload")) {
            sender.sendMessage(msg("&cNo permission."));
            return;
        }
        if (plugin == null) {
            sender.sendMessage(msg("&cReload not wired."));
            return;
        }
        plugin.reloadConfig();
        regions.loadAll();
        sender.sendMessage(msg("&arpg-regions reloaded."));
    }

    private void handleDefine(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.regions.admin.define")) { sender.sendMessage(msg("&cNo permission.")); return; }
        if (!(sender instanceof Player p)) { sender.sendMessage(msg("&cPlayers only.")); return; }
        if (args.length < 2) {
            sender.sendMessage(msg("&7Usage: &e/region define <id> [radius]  &7(omit radius to use wand selection)"));
            return;
        }
        String id = args[1];

        // If the player has a wand selection (and no explicit radius given), use the cuboid.
        if (args.length < 3) {
            var sel = tryGetSelection(p);
            if (sel.isEmpty()) {
                sender.sendMessage(msg("&7Usage: &e/region define <id> [radius]  &7(no wand selection found)"));
                return;
            }
            org.bukkit.util.Vector min = sel.get().min();
            org.bukkit.util.Vector max = sel.get().max();
            CoreRegion region = new CoreRegion(id, sel.get().corner1().getWorld().getName(),
                    (int) min.getX(), (int) min.getY(), (int) min.getZ(),
                    (int) max.getX(), (int) max.getY(), (int) max.getZ(),
                    0, new HashMap<>());
            regions.put(region);
            sender.sendMessage(msg("&aDefined region &e" + id + " &afrom wand selection."));
            try { com.github._255_ping.rpg.api.RpgServices.wands().clearSelection(p); } catch (IllegalStateException ignored) {}
            return;
        }

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

    private static java.util.Optional<com.github._255_ping.rpg.api.wand.WandSelection> tryGetSelection(Player p) {
        try { return com.github._255_ping.rpg.api.RpgServices.wands().selectionOf(p); }
        catch (IllegalStateException ex) { return java.util.Optional.empty(); }
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
        var globalFlags = regions.globalFlags();
        sender.sendMessage(msg("&6&l=== Regions (" + (all.size() + 1) + ") ==="));
        sender.sendMessage(msg("&7- &6[global] &7(world-wide, " + globalFlags.size() + " flag(s)) &8— /region global"));
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
            sender.sendMessage(msg("&7Usage: &e/region flag <id|__global__> <flag> <value|clear>")); return;
        }
        // __global__ is a reserved alias for the world-wide global region.
        if (args[1].equalsIgnoreCase("__global__")) {
            if (!sender.hasPermission("rpg.regions.admin.global")) {
                sender.sendMessage(msg("&cNo permission.")); return;
            }
            String flag = args[2].toLowerCase(Locale.ROOT);
            String rawValue = args[3];
            if (rawValue.equalsIgnoreCase("clear") || rawValue.equalsIgnoreCase("remove")) {
                regions.removeGlobalFlag(flag);
                sender.sendMessage(msg("&aCleared global flag &e" + flag + "&a."));
            } else {
                Object value;
                if (rawValue.equalsIgnoreCase("true") || rawValue.equalsIgnoreCase("false")) {
                    value = Boolean.parseBoolean(rawValue);
                } else {
                    try { value = Integer.parseInt(rawValue); }
                    catch (NumberFormatException ex) { value = rawValue; }
                }
                regions.setGlobalFlag(flag, value);
                sender.sendMessage(msg("&aSet global &e" + flag + " &7= &f" + value));
            }
            return;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return filter(args[0], List.of("define", "delete", "list", "info", "flag", "global", "reload"));
        String sub = args[0].toLowerCase(Locale.ROOT);

        // arg2: suggest region ids only where a region id is actually needed
        if (args.length == 2) {
            if (sub.equals("delete")) return filterRegions(args[1]);
            if (sub.equals("flag")) {
                // __global__ is the reserved alias for the world-wide region.
                List<String> ids = new ArrayList<>(filterRegions(args[1]));
                if ("__global__".startsWith(args[1].toLowerCase())) ids.add(0, "__global__");
                return ids;
            }
            // "info" and "list" take no id; "global" takes a sub-subcommand
            if (sub.equals("global")) return filter(args[1], List.of("info", "flag"));
        }

        // /region flag <id> <flagName>
        if (sub.equals("flag") && args.length == 3) return filter(args[2], KNOWN_FLAGS);
        // /region flag <id> <flagName> <value>
        if (sub.equals("flag") && args.length == 4) {
            String flagName = args[2].toLowerCase(Locale.ROOT);
            if (flagName.equals("enter-message") || flagName.equals("leave-message")) {
                return List.of(); // free-text — no suggestions for message strings
            }
            return filter(args[3], List.of("true", "false", "clear"));
        }

        // /region global flag <flagName>
        if (sub.equals("global") && args.length == 3 && args[1].equalsIgnoreCase("flag"))
            return filter(args[2], KNOWN_FLAGS);
        // /region global flag <flagName> <value>
        if (sub.equals("global") && args.length == 4 && args[1].equalsIgnoreCase("flag"))
            return filter(args[3], List.of("true", "false", "clear"));

        return List.of();
    }

    private List<String> filterRegions(String prefix) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (Region r : regions.all()) {
            if (r.id().toLowerCase().startsWith(lower)) out.add(r.id());
        }
        return out;
    }

    private static List<String> filter(String prefix, List<String> options) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String o : options) { if (o.startsWith(lower)) out.add(o); }
        return out;
    }

    private static Component msg(String legacy) { return LEGACY.deserialize(legacy); }
}
