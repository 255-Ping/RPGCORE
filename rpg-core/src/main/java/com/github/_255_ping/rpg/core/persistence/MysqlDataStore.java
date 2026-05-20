package com.github._255_ping.rpg.core.persistence;

import com.github._255_ping.rpg.api.persistence.DataStore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * JDBC-backed DataStore. One table per repository (e.g., {@code rpg_kv_quests}); rows are
 * {@code (key, data)} where {@code data} is the YAML serialization of the value map.
 *
 * <p>HikariCP wraps the JDBC driver for connection pooling. Both libraries are declared in
 * {@code rpg-core}'s {@code plugin.yml} under {@code libraries:} so Paper resolves them at runtime.
 */
public final class MysqlDataStore implements DataStore, AutoCloseable {

    private final HikariDataSource pool;
    private final String tablePrefix;
    private final ConcurrentMap<String, MysqlRepository> repos = new ConcurrentHashMap<>();
    private final Logger logger;

    public MysqlDataStore(ConfigurationSection cfg, Logger logger) {
        this.logger = logger;
        this.tablePrefix = cfg.getString("table-prefix", "rpg_kv_");
        String host = cfg.getString("host", "127.0.0.1");
        int port = cfg.getInt("port", 3306);
        String database = cfg.getString("database", "rpg");
        boolean useSsl = cfg.getBoolean("use-ssl", false);
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSsl + "&allowPublicKeyRetrieval=true";

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(jdbcUrl);
        hc.setUsername(cfg.getString("user", "rpg"));
        hc.setPassword(cfg.getString("password", ""));
        hc.setMaximumPoolSize(cfg.getInt("pool-size", 8));
        hc.setMinimumIdle(cfg.getInt("min-idle", 2));
        hc.setConnectionTimeout(cfg.getLong("connection-timeout-ms", 5000));
        hc.setPoolName("rpg-core");
        this.pool = new HikariDataSource(hc);

        // Run versioned migrations bundled in the jar at /migrations/V*.sql.
        new MigrationRunner(pool, tablePrefix, logger).run();
    }

    @Override
    public Repository repository(String name) {
        return repos.computeIfAbsent(name,
                n -> new MysqlRepository(pool, tablePrefix + sanitize(n), logger));
    }

    @Override
    public void close() {
        if (pool != null && !pool.isClosed()) pool.close();
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
