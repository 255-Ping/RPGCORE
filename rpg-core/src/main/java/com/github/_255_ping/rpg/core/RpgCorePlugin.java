package com.github._255_ping.rpg.core;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.core.abilities.AbilityLoader;
import com.github._255_ping.rpg.core.abilities.CoreAbilityRegistry;
import com.github._255_ping.rpg.core.abilities.ItemAbilityListener;
import com.github._255_ping.rpg.core.abilities.PassiveAbilityFirer;
import com.github._255_ping.rpg.core.abilities.PlayerAttackAbilityListener;
import com.github._255_ping.rpg.core.abilities.PlayerBlockAbilityListener;
import com.github._255_ping.rpg.core.abilities.PlayerHitAbilityListener;
import com.github._255_ping.rpg.core.abilities.PlayerHurtAbilityListener;
import com.github._255_ping.rpg.core.abilities.PlayerJumpAbilityListener;
import com.github._255_ping.rpg.core.abilities.PlayerKillAbilityListener;
import com.github._255_ping.rpg.core.abilities.PlayerLoginAbilityListener;
import com.github._255_ping.rpg.core.abilities.PlayerPassiveAbilityTask;
import com.github._255_ping.rpg.core.sets.ArmorSetListener;
import com.github._255_ping.rpg.core.sets.ArmorSetLoader;
import com.github._255_ping.rpg.core.sets.CoreArmorSetRegistry;
import com.github._255_ping.rpg.core.blocks.BlockBreakHandler;
import com.github._255_ping.rpg.core.blocks.BlockHologramService;
import com.github._255_ping.rpg.core.blocks.BlockInteractListener;
import com.github._255_ping.rpg.core.blocks.BlockPlaceListener;
import com.github._255_ping.rpg.core.blocks.BlockLoader;
import com.github._255_ping.rpg.core.blocks.BlockPersistence;
import com.github._255_ping.rpg.core.blocks.CoreBlockRegistry;
import com.github._255_ping.rpg.core.station.CoreStationService;
import com.github._255_ping.rpg.core.command.EffectsCommand;
import com.github._255_ping.rpg.core.command.SpawnerCommand;
import com.github._255_ping.rpg.core.command.SpawnerGui;
import com.github._255_ping.rpg.core.spawners.SpawnerManager;
import com.github._255_ping.rpg.core.command.RpgCommand;
import com.github._255_ping.rpg.core.command.SkillCommand;
import com.github._255_ping.rpg.core.command.StatsCommand;
import com.github._255_ping.rpg.core.command.StatsGui;
import com.github._255_ping.rpg.core.cooldown.CoreCooldownService;
import com.github._255_ping.rpg.core.currency.CoreCurrencyRegistry;
import com.github._255_ping.rpg.core.death.DeathRulesListener;
import com.github._255_ping.rpg.core.damage.DamageIndicatorListener;
import com.github._255_ping.rpg.core.damage.DamagePipelineListener;
import com.github._255_ping.rpg.core.items.ConsumableItemListener;
import com.github._255_ping.rpg.core.mobs.MobDeathAnimListener;
import com.github._255_ping.rpg.core.mobs.MobAbilityRuntime;
import com.github._255_ping.rpg.core.formatting.CoreMessageFormatter;
import com.github._255_ping.rpg.core.formatting.CoreNameFormatter;
import com.github._255_ping.rpg.core.gui.CoreGuiConfig;
import com.github._255_ping.rpg.core.hud.CoreActionBarService;
import com.github._255_ping.rpg.core.drops.DropManager;
import com.github._255_ping.rpg.core.items.BowListener;
import com.github._255_ping.rpg.core.particles.ParticleManager;
import com.github._255_ping.rpg.core.mobs.CoreMobStatService;
import com.github._255_ping.rpg.core.suppression.DurabilityListener;
import com.github._255_ping.rpg.core.formula.CoreExpressionEvaluator;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import com.github._255_ping.rpg.core.health.RegenTask;
import com.github._255_ping.rpg.core.items.CoreItemRegistry;
import com.github._255_ping.rpg.core.loot.CoreLootPoolRegistry;
import com.github._255_ping.rpg.core.loot.LootChestRegistry;
import com.github._255_ping.rpg.core.loot.LootPoolLoader;
import com.github._255_ping.rpg.core.items.ItemLoader;
import com.github._255_ping.rpg.core.mobs.CoreMobRegistry;
import com.github._255_ping.rpg.core.mobs.MobAiTask;
import com.github._255_ping.rpg.core.spawning.NaturalSpawnLoader;
import com.github._255_ping.rpg.core.spawning.NaturalSpawnTask;
import com.github._255_ping.rpg.core.mobs.DamagerTracker;
import com.github._255_ping.rpg.core.mobs.FactionAlertMap;
import com.github._255_ping.rpg.core.mobs.MobAbilityEventListener;
import com.github._255_ping.rpg.core.mobs.MobAbilityTimerTask;
import com.github._255_ping.rpg.core.mobs.CoreLootTable;
import com.github._255_ping.rpg.core.mobs.MobLoader;
import com.github._255_ping.rpg.core.mobs.MobLootListener;
import com.github._255_ping.rpg.core.persistence.BackendMigrator;
import com.github._255_ping.rpg.core.persistence.MysqlDataStore;
import com.github._255_ping.rpg.core.persistence.YamlDataStore;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.core.input.CoreSignInputService;
import com.github._255_ping.rpg.core.achievement.AchievementGui;
import com.github._255_ping.rpg.core.achievement.AchievementLoader;
import com.github._255_ping.rpg.core.achievement.CoreAchievementService;
import com.github._255_ping.rpg.core.command.AchievementsCommand;
import com.github._255_ping.rpg.core.command.AdventureCommand;
import com.github._255_ping.rpg.core.command.AdventureGui;
import com.github._255_ping.rpg.core.command.CraftingNavListener;
import com.github._255_ping.rpg.core.command.ItemBrowserGui;
import com.github._255_ping.rpg.core.command.ProfileCommand;
import com.github._255_ping.rpg.core.command.ProfileGui;
import com.github._255_ping.rpg.core.command.SettingsCommand;
import com.github._255_ping.rpg.core.command.SettingsGui;
import com.github._255_ping.rpg.core.command.SkillsGui;
import com.github._255_ping.rpg.core.command.SocialCommand;
import com.github._255_ping.rpg.core.command.SocialGui;
import com.github._255_ping.rpg.core.command.WalletGui;
import com.github._255_ping.rpg.core.mobs.EliteService;
import com.github._255_ping.rpg.core.mobs.OwnedMobTracker;
import com.github._255_ping.rpg.core.wand.CoreWandService;
import com.github._255_ping.rpg.core.wand.WandListener;
import com.github._255_ping.rpg.core.player.CoreManaService;
import com.github._255_ping.rpg.core.player.CorePlayerLookup;
import com.github._255_ping.rpg.core.player.EquipmentListener;
import com.github._255_ping.rpg.core.player.PlayerLifecycleListener;
import com.github._255_ping.rpg.core.player.PlayerPreferencesService;
import com.github._255_ping.rpg.core.player.ResourcePackListener;
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
    private DataStore dataStore;
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
    private BlockPersistence blockPersistence;
    private BlockHologramService blockHologramService;
    private NamespacedKey blockItemKey;
    private DamagerTracker damagerTracker;
    private CoreCurrencyRegistry currencyRegistry;
    private CoreLootPoolRegistry lootPoolRegistry;
    private LootPoolLoader lootPoolLoader;
    private SpawnerManager spawnerManager;
    private DamagePipelineListener damagePipeline;
    private CoreWandService wandService;
    private WandListener wandListener;
    private LootChestRegistry lootChestRegistry;
    private ParticleManager particleManager;
    private CoreArmorSetRegistry armorSetRegistry;
    private ArmorSetLoader armorSetLoader;
    private PassiveAbilityFirer passiveAbilityFirer;
    private ArmorSetListener armorSetListener;
    private CoreSignInputService signInputService;
    private EquipmentListener equipmentListener;

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
        blockItemKey = new NamespacedKey(this, "rpg_block_id");

        dataStore = openDataStore(dataDir);
        CoreGuiConfig guiCfg = new CoreGuiConfig(getConfig());
        RpgServices.setGuiConfig(guiCfg);
        CoreMobStatService mobStatService = new CoreMobStatService();
        RpgServices.setMobStats(mobStatService);
        CoreActionBarService actionBarService = new CoreActionBarService();
        RpgServices.setActionBar(actionBarService);
        CoreStationService stationService = new CoreStationService();
        RpgServices.setStations(stationService);
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
        armorSetRegistry = new CoreArmorSetRegistry();
        blockRegistry = new CoreBlockRegistry();
        damagerTracker = new DamagerTracker();
        currencyRegistry = new CoreCurrencyRegistry();
        lootPoolRegistry = new CoreLootPoolRegistry();

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
        RpgServices.setArmorSets(armorSetRegistry);
        RpgServices.setBlocks(blockRegistry);
        RpgServices.setCurrencies(currencyRegistry);
        RpgServices.setLootPools(lootPoolRegistry);

        CoreLootTable.setMagicFindMultiplierCap(getConfig().getDouble("loot.max-magic-find-multiplier", 3.0));

        signInputService = new CoreSignInputService(this);
        getServer().getPluginManager().registerEvents(signInputService, this);
        RpgServices.setSignInput(signInputService);

        OwnedMobTracker ownedMobTracker = new OwnedMobTracker(this);
        getServer().getPluginManager().registerEvents(ownedMobTracker, this);

        ItemAbilityListener.registerBuiltins(abilityRegistry, this);

        // Armor set loader
        File setsDir = new File(getDataFolder(), "sets");
        if (!setsDir.isDirectory()) setsDir.mkdirs();
        if (!new File(setsDir, "example.yml").exists()) {
            saveResource("sets/example.yml", false);
        }
        armorSetLoader = new ArmorSetLoader(setsDir, armorSetRegistry, getLogger());
        armorSetLoader.loadAll();

        // Passive/proc ability infrastructure
        passiveAbilityFirer = new PassiveAbilityFirer(abilityRegistry, getLogger());
        armorSetListener = new ArmorSetListener(this, armorSetRegistry, playerLookup);
        passiveAbilityFirer.setArmorSetListener(armorSetListener);

        statusEffectLoader = new StatusEffectLoader(statusEffectsDir, statusEffectRegistry, getLogger());
        statusEffectLoader.loadAll();

        File statOrderFile = new File(getDataFolder(), "stat-order.yml");
        if (!statOrderFile.exists()) saveResource("stat-order.yml", false);
        itemLoader = new ItemLoader(itemsDir, itemRegistry, itemIdKey, getLogger(), statOrderFile);
        itemLoader.loadAll();

        File lootPoolsDir = new File(getDataFolder(), "loot-pools");
        if (!lootPoolsDir.isDirectory()) lootPoolsDir.mkdirs();
        if (!new File(lootPoolsDir, "example.yml").exists()) {
            saveResource("loot-pools/example.yml", false);
        }
        lootPoolLoader = new LootPoolLoader(lootPoolsDir, lootPoolRegistry, getLogger());
        lootPoolLoader.loadAll();

        mobLoader = new MobLoader(mobsDir, mobRegistry, mobIdKey, healthService, getLogger());
        mobLoader.loadAll();

        abilityLoader = new AbilityLoader(abilitiesDir, abilityRegistry, getLogger());
        abilityLoader.loadAll();

        blockLoader = new BlockLoader(blocksDir, blockRegistry, getLogger());
        blockLoader.loadAll();

        blockPersistence = new BlockPersistence(dataStore, blockRegistry);
        blockPersistence.load();

        blockHologramService = new BlockHologramService(this);
        blockHologramService.initAll(blockRegistry);

        getServer().getPluginManager().registerEvents(new VanillaSuppressionListener(this), this);
        getServer().getPluginManager().registerEvents(new DurabilityListener(getConfig()), this);
        getServer().getPluginManager().registerEvents(new BowListener(this), this);
        PlayerPreferencesService preferencesService = new PlayerPreferencesService();
        getServer().getPluginManager().registerEvents(
                new PlayerLifecycleListener(this, playerLookup, healthService, skillsService, preferencesService), this);
        damagePipeline = new DamagePipelineListener(this, healthService);
        getServer().getPluginManager().registerEvents(damagePipeline, this);
        getServer().getPluginManager().registerEvents(new DamageIndicatorListener(this), this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);
        getServer().getPluginManager().registerEvents(new ConsumableItemListener(itemIdKey), this);
        equipmentListener = new EquipmentListener(this, healthService);
        getServer().getPluginManager().registerEvents(equipmentListener, this);
        equipmentListener.startResyncTask(this, 60L); // resync all players every 3 s as a safety net
        getServer().getPluginManager().registerEvents(
                new ItemAbilityListener(this, abilityRegistry), this);
        getServer().getPluginManager().registerEvents(armorSetListener, this);
        getServer().getPluginManager().registerEvents(
                new PlayerHitAbilityListener(passiveAbilityFirer), this);
        getServer().getPluginManager().registerEvents(
                new PlayerHurtAbilityListener(passiveAbilityFirer), this);
        getServer().getPluginManager().registerEvents(
                new PlayerJumpAbilityListener(passiveAbilityFirer), this);
        getServer().getPluginManager().registerEvents(
                new PlayerAttackAbilityListener(passiveAbilityFirer), this);
        getServer().getPluginManager().registerEvents(
                new PlayerKillAbilityListener(passiveAbilityFirer, damagerTracker), this);
        getServer().getPluginManager().registerEvents(
                new PlayerBlockAbilityListener(passiveAbilityFirer), this);
        getServer().getPluginManager().registerEvents(
                new PlayerLoginAbilityListener(passiveAbilityFirer), this);

        long passiveInterval = getConfig().getLong("abilities.passive-interval-ticks", 20L);
        getServer().getScheduler().runTaskTimer(
                this, new PlayerPassiveAbilityTask(getServer(), passiveAbilityFirer),
                passiveInterval, passiveInterval);

        DropManager dropManager = new DropManager(this);
        getServer().getPluginManager().registerEvents(dropManager, this);

        ParticleManager particleManager = new ParticleManager(this);
        particleManager.start();

        BlockBreakHandler blockBreakHandler = new BlockBreakHandler(this, blockRegistry, dropManager, blockHologramService);
        getServer().getPluginManager().registerEvents(blockBreakHandler, this);
        blockBreakHandler.start();
        getServer().getPluginManager().registerEvents(new BlockInteractListener(blockRegistry), this);
        getServer().getPluginManager().registerEvents(
                new BlockPlaceListener(blockRegistry, blockPersistence, blockItemKey, blockHologramService), this);
        getServer().getPluginManager().registerEvents(damagerTracker, this);
        getServer().getPluginManager().registerEvents(new MobLootListener(damagerTracker, dropManager), this);

        // Make particle manager accessible to RpgCommand.
        this.particleManager = particleManager;
        FactionAlertMap factionAlerts = new FactionAlertMap();
        getServer().getPluginManager().registerEvents(new MobAbilityEventListener(factionAlerts), this);
        getServer().getPluginManager().registerEvents(new MobDeathAnimListener(mobIdKey), this);
        getServer().getPluginManager().registerEvents(new DeathRulesListener(this), this);

        long regenInterval = getConfig().getLong("regen.interval-ticks", 20L);
        getServer().getScheduler().runTaskTimer(
                this, new RegenTask(healthService), regenInterval, regenInterval);

        long statusInterval = getConfig().getLong("status-effects.scheduler-interval-ticks", 1L);
        getServer().getScheduler().runTaskTimer(
                this, new StatusEffectTickTask(statusEffectService, healthService),
                statusInterval, statusInterval);

        getServer().getScheduler().runTaskTimer(
                this, new MobAbilityTimerTask(mobRegistry, mobIdKey), 1L, 1L);

        long mobAiInterval = Math.max(1, getConfig().getLong("mob-ai.interval-ticks", 10));
        getServer().getScheduler().runTaskTimer(
                this, new MobAiTask(mobRegistry, mobIdKey, factionAlerts, healthService),
                mobAiInterval, mobAiInterval);

        File naturalSpawnDir = new File(getDataFolder(), "natural-spawning");
        if (!new File(naturalSpawnDir, "example.yml").exists()) {
            saveResource("natural-spawning/example.yml", false);
        }
        NaturalSpawnLoader naturalLoader = new NaturalSpawnLoader(naturalSpawnDir, getLogger());
        naturalLoader.loadAll();
        long natInterval = Math.max(1, getConfig().getLong("natural-spawning.interval-ticks", 20));
        getServer().getScheduler().runTaskTimer(
                this, new NaturalSpawnTask(this, naturalLoader, (int) natInterval),
                natInterval, natInterval);

        PluginCommand rpg = Objects.requireNonNull(getCommand("rpg"), "command 'rpg' missing from plugin.yml");
        ItemBrowserGui itemBrowserGui = new ItemBrowserGui(this);
        getServer().getPluginManager().registerEvents(itemBrowserGui, this);
        RpgCommand handler = new RpgCommand(this, itemBrowserGui);
        rpg.setExecutor(handler);
        rpg.setTabCompleter(handler);
        StatsGui statsGui = new StatsGui(this);
        getServer().getPluginManager().registerEvents(statsGui, this);
        var statsCmd = Objects.requireNonNull(getCommand("stats"));
        StatsCommand statsCommand = new StatsCommand(this, statsGui);
        statsCmd.setExecutor(statsCommand);
        statsCmd.setTabCompleter(statsCommand);
        Objects.requireNonNull(getCommand("skill")).setExecutor(new SkillCommand(this));
        Objects.requireNonNull(getCommand("effects")).setExecutor(new EffectsCommand(this));
        ProfileGui profileGui = new ProfileGui(this, statsGui);
        getServer().getPluginManager().registerEvents(profileGui, this);
        var profileCmd = Objects.requireNonNull(getCommand("profile"), "command 'profile' missing from plugin.yml");
        ProfileCommand profileCommand = new ProfileCommand(profileGui);
        profileCmd.setExecutor(profileCommand);
        profileCmd.setTabCompleter(profileCommand);

        spawnerManager = new SpawnerManager(this, healthService);
        spawnerManager.loadAll();
        new EliteService(this);   // registers itself as the static singleton
        damagePipeline.setMobKeys(mobIdKey, spawnerManager.mobLevelKey());
        MobAbilityRuntime.setMobLevelKey(spawnerManager.mobLevelKey());
        SpawnerGui spawnerGui = new SpawnerGui(this, spawnerManager);
        getServer().getPluginManager().registerEvents(spawnerGui, this);
        SpawnerCommand spawnerCommand = new SpawnerCommand(this, spawnerManager, spawnerGui);
        var spawnerCmd = Objects.requireNonNull(getCommand("spawner"));
        spawnerCmd.setExecutor(spawnerCommand);
        spawnerCmd.setTabCompleter(spawnerCommand);
        getServer().getScheduler().runTaskTimer(this, spawnerManager::tick, 20L, 20L);

        // Achievement system
        File achievementsDir = new File(getDataFolder(), "achievements");
        if (!achievementsDir.isDirectory()) achievementsDir.mkdirs();
        if (!new File(achievementsDir, "example.yml").exists()) {
            saveResource("achievements/example.yml", false);
        }
        AchievementLoader achievementLoader = new AchievementLoader(achievementsDir, getLogger());
        CoreAchievementService achievementService = new CoreAchievementService(this, achievementLoader.loadAll());
        RpgServices.setAchievements(achievementService);
        getServer().getPluginManager().registerEvents(achievementService, this);
        AchievementGui achievementGui = new AchievementGui(this);
        getServer().getPluginManager().registerEvents(achievementGui, this);
        var achCmd = Objects.requireNonNull(getCommand("achievements"));
        AchievementsCommand achCommand = new AchievementsCommand(achievementGui);
        achCmd.setExecutor(achCommand);
        achCmd.setTabCompleter(achCommand);

        // Navigation GUIs — all opened from crafting-slot nav buttons or slash commands
        SkillsGui skillsGui = new SkillsGui(this);
        getServer().getPluginManager().registerEvents(skillsGui, this);
        WalletGui walletGui = new WalletGui(this);
        getServer().getPluginManager().registerEvents(walletGui, this);
        AdventureGui adventureGui = new AdventureGui(this, walletGui, achievementGui);
        getServer().getPluginManager().registerEvents(adventureGui, this);
        Objects.requireNonNull(getCommand("adventure")).setExecutor(new AdventureCommand(adventureGui));
        SocialGui socialGui = new SocialGui(this);
        getServer().getPluginManager().registerEvents(socialGui, this);
        Objects.requireNonNull(getCommand("social")).setExecutor(new SocialCommand(socialGui));
        SettingsGui settingsGui = new SettingsGui(preferencesService);
        getServer().getPluginManager().registerEvents(settingsGui, this);
        Objects.requireNonNull(getCommand("settings")).setExecutor(new SettingsCommand(settingsGui));
        // Crafting-slot nav buttons — places phantom buttons in the survival crafting grid
        CraftingNavListener craftingNav = new CraftingNavListener(
                this, profileGui, skillsGui, socialGui, adventureGui, settingsGui);
        getServer().getPluginManager().registerEvents(craftingNav, this);

        wandService = new CoreWandService();
        wandListener = new WandListener(this, wandService);
        getServer().getPluginManager().registerEvents(wandListener, this);
        RpgServices.setWands(wandService);

        lootChestRegistry = new LootChestRegistry(this);
        lootChestRegistry.load();
        getServer().getPluginManager().registerEvents(lootChestRegistry, this);

        getLogger().info("rpg-core v" + getPluginMeta().getVersion() + " enabled.");
        getLogger().info(messageFormatter.format("debug.ready"));
        getLogger().info("Loaded "
                + statusEffectRegistry.all().size() + " status effects, "
                + skillRegistry.all().size() + " skills, "
                + itemRegistry.all().size() + " items, "
                + mobRegistry.all().size() + " mobs, "
                + blockRegistry.all().size() + " block defs, "
                + blockRegistry.trackedCount() + " tagged block locations.");
    }

    @Override
    public void onDisable() {
        if (blockHologramService != null) {
            blockHologramService.despawnAll();
        }
        if (blockPersistence != null) {
            try {
                blockPersistence.save();
            } catch (Exception ex) {
                getLogger().warning("Failed to save block locations: " + ex.getMessage());
            }
        }
        if (spawnerManager != null) {
            try {
                spawnerManager.saveAll();
            } catch (Exception ex) {
                getLogger().warning("Failed to save spawners: " + ex.getMessage());
            }
        }
        if (dataStore instanceof AutoCloseable closeable) {
            try { closeable.close(); } catch (Exception ignored) {}
        }
        getLogger().info("rpg-core disabled.");
        instance = null;
    }

    public void reloadAll() {
        reloadConfig();
        messageFormatter.reload();
        statusEffectLoader.loadAll();
        itemLoader.loadAll();
        if (lootPoolLoader != null) lootPoolLoader.loadAll();
        mobLoader.loadAll();
        abilityLoader.loadAll();
        if (armorSetLoader != null) armorSetLoader.loadAll();
        blockLoader.loadAll();
        blockHologramService.initAll(blockRegistry);   // re-spawn with updated definitions
        skillsService.onReload();
    }

    public CoreMessageFormatter messages() {
        return messageFormatter;
    }

    public WandListener wandListener() { return wandListener; }
    public CoreWandService wandService() { return wandService; }
    public ParticleManager particleManager() { return particleManager; }
    public LootChestRegistry lootChestRegistry() { return lootChestRegistry; }
    public NamespacedKey blockItemKey() { return blockItemKey; }
    public EquipmentListener equipmentListener() { return equipmentListener; }

    private DataStore openDataStore(File dataDir) {
        String backend = getConfig().getString("persistence.backend", "yaml").toLowerCase();
        var mysqlCfg = getConfig().getConfigurationSection("persistence.mysql");
        BackendMigrator migrator = new BackendMigrator(getDataFolder(), dataDir, getLogger());

        if ("mysql".equals(backend)) {
            try {
                if (mysqlCfg == null) throw new IllegalStateException("persistence.mysql missing");
                MysqlDataStore mysql = new MysqlDataStore(mysqlCfg, getLogger());
                // Migrate YAML → MySQL if the backend was previously YAML.
                migrator.maybeRun("mysql", mysql, mysqlCfg);
                return mysql;
            } catch (Throwable t) {
                getLogger().warning("MySQL datastore failed to initialize ("
                        + t.getMessage() + "); falling back to YAML (backend migration skipped).");
            }
        }

        // YAML backend — but first migrate MySQL → YAML if the backend was previously MySQL.
        YamlDataStore yaml = new YamlDataStore(dataDir);
        if ("yaml".equals(backend)) {
            migrator.maybeRun("yaml", yaml, mysqlCfg);
        }
        return yaml;
    }
}
