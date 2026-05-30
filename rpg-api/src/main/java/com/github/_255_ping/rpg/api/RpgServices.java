package com.github._255_ping.rpg.api;

import com.github._255_ping.rpg.api.abilities.AbilityRegistry;
import com.github._255_ping.rpg.api.accessories.AccessoryService;
import com.github._255_ping.rpg.api.gui.GuiConfig;
import com.github._255_ping.rpg.api.hud.ActionBarService;
import com.github._255_ping.rpg.api.mobs.MobStatService;
import com.github._255_ping.rpg.api.station.StationService;
import com.github._255_ping.rpg.api.blocks.BlockRegistry;
import com.github._255_ping.rpg.api.cooldown.CooldownService;
import com.github._255_ping.rpg.api.currency.CurrencyRegistry;
import com.github._255_ping.rpg.api.economy.Economy;
import com.github._255_ping.rpg.api.formatting.MessageFormatter;
import com.github._255_ping.rpg.api.guilds.GuildService;
import com.github._255_ping.rpg.api.formatting.NameFormatter;
import com.github._255_ping.rpg.api.formula.ExpressionEvaluator;
import com.github._255_ping.rpg.api.health.HealthService;
import com.github._255_ping.rpg.api.items.ItemRegistry;
import com.github._255_ping.rpg.api.loot.LootTableRegistry;
import com.github._255_ping.rpg.api.mobs.MobRegistry;
import com.github._255_ping.rpg.api.parties.PartyService;
import com.github._255_ping.rpg.api.regions.RegionService;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.player.ManaService;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.scheduler.SchedulerService;
import com.github._255_ping.rpg.api.skills.SkillRegistry;
import com.github._255_ping.rpg.api.skills.SkillsService;
import com.github._255_ping.rpg.api.stats.StatRegistry;
import com.github._255_ping.rpg.api.status.StatusEffectRegistry;
import com.github._255_ping.rpg.api.status.StatusEffectService;
import com.github._255_ping.rpg.api.wand.WandService;
import org.bukkit.entity.Player;

/**
 * Static service locator. rpg-core sets the implementations on enable; addons read them.
 * Calling an accessor before its service is set throws IllegalStateException.
 *
 * <p>Setters are public for testing/injection purposes but should only be called by rpg-core.
 */
public final class RpgServices {

    // Existing services
    private static StatRegistry stats;
    private static ItemRegistry items;
    private static MobRegistry mobs;
    private static AbilityRegistry abilities;
    private static ManaService mana;
    private static PlayerLookup players;

    // New services
    private static DataStore dataStore;
    private static SkillRegistry skillRegistry;
    private static SkillsService skills;
    private static StatusEffectRegistry statusEffectRegistry;
    private static StatusEffectService statusEffects;
    private static CooldownService cooldowns;
    private static NameFormatter nameFormatter;
    private static MessageFormatter messageFormatter;
    private static SchedulerService scheduler;
    private static HealthService health;
    private static ExpressionEvaluator expressions;
    private static CurrencyRegistry currencies;
    private static LootTableRegistry lootTables;
    private static BlockRegistry blocks;
    private static Economy economy;
    private static AccessoryService accessories;
    private static PartyService parties;
    private static RegionService regionsSvc;
    private static GuildService guilds;
    private static WandService wands;
    private static GuiConfig guiConfig;
    private static MobStatService mobStats;
    private static ActionBarService actionBar;
    private static StationService stationService;

    private RpgServices() {}

    // ----- Accessors -----

    public static StatRegistry stats() { return require(stats, "StatRegistry"); }
    public static ItemRegistry items() { return require(items, "ItemRegistry"); }
    public static MobRegistry mobs() { return require(mobs, "MobRegistry"); }
    public static AbilityRegistry abilities() { return require(abilities, "AbilityRegistry"); }
    public static ManaService mana() { return require(mana, "ManaService"); }
    public static RpgPlayer player(Player p) { return require(players, "PlayerLookup").get(p); }

