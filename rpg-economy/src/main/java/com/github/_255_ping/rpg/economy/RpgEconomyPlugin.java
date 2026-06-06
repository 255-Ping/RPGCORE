package com.github._255_ping.rpg.economy;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.Objects;

public final class RpgEconomyPlugin extends JavaPlugin {

    private CoreEconomy economy;
    private CoreCurrency currency;
    private TxLog txLog;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        currency = new CoreCurrency(
                getConfig().getString("currency.id", "coins"),
                getConfig().getString("currency.display-singular", "coin"),
                getConfig().getString("currency.display-plural", "coins"),
                getConfig().getString("currency.prefix", ""),
                getConfig().getString("currency.suffix", " coins"),
                getConfig().getInt("currency.decimals", 0),
                BigDecimal.valueOf(getConfig().getLong("currency.max-balance", 1_000_000_000L))
        );
        RpgServices.currencies().register(currency);

        BigDecimal starting = BigDecimal.valueOf(getConfig().getLong("currency.starting-balance", 100));
        economy = new CoreEconomy(currency, starting);
        economy.loadAll();
        txLog = new TxLog(getConfig().getInt("transaction-log.max-entries", 100));
        economy.setTxLog(txLog);
        RpgServices.setEconomy(economy);

        getServer().getPluginManager().registerEvents(new PlayerBalanceListener(economy, txLog), this);

        // Register as a Vault economy provider if Vault is loaded.
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            getServer().getServicesManager().register(
                    net.milkbowl.vault.economy.Economy.class,
                    new VaultEconomyProvider(economy, currency),
                    this,
                    org.bukkit.plugin.ServicePriority.Normal
            );
            getLogger().info("Vault economy provider registered.");
        }

        EconomyCommands handler = new EconomyCommands(this, economy);
        for (String cmd : new String[]{"balance", "pay", "eco", "baltop"}) {
            PluginCommand pc = Objects.requireNonNull(getCommand(cmd), "command '" + cmd + "' missing from plugin.yml");
            pc.setExecutor(handler);
            pc.setTabCompleter(handler);
        }

        getLogger().info("rpg-economy v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (economy != null) {
            try { economy.saveAll(); }
            catch (Exception ex) { getLogger().warning("Failed to save balances: " + ex.getMessage()); }
        }
        getLogger().info("rpg-economy disabled.");
    }
}
