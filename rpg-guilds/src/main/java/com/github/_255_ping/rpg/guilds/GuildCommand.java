package com.github._255_ping.rpg.guilds;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.guilds.Guild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class GuildCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final GuildManager manager;

    public GuildCommand(JavaPlugin plugin, GuildManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.guilds.admin.reload")) {
                sender.sendMessage(msg("&cNo permission.")); return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(msg("&arpg-guilds reloaded."));
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg("&cPlayers only.")); return true;
        }
        if (args.length == 0) {
            p.sendMessage(msg("&7Usage: &e/guild <create|invite|accept|kick|promote|demote|leave|disband|info|list|deposit|withdraw>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(p, args);
            case "invite" -> handleInvite(p, args);
            case "accept" -> handleAccept(p);
            case "kick" -> handleKick(p, args);
            case "promote" -> handlePromote(p, args);
            case "demote" -> handleDemote(p, args);
            case "leave" -> handleLeave(p);
            case "disband" -> handleDisband(p);
            case "info" -> handleInfo(p, args);
            case "list" -> handleList(p);
            case "deposit" -> handleDeposit(p, args);
            case "withdraw" -> handleWithdraw(p, args);
            default -> p.sendMessage(msg("&cUnknown subcommand: " + args[0]));
        }
        return true;
    }

    private void handleCreate(Player p, String[] args) {
        if (!p.hasPermission("rpg.guilds.create")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/guild create <name>")); return; }
        String name = args[1];
        long cost = plugin.getConfig().getLong("creation.cost", 0);
        if (cost > 0) {
            try {
                BigDecimal balance = RpgServices.economy().balance(p);
                if (balance.compareTo(BigDecimal.valueOf(cost)) < 0) {
                    p.sendMessage(msg("&cYou need &e" + cost + " &ccoins to create a guild."));
                    return;
                }
                if (!RpgServices.economy().withdraw(p, BigDecimal.valueOf(cost))) {
                    p.sendMessage(msg("&cYou can't afford that.")); return;
                }
            } catch (IllegalStateException ignored) {
                // rpg-economy not loaded — creation is free
            }
        }
        CoreGuild created = manager.create(p, name);
        if (created == null) {
            p.sendMessage(msg("&cCouldn't create guild — name taken or you're already in one."));
            return;
        }
        p.sendMessage(msg("&aGuild &e" + name + " &acreated."));
    }

    private void handleInvite(Player p, String[] args) {
        if (!p.hasPermission("rpg.guilds.invite")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/guild invite <player>")); return; }
        CoreGuild guild = mustHaveGuild(p);
        if (guild == null) return;
        if (!rankAllows(guild, p, "invite")) {
            p.sendMessage(msg("&cYou need officer or owner rank to invite.")); return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { p.sendMessage(msg("&cPlayer not found.")); return; }
        if (!manager.invite(guild, target)) {
            p.sendMessage(msg("&cCan't invite — already in a guild, full, or already a member.")); return;
        }
        p.sendMessage(msg("&aInvited &e" + target.getName() + "&a."));
        target.sendMessage(msg("&e" + p.getName() + " &7invited you to &e" + guild.name() + "&7. &e/guild accept&7 to join."));
    }

    private void handleAccept(Player p) {
        if (!p.hasPermission("rpg.guilds.accept")) { p.sendMessage(msg("&cNo permission.")); return; }
        Optional<CoreGuild> joined = manager.acceptInvite(p);
        if (joined.isEmpty()) { p.sendMessage(msg("&cNo valid pending invite.")); return; }
        Guild guild = joined.get();
        broadcast(joined.get(), msg("&e" + p.getName() + " &7joined &e" + guild.name() + "&7."));
    }

    private void handleKick(Player p, String[] args) {
        if (!p.hasPermission("rpg.guilds.kick")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/guild kick <player>")); return; }
        CoreGuild guild = mustHaveGuild(p);
        if (guild == null) return;
        if (!rankAllows(guild, p, "kick")) { p.sendMessage(msg("&cInsufficient rank.")); return; }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null || !guild.rawRanks().containsKey(target.getUniqueId())) {
            p.sendMessage(msg("&cThat player isn't in your guild.")); return;
        }
        if (target.getUniqueId().equals(guild.ownerId())) {
            p.sendMessage(msg("&cYou can't kick the owner.")); return;
        }
        manager.removeMember(guild, target.getUniqueId());
        broadcast(guild, msg("&e" + target.getName() + " &7was kicked from the guild."));
    }

    private void handlePromote(Player p, String[] args) {
        if (!p.hasPermission("rpg.guilds.promote")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/guild promote <player>")); return; }
        CoreGuild guild = mustHaveGuild(p);
        if (guild == null) return;
        if (!guild.isOwner(p)) { p.sendMessage(msg("&cOnly the owner can promote.")); return; }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null || !guild.rawRanks().containsKey(target.getUniqueId())) {
            p.sendMessage(msg("&cThat player isn't in your guild.")); return;
        }
        if (manager.promote(guild, target.getUniqueId())) {
            broadcast(guild, msg("&e" + target.getName() + " &7is now an Officer."));
        } else {
            p.sendMessage(msg("&7They're already an officer / owner."));
        }
    }

    private void handleDemote(Player p, String[] args) {
        if (!p.hasPermission("rpg.guilds.demote")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/guild demote <player>")); return; }
        CoreGuild guild = mustHaveGuild(p);
        if (guild == null) return;
        if (!guild.isOwner(p)) { p.sendMessage(msg("&cOnly the owner can demote.")); return; }
        OfflinePlayer target = resolveOffline(args[1]);
        if (target == null || !guild.rawRanks().containsKey(target.getUniqueId())) {
            p.sendMessage(msg("&cThat player isn't in your guild.")); return;
        }
        if (manager.demote(guild, target.getUniqueId())) {
            broadcast(guild, msg("&e" + target.getName() + " &7is no longer an Officer."));
        } else {
            p.sendMessage(msg("&7They weren't an officer."));
        }
    }

    private void handleLeave(Player p) {
        if (!p.hasPermission("rpg.guilds.leave")) { p.sendMessage(msg("&cNo permission.")); return; }
        CoreGuild guild = mustHaveGuild(p);
        if (guild == null) return;
        manager.removeMember(guild, p.getUniqueId());
        p.sendMessage(msg("&7You left the guild."));
        broadcast(guild, msg("&e" + p.getName() + " &7left the guild."));
    }

    private void handleDisband(Player p) {
        if (!p.hasPermission("rpg.guilds.disband")) { p.sendMessage(msg("&cNo permission.")); return; }
        CoreGuild guild = mustHaveGuild(p);
        if (guild == null) return;
        if (!guild.isOwner(p)) { p.sendMessage(msg("&cOnly the owner can disband.")); return; }
        broadcast(guild, msg("&cThe guild was disbanded."));
        manager.disband(guild);
    }

    private void handleInfo(Player p, String[] args) {
        if (!p.hasPermission("rpg.guilds.info")) { p.sendMessage(msg("&cNo permission.")); return; }
        Guild guild;
        if (args.length >= 2) {
            Optional<Guild> opt = manager.getByName(args[1]);
            if (opt.isEmpty()) { p.sendMessage(msg("&cNo guild with name &7" + args[1])); return; }
            guild = opt.get();
        } else {
            guild = mustHaveGuild(p);
            if (guild == null) return;
        }
        p.sendMessage(msg("&6&l=== &e" + guild.name() + " &6&l==="));
        p.sendMessage(msg("&7Owner: &e" + Bukkit.getOfflinePlayer(guild.ownerId()).getName()));
        p.sendMessage(msg("&7Members: &e" + guild.memberIds().size() + "&7/&e" + manager.maxMembers()));
        p.sendMessage(msg("&7Bank: &e" + guild.bankBalance().toPlainString() + " coins"));
        p.sendMessage(msg("&7Total XP: &e" + guild.totalXp()));
    }

    private void handleList(Player p) {
        if (!p.hasPermission("rpg.guilds.list")) { p.sendMessage(msg("&cNo permission.")); return; }
        var all = manager.all();
        p.sendMessage(msg("&6&l=== Guilds (" + all.size() + ") ==="));
        for (Guild g : all) {
            p.sendMessage(msg("&7- &e" + g.name() + " &7(" + g.memberIds().size() + " members)"));
        }
    }

    private void handleDeposit(Player p, String[] args) {
        if (!p.hasPermission("rpg.guilds.bank")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/guild deposit <amount>")); return; }
        CoreGuild guild = mustHaveGuild(p);
        if (guild == null) return;
        BigDecimal amount = parseAmount(p, args[1]);
        if (amount == null) return;
        try {
            if (!RpgServices.economy().withdraw(p, amount)) {
                p.sendMessage(msg("&cYou can't afford that.")); return;
            }
        } catch (IllegalStateException ignored) {
            p.sendMessage(msg("&cEconomy not loaded.")); return;
        }
        manager.deposit(guild, amount);
        broadcast(guild, msg("&e" + p.getName() + " &7deposited &e" + amount + " &7into the bank."));
    }

    private void handleWithdraw(Player p, String[] args) {
        if (!p.hasPermission("rpg.guilds.bank")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/guild withdraw <amount>")); return; }
        CoreGuild guild = mustHaveGuild(p);
        if (guild == null) return;
        if (!rankAllows(guild, p, "withdraw")) {
            p.sendMessage(msg("&cYou need officer or owner rank to withdraw.")); return;
        }
        BigDecimal amount = parseAmount(p, args[1]);
        if (amount == null) return;
        if (!manager.withdraw(guild, amount)) {
            p.sendMessage(msg("&cThe bank doesn't have that much.")); return;
        }
        try {
            RpgServices.economy().deposit(p, amount);
        } catch (IllegalStateException ignored) {}
        broadcast(guild, msg("&e" + p.getName() + " &7withdrew &e" + amount + " &7from the bank."));
    }

    // ---- Helpers ----

    private CoreGuild mustHaveGuild(Player p) {
        Optional<Guild> opt = manager.guildOf(p);
        if (opt.isEmpty()) { p.sendMessage(msg("&cYou're not in a guild.")); return null; }
        return (CoreGuild) opt.get();
    }

    private boolean rankAllows(CoreGuild guild, Player p, String action) {
        String rank = guild.rankOf(p.getUniqueId());
        if (CoreGuild.RANK_OWNER.equals(rank)) return true;
        if (CoreGuild.RANK_OFFICER.equals(rank)) {
            return switch (action) {
                case "invite", "kick", "withdraw" -> true;
                default -> false;
            };
        }
        return false;
    }

    private BigDecimal parseAmount(Player p, String raw) {
        try {
            BigDecimal amount = new BigDecimal(raw);
            if (amount.signum() <= 0) {
                p.sendMessage(msg("&cAmount must be positive.")); return null;
            }
            return amount;
        } catch (NumberFormatException ex) {
            p.sendMessage(msg("&cBad amount.")); return null;
        }
    }

    private OfflinePlayer resolveOffline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) return op;
        return null;
    }

    private void broadcast(Guild guild, Component component) {
        for (UUID id : guild.memberIds()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(component);
        }
    }

    private static Component msg(String legacy) { return LEGACY.deserialize(legacy); }
}
