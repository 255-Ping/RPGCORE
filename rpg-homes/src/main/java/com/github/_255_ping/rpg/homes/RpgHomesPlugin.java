package com.github._255_ping.rpg.homes;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * rpg-homes — player homes and server warps.
 *
 * <h3>Commands</h3>
 * <ul>
 *   <li>{@code /home [name]} — teleport to home (default: "home")</li>
 *   <li>{@code /home set [name]} — set a home at current location</li>
 *   <li>{@code /home delete <name>} — delete a home</li>
 *   <li>{@code /home list} — list all homes</li>
 *   <li>{@code /warp <name>} — teleport to a server warp</li>
 *   <li>{@code /warps} — list all warps</li>
 *   <li>{@code /setwarp <name>} — admin: set a warp (op)</li>
 *   <li>{@code /delwarp <name>} — admin: delete a warp (op)</li>
 * </ul>
 */
public final class RpgHomesPlugin extends JavaPlugin implements Listener {

    private HomeManager homeManager;
    private WarpManager warpManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        int maxHomes = getConfig().getInt("max-homes", 3);
        homeManager  = new HomeManager(maxHomes, getLogger());
        warpManager  = new WarpManager(getDataFolder(), getLogger());
        warpManager.load();

        getServer().getPluginManager().registerEvents(this, this);

        registerCmd("home",    new HomeCommand());
        registerCmd("warp",    new WarpCommand());
        registerCmd("setwarp", new SetWarpCommand());
        registerCmd("delwarp", new DelWarpCommand());
        registerCmd("warps",   new WarpsCommand());

        getLogger().info("rpg-homes v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-homes disabled.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        homeManager.unload(e.getPlayer().getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Message helper
    // -------------------------------------------------------------------------

    private String msg(String key, Object... pairs) {
        String raw = getConfig().getString("messages." + key, "[" + key + "]");
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            raw = raw.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return raw.replace("&", "§");
    }

    private void registerCmd(String name, Object handler) {
        var cmd = getCommand(name);
        if (cmd == null) { getLogger().warning("Command '" + name + "' missing from plugin.yml"); return; }
        if (handler instanceof CommandExecutor ce) cmd.setExecutor(ce);
        if (handler instanceof TabCompleter tc) cmd.setTabCompleter(tc);
    }

    // -------------------------------------------------------------------------
    // /home
    // -------------------------------------------------------------------------

    private final class HomeCommand implements CommandExecutor, TabCompleter {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
            if (!p.hasPermission("rpg.homes.use")) { p.sendMessage(msg("no-permission")); return true; }

            if (args.length == 0) {
                // /home — tp to "home"
                return teleportHome(p, "home");
            }
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set"    -> handleSet(p, args.length > 1 ? args[1] : "home");
                case "delete", "del", "remove" -> handleDelete(p, args.length > 1 ? args[1] : null);
                case "list"   -> handleList(p);
                default       -> teleportHome(p, args[0]);
            };
        }

        private boolean teleportHome(Player p, String name) {
            Optional<Location> loc = homeManager.getHome(p, name);
            if (loc.isEmpty()) { p.sendMessage(msg("home-not-found", "name", name)); return true; }
            p.teleport(loc.get());
            return true;
        }

        private boolean handleSet(Player p, String name) {
            boolean ok = homeManager.setHome(p, name, p.getLocation());
            if (!ok) { p.sendMessage(msg("home-limit", "max", homeManager.maxHomes())); return true; }
            p.sendMessage(msg("home-set", "name", name));
            return true;
        }

        private boolean handleDelete(Player p, String name) {
            if (name == null) { p.sendMessage("§7Usage: /home delete <name>"); return true; }
            boolean ok = homeManager.deleteHome(p, name);
            if (!ok) { p.sendMessage(msg("home-not-found", "name", name)); return true; }
            p.sendMessage(msg("home-deleted", "name", name));
            return true;
        }

        private boolean handleList(Player p) {
            List<String> names = homeManager.listHomes(p);
            if (names.isEmpty()) { p.sendMessage(msg("home-list-empty")); return true; }
            p.sendMessage(msg("home-list-header"));
            for (String name : names) {
                homeManager.getHome(p, name).ifPresent(loc -> {
                    int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
                    p.sendMessage(msg("home-list-entry",
                            "name", name, "world", loc.getWorld().getName(),
                            "x", x, "y", y, "z", z));
                });
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player p)) return List.of();
            if (args.length == 1) {
                List<String> options = new java.util.ArrayList<>(List.of("set", "delete", "list"));
                options.addAll(homeManager.listHomes(p));
                return filter(options, args[0]);
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("del"))) {
                return filter(homeManager.listHomes(p), args[1]);
            }
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // /warp
    // -------------------------------------------------------------------------

    private final class WarpCommand implements CommandExecutor, TabCompleter {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
            if (!p.hasPermission("rpg.homes.use")) { p.sendMessage(msg("no-permission")); return true; }
            if (args.length == 0) {
                listWarps(p);
                return true;
            }
            Optional<Location> loc = warpManager.getWarp(args[0]);
            if (loc.isEmpty()) { p.sendMessage(msg("warp-not-found", "name", args[0])); return true; }
            p.teleport(loc.get());
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            return args.length == 1 ? filter(warpManager.listWarps(), args[0]) : List.of();
        }
    }

    // -------------------------------------------------------------------------
    // /setwarp, /delwarp, /warps
    // -------------------------------------------------------------------------

    private final class SetWarpCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
            if (!p.hasPermission("rpg.homes.admin")) { p.sendMessage(msg("no-permission")); return true; }
            if (args.length == 0) { p.sendMessage("§7Usage: /setwarp <name>"); return true; }
            String name = args[0].toLowerCase(Locale.ROOT);
            warpManager.setWarp(name, p.getLocation());
            p.sendMessage(msg("warp-set", "name", name));
            return true;
        }
    }

    private final class DelWarpCommand implements CommandExecutor, TabCompleter {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("rpg.homes.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            if (args.length == 0) { sender.sendMessage("§7Usage: /delwarp <name>"); return true; }
            boolean ok = warpManager.deleteWarp(args[0]);
            if (!ok) sender.sendMessage(msg("warp-not-found", "name", args[0]));
            else     sender.sendMessage(msg("warp-deleted",   "name", args[0]));
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            return args.length == 1 ? filter(warpManager.listWarps(), args[0]) : List.of();
        }
    }

    private final class WarpsCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
            if (!p.hasPermission("rpg.homes.use")) { p.sendMessage(msg("no-permission")); return true; }
            listWarps(p);
            return true;
        }
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private void listWarps(Player p) {
        List<String> names = warpManager.listWarps();
        if (names.isEmpty()) { p.sendMessage(msg("warp-list-empty")); return; }
        p.sendMessage(msg("warp-list-header"));
        for (String name : names) {
            warpManager.getWarp(name).ifPresent(loc -> {
                int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
                p.sendMessage(msg("warp-list-entry",
                        "name", name, "world", loc.getWorld().getName(),
                        "x", x, "y", y, "z", z));
            });
        }
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.startsWith(lower)).toList();
    }
}
