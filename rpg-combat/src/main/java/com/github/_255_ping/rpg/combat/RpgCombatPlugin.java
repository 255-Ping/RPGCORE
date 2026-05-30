package com.github._255_ping.rpg.combat;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

public final class RpgCombatPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        java.util.Objects.requireNonNull(getCommand("combat"), "command 'combat' missing").setExecutor(this);
        getLogger().info("rpg-combat v" + getPluginMeta().getVersion() + " enabled.");
    }

    /** XP per point of damage dealt — scales with COMBAT_WISDOM. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(PostDamageEvent event) {
        if (!(event.context().attacker() instanceof Player attacker)) return;
        if (event.context().victim() == null) return;
        if (event.dealtDamage() <= 0) return;
        if (!whitelisted(event.context().victim().getType().name())) return;

        double rate = getConfig().getDouble("xp-per-damage", 1.0);
        double abilityMult = event.context().source() != null
                && event.context().source().startsWith("ability")
                ? getConfig().getDouble("combat-xp-multiplier-from-abilities", 1.0)
                : 1.0;
        double wisdom = RpgServices.player(attacker).get(BuiltinStat.COMBAT_WISDOM);

        long amount = Math.round(event.dealtDamage() * rate * abilityMult * (1.0 + wisdom / 100.0));
        if (amount <= 0) return;
        RpgServices.skills().awardXp(attacker, BuiltinSkill.COMBAT.id(), amount);
    }

    /** Bonus XP on kill — looks up the mob's RPG id first, falls back to entity type name. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!whitelisted(event.getEntity().getType().name())) return;

        // Prefer the RPG mob-id for the config lookup so per-mob overrides work.
        String mobKey = RpgServices.mobs().from(event.getEntity())
                .map(RpgMob::id)
                .orElse(event.getEntity().getType().name().toLowerCase(Locale.ROOT));

        long base = getConfig().getLong("xp-per-kill." + mobKey,
                getConfig().getLong("default-kill-xp", 10));
        if (base <= 0) return;

        double wisdom = RpgServices.player(killer).get(BuiltinStat.COMBAT_WISDOM);
        long amount = Math.round(base * (1.0 + wisdom / 100.0));
        if (amount <= 0) return;
        RpgServices.skills().awardXp(killer, BuiltinSkill.COMBAT.id(), amount);
    }

    private boolean whitelisted(String entityTypeName) {
        List<String> wl = getConfig().getStringList("victim-whitelist");
        if (wl.isEmpty()) return true;
        for (String s : wl) {
            if ("any".equalsIgnoreCase(s)) return true;
            if (s.equalsIgnoreCase(entityTypeName)) return true;
        }
        return false;
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.combat.admin.reload")) {
                sender.sendMessage("§cNo permission."); return true;
            }
            reloadConfig();
            sender.sendMessage("§arpg-combat reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: §e/combat reload");
        return true;
    }
}
