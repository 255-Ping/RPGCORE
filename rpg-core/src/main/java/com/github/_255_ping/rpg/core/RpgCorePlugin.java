package com.github._255_ping.rpg.core;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.core.abilities.AbilityLoader;
import com.github._255_ping.rpg.core.abilities.CoreAbilityRegistry;
import com.github._255_ping.rpg.core.abilities.ItemAbilityListener;
import com.github._255_ping.rpg.core.blocks.BlockBreakHandler;
import com.github._255_ping.rpg.core.blocks.BlockLoader;
import com.github._255_ping.rpg.core.blocks.CoreBlockRegistry;
import com.github._255_ping.rpg.core.command.RpgCommand;
import com.github._255_ping.rpg.core.cooldown.CoreCooldownService;
import com.github._255_ping.rpg.core.damage.DamagePipelineListener;
import com.github._255_ping.rpg.core.formatting.CoreMessageFormatter;
import com.github._255_ping.rpg.core.formatting.CoreNameFormatter;
import com.github._255_ping.rpg.core.formula.CoreExpressionEvaluator;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import com.github._255_ping.rpg.core.health.RegenTask;
import com.github._255_ping.rpg.core.items.CoreItemRegistry;
import com.github._255_ping.rpg.core.items.ItemLoader;
import com.github._255_ping.rpg.core.mobs.CoreMobRegistry;
import com.github._255_ping.rpg.core.mobs.MobLoader;
import com.github._255_ping.rpg.core.persistence.YamlDataStore;
import com.github._255_ping.rpg.core.player.CoreManaService;
import com.github._255_ping.rpg.core.player.CorePlayerLookup;
import com.github._255_ping.rpg.core.player.EquipmentListener;
import com.github._255_ping.rpg.core.player.PlayerLifecycleListener;
import com.github._255_ping.rpg.core.scheduler.CoreSchedulerService;
import com.github._255_ping.rpg.core.skills.CoreSkillRegistry;
import com.github._255_ping.rpg.core.skills.CoreSkillsService;
import com.github._255_ping.rpg.core.stats.CoreStatRegistry;
import com.github._255_ping.rpg.core.status.CoreStatusEffectRegistry;
import com.github._255_ping.rpg.core.status.CoreStatusEffectService;
import com.github._255_ping.rpg.core.status.StatusEffectLoader;
import com.github._255_ping.rpg.core.status.StatusEffectTickTask;
import com.github._255_ping.rpg.core.suppression.VanillaSuppressionListener;
import org.bukkit.NamespacedKey;
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
    private CoreStatRegistry statRegistry;
    private CorePlayerLookup playerLookup;
    private CoreManaService manaService;
    private CoreHealthService healthService;
    private CoreStatusEffectRegistry statusEffectRegistry;
    private CoreStatusEffectService statusEffectService;
    private StatusEffectLoader statusEffectLoader;
    private CoreSkillRegistry skillRegistry;
    private CoreSkillsService skillsService;
    private CoreItemRegistry itemRegistry;
    private ItemLoader itemLoader;
    private CoreMobRegistry mobRegistry;
    private MobLoader mobLoader;
    private CoreAbilityRegistry abilityRegistry;
    private AbilityLoader abilityLoader;
    private CoreBlockRegistry blockRegistry;
    private BlockLoader blockLoader;

    public static RpgCorePlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);

        File statusEffectsDir = new File(getDataFolder(), "status-effects");
        if (!new File(statusEffectsDir, "example.yml").exists()) {
            saveResource("status-effects/example.yml", false);
        }

        File itemsDir = new File(getDataFolder(), "items");
        if (!new File(itemsDir, "example.yml").exists()) {
            saveResource("items/example.yml", false);
        }

        File mobsDir = new File(getDataFolder(), "mobs");
        if (!new File(mobsDir, "example.yml").exists()) {
            saveResource("mobs/example.yml", false);
        }

        File abilitiesDir = new File(getDataFolder(), "abilities");
        if (!new File(abilitiesDir, "example.yml").exists()) {
            saveResource("abilities/example.yml", false);
        }

        File blocksDir = new File(getDataFolder(), "blocks");
        if (!new File(blocksDir, "example.yml").exists()) {
            saveResource("blocks/example.yml", false);
        }

        File dataDir = new File(getDataFolder(), "data");

        NamespacedKey itemIdKey = new NamespacedKey(this, "item_id");
        NamespacedKey mobIdKey = new NamespacedKey(this, "mob_id");

        dataStore = new YamlDataStore(dataDir);
        messageFormatter = new CoreMessageFormatter(messagesFile);
        nameFormatter = new CoreNameFormatter();
        scheduler = new CoreSchedulerService();
        cooldowns = new CoreCooldownService();
        expressions = new CoreExpressionEvaluator();
        statRegistry = new CoreStatRegistry();
        playerLookup = new CorePlayerLookup();
        manaService = new CoreManaService();
        healthService = new CoreHealthService(this);
        statusEffectRegistry = new CoreStatusEffectRegistry();
        statusEffectService = new CoreStatusEffectService(statusEffectRegistry);
        skillRegistry = new CoreSkillRegistry();
        skillsService = new CoreSkillsService(this);
        itemRegistry = new CoreItemRegistry(itemIdKey);
        mobRegistry = new CoreMobRegistry(mobIdKey);
        abilityRegistry = new CoreAbilityRegistry();
        blockRegistry = new CoreBlockRegistry();

        RpgServices.setDataStore(dataStore);
        RpgServices.setMessageFormatter(messageFormatter);
        RpgServices.setNameFormatter(nameFormatter);
        RpgServices.setScheduler(scheduler);
        RpgServices.setCooldowns(cooldowns);
        RpgServices.setExpressions(expressions);
        RpgServices.setStats(statRegistry);
        RpgServices.setPlayerLookup(playerLookup);
        RpgServices.setMana(manaService);
        RpgServices.setHealth(healthService);
        RpgServices.setStatusEffectRegistry(statusEffectRegistry);
        RpgServices.setStatusEffects(statusEffectService);
        RpgServices.setSkillRegistry(skillRegistry);
        RpgServices.setSkills(skillsService);
        RpgServices.setItems(itemRegistry);
        RpgServices.setMobs(mobRegistry);
        RpgServices.setAbilities(abilityRegistry);
        RpgServices.setBlocks(blockRegistry);

        // Register built-in ability effects, then load admin-defined custom abilities on top.
        ItemAbilityListener.registerBuiltins(abilityRegistry);

        statusEffectLoader = new StatusEffectLoader(statusEffectsDir, statusEffectRegistry, getLogger());
        statusEffectLoader.loadAll();

        itemLoader = new ItemLoader(itemsDir, itemRegistry, itemIdKey, getLogger());
        itemLoader.loadAll();

        mobLoader = new MobLoader(mobsDir, mobRegistry, mobIdKey, healthService, getLogger());
        mobLoader.loadAll();

        abilityLoader = new AbilityLoader(abilitiesDir, abilityRegistry, getLogger());
        abilityLoader.loadAll();

        blockLoader = new BlockLoader(blocksDir, blockRegistry, getLogger());
        blockLoader.loadAll();

        getServer().getPluginManager().registerEvents(new VanillaSuppressionListener(this), this);
        getServer().getPluginManager().registerEvents(
                new PlayerLifecycleListener(this, playerLookup, healthService, skillsService), this);
        getServer().getPluginManager().registerEvents(
                new DamagePipelineListener(this, healthService), this);
        getServer().getPluginManager().registerEvents(
                new EquipmentListener(this, healthService), this);
        getServer().getPluginManager().registerEvents(
                new ItemAbilityListener(this, abilityRegistry), this);
        getServer().getPluginManager().registerEvents(
                new BlockBreakHandler(this, blockRegistry), this);

        long regenInterval = getConfig().getLong("regen.interval-ticks", 20L);
        getServer().getScheduler().runTaskTimer(
                this, new RegenTask(healthService), regenInterval, regenInterval);

        long statusInterval = getConfig().getLong("status-effects.scheduler-interval-ticks", 1L);
        getServer().getScheduler().runTaskTimer(
                this, new StatusEffectTickTask(statusEffectService, healthService),
                statusInterval, statusInterval);

        PluginCommand rpg = Objects.requireNonNull(getCommand("rpg"), "command 'rpg' missing from plugin.yml");
        RpgCommand handler = new RpgCommand(this);
        rpg.setExecutor(handler);
        rpg.setTabCompleter(handler);

        getLogger().info("rpg-core v" + getPluginMeta().getVersion() + " enabled.");
        getLogger().info(messageFormatter.format("debug.ready"));
        getLogger().info("Loaded "
                + statusEffectRegistry.all().size() + " status effects, "
                + skillRegistry.all().size() + " skills, "
                + itemRegistry.all().size() + " items, "
                + mobRegistry.all().size() + " mobs, "
                + blockRegistry.all().size() + " block defs.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-core disabled.");
        instance = null;
    }

    public void reloadAll() {
        reloadConfig();
        messageFormatter.reload();
        statusEffectLoader.loadAll();
        itemLoader.loadAll();
        mobLoader.loadAll();
        abilityLoader.loadAll();
        blockLoader.loadAll();
        skillsService.onReload();
    }

    public CoreMessageFormatter messages() {
        return messageFormatter;
    }
}
