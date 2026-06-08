package com.github._255_ping.rpg.parties;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.parties.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Repeating task that sends each online party member an action-bar summary of their
 * teammates' current HP.
 *
 * <p>Format (per viewer):  {@code ❤ Alice 85%  |  ❤ Bob 23%}
 * <ul>
 *   <li>Green  — HP &gt; 70%</li>
 *   <li>Yellow — HP 30–70%</li>
 *   <li>Red    — HP &lt; 30%</li>
 * </ul>
 *
 * <p>Registered in {@link RpgPartiesPlugin} when {@code party-hud.enabled} is true.
 */
public final class PartyHudTask implements Runnable {

    private final PartyManager manager;

    public PartyHudTask(PartyManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        for (Party party : manager.all()) {
            Collection<Player> online = party.members();
            if (online.size() < 2) continue; // solo member — nothing to show

            List<Player> memberList = new ArrayList<>(online);
            for (Player viewer : memberList) {
                Component bar = buildBar(viewer, memberList);
                viewer.sendActionBar(bar);
            }
        }
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    /**
     * Builds the action-bar component for {@code viewer}, listing every OTHER
     * online party member's HP percentage.
     */
    private static Component buildBar(Player viewer, List<Player> members) {
        Component bar = Component.empty();
        boolean first = true;
        for (Player member : members) {
            if (member.getUniqueId().equals(viewer.getUniqueId())) continue;

            double maxHp  = RpgServices.health().maxHp(member);
            double curHp  = RpgServices.health().currentHp(member);
            double pct    = maxHp > 0 ? curHp / maxHp : 0.0;
            int    pctInt = (int) Math.round(pct * 100.0);

            NamedTextColor color = pct > 0.70 ? NamedTextColor.GREEN
                                 : pct > 0.30 ? NamedTextColor.YELLOW
                                 :              NamedTextColor.RED;

            if (!first) {
                bar = bar.append(
                        Component.text("  |  ", NamedTextColor.DARK_GRAY)
                                 .decoration(TextDecoration.ITALIC, false));
            }
            bar = bar
                    .append(Component.text("❤ ", color)
                                     .decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(member.getName(), NamedTextColor.WHITE)
                                     .decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(" " + pctInt + "%", color)
                                     .decoration(TextDecoration.ITALIC, false));
            first = false;
        }
        return bar;
    }
}
