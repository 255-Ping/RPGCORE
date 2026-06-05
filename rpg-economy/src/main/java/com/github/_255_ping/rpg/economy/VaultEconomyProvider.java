package com.github._255_ping.rpg.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Wraps {@link CoreEconomy} to satisfy the Vault {@link Economy} interface.
 *
 * <p>Registered as a service provider when Vault is present on the server, so any
 * third-party plugin that reads or writes balances through Vault (shop plugins,
 * permission cost deductions, etc.) talks to our system transparently.
 *
 * <p>Bank operations are not supported — {@link #hasBankSupport()} returns {@code false}
 * and every bank method returns {@link EconomyResponse.ResponseType#NOT_IMPLEMENTED}.
 *
 * <p>All players are treated as having an account (lazy-created on first deposit).
 * String-name overloads delegate to {@link Bukkit#getOfflinePlayer(String)} — standard
 * Vault behaviour that all Vault-consuming plugins expect.
 */
public final class VaultEconomyProvider implements Economy {

    private static final EconomyResponse NOT_IMPL =
            new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");

    private final CoreEconomy economy;
    private final CoreCurrency currency;

    public VaultEconomyProvider(CoreEconomy economy, CoreCurrency currency) {
        this.economy = economy;
        this.currency = currency;
    }

    // ---- metadata ----

    @Override public boolean isEnabled()            { return true; }
    @Override public String  getName()              { return "rpg-economy"; }
    @Override public boolean hasBankSupport()       { return false; }
    @Override public int     fractionalDigits()     { return currency.decimals(); }

    @Override
    public String format(double amount) {
        BigDecimal bd = BigDecimal.valueOf(amount).setScale(currency.decimals(), RoundingMode.HALF_UP);
        return currency.prefix() + bd.toPlainString() + currency.suffix();
    }

    @Override public String currencyNamePlural()    { return currency.displayPlural(); }
    @Override public String currencyNameSingular()  { return currency.displaySingular(); }

    // ---- account existence (all players always have an account) ----

    @Override public boolean hasAccount(OfflinePlayer p)                       { return true; }
    @Override public boolean hasAccount(OfflinePlayer p, String world)         { return true; }
    @Override public boolean createPlayerAccount(OfflinePlayer p)              { return true; }
    @Override public boolean createPlayerAccount(OfflinePlayer p, String world){ return true; }

    @SuppressWarnings("deprecation")
    @Override public boolean hasAccount(String name)                           { return true; }
    @SuppressWarnings("deprecation")
    @Override public boolean hasAccount(String name, String world)             { return true; }
    @SuppressWarnings("deprecation")
    @Override public boolean createPlayerAccount(String name)                  { return true; }
    @SuppressWarnings("deprecation")
    @Override public boolean createPlayerAccount(String name, String world)    { return true; }

    // ---- balance ----

    @Override
    public double getBalance(OfflinePlayer p) {
        return economy.balance(p).doubleValue();
    }

    @Override public double getBalance(OfflinePlayer p, String world) { return getBalance(p); }

    @SuppressWarnings("deprecation")
    @Override public double getBalance(String name)               { return getBalance(offline(name)); }
    @SuppressWarnings("deprecation")
    @Override public double getBalance(String name, String world) { return getBalance(name); }

    // ---- has ----

    @Override
    public boolean has(OfflinePlayer p, double amount) {
        return getBalance(p) >= amount;
    }

    @Override public boolean has(OfflinePlayer p, String world, double amount) { return has(p, amount); }

    @SuppressWarnings("deprecation")
    @Override public boolean has(String name, double amount)               { return has(offline(name), amount); }
    @SuppressWarnings("deprecation")
    @Override public boolean has(String name, String world, double amount) { return has(name, amount); }

    // ---- withdraw ----

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer p, double amount) {
        if (amount < 0) return fail(p, "Amount must be positive", amount);
        boolean ok = economy.withdraw(p, BigDecimal.valueOf(amount));
        if (!ok) return fail(p, "Insufficient funds", amount);
        return ok(p, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer p, String world, double amount) {
        return withdrawPlayer(p, amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse withdrawPlayer(String name, double amount) {
        return withdrawPlayer(offline(name), amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse withdrawPlayer(String name, String world, double amount) {
        return withdrawPlayer(name, amount);
    }

    // ---- deposit ----

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer p, double amount) {
        if (amount < 0) return fail(p, "Amount must be positive", amount);
        economy.deposit(p, BigDecimal.valueOf(amount));
        return ok(p, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer p, String world, double amount) {
        return depositPlayer(p, amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse depositPlayer(String name, double amount) {
        return depositPlayer(offline(name), amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse depositPlayer(String name, String world, double amount) {
        return depositPlayer(name, amount);
    }

    // ---- bank (not supported — all return NOT_IMPLEMENTED) ----

    @Override public EconomyResponse createBank(String n, String p)         { return NOT_IMPL; }
    @Override public EconomyResponse createBank(String n, OfflinePlayer p)  { return NOT_IMPL; }
    @Override public EconomyResponse deleteBank(String n)                   { return NOT_IMPL; }
    @Override public EconomyResponse bankBalance(String n)                  { return NOT_IMPL; }
    @Override public EconomyResponse bankHas(String n, double a)            { return NOT_IMPL; }
    @Override public EconomyResponse bankWithdraw(String n, double a)       { return NOT_IMPL; }
    @Override public EconomyResponse bankDeposit(String n, double a)        { return NOT_IMPL; }
    @Override public EconomyResponse isBankOwner(String n, String p)        { return NOT_IMPL; }
    @Override public EconomyResponse isBankOwner(String n, OfflinePlayer p) { return NOT_IMPL; }
    @Override public EconomyResponse isBankMember(String n, String p)       { return NOT_IMPL; }
    @Override public EconomyResponse isBankMember(String n, OfflinePlayer p){ return NOT_IMPL; }
    @Override public List<String> getBanks()                                { return List.of(); }

    // ---- helpers ----

    private EconomyResponse ok(OfflinePlayer p, double amount) {
        return new EconomyResponse(amount, getBalance(p), EconomyResponse.ResponseType.SUCCESS, null);
    }

    private EconomyResponse fail(OfflinePlayer p, String msg, double amount) {
        return new EconomyResponse(amount, getBalance(p), EconomyResponse.ResponseType.FAILURE, msg);
    }

    @SuppressWarnings("deprecation")
    private static OfflinePlayer offline(String name) {
        return Bukkit.getOfflinePlayer(name);
    }
}
