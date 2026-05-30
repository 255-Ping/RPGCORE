package com.github._255_ping.rpg.admin.command;

import com.github._255_ping.rpg.admin.RpgAdminPlugin;
import com.github._255_ping.rpg.api.damage.PreDamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class GodCommand implements CommandExecutor, TabCompleter, Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final RpgAdminPlugin plugin;
    private final Set<UUID> godPlayers;

    public GodCommand(RpgAdminPlugin plugin, Set<UUID> godPlayers) {
        this.plugin = plugin;
        this.godPlayers = godPlayers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String perm = plugin.getConfig().getString("commands.god.permission", "rpg.admin.god");
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(msg("&cNo permission.")); return true;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(msg("&cConsole must specify a player.")); return true;
        }

        boolean enabled;
        if (godPlayers.contains(target.getUniqueId())) {
            godPlayers.remove(target.getUniqueId());
            enabled = false;
        } else {
            godPlayers.add(target.getUniqueId());
            enabled = true;
        }

        String state = enabled ? "&aenabled" : "&cdisabled";
        target.sendMessage(msg("&7God mode " + state + "&7."));
        if (!target.equals(sender)) {
            sender.sendMessage(msg("&7God mode " + state + " &7for &e" + target.getName() + "&7."));
        }
        return true;
    }

    /** Cancels incoming damage for players in god mode via the RPG damage pipeline. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreDamage(PreDamageEvent event) {
        if (event.context().victim() instanceof Player p && godPlayers.contains(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static Component msg(String s) { return LEGACY.deserialize(s); }
}
