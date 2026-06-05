package com.github._255_ping.rpg.kits;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * rpg-kits — starter and reward kits.
 *
 * <h3>Commands</h3>
 * <ul>
 *   <li>{@code /kit}           — list available kits</li>
 *   <li>{@code /kit <name>}    — claim a kit</li>
 *   <li>{@code /givenkit <player> <kit>} — admin force-give</li>
 *   <li>{@code /kitreset <player> [kit]} — admin reset claim history</li>
 * </ul>
 */
public final class RpgKitsPlugin extends JavaPlugin {

    private KitRegistry   registry;
    private KitClaimStore claimStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureExample("kits/example.yml");

        registry   = new KitRegistry(new File(getDataFolder(), "kits"), getLogger());
        claimStore = new KitClaimStore();
        registry.reload();

        registerCmd("kit",      new KitCommand());
        registerCmd("givenkit", new GiveKitCommand());
        registerCmd("kitreset", new KitResetCommand());

        getLogger().info("rpg-kits v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-kits disabled.");
    }

    // -------------------------------------------------------------------------
    // /kit
    // -------------------------------------------------------------------------

    private final class KitCommand implements CommandExecutor, TabCompleter {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
            if (!p.hasPermission("rpg.kits.use")) { p.sendMessage("§cNo permission."); return true; }

            if (args.length == 0) {
                listKits(p);
                return true;
            }
            Optional<KitDef> opt = registry.get(args[0]);
            if (opt.isEmpty()) { p.sendMessage("§cKit §e" + args[0] + " §cnot found. Use §e/kit §cto see available kits."); return true; }
            giveKit(p, opt.get());
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 1) return filter(registry.all().stream().map(KitDef::id).toList(), args[0]);
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // /givenkit
    // -------------------------------------------------------------------------

    private final class GiveKitCommand implements CommandExecutor, TabCompleter {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("rpg.kits.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length < 2) { sender.sendMessage("§7Usage: /givenkit <player> <kit>"); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { sender.sendMessage("§cPlayer §e" + args[0] + " §cnot found."); return true; }
            Optional<KitDef> opt = registry.get(args[1]);
            if (opt.isEmpty()) { sender.sendMessage("§cKit §e" + args[1] + " §cnot found."); return true; }
            deliverItems(target, opt.get());
            sender.sendMessage("§aGave kit §e" + opt.get().id() + " §ato §e" + target.getName() + "§a.");
            target.sendMessage("§aYou received kit §e" + opt.get().displayName() + "§a from an admin.");
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 1) return filter(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
            if (args.length == 2) return filter(registry.all().stream().map(KitDef::id).toList(), args[1]);
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // /kitreset
    // -------------------------------------------------------------------------

    private final class KitResetCommand implements CommandExecutor, TabCompleter {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("rpg.kits.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length == 0) { sender.sendMessage("§7Usage: /kitreset <player> [kit]"); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { sender.sendMessage("§cPlayer §e" + args[0] + " §cnot found or offline."); return true; }
            if (args.length >= 2) {
                claimStore.resetClaim(target.getUniqueId(), args[1]);
                sender.sendMessage("§aReset claim for kit §e" + args[1] + " §aon §e" + target.getName() + "§a.");
            } else {
                claimStore.resetAll(target.getUniqueId());
                sender.sendMessage("§aReset all kit claims for §e" + target.getName() + "§a.");
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 1) return filter(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
            if (args.length == 2) return filter(registry.all().stream().map(KitDef::id).toList(), args[1]);
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // Kit delivery logic
    // -------------------------------------------------------------------------

    private void listKits(Player p) {
        List<KitDef> kits = registry.all().stream()
                .filter(k -> k.permission() == null || p.hasPermission(k.permission()))
                .toList();
        if (kits.isEmpty()) { p.sendMessage("§7No kits are available to you."); return; }
        p.sendMessage("§6Available kits:");
        for (KitDef kit : kits) {
            String status = kitStatus(p, kit);
            p.sendMessage("  §e" + kit.id() + " §7— " + kit.displayName() + " §8(" + status + "§8)");
            for (String line : kit.description()) p.sendMessage("    §8" + line.replace("&", "§"));
        }
    }

    private String kitStatus(Player p, KitDef kit) {
        if (kit.oneTime()) {
            return claimStore.hasClaimed(p.getUniqueId(), kit.id()) ? "§cClaimed" : "§aReady";
        }
        long remainMs = claimStore.cooldownRemainingMs(p.getUniqueId(), kit.id());
        if (remainMs <= 0) return "§aReady";
        return "§c" + formatDuration(remainMs);
    }

    private void giveKit(Player p, KitDef kit) {
        // Permission check
        if (kit.permission() != null && !p.hasPermission(kit.permission())) {
            p.sendMessage("§cYou don't have permission to claim kit §e" + kit.id() + "§c.");
            return;
        }
        // One-time check
        if (kit.oneTime() && claimStore.hasClaimed(p.getUniqueId(), kit.id())) {
            p.sendMessage("§cYou have already claimed §e" + kit.displayName() + "§c.");
            return;
        }
        // Cooldown check
        if (!kit.oneTime() && kit.cooldownSeconds() > 0) {
            long remainMs = claimStore.cooldownRemainingMs(p.getUniqueId(), kit.id());
            if (remainMs > 0) {
                p.sendMessage("§cKit §e" + kit.displayName() + " §cis on cooldown. Available in §e"
                        + formatDuration(remainMs) + "§c.");
                return;
            }
        }
        // Deliver
        deliverItems(p, kit);
        // Record claim
        if (kit.oneTime()) {
            claimStore.markClaimed(p.getUniqueId(), kit.id());
        } else if (kit.cooldownSeconds() > 0) {
            claimStore.setCooldown(p.getUniqueId(), kit.id(), kit.cooldownSeconds());
        }
        p.sendMessage("§aYou received §e" + kit.displayName() + "§a!");
    }

    private void deliverItems(Player p, KitDef kit) {
        for (KitDef.ItemEntry entry : kit.items()) {
            ItemStack stack = buildItem(entry);
            if (stack == null) {
                getLogger().warning("Kit '" + kit.id() + "': unknown item '" + entry.itemId() + "' — skipped.");
                continue;
            }
            HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(stack);
            for (ItemStack drop : overflow.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), drop);
            }
        }
    }

    private ItemStack buildItem(KitDef.ItemEntry entry) {
        Optional<RpgItem> custom = RpgServices.items().get(entry.itemId());
        if (custom.isPresent()) {
            ItemStack s = custom.get().toItemStack();
            s.setAmount(entry.amount());
            return s;
        }
        Material mat = Material.matchMaterial(entry.itemId());
        return mat == null ? null : new ItemStack(mat, entry.amount());
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static String formatDuration(long ms) {
        long secs = ms / 1000;
        if (secs < 60)   return secs + "s";
        if (secs < 3600) return (secs / 60) + "m " + (secs % 60) + "s";
        long h = secs / 3600, m = (secs % 3600) / 60;
        return h + "h " + m + "m";
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.startsWith(lower)).toList();
    }

    private void registerCmd(String name, Object handler) {
        var cmd = getCommand(name);
        if (cmd == null) { getLogger().warning("Command '" + name + "' missing from plugin.yml"); return; }
        if (handler instanceof CommandExecutor ce) cmd.setExecutor(ce);
        if (handler instanceof TabCompleter tc)    cmd.setTabCompleter(tc);
    }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try { saveResource(resourcePath, false); } catch (IllegalArgumentException ignored) {}
    }
}
