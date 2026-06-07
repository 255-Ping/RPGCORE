package com.github._255_ping.rpg.quests;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.economy.Economy;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Holds in-memory player quest state, persists it via core's DataStore, and applies rewards. */
public final class QuestManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final RpgQuestsPlugin plugin;
    private final QuestRegistry registry;
    private final Map<UUID, PlayerQuestState> cache = new ConcurrentHashMap<>();

    public QuestManager(RpgQuestsPlugin plugin, QuestRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public QuestRegistry registry() { return registry; }

    public PlayerQuestState load(UUID id) {
        return cache.computeIfAbsent(id, this::loadFromStore);
    }

    public void unload(UUID id) {
        PlayerQuestState s = cache.remove(id);
        if (s != null) save(s);
    }

    public void save(PlayerQuestState state) {
        try {
            DataStore.Repository repo = repo();
            repo.save(state.playerId.toString(), state.toMap());
        } catch (IllegalStateException ignored) {
        }
    }

    private DataStore.Repository repo() {
        String repoName = plugin.getConfig().getString("data-repository", "quests");
        return RpgServices.dataStore().repository(repoName);
    }

    private PlayerQuestState loadFromStore(UUID id) {
        try {
            DataStore.Repository repo = repo();
            return repo.get(id.toString())
                    .map(map -> PlayerQuestState.fromMap(id, map))
                    .orElseGet(() -> new PlayerQuestState(id));
        } catch (IllegalStateException ex) {
            return new PlayerQuestState(id);
        }
    }

    // ----- Public ops -----

    public boolean accept(Player p, String questId) {
        PlayerQuestState s = load(p.getUniqueId());
        Optional<QuestDef> opt = registry.get(questId);
        if (opt.isEmpty()) {
            p.sendMessage(plugin.messages().get("quest.unknown", Map.of("id", questId)));
            return false;
        }
        QuestDef def = opt.get();
        if (s.active.stream().anyMatch(a -> a.questId.equals(def.id()))) {
            p.sendMessage(plugin.messages().get("quest.already-active"));
            return false;
        }
        if (def.requiredLevel() > 0) {
            int lvl = RpgServices.skills().level(p, BuiltinSkill.COMBAT.id());
            if (lvl < def.requiredLevel()) {
                p.sendMessage(plugin.messages().get("quest.requires-level",
                        Map.of("level", def.requiredLevel())));
                return false;
            }
        }

        // ── Chain prerequisite check ─────────────────────────────────────────
        for (String req : def.requires()) {
            if (!s.completed.contains(req)) {
                p.sendMessage(plugin.messages().get("quest.requires-quest", Map.of("quest", req)));
                return false;
            }
        }

        // ── Already-completed check (repeatable cooldown) ────────────────────
        if (s.completed.contains(def.id())) {
            if (!def.repeatable()) {
                p.sendMessage(plugin.messages().get("quest.already-completed"));
                return false;
            }
            if (def.cooldownSeconds() > 0) {
                long lastTime = s.lastCompletionEpochSeconds.getOrDefault(def.id(), 0L);
                long elapsed  = System.currentTimeMillis() / 1000L - lastTime;
                if (elapsed < def.cooldownSeconds()) {
                    long remaining = def.cooldownSeconds() - elapsed;
                    p.sendMessage(plugin.messages().get("quest.cooldown",
                            Map.of("time", formatCooldown(remaining))));
                    return false;
                }
            }
        }

        s.active.add(new PlayerQuestState.Active(def.id(), new int[def.objectives().size()]));
        save(s);
        p.sendMessage(plugin.messages().get("quest.accepted", Map.of("name", def.displayName())));
        return true;
    }

    public boolean abandon(Player p, String questId) {
        PlayerQuestState s = load(p.getUniqueId());
        boolean removed = s.active.removeIf(a -> a.questId.equalsIgnoreCase(questId));
        if (removed) {
            save(s);
            p.sendMessage(plugin.messages().get("quest.abandoned",
                    Map.of("name", questId)));
        } else {
            p.sendMessage(plugin.messages().get("quest.not-active"));
        }
        return removed;
    }

    public void progressFor(Player p, QuestObjective.Type type, String target) {
        progressFor(p, type, target, 1);
    }

    public void progressFor(Player p, QuestObjective.Type type, String target, int amount) {
        if (amount <= 0) return;
        PlayerQuestState s = load(p.getUniqueId());
        if (s.active.isEmpty()) return;
        boolean dirty = false;
        boolean actionBar = plugin.getConfig().getBoolean("progress-action-bar", true);
        for (PlayerQuestState.Active a : new ArrayList<>(s.active)) {
            Optional<QuestDef> def = registry.get(a.questId);
            if (def.isEmpty()) continue;
            List<QuestObjective> objs = def.get().objectives();
            for (int i = 0; i < objs.size(); i++) {
                QuestObjective o = objs.get(i);
                if (o.type() != type) continue;
                if (!o.target().equalsIgnoreCase(target) && !o.target().equalsIgnoreCase("any")) continue;
                if (a.progress[i] >= o.count()) continue;
                a.progress[i] = Math.min(o.count(), a.progress[i] + amount);
                dirty = true;
                if (actionBar) {
                    p.sendActionBar(LEGACY.deserialize("&7" + o.describe() + " &8(" + a.progress[i] + "/" + o.count() + ")"));
                }
                if (a.progress[i] >= o.count() && plugin.getConfig().getBoolean("auto-complete", true)
                        && allComplete(a, def.get())) {
                    complete(p, def.get(), a);
                }
            }
        }
        if (dirty) save(s);
    }

    public void completeByCommand(Player target, String questId) {
        PlayerQuestState s = load(target.getUniqueId());
        Optional<QuestDef> opt = registry.get(questId);
        if (opt.isEmpty()) return;
        PlayerQuestState.Active a = s.active.stream()
                .filter(x -> x.questId.equals(opt.get().id()))
                .findFirst().orElse(null);
        if (a == null) return;
        for (int i = 0; i < a.progress.length; i++) {
            a.progress[i] = opt.get().objectives().get(i).count();
        }
        complete(target, opt.get(), a);
        save(s);
    }

    public boolean isObjectiveActive(Player p, QuestObjective.Type type, String target) {
        for (PlayerQuestState.Active a : load(p.getUniqueId()).active) {
            Optional<QuestDef> def = registry.get(a.questId);
            if (def.isEmpty()) continue;
            for (QuestObjective o : def.get().objectives()) {
                if (o.type() == type && o.target().equalsIgnoreCase(target)) return true;
            }
        }
        return false;
    }

    private void complete(Player p, QuestDef def, PlayerQuestState.Active active) {
        PlayerQuestState s = load(p.getUniqueId());
        s.active.removeIf(a -> a == active || a.questId.equals(def.id()));
        // Avoid duplicates — repeatable quests stay in the list between runs so
        // chain prerequisites continue to be satisfied.
        if (!s.completed.contains(def.id())) {
            s.completed.add(def.id());
        }
        s.lastCompletionEpochSeconds.put(def.id(), System.currentTimeMillis() / 1000L);
        // Award rewards
        for (Map.Entry<String, Long> e : def.xpRewards().entrySet()) {
            RpgServices.skills().awardXp(p, e.getKey(), e.getValue());
        }
        if (def.currencyReward() > 0) {
            try {
                Economy economy = RpgServices.economy();
                economy.deposit(p, BigDecimal.valueOf(def.currencyReward()));
            } catch (IllegalStateException ignored) {}
        }
        for (QuestDef.ItemReward ir : def.itemRewards()) {
            ItemStack stack = buildReward(ir);
            if (stack == null) continue;
            HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(stack);
            for (ItemStack drop : overflow.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), drop);
            }
        }
        p.sendMessage(plugin.messages().get("quest.completed", Map.of("name", def.displayName())));
        save(s);
    }

    private ItemStack buildReward(QuestDef.ItemReward ir) {
        Optional<RpgItem> custom = RpgServices.items().get(ir.itemId());
        if (custom.isPresent()) {
            ItemStack s = custom.get().toItemStack();
            s.setAmount(ir.amount());
            return s;
        }
        Material mat = Material.matchMaterial(ir.itemId());
        return mat == null ? null : new ItemStack(mat, ir.amount());
    }

    private boolean allComplete(PlayerQuestState.Active a, QuestDef def) {
        for (int i = 0; i < a.progress.length; i++) {
            if (a.progress[i] < def.objectives().get(i).count()) return false;
        }
        return true;
    }

    /** Format a cooldown in seconds as a human-readable string (e.g. "1d 3h", "45m", "30s"). */
    private static String formatCooldown(long seconds) {
        if (seconds <= 0) return "0s";
        long days    = seconds / 86400;
        long hours   = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days    > 0) sb.append(days).append("d ");
        if (hours   > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs    > 0 || sb.isEmpty()) sb.append(secs).append("s");
        return sb.toString().trim();
    }

    private static void msg(Player p, String legacy) {
        p.sendMessage(LEGACY.deserialize(legacy).colorIfAbsent(NamedTextColor.WHITE));
    }
}
