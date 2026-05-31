package com.github._255_ping.rpg.hud;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public final class RpgHudPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private HudTask hudTask;
    private NametagManager nametagManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        hudTask = new HudTask(this);
        nametagManager = new NametagManager(this);
        hudTask.setNametagManager(nametagManager);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(nametagManager, this);
        getServer().getScheduler().runTaskTimer(this, hudTask, 20L, 1L);
        var cmd = Objects.requireNonNull(getCommand("hud"), "command 'hud' missing");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
        getLogger().info("rpg-hud v" + getPluginMeta().getVersion() + " enabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        hudTask.onJoin(e.getPlayer());
        nametagManager.onJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        hudTask.onQuit(e.getPlayer());
        nametagManager.onQuit(e.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usage: §e/hud <toggle|reload> [scoreboard|tablist|actionbar]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                if (!sender.hasPermission("rpg.hud.toggle")) {
                    sender.sendMessage("§cNo permission."); return true;
                }
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("§cPlayers only."); return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§7Specify §escoreboard§7, §etablist§7, or §eactionbar§7."); return true;
                }
                boolean newState = switch (args[1].toLowerCase()) {
                    case "scoreboard" -> hudTask.toggleScoreboard(p);
                    case "tablist" -> hudTask.toggleTablist(p);
                    case "actionbar", "action-bar" -> hudTask.toggleActionBar(p);
                    default -> { sender.sendMessage("§cUnknown element: " + args[1]); yield false; }
                };
                sender.sendMessage("§7" + args[1] + ": " + (newState ? "§aon" : "§coff"));
            }
            case "reload" -> {
                if (!sender.hasPermission("rpg.hud.reload")) {
                    sender.sendMessage("§cNo permission."); return true;
                }
                reloadConfig();
                sender.sendMessage("§aReloaded HUD config.");
            }
            default -> sender.sendMessage("§cUnknown subcommand: " + args[0]);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of("toggle", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            return filter(List.of("scoreboard", "tablist", "actionbar"), args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(s -> s.startsWith(lower)).toList();
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-hud disabled.");
    }
}
