package com.github._255_ping.rpg.parties;

import com.github._255_ping.rpg.api.parties.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class PartyCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final PartyManager manager;

    public PartyCommand(PartyManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg("&cPlayers only.")); return true;
        }
        if (args.length == 0) {
            p.sendMessage(msg("&7Usage: &e/party <create|invite|accept|kick|promote|demote|leave|disband|list>"));
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreate(p);
            case "invite" -> handleInvite(p, args);
            case "accept" -> handleAccept(p);
            case "kick" -> handleKick(p, args);
            case "promote" -> handlePromote(p, args);
            case "demote" -> handleDemote(p, args);
            case "leave" -> handleLeave(p);
            case "disband" -> handleDisband(p);
            case "list" -> handleList(p);
            default -> p.sendMessage(msg("&cUnknown subcommand: " + sub));
        }
        return true;
    }

    private void handleCreate(Player p) {
        if (!p.hasPermission("rpg.parties.create")) { p.sendMessage(msg("&cNo permission.")); return; }
        CoreParty created = manager.create(p);
        if (created == null) {
            p.sendMessage(msg("&cYou're already in a party."));
            return;
        }
        p.sendMessage(msg("&aParty created. Use &e/party invite <player> &ato add members."));
    }

    private void handleInvite(Player p, String[] args) {
        if (!p.hasPermission("rpg.parties.invite")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/party invite <player>")); return; }
        Optional<Party> opt = manager.partyOf(p);
        if (opt.isEmpty()) { p.sendMessage(msg("&cYou're not in a party.")); return; }
        CoreParty party = (CoreParty) opt.get();
        if (!party.isOwner(p) && !party.isModerator(p)) {
            p.sendMessage(msg("&cOnly the owner or moderators can invite.")); return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { p.sendMessage(msg("&cPlayer not found: &7" + args[1])); return; }
        if (!manager.invite(party, target)) {
            p.sendMessage(msg("&cCan't invite that player (full, already in a party, or already a member).")); return;
        }
        p.sendMessage(msg("&aInvited &e" + target.getName() + "&a."));
        target.sendMessage(msg("&e" + p.getName() + " &7invited you to a party. &7Use &e/party accept &7to join."));
    }

    private void handleAccept(Player p) {
        if (!p.hasPermission("rpg.parties.accept")) { p.sendMessage(msg("&cNo permission.")); return; }
        Optional<CoreParty> joined = manager.acceptInvite(p);
        if (joined.isEmpty()) { p.sendMessage(msg("&cNo valid pending invite.")); return; }
        CoreParty party = joined.get();
        Component announce = msg("&e" + p.getName() + " &7joined the party.");
        for (Player m : party.members()) m.sendMessage(announce);
    }

    private void handleKick(Player p, String[] args) {
        if (!p.hasPermission("rpg.parties.kick")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/party kick <player>")); return; }
        Optional<Party> opt = manager.partyOf(p);
        if (opt.isEmpty()) { p.sendMessage(msg("&cYou're not in a party.")); return; }
        CoreParty party = (CoreParty) opt.get();
        if (!party.isOwner(p) && !party.isModerator(p)) {
            p.sendMessage(msg("&cOnly owner or moderators can kick.")); return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !party.isMember(target)) {
            p.sendMessage(msg("&cThat player isn't in your party.")); return;
        }
        if (party.isOwner(target)) {
            p.sendMessage(msg("&cYou can't kick the owner.")); return;
        }
        manager.removeMember(party, target);
        target.sendMessage(msg("&cYou were kicked from the party."));
        for (Player m : party.members()) m.sendMessage(msg("&e" + target.getName() + " &7was kicked from the party."));
    }

    private void handlePromote(Player p, String[] args) {
        if (!p.hasPermission("rpg.parties.promote")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/party promote <player>")); return; }
        Optional<Party> opt = manager.partyOf(p);
        if (opt.isEmpty()) { p.sendMessage(msg("&cYou're not in a party.")); return; }
        CoreParty party = (CoreParty) opt.get();
        if (!party.isOwner(p)) { p.sendMessage(msg("&cOnly the owner can promote.")); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !party.isMember(target)) {
            p.sendMessage(msg("&cThat player isn't in your party.")); return;
        }
        if (manager.promote(party, target)) {
            for (Player m : party.members()) m.sendMessage(msg("&e" + target.getName() + " &7is now a moderator."));
        } else {
            p.sendMessage(msg("&7They're already a moderator (or the owner)."));
        }
    }

    private void handleDemote(Player p, String[] args) {
        if (!p.hasPermission("rpg.parties.demote")) { p.sendMessage(msg("&cNo permission.")); return; }
        if (args.length < 2) { p.sendMessage(msg("&7Usage: &e/party demote <player>")); return; }
        Optional<Party> opt = manager.partyOf(p);
        if (opt.isEmpty()) { p.sendMessage(msg("&cYou're not in a party.")); return; }
        CoreParty party = (CoreParty) opt.get();
        if (!party.isOwner(p)) { p.sendMessage(msg("&cOnly the owner can demote.")); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !party.isMember(target)) {
            p.sendMessage(msg("&cThat player isn't in your party.")); return;
        }
        if (manager.demote(party, target)) {
            for (Player m : party.members()) m.sendMessage(msg("&e" + target.getName() + " &7is no longer a moderator."));
        } else {
            p.sendMessage(msg("&7They weren't a moderator."));
        }
    }

    private void handleLeave(Player p) {
        if (!p.hasPermission("rpg.parties.leave")) { p.sendMessage(msg("&cNo permission.")); return; }
        Optional<Party> opt = manager.partyOf(p);
        if (opt.isEmpty()) { p.sendMessage(msg("&cYou're not in a party.")); return; }
        CoreParty party = (CoreParty) opt.get();
        manager.removeMember(party, p);
        p.sendMessage(msg("&7You left the party."));
        for (Player m : party.members()) m.sendMessage(msg("&e" + p.getName() + " &7left the party."));
    }

    private void handleDisband(Player p) {
        if (!p.hasPermission("rpg.parties.disband")) { p.sendMessage(msg("&cNo permission.")); return; }
        Optional<Party> opt = manager.partyOf(p);
        if (opt.isEmpty()) { p.sendMessage(msg("&cYou're not in a party.")); return; }
        CoreParty party = (CoreParty) opt.get();
        if (!party.isOwner(p)) { p.sendMessage(msg("&cOnly the owner can disband.")); return; }
        for (Player m : party.members()) m.sendMessage(msg("&cThe party was disbanded."));
        manager.disband(party);
    }

    private void handleList(Player p) {
        if (!p.hasPermission("rpg.parties.list")) { p.sendMessage(msg("&cNo permission.")); return; }
        Optional<Party> opt = manager.partyOf(p);
        if (opt.isEmpty()) { p.sendMessage(msg("&cYou're not in a party.")); return; }
        CoreParty party = (CoreParty) opt.get();
        p.sendMessage(msg("&6&l=== Party (" + party.members().size() + "/" + manager.maxSize() + ") ==="));
        for (Player m : party.members()) {
            String rank = party.isOwner(m) ? "&6Owner" : party.isModerator(m) ? "&aMod" : "&7Member";
            p.sendMessage(msg("  " + rank + " &7- &e" + m.getName()));
        }
    }

    private static Component msg(String legacy) {
        return LEGACY.deserialize(legacy);
    }
}
