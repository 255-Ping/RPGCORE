package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.status.ActiveStatusEffect;
import com.github._255_ping.rpg.api.status.StatusEffect;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class EffectsCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final RpgCorePlugin plugin;

    public EffectsCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(plugin.messages().component("command.player-only"));
                return true;
            }
            if (!sender.hasPermission("rpg.core.effects")) {
                sender.sendMessage(plugin.messages().component("command.no-permission"));
                return true;
            }
            target = p;
        } else {
            if (!sender.hasPermission("rpg.core.effects.other")) {
                sender.sendMessage(plugin.messages().component("command.no-permission"));
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.messages().component("player.not-found", Map.of("name", args[0])));
                return true;
            }
        }

        Collection<ActiveStatusEffect> active = RpgServices.statusEffects().active(target);
        sender.sendMessage(LEGACY.deserialize("&6&l=== Effects: &e" + target.getName() + " &6&l==="));
        if (active.isEmpty()) {
            sender.sendMessage(LEGACY.deserialize("&7No active effects."));
            return true;
        }
        for (ActiveStatusEffect ae : active) {
            Optional<StatusEffect> def = RpgServices.statusEffectRegistry().get(ae.effectId());
            if (def.isPresent() && def.get().hidden()) continue;
            String display = def.map(StatusEffect::displayName).orElse(ae.effectId());
            int seconds = ae.remainingTicks() / 20;
            sender.sendMessage(LEGACY.deserialize(
                    display + " &7Lv&f" + ae.level() + " &7(" + seconds + "s remaining)"));
        }
        return true;
    }
}
