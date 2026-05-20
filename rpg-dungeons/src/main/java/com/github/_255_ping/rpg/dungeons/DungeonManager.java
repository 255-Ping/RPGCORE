package com.github._255_ping.rpg.dungeons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class DungeonManager {

    private final JavaPlugin plugin;
    private final DungeonRegistry registry;
    private final Map<UUID, DungeonInstance> instances = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToInstance = new ConcurrentHashMap<>();
    private final AtomicInteger nextGridSlot = new AtomicInteger();
    private TemplatePaster paster;

    private World instanceWorld;

    public DungeonManager(JavaPlugin plugin, DungeonRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void initialize() {
        int budget = plugin.getConfig().getInt("instance.paste-blocks-per-tick", 4096);
        paster = new TemplatePaster(plugin, budget);
        if (plugin.getConfig().getBoolean("instance-world.auto-create", true)) {
            String name = plugin.getConfig().getString("instance-world.name", "rpg_dungeon_instances");
            instanceWorld = InstanceWorldBootstrap.ensure(name, plugin.getLogger());
            if (instanceWorld != null) {
                plugin.getLogger().info("Instance world: " + instanceWorld.getName());
            }
        }
    }

    public Optional<DungeonInstance> instanceOf(Player p) {
        UUID instId = playerToInstance.get(p.getUniqueId());
        if (instId == null) return Optional.empty();
        return Optional.ofNullable(instances.get(instId));
    }

    public Optional<DungeonInstance> instanceById(UUID id) {
        return Optional.ofNullable(instances.get(id));
    }

    public java.util.Collection<DungeonInstance> activeInstances() {
        return instances.values();
    }

    public boolean enter(Player p, String dungeonId) {
        Optional<DungeonDef> opt = registry.get(dungeonId);
        if (opt.isEmpty()) {
            msg(p, "&cUnknown dungeon: " + dungeonId, NamedTextColor.RED);
            return false;
        }
        DungeonDef def = opt.get();
        if (instanceWorld == null) {
            msg(p, "&cInstance world not available.", NamedTextColor.RED);
            return false;
        }
        if (playerToInstance.containsKey(p.getUniqueId())) {
            msg(p, "&eYou're already inside a dungeon.", NamedTextColor.YELLOW);
            return false;
        }
        List<Player> party = collectParty(p, def.maxPlayers());
        DungeonInstance inst = new DungeonInstance(def.id(), reserveOrigin(def));
        instances.put(inst.instanceId, inst);
        for (Player member : party) {
            msg(member, "&7Preparing dungeon...", NamedTextColor.GRAY);
        }
        paster.pasteAsync(Bukkit.getWorld(def.templateWorld()), def.min(), def.max(),
                instanceWorld, inst.originInInstanceWorld, copied -> {
            Location spawn = computeInstanceSpawn(def, inst);
            for (Player member : party) {
                if (!member.isOnline()) continue;
                playerToInstance.put(member.getUniqueId(), inst.instanceId);
                inst.players.add(member.getUniqueId());
                inst.alive.add(member.getUniqueId());
                member.teleport(spawn);
                member.setGameMode(GameMode.SURVIVAL);
                msg(member, "&6Entering " + def.displayName(), NamedTextColor.GOLD);
            }
        });
        return true;
    }

    public boolean leave(Player p) {
        UUID instId = playerToInstance.remove(p.getUniqueId());
        if (instId == null) {
            msg(p, "&eYou're not in a dungeon.", NamedTextColor.YELLOW);
            return false;
        }
        DungeonInstance inst = instances.get(instId);
        if (inst == null) return false;
        inst.players.remove(p.getUniqueId());
        inst.alive.remove(p.getUniqueId());
        sendToExit(p, inst);
        if (inst.players.isEmpty()) {
            destroy(inst);
        }
        return true;
    }

    /** Called from listener on player death inside an instance. */
    public void handleDeath(Player victim) {
        UUID instId = playerToInstance.get(victim.getUniqueId());
        if (instId == null) return;
        DungeonInstance inst = instances.get(instId);
        if (inst == null) return;
        inst.alive.remove(victim.getUniqueId());

        boolean solo = inst.players.size() == 1;
        boolean wipe = inst.alive.isEmpty();

        if (solo && plugin.getConfig().getBoolean("death.solo", true)) {
            evict(inst); // single-player path == evict everyone (just the one)
            return;
        }
        if (wipe) {
            evict(inst);
            return;
        }
        if (plugin.getConfig().getBoolean("death.party-spectator", true)) {
            // Tether the dead player to a remaining alive party member as spectator.
            UUID anchor = inst.alive.iterator().next();
            Player anchorP = Bukkit.getPlayer(anchor);
            if (anchorP != null && anchorP.isOnline()) {
                victim.setGameMode(GameMode.SPECTATOR);
                victim.teleport(anchorP.getLocation());
            }
        }
    }

    public void onQuit(Player p) {
        // Quitting mid-dungeon evicts the player only; don't tear down others.
        UUID instId = playerToInstance.remove(p.getUniqueId());
        if (instId == null) return;
        DungeonInstance inst = instances.get(instId);
        if (inst == null) return;
        inst.players.remove(p.getUniqueId());
        inst.alive.remove(p.getUniqueId());
        if (inst.players.isEmpty()) destroy(inst);
    }

    private void evict(DungeonInstance inst) {
        for (UUID id : new ArrayList<>(inst.players)) {
            Player member = Bukkit.getPlayer(id);
            if (member != null) {
                member.setGameMode(GameMode.SURVIVAL);
                sendToExit(member, inst);
                msg(member, "&cYour dungeon run has ended.", NamedTextColor.RED);
            }
            playerToInstance.remove(id);
        }
        destroy(inst);
    }

    private void destroy(DungeonInstance inst) {
        DungeonDef def = registry.get(inst.dungeonId).orElse(null);
        if (def != null && instanceWorld != null && paster != null) {
            paster.clear(instanceWorld, inst.originInInstanceWorld, def.min(), def.max());
        }
        instances.remove(inst.instanceId);
    }

    private void sendToExit(Player p, DungeonInstance inst) {
        Location loc = registry.get(inst.dungeonId)
                .map(DungeonDef::exitLocation)
                .orElse(null);
        if (loc == null) {
            // Fallback: world spawn.
            loc = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        p.teleport(loc);
    }

    private Vector reserveOrigin(DungeonDef def) {
        int spacing = plugin.getConfig().getInt("instance.grid-spacing-blocks", 256);
        int yBase = plugin.getConfig().getInt("instance.y-base", 64);
        int slot = nextGridSlot.getAndIncrement();
        int row = slot / 32;
        int col = slot % 32;
        return new Vector(col * spacing, yBase, row * spacing);
    }

    private Location computeInstanceSpawn(DungeonDef def, DungeonInstance inst) {
        return new Location(instanceWorld,
                inst.originInInstanceWorld.getX() + def.spawnOffset().getX(),
                inst.originInInstanceWorld.getY() + def.spawnOffset().getY(),
                inst.originInInstanceWorld.getZ() + def.spawnOffset().getZ());
    }

    private List<Player> collectParty(Player p, int max) {
        // Soft-dep on rpg-parties via PartyService. If not loaded, solo run.
        List<Player> result = new ArrayList<>();
        result.add(p);
        if (!plugin.getConfig().getBoolean("party-enter.enabled", true)) return result;
        try {
            var party = com.github._255_ping.rpg.api.RpgServices.parties().partyOf(p);
            if (party.isPresent()) {
                for (Player member : party.get().members()) {
                    if (member.getUniqueId().equals(p.getUniqueId())) continue;
                    if (member.isOnline()) result.add(member);
                    if (result.size() >= max) break;
                }
            }
        } catch (IllegalStateException ignored) {
            // parties not loaded; solo
        }
        return result;
    }

    private static void msg(Player p, String text, NamedTextColor color) {
        p.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(text).colorIfAbsent(color));
    }

    @SuppressWarnings("unused")
    Map<UUID, UUID> debugMap() { return new HashMap<>(playerToInstance); }
}
