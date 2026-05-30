package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public final class CoreRpgMob implements RpgMob {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final String id;
    private final String displayName;
    private final EntityType baseType;
    private final double maxHealth;
    private final double damage;
    private final double defense;
    private final Map<Stat, Double> stats;
    private final ItemStack helmet, chestplate, leggings, boots, mainHand, offHand;
    private final String customHeadTexture;
    private final List<MobAbilityBinding> bindings;
    private final CoreLootTable lootTable;
    private final MobAiProfile aiProfile;

    private final long xp;
    private final NamespacedKey mobIdKey;
    private final CoreHealthService healthService;

    public CoreRpgMob(String id, String displayName, EntityType baseType,
                      double maxHealth, double damage, double defense,
                      Map<Stat, Double> stats,
                      ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots,
                      ItemStack mainHand, ItemStack offHand,
                      String customHeadTexture,
                      List<MobAbilityBinding> bindings,
                      CoreLootTable lootTable,
                      MobAiProfile aiProfile,
                      long xp,
                      NamespacedKey mobIdKey, CoreHealthService healthService) {
        this.id = id;
        this.displayName = displayName;
        this.baseType = baseType;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.defense = defense;
        this.stats = Map.copyOf(stats);
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
        this.mainHand = mainHand;
        this.offHand = offHand;
        this.customHeadTexture = customHeadTexture;
        this.bindings = List.copyOf(bindings);
        this.lootTable = lootTable;
        this.aiProfile = aiProfile == null ? MobAiProfile.DEFAULT : aiProfile;
        this.xp = xp;
        this.mobIdKey = mobIdKey;
        this.healthService = healthService;
    }

    public MobAiProfile aiProfile() { return aiProfile; }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public EntityType baseType() { return baseType; }
    @Override public double maxHealth() { return maxHealth; }
    @Override public double damage() { return damage; }
    @Override public double defense() { return defense; }
    @Override public Map<Stat, Double> stats() { return stats; }
    @Override public ItemStack helmet() { return helmet; }
    @Override public ItemStack chestplate() { return chestplate; }
    @Override public ItemStack leggings() { return leggings; }
    @Override public ItemStack boots() { return boots; }
    @Override public ItemStack mainHand() { return mainHand; }
    @Override public ItemStack offHand() { return offHand; }
    @Override public String customHeadTexture() { return customHeadTexture; }
    @Override public long xp() { return xp; }

    public List<MobAbilityBinding> abilityBindings() { return bindings; }
    public CoreLootTable lootTable() { return lootTable; }

    @Override
    public LivingEntity spawn(Location loc) {
        if (loc.getWorld() == null) throw new IllegalArgumentException("Location has no world");

        Entity entity = loc.getWorld().spawnEntity(loc, baseType, CreatureSpawnEvent.SpawnReason.CUSTOM);
        if (!(entity instanceof LivingEntity le)) {
            entity.remove();
            throw new IllegalStateException("EntityType " + baseType + " did not yield a LivingEntity");
        }

        if (displayName != null && !displayName.isEmpty()) {
            le.customName(LEGACY.deserialize(displayName));
            le.setCustomNameVisible(true);
        }

        AttributeInstance maxAttr = le.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr != null) maxAttr.setBaseValue(maxHealth);
        le.setHealth(maxHealth);
        healthService.initMob(le, maxHealth);

        EntityEquipment eq = le.getEquipment();
        if (eq != null) {
            if (helmet != null) eq.setHelmet(helmet);
            if (chestplate != null) eq.setChestplate(chestplate);
            if (leggings != null) eq.setLeggings(leggings);
            if (boots != null) eq.setBoots(boots);
            if (mainHand != null) eq.setItemInMainHand(mainHand);
            if (offHand != null) eq.setItemInOffHand(offHand);
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
            eq.setItemInOffHandDropChance(0f);
        }

        le.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, id);

        // Register stat holder so the damage pipeline can read this mob's stats.
        com.github._255_ping.rpg.api.RpgServices.mobStats().register(le, stats);

        applyAiProfile(le);

        // Fire OnSpawn ability bindings now that the entity is in-world and tagged.
        MobAbilityRuntime.fireTrigger(le, this, MobAbilityTrigger.OnSpawn.class);

        return le;
    }

    private void applyAiProfile(org.bukkit.entity.LivingEntity le) {
        if (aiProfile.kind() == MobAiProfile.Kind.PASSIVE || aiProfile.kind() == MobAiProfile.Kind.STATIONARY) {
            if (le instanceof org.bukkit.entity.Mob mob) mob.setAware(false);
        }
        if (aiProfile.immuneToKnockback()) {
            var attr = le.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (attr != null) attr.setBaseValue(1.0);
        }
    }
}
