package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.Skill;
import com.github._255_ping.rpg.api.skills.SkillsService;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SkillCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final RpgCorePlugin plugin;

    public SkillCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(plugin.messages().component("command.player-only"));
                return true;
            }
            if (!sender.hasPermission("rpg.core.skill")) {
                sender.sendMessage(plugin.messages().component("command.no-permission"));
                return true;
            }
            target = p;
        } else {
            if (!sender.hasPermission("rpg.core.skill.other")) {
                sender.sendMessage(plugin.messages().component("command.no-permission"));
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.messages().component("player.not-found", Map.of("name", args[0])));
                return true;
            }
        }

        SkillsService svc = RpgServices.skills();
        List<Skill> sorted = new ArrayList<>(RpgServices.skillRegistry().all());
        sorted.sort((a, b) -> a.id().compareTo(b.id()));

        sender.sendMessage(LEGACY.deserialize("&6&l=== Skills: &e" + target.getName() + " &6&l==="));
        for (Skill s : sorted) {
            int level = svc.level(target, s.id());
            long toNext = svc.xpToNext(target, s.id());
            long total = svc.totalXp(target, s.id());
            int max = svc.maxLevel(s.id());
            String line = "&e" + s.displayName() + " &7Lv &f" + level + "&7/&f" + max
                    + " &7(&e" + total + " &7total, &e" + toNext + " &7to next)";
            sender.sendMessage(LEGACY.deserialize(line));
        }
        return true;
    }
}
