package com.github._255_ping.rpg.core.persistence;

import com.github._255_ping.rpg.api.persistence.DataStore;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/** One repository == one MySQL table. Values are serialized as YAML strings. */
public final class MysqlRepository implements DataStore.Repository {

    private final HikariDataSource pool;
    private final String table;
    private final Logger logger;

    public MysqlRepository(HikariDataSource pool, String table, Logger logger) {
        this.pool = pool;
        this.table = table;
        this.logger = logger;
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                             + "`k` VARCHAR(190) NOT NULL PRIMARY KEY, "
                             + "`v` MEDIUMTEXT NOT NULL, "
                             + "`updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP "
                             + "ON UPDATE CURRENT_TIMESTAMP)")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Failed to ensure table " + table, ex);
        }
    }

    @Override
    public Optional<Map<String, Object>> get(String key) {
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT `v` FROM `" + table + "` WHERE `k`=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                String yaml = rs.getString(1);
                YamlConfiguration cfg = new YamlConfiguration();
                cfg.loadFromString(yaml);
                return Optional.of(cfg.getValues(false));
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to read " + table + "/" + key, ex);
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Void> save(String key, Map<String, Object> data) {
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<String, Object> e : data.entrySet()) cfg.set(e.getKey(), e.getValue());
            String yaml = cfg.saveToString();
            try (Connection c = pool.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO `" + table + "` (`k`,`v`) VALUES (?,?) "
                                 + "ON DUPLICATE KEY UPDATE `v`=VALUES(`v`)")) {
                ps.setString(1, key);
                ps.setString(2, yaml);
                ps.executeUpdate();
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Failed to save " + table + "/" + key, ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(String key) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = pool.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM `" + table + "` WHERE `k`=?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Failed to delete " + table + "/" + key, ex);
            }
        });
    }

    @Override
    public Collection<String> keys() {
        List<String> out = new ArrayList<>();
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT `k` FROM `" + table + "`");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString(1));
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Failed to list keys in " + table, ex);
        }
        return out;
    }
}