    public static DataStore dataStore() { return require(dataStore, "DataStore"); }
    public static SkillRegistry skillRegistry() { return require(skillRegistry, "SkillRegistry"); }
    public static SkillsService skills() { return require(skills, "SkillsService"); }
    public static StatusEffectRegistry statusEffectRegistry() { return require(statusEffectRegistry, "StatusEffectRegistry"); }
    public static StatusEffectService statusEffects() { return require(statusEffects, "StatusEffectService"); }
    public static CooldownService cooldowns() { return require(cooldowns, "CooldownService"); }
    public static NameFormatter nameFormatter() { return require(nameFormatter, "NameFormatter"); }
    public static MessageFormatter messageFormatter() { return require(messageFormatter, "MessageFormatter"); }
    public static SchedulerService scheduler() { return require(scheduler, "SchedulerService"); }
    public static HealthService health() { return require(health, "HealthService"); }
    public static ExpressionEvaluator expressions() { return require(expressions, "ExpressionEvaluator"); }
    public static CurrencyRegistry currencies() { return require(currencies, "CurrencyRegistry"); }
    public static LootTableRegistry lootTables() { return require(lootTables, "LootTableRegistry"); }
    public static BlockRegistry blocks() { return require(blocks, "BlockRegistry"); }
    public static Economy economy() { return require(economy, "Economy"); }
    public static AccessoryService accessories() { return require(accessories, "AccessoryService"); }
    public static PartyService parties() { return require(parties, "PartyService"); }
    public static RegionService regionService() { return require(regionsSvc, "RegionService"); }
    public static GuildService guilds() { return require(guilds, "GuildService"); }
    public static WandService wands() { return require(wands, "WandService"); }
    public static GuiConfig guiConfig() { return require(guiConfig, "GuiConfig"); }
    public static MobStatService mobStats() { return require(mobStats, "MobStatService"); }
    public static ActionBarService actionBar() { return require(actionBar, "ActionBarService"); }
    public static StationService stations() { return require(stationService, "StationService"); }

    // ----- Setters (rpg-core only) -----

    public static void setStats(StatRegistry svc) { stats = svc; }
    public static void setItems(ItemRegistry svc) { items = svc; }
    public static void setMobs(MobRegistry svc) { mobs = svc; }
    public static void setAbilities(AbilityRegistry svc) { abilities = svc; }
    public static void setMana(ManaService svc) { mana = svc; }
    public static void setPlayerLookup(PlayerLookup svc) { players = svc; }
    public static void setDataStore(DataStore svc) { dataStore = svc; }
    public static void setSkillRegistry(SkillRegistry svc) { skillRegistry = svc; }
    public static void setSkills(SkillsService svc) { skills = svc; }
    public static void setStatusEffectRegistry(StatusEffectRegistry svc) { statusEffectRegistry = svc; }
    public static void setStatusEffects(StatusEffectService svc) { statusEffects = svc; }
    public static void setCooldowns(CooldownService svc) { cooldowns = svc; }
    public static void setNameFormatter(NameFormatter svc) { nameFormatter = svc; }
    public static void setMessageFormatter(MessageFormatter svc) { messageFormatter = svc; }
    public static void setScheduler(SchedulerService svc) { scheduler = svc; }
    public static void setHealth(HealthService svc) { health = svc; }
    public static void setExpressions(ExpressionEvaluator svc) { expressions = svc; }
    public static void setCurrencies(CurrencyRegistry svc) { currencies = svc; }
    public static void setLootTables(LootTableRegistry svc) { lootTables = svc; }
    public static void setBlocks(BlockRegistry svc) { blocks = svc; }
    public static void setEconomy(Economy svc) { economy = svc; }
    public static void setAccessories(AccessoryService svc) { accessories = svc; }
    public static void setParties(PartyService svc) { parties = svc; }
    public static void setRegionService(RegionService svc) { regionsSvc = svc; }
    public static void setGuilds(GuildService svc) { guilds = svc; }
    public static void setWands(WandService svc) { wands = svc; }
    public static void setGuiConfig(GuiConfig svc) { guiConfig = svc; }
    public static void setMobStats(MobStatService svc) { mobStats = svc; }
    public static void setActionBar(ActionBarService svc) { actionBar = svc; }
    public static void setStations(StationService svc) { stationService = svc; }

    private static <T> T require(T svc, String name) {
        if (svc == null) throw new IllegalStateException(name + " not bootstrapped — is rpg-core loaded?");
        return svc;
    }

    @FunctionalInterface
    public interface PlayerLookup {
        RpgPlayer get(Player player);
    }
}
