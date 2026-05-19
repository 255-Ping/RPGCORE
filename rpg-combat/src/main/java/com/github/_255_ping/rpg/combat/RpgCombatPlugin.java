package com.github._255_ping.rpg.combat;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class RpgCombatPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("rpg-combat v" + getPluginMeta().getVersion() + " enabled.");
    }

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
        // No commands yet; future: /combat reload, /combat give <item>
        return false;
    }
}
