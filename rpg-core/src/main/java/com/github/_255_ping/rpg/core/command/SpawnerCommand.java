package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import com.github._255_ping.rpg.core.spawners.SpawnerDef;
import com.github._255_ping.rpg.core.spawners.SpawnerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

public final class SpawnerCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final RpgCorePlugin plugin;
    private final SpawnerManager manager;

    public SpawnerCommand(RpgCorePlugin plugin, SpawnerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(msg("&7Usage: &e/spawner <create|delete|list|tp|set>"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "tp" -> handleTp(sender, args);
            case "set" -> handleSet(sender, args);
            default -> sender.sendMessage(msg("&cUnknown subcommand: " + args[0]));
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.spawners.admin.create")) {
            sender.sendMessage(msg("&cNo permission.")); return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.messages().component("command.player-only")); return;
        }
        if (args.length < 3) {
            sender.sendMessage(msg("&7Usage: &e/spawner create <id> <mobId>")); return;
        }
        String id = args[1];
        if (manager.get(id).isPresent()) {
            sender.sendMessage(msg("&cSpawner already exists: &7" + id)); return;
        }
        String mobId = args[2];
        if (RpgServices.mobs().get(mobId).isEmpty()) {
            sender.sendMessage(msg("&cNo mob with id &7" + mobId)); return;
        }
        SpawnerDef def = new SpawnerDef(id, mobId, p.getLocation());
        manager.put(def);
        sender.sendMessage(msg("&aCreated spawner &e" + id + " &afor mob &7" + mobId + "&a."));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.spawners.admin.delete")) {
            sender.sendMessage(msg("&cNo permission.")); return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&7Usage: &e/spawner delete <id>")); return;
        }
        if (manager.remove(args[1])) sender.sendMessage(msg("&aDeleted spawner &e" + args[1]));
        else sender.sendMessage(msg("&cNo spawner with id &7" + args[1]));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("rpg.spawners.admin.list")) {
            sender.sendMessage(msg("&cNo permission.")); return;
        }
        var all = manager.all();
        sender.sendMessage(msg("&6&l=== Spawners (" + all.size() + ") ==="));
        for (SpawnerDef def : all) {
            sender.sendMessage(msg("&7- &e" + def.id() + " &7→ &f" + def.mobId()
                    + " &7@ &f" + def.worldName() + " " + def.x() + "," + def.y() + "," + def.z()
                    + " &7max " + def.maxAlive() + " every " + def.cooldownTicks() + "t"));
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.spawners.admin.tp")) {
            sender.sendMessage(msg("&cNo permission.")); return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.messages().component("command.player-only")); return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&7Usage: &e/spawner tp <id>")); return;
        }
        Optional<SpawnerDef> opt = manager.get(args[1]);
        if (opt.isEmpty()) { sender.sendMessage(msg("&cNo spawner with id &7" + args[1])); return; }
        SpawnerDef def = opt.get();
        var world = Bukkit.getWorld(def.worldName());
        if (world == null) { sender.sendMessage(msg("&cWorld not loaded: &7" + def.worldName())); return; }
        p.teleport(new Location(world, def.x() + 0.5, def.y(), def.z() + 0.5));
        sender.sendMessage(msg("&aTeleported to &e" + def.id()));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpg.spawners.admin.edit")) {
            sender.sendMessage(msg("&cNo permission.")); return;
        }
        if (args.length < 4) {
            sender.sendMessage(msg("&7Usage: &e/spawner set <id> <field> <value>")); return;
        }
        Optional<SpawnerDef> opt = manager.get(args[1]);
        if (opt.isEmpty()) { sender.sendMessage(msg("&cNo spawner with id &7" + args[1])); return; }
        SpawnerDef def = opt.get();
        String field = args[2].toLowerCase(Locale.ROOT);
        String value = args[3];
        try {
            switch (field) {
                case "max-alive", "max" -> def.setMaxAlive(Integer.parseInt(value));
                case "cooldown", "cooldown-ticks" -> def.setCooldownTicks(Integer.parseInt(value));
                case "spawn-radius", "radius" -> def.setSpawnRadius(Integer.parseInt(value));
                case "continuous" -> def.setContinuous(Boolean.parseBoolean(value));
                default -> { sender.sendMessage(msg("&cUnknown field: &7" + field)); return; }
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(msg("&cBad value: &7" + value)); return;
        }
        manager.saveOne(def.id());
        sender.sendMessage(msg("&aSet &e" + def.id() + "." + field + " &7= &f" + value));
    }

    private static Component msg(String legacy) { return LEGACY.deserialize(legacy); }
}
