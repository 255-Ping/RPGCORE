package com.github._255_ping.rpg.economy;

import com.github._255_ping.rpg.api.RpgServices;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EconomyCommands implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final CoreEconomy economy;
    private final YamlConfiguration messages;

    public EconomyCommands(JavaPlugin plugin, CoreEconomy economy) {
        this.plugin = plugin;
        this.economy = economy;
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "balance" -> handleBalance(sender, args);
            case "pay" -> handlePay(sender, args);
            case "eco" -> handleEcoAdmin(sender, args);
            case "baltop" -> handleBaltop(sender, args);
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg("command.player-only"));
                return;
            }
            if (!sender.hasPermission("rpg.economy.balance")) {
                sender.sendMessage(msg("command.no-permission")); return;
            }
            sender.sendMessage(msg("balance.self", Map.of("amount", economy.currency().format(economy.balance(p)))));
            return;
        }
        if (!sender.hasPermission("rpg.economy.balance.other")) {
            sender.sendMessage(msg("command.no-permission")); return;
        }
        OfflinePlayer target = resolveOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(msg("command.player-not-found", Map.of("name", args[0])));
            return;
        }
        sender.sendMessage(msg("balance.other",
                Map.of("player", fmt(target),
                        "amount", economy.currency().format(economy.balance(target)))));
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.economy.pay")) {
            sender.sendMessage(msg("command.no-permission")); return;
        }
        if (!(sender instanceof Player payer)) {
            sender.sendMessage(msg("command.player-only")); return;
        }
        if (args.length < 2) return;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(msg("command.player-not-found", Map.of("name", args[0])));
            return;
        }
        if (target.getUniqueId().equals(payer.getUniqueId())
                && !plugin.getConfig().getBoolean("pay.allow-self", false)) {
            sender.sendMessage(msg("pay.cannot-self")); return;
        }
        BigDecimal amount;
        try { amount = new BigDecimal(args[1]); } catch (NumberFormatException ex) {
            sender.sendMessage(msg("command.bad-amount")); return;
        }
        long minAmount = plugin.getConfig().getLong("pay.min-amount", 1);
        if (amount.compareTo(BigDecimal.valueOf(minAmount)) < 0) {
            sender.sendMessage(msg("pay.amount-too-small", Map.of("min", minAmount))); return;
        }
        long cooldownSec = plugin.getConfig().getLong("pay.cooldown-seconds", 0);
        if (cooldownSec > 0
                && RpgServices.cooldowns().isOnCooldown(payer.getUniqueId(), "economy.pay")) {
            sender.sendMessage(msg("pay.cooldown")); return;
        }

        if (!economy.transfer(payer, target, amount)) {
            sender.sendMessage(msg("pay.insufficient")); return;
        }
        if (cooldownSec > 0) {
            RpgServices.cooldowns().set(payer.getUniqueId(), "economy.pay", cooldownSec * 20L);
        }
        String formatted = economy.currency().format(amount);
        sender.sendMessage(msg("pay.sent", Map.of("amount", formatted, "player", fmt(target))));
        target.sendMessage(msg("pay.received", Map.of("amount", formatted, "player", fmt(payer))));
    }

    private void handleEcoAdmin(CommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.economy.admin.reload")) { sender.sendMessage(msg("command.no-permission")); return; }
            plugin.reloadConfig();
            sender.sendMessage(msg("eco.reloaded", Map.of()));
            return;
        }
        if (args.length < 2) return;
        String sub = args[0].toLowerCase();
        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(msg("command.player-not-found", Map.of("name", args[1])));
            return;
        }
        switch (sub) {
            case "set" -> {
                if (!sender.hasPermission("rpg.economy.admin.set")) { sender.sendMessage(msg("command.no-permission")); return; }
                BigDecimal amount = parseAmountOrReject(sender, args, 2); if (amount == null) return;
                economy.set(target, amount);
                sender.sendMessage(msg("eco.set", Map.of("player", fmt(target), "amount", economy.currency().format(amount))));
            }
            case "add" -> {
                if (!sender.hasPermission("rpg.economy.admin.add")) { sender.sendMessage(msg("command.no-permission")); return; }
                BigDecimal amount = parseAmountOrReject(sender, args, 2); if (amount == null) return;
                economy.deposit(target, amount);
                sender.sendMessage(msg("eco.added", Map.of("player", fmt(target), "amount", economy.currency().format(amount))));
            }
            case "remove" -> {
                if (!sender.hasPermission("rpg.economy.admin.remove")) { sender.sendMessage(msg("command.no-permission")); return; }
                BigDecimal amount = parseAmountOrReject(sender, args, 2); if (amount == null) return;
                economy.withdraw(target, amount);
                sender.sendMessage(msg("eco.removed", Map.of("player", fmt(target), "amount", economy.currency().format(amount))));
            }
            case "reset" -> {
                if (!sender.hasPermission("rpg.economy.admin.reset")) { sender.sendMessage(msg("command.no-permission")); return; }
                BigDecimal start = BigDecimal.valueOf(plugin.getConfig().getLong("currency.starting-balance", 100));
                economy.set(target, start);
                sender.sendMessage(msg("eco.reset", Map.of("player", fmt(target), "amount", economy.currency().format(start))));
            }
        }
    }

    private void handleBaltop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.economy.baltop")) { sender.sendMessage(msg("command.no-permission")); return; }
        int pageSize = plugin.getConfig().getInt("baltop.page-size", 10);
        int page = 1;
        if (args.length >= 1) {
            try { page = Math.max(1, Integer.parseInt(args[0])); } catch (NumberFormatException ignored) {}
        }
        List<Map.Entry<UUID, BigDecimal>> sorted = new ArrayList<>(economy.snapshot().entrySet());
        sorted.sort(Comparator.<Map.Entry<UUID, BigDecimal>, BigDecimal>comparing(Map.Entry::getValue).reversed());
        if (sorted.isEmpty()) { sender.sendMessage(msg("baltop.empty")); return; }
        int pages = Math.max(1, (sorted.size() + pageSize - 1) / pageSize);
        page = Math.min(page, pages);

        sender.sendMessage(msg("baltop.header", Map.of("page", page, "pages", pages)));
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, sorted.size());
        for (int i = start; i < end; i++) {
            Map.Entry<UUID, BigDecimal> e = sorted.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            sender.sendMessage(msg("baltop.entry",
                    Map.of("rank", i + 1,
                            "player", String.valueOf(op.getName()),
                            "amount", economy.currency().format(e.getValue()))));
        }
    }

    private BigDecimal parseAmountOrReject(CommandSender sender, String[] args, int idx) {
        if (args.length <= idx) { sender.sendMessage(msg("command.bad-amount")); return null; }
        try { return new BigDecimal(args[idx]); } catch (NumberFormatException ex) {
            sender.sendMessage(msg("command.bad-amount")); return null;
        }
    }

    private OfflinePlayer resolveOfflinePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) return op;
        return null;
    }

    private Component msg(String key) { return msg(key, Map.of()); }

    private Component msg(String key, Map<String, Object> placeholders) {
        String raw = messages.getString(key, "[missing:" + key + "]");
        for (Map.Entry<String, Object> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return LEGACY.deserialize(raw);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase();
        if (name.equals("eco")) {
            if (args.length == 1) return filter(args[0], List.of("set", "add", "remove", "reset", "reload"));
            if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) return filterPlayers(args[1]);
        }
        if (name.equals("balance") || name.equals("baltop")) {
            if (args.length == 1) return filterPlayers(args[0]);
        }
        if (name.equals("pay")) {
            if (args.length == 1) return filterPlayers(args[0]);
        }
        return List.of();
    }

    private static List<String> filter(String prefix, List<String> options) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String o : options) { if (o.startsWith(lower)) out.add(o); }
        return out;
    }

    private static List<String> filterPlayers(String prefix) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(lower)) out.add(p.getName());
        }
        return out;
    }

    private static String fmt(OfflinePlayer p) {
        try { return RpgServices.nameFormatter().format(p); }
        catch (IllegalStateException ex) { return p.getName() == null ? "unknown" : p.getName(); }
    }
}
