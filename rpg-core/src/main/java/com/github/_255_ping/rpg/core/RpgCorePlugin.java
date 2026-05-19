package com.github._255_ping.rpg.core;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.core.command.RpgCommand;
import com.github._255_ping.rpg.core.cooldown.CoreCooldownService;
import com.github._255_ping.rpg.core.formatting.CoreMessageFormatter;
import com.github._255_ping.rpg.core.formatting.CoreNameFormatter;
import com.github._255_ping.rpg.core.formula.CoreExpressionEvaluator;
import com.github._255_ping.rpg.core.persistence.YamlDataStore;
import com.github._255_ping.rpg.core.scheduler.CoreSchedulerService;
import com.github._255_ping.rpg.core.suppression.VanillaSuppressionListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgCorePlugin extends JavaPlugin {

    private static RpgCorePlugin instance;

    private CoreMessageFormatter messageFormatter;
    private CoreNameFormatter nameFormatter;
    private YamlDataStore dataStore;
    private CoreSchedulerService scheduler;
    private CoreCooldownService cooldowns;
    private CoreExpressionEvaluator expressions;

    public static RpgCorePlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);

        File dataDir = new File(getDataFolder(), "data");

        dataStore = new YamlDataStore(dataDir);
        messageFormatter = new CoreMessageFormatter(messagesFile);
        nameFormatter = new CoreNameFormatter();
        scheduler = new CoreSchedulerService();
        cooldowns = new CoreCooldownService();
        expressions = new CoreExpressionEvaluator();

        RpgServices.setDataStore(dataStore);
        RpgServices.setMessageFormatter(messageFormatter);
        RpgServices.setNameFormatter(nameFormatter);
        RpgServices.setScheduler(scheduler);
        RpgServices.setCooldowns(cooldowns);
        RpgServices.setExpressions(expressions);

        getServer().getPluginManager().registerEvents(new VanillaSuppressionListener(this), this);

        PluginCommand rpg = Objects.requireNonNull(getCommand("rpg"), "command 'rpg' missing from plugin.yml");
        RpgCommand handler = new RpgCommand(this);
        rpg.setExecutor(handler);
        rpg.setTabCompleter(handler);

        getLogger().info("rpg-core v" + getPluginMeta().getVersion() + " enabled.");
        getLogger().info(messageFormatter.format("debug.ready"));
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-core disabled.");
        instance = null;
    }

    public void reloadAll() {
        reloadConfig();
        messageFormatter.reload();
    }

    public CoreMessageFormatter messages() {
        return messageFormatter;
    }
}
