package com.github._255_ping.rpg.api.economy;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Balance lookups and transfers for a single currency (the primary currency from
 * {@link com.github._255_ping.rpg.api.currency.CurrencyRegistry#primary()} by default).
 *
 * <p>Writes are persisted via {@code DataStore} asynchronously and the returned future
 * completes when the write hits disk. Reads are synchronous and reflect the in-memory
 * cache, which is kept in sync with persistence on player join/quit.
 */
public interface Economy {

    BigDecimal balance(OfflinePlayer player);

    /** Adds the given amount to the player's balance. Negative values are not accepted. */
    CompletableFuture<Void> deposit(OfflinePlayer player, BigDecimal amount);

    /** Subtracts. Returns true synchronously if there was enough balance and the
     *  withdrawal was scheduled; false if the player can't afford it. */
    boolean withdraw(OfflinePlayer player, BigDecimal amount);

    /** Atomic transfer: withdraw from, deposit into. Returns true on success. */
    boolean transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount);

    /** Sets the balance to {@code amount} regardless of current value (admin). */
    CompletableFuture<Void> set(OfflinePlayer player, BigDecimal amount);

    /**
     * Deposits with a human-readable reason tag for the transaction log
     * (e.g. {@code "mob_drop"}, {@code "quest_reward"}, {@code "npc_shop"}).
     * Default delegates to {@link #deposit(OfflinePlayer, BigDecimal)} with no log entry.
     */
    default CompletableFuture<Void> deposit(OfflinePlayer player, BigDecimal amount, String reason) {
        return deposit(player, amount);
    }

    /** Withdraws with a reason tag. Default delegates to {@link #withdraw(OfflinePlayer, BigDecimal)}. */
    default boolean withdraw(OfflinePlayer player, BigDecimal amount, String reason) {
        return withdraw(player, amount);
    }

    /** Transfers with a reason tag. Default delegates to {@link #transfer(OfflinePlayer, OfflinePlayer, BigDecimal)}. */
    default boolean transfer(OfflinePlayer from, OfflinePlayer to, BigDecimal amount, String reason) {
        return transfer(from, to, amount);
    }
}
