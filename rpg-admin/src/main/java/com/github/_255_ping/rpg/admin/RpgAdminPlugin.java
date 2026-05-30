package com.github._255_ping.rpg.admin;

import com.github._255_ping.rpg.admin.command.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RpgAdminPlugin extends JavaPlugin {

    /** Players currently in god mode — shared with GodCommand's Listener. */
    private final Set<UUID> godPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        GamemodeCommand gm = new GamemodeCommand(this);
        register("gmc", gm);
        register("gms", gm);
        register("gma", gm);
        register("gmsp", gm);

        FlyCommand fly = new FlyCommand(this);
        register("fly", fly);

        GodCommand god = new GodCommand(this, godPlayers);
        register("god", god);
        getServer().getPluginManager().registerEvents(god, this);

        TeleportCommand tp = new TeleportCommand(this);
        register("tp", tp);
        register("tphere", tp);

        HealFeedCommand hf = new HealFeedCommand(this);
        register("heal", hf);
        register("feed", hf);

        SpeedClearCommand sc = new SpeedClearCommand(this);
        register("speed", sc);
        register("clear", sc);

        BroadcastSudoCommand bs = new BroadcastSudoCommand(this);
        register("broadcast", bs);
        register("sudo", bs);

        getLogger().info("rpg-admin v" + getPluginMeta().getVersion() + " enabled.");
    }

    /** Registers a command's executor and tab completer if the command is enabled in config. */
    private void register(String name, Object handler) {
        if (!getConfig().getBoolean("commands." + name + ".enabled", true)) return;
        PluginCommand cmd = getCommand(name);
        if (cmd == null) return;
        if (handler instanceof CommandExecutor ce) cmd.setExecutor(ce);
        if (handler instanceof TabCompleter tc) cmd.setTabCompleter(tc);
    }
}
