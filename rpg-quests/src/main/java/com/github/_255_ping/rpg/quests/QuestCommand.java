package com.github._255_ping.rpg.quests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class QuestCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final RpgQuestsPlugin plugin;

    public QuestCommand(RpgQuestsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/quest <list|accept|abandon|progress|complete|reload>")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> handleList(sender);
            case "accept" -> handleAccept(sender, args);
            case "abandon" -> handleAbandon(sender, args);
            case "progress" -> handleProgress(sender);
            case "complete" -> handleComplete(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(Component.text("Unknown: " + args[0]).color(NamedTextColor.YELLOW));
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("rpg.quests.use.list")) return;
        sender.sendMessage(Component.text("Quests:").color(NamedTextColor.AQUA));
        for (QuestDef def : plugin.registry().all()) {
            sender.sendMessage(LEGACY.deserialize("&7 - " + def.id() + " &8(" + def.displayName() + ")"));
        }
    }

    private void handleAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        if (!p.hasPermission("rpg.quests.use.accept")) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("/quest accept <id>").color(NamedTextColor.YELLOW));
            return;
        }
        plugin.manager().accept(p, args[1]);
    }

    private void handleAbandon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return;
        if (!p.hasPermission("rpg.quests.use.abandon")) return;
        if (args.length < 2) {
            sender.sendMessage(Component.text("/quest abandon <id>").color(NamedTextColor.YELLOW));
            return;
        }
        plugin.manager().abandon(p, args[1]);
    }

    private void handleProgress(CommandSender sender) {
        if (!(sender instanceof Player p)) return;
        if (!p.hasPermission("rpg.quests.use.progress")) return;
        PlayerQuestState s = plugin.manager().load(p.getUniqueId());
        if (s.active.isEmpty()) {
            sender.sendMessage(Component.text("No active quests.").color(NamedTextColor.GRAY));
            return;
        }
        for (PlayerQuestState.Active a : s.active) {
            Optional<QuestDef> def = plugin.registry().get(a.questId);
            if (def.isEmpty()) continue;
            sender.sendMessage(LEGACY.deserialize("&6" + def.get().displayName()));
            for (int i = 0; i < def.get().objectives().size(); i++) {
                QuestObjective o = def.get().objectives().get(i);
                int prog = i < a.progress.length ? a.progress[i] : 0;
                sender.sendMessage(LEGACY.deserialize("&7 - " + o.describe() + " &8(" + prog + "/" + o.count() + ")"));
            }
        }
    }

    private void handleComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.quests.admin.complete")) return;
        if (args.length < 3) {
            sender.sendMessage(Component.text("/quest complete <player> <questId>").color(NamedTextColor.YELLOW));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player offline.").color(NamedTextColor.RED));
            return;
        }
        plugin.manager().completeByCommand(target, args[2]);
        sender.sendMessage(Component.text("Marked complete.").color(NamedTextColor.GREEN));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpg.quests.admin.reload")) return;
        plugin.reloadAll();
        sender.sendMessage(Component.text("rpg-quests reloaded.").color(NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("list", "accept", "abandon", "progress", "complete", "reload");
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.equals("accept") || sub.equals("abandon"))
                return plugin.registry().all().stream().map(QuestDef::id).toList();
            if (sub.equals("complete"))
                return org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .map(org.bukkit.entity.Player::getName).toList();
        }
        if (args.length == 3 && sub.equals("complete"))
            return plugin.registry().all().stream().map(QuestDef::id)
                    .filter(id -> id.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        return List.of();
    }
}
