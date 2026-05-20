package com.github._255_ping.rpg.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RpgChatPlugin extends JavaPlugin implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final Map<UUID, UUID> lastDmFrom = new HashMap<>();
    private final Map<UUID, String> activeChannel = new HashMap<>();
    private boolean muted;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        muted = getConfig().getBoolean("mutechat-default", false);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this), this);
        for (String c : new String[]{"msg", "reply", "clearchat", "mutechat", "chat"}) {
            Objects.requireNonNull(getCommand(c), "command '" + c + "' missing").setExecutor(this);
        }
        getLogger().info("rpg-chat v" + getPluginMeta().getVersion() + " enabled.");
    }

    public boolean isMuted() { return muted; }

    public String activeChannel(Player p) {
        return activeChannel.getOrDefault(p.getUniqueId(), "global");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "msg", "tell", "w", "whisper" -> handleMsg(sender, args);
            case "reply", "r" -> handleReply(sender, args);
            case "clearchat", "cc" -> handleClearChat(sender);
            case "mutechat", "mc" -> handleMuteChat(sender);
            case "chat" -> handleChatChannel(sender, args);
            default -> true;
        };
    }

    private boolean handleChatChannel(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.chat.admin.reload")) {
                sender.sendMessage(LEGACY.deserialize("&cNo permission.")); return true;
            }
            reloadConfig();
            sender.sendMessage(LEGACY.deserialize("&arpg-chat reloaded."));
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(LEGACY.deserialize("&cPlayers only.")); return true;
        }
        if (args.length == 0) {
            p.sendMessage(LEGACY.deserialize("&7Active channel: &e" + activeChannel(p)));
            p.sendMessage(LEGACY.deserialize("&7Usage: &e/chat <global|party|guild>"));
            return true;
        }
        String channel = args[0].toLowerCase();
        if (!java.util.Set.of("global", "party", "guild").contains(channel)) {
            p.sendMessage(LEGACY.deserialize("&cUnknown channel: " + channel)); return true;
        }
        String perm = "rpg.chat.use." + channel;
        if (!p.hasPermission(perm)) {
            p.sendMessage(LEGACY.deserialize("&cNo permission for channel: " + channel)); return true;
        }
        if (channel.equals("global")) activeChannel.remove(p.getUniqueId());
        else activeChannel.put(p.getUniqueId(), channel);
        p.sendMessage(LEGACY.deserialize("&7Channel set to &e" + channel + "&7."));
        return true;
    }

    private boolean handleMsg(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.chat.msg")) {
            sender.sendMessage(LEGACY.deserialize("&cNo permission.")); return true;
        }
        if (!(sender instanceof Player from)) {
            sender.sendMessage(LEGACY.deserialize("&cPlayers only.")); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(LEGACY.deserialize("&7Usage: &e/msg <player> <message>")); return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(LEGACY.deserialize("&cPlayer not found: &7" + args[0])); return true;
        }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        sendDm(from, target, message);
        lastDmFrom.put(target.getUniqueId(), from.getUniqueId());
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.chat.reply")) {
            sender.sendMessage(LEGACY.deserialize("&cNo permission.")); return true;
        }
        if (!(sender instanceof Player from)) {
            sender.sendMessage(LEGACY.deserialize("&cPlayers only.")); return true;
        }
        UUID targetId = lastDmFrom.get(from.getUniqueId());
        if (targetId == null) {
            sender.sendMessage(LEGACY.deserialize("&cNo one to reply to.")); return true;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            sender.sendMessage(LEGACY.deserialize("&cThat player is offline.")); return true;
        }
        if (args.length == 0) {
            sender.sendMessage(LEGACY.deserialize("&7Usage: &e/reply <message>")); return true;
        }
        String message = String.join(" ", args);
        sendDm(from, target, message);
        lastDmFrom.put(target.getUniqueId(), from.getUniqueId());
        return true;
    }

    private boolean handleClearChat(CommandSender sender) {
        if (!sender.hasPermission("rpg.chat.clearchat")) {
            sender.sendMessage(LEGACY.deserialize("&cNo permission.")); return true;
        }
        int lines = getConfig().getInt("clearchat-lines", 100);
        StringBuilder blank = new StringBuilder();
        for (int i = 0; i < lines; i++) blank.append('\n');
        Component blanks = LEGACY.deserialize(blank.toString());
        Component header = LEGACY.deserialize("&8[&cClearChat&8] &7Chat cleared by &e"
                + (sender instanceof Player p ? p.getName() : "console") + "&7.");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(blanks);
            p.sendMessage(header);
        }
        return true;
    }

    private boolean handleMuteChat(CommandSender sender) {
        if (!sender.hasPermission("rpg.chat.mutechat")) {
            sender.sendMessage(LEGACY.deserialize("&cNo permission.")); return true;
        }
        muted = !muted;
        Component msg = LEGACY.deserialize("&8[&cMuteChat&8] &7Chat is now "
                + (muted ? "&cmuted" : "&aunmuted") + "&7.");
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(msg);
        return true;
    }

    private void sendDm(Player from, Player to, String message) {
        String template = getConfig().getString("message-format",
                "&d{sender} &7-> &d{target}&7: &f{message}");
        String rendered = template
                .replace("{sender}", from.getName())
                .replace("{target}", to.getName())
                .replace("{message}", message);
        Component component = LEGACY.deserialize(rendered);
        from.sendMessage(component);
        to.sendMessage(component);
    }
}
