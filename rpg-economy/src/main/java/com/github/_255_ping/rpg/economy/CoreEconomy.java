package com.github._255_ping.rpg.economy;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.currency.Currency;
import com.github._255_ping.rpg.api.economy.Economy;
import com.github._255_ping.rpg.api.persistence.DataStore;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory balance cache backed by DataStore. Persistence is async; balance() reads from
 * the in-memory map (synchronously fast), writes queue through CompletableFuture.
 */
public final class CoreEconomy implements Economy {

    static final String REPO = "balances";

    private final Currency currency;
    private final BigDecimal startingBalance;
    private final ConcurrentHashMap<UUID, BigDecimal> balances = new ConcurrentHashMap<>();

    public CoreEconomy(Currency currency, BigDecimal startingBalance) {
        this.currency = currency;
        this.startingBalance = startingBalance;
    }

    public Currency currency() {
        return currency;
    }

    public void loadAll() {
        DataStore.Repository repo = RpgServices.dataStore().repository(REPO);
        for (String key : repo.keys()) {
            try {
                UUID id = UUID.fromString(key);
                repo.get(key).ifPresent(data -> {
                    Object raw = data.get("balance");
                    if (raw instanceof Number n) balances.put(id, BigDecimal.valueOf(n.doubleValue()));
                    else if (raw instanceof String s) balances.put(id, new BigDecimal(s));
                });
            } catch (IllegalArgumentException ignored) {
                // not a UUID-keyed record
            }
        }
    }

    public void loadOne(UUID id) {
        if (balances.containsKey(id)) return;
        RpgServices.dataStore().repository(REPO).get(id.toString()).ifPresent(data -> {
            Object raw = data.get("balance");
            if (raw instanceof Number n) balances.put(id, BigDecimal.valueOf(n.doubleValue()));
            else if (raw instanceof String s) balances.put(id, new BigDecimal(s));
        });
    }

    public void saveOne(UUID id) {
        BigDecimal bal = balances.get(id);
        if (bal == null) return;
        Map<String, Object> data = Map.of(
                "schema-version", 1,
                "balance", bal.toPlainString()
        );
        RpgServices.dataStore().repository(REPO).save(id.toString(), data);
    }

    public void saveAll() {
        for (UUID id : balances.keySet()) saveOne(id);
    }

    public Map<UUID, BigDecimal> snapshot() {
        return Map.copyOf(balances);
    }

    @Override
    public BigDecimal balance(OfflinePlayer player) {
        BigDecimal b = balances.get(player.getUniqueId());
        return b == null ? startingBalance : b;
    }

    @Override
    public CompletableFuture<Void> deposit(OfflinePlayer player, BigDecimal amount) {
        if (amount.signum() <= 0) return CompletableFuture.completedFuture(null);
        balances.compute(player.getUniqueId(), (k, cur) -> {
            BigDecimal start = cur == null ? startingBalance : cur;
            BigDecimal next = start.add(amount);
            if (next.compareTo(currency.maxBalance()) > 0) next = currency.maxBalance();
            return next;
        });
        return CompletableFuture.runAsync(() -> saveOne(player.getUniqueId()));
    }

    @Override
    public boolean withdraw(OfflinePlayer player, BigDecimal amount) {
        if (amount.signum() <= 0) return false;
        boolean[] ok = {false};
        balances.compute(player.getUniqueId(), (k, cur) -> {
            BigDecimal start = cur == null ? startingBalance : cur;
            if (start.compareTo(amount) < 0) {
                return start;   // unchanged
            }
            ok[0] = true;
            return start.subtract(amount);
        });
        if (ok[0]) saveOne(player.getUniqueId());
        return ok[0];
    }

    @Override
    public boolean transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount) {
        if (!withdraw(from, amount)) return false;
        deposit(to, amount);
        return true;
    }

    @Override
    public CompletableFuture<Void> set(OfflinePlayer player, BigDecimal amount) {
        BigDecimal clamped = amount.signum() < 0 ? BigDecimal.ZERO : amount;
        if (clamped.compareTo(currency.maxBalance()) > 0) clamped = currency.maxBalance();
        balances.put(player.getUniqueId(), clamped);
        return CompletableFuture.runAsync(() -> saveOne(player.getUniqueId()));
    }
}
