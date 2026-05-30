package com.github._255_ping.rpg.core.persistence;

import com.github._255_ping.rpg.api.persistence.DataStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects when the storage backend changes between restarts and migrates all existing
 * data from the old backend to the new one automatically on the next startup.
 *
 * <h3>How it works</h3>
 * The last active backend is stored in {@code <pluginDataFolder>/backend.yml}.
 * On each startup, {@link #maybeRun} compares that value to the backend chosen in
 * {@code config.yml}:
 *
 * <ul>
 *   <li><b>YAML → MySQL:</b> every repository directory under {@code <dataDir>} is scanned;
 *       all records are inserted into MySQL (upsert — existing rows are overwritten).</li>
 *   <li><b>MySQL → YAML:</b> a temporary MySQL connection is opened (using the current
 *       {@code persistence.mysql} config), every table with the configured prefix is copied
 *       into YAML files, then the connection is closed.</li>
 * </ul>
 *
 * Migration is <em>idempotent</em>: if the server crashes mid-migration the backend
 * metadata file is not updated, so the full migration re-runs on the next restart and
 * any existing records in the destination are simply overwritten with the same data.
 *
 * <p><b>Note:</b> migration runs synchronously during {@code onEnable} before any
 * players join, so no async/lock concerns apply.
 */
public final class BackendMigrator {

    private static final String BACKEND_FILE = "backend.yml";

    /** Plugin data folder — backend.yml lives here (not inside data/). */
    private final File pluginDataFolder;
    /** The data sub-directory that YamlDataStore uses (e.g. plugins/rpg-core/data). */
    private final File dataDir;
    private final Logger logger;

    public BackendMigrator(File pluginDataFolder, File dataDir, Logger logger) {
        this.pluginDataFolder = pluginDataFolder;
        this.dataDir = dataDir;
        this.logger = logger;
    }

    /**
     * Checks whether the backend has changed since the last startup and migrates data if so.
     *
     * @param currentBackend  "yaml" or "mysql" — whatever was just opened
     * @param activeStore     the DataStore that was just opened and will be used this session
     * @param mysqlCfg        the {@code persistence.mysql} config section; may be {@code null}
     *                        if the admin removed it, in which case MySQL→YAML migration is skipped
     */
    public void maybeRun(String currentBackend,
                          DataStore activeStore,
                          ConfigurationSection mysqlCfg) {
        String lastBackend = readLastBackend();

        if (lastBackend.isEmpty()) {
            // First ever startup — record the current backend, nothing to migrate.
            writeLastBackend(currentBackend);
            return;
        }

        if (lastBackend.equalsIgnoreCase(currentBackend)) {
            return; // same backend as last time — no migration needed
        }

        logger.info("[BackendMigrator] Storage backend changed: "
                + lastBackend.toUpperCase() + " → " + currentBackend.toUpperCase()
                + ". Starting data migration...");

        boolean success;
        if ("mysql".equals(currentBackend) && "yaml".equals(lastBackend)) {
            success = migrateYamlToMysql(activeStore);
        } else if ("yaml".equals(currentBackend) && "mysql".equals(lastBackend)) {
            if (mysqlCfg == null) {
                logger.warning("[BackendMigrator] persistence.mysql is missing from config.yml — "
                        + "cannot read old MySQL data. Migration skipped; old data remains in MySQL.");
                return;
            }
            success = migrateMysqlToYaml(activeStore, mysqlCfg);
        } else {
            logger.warning("[BackendMigrator] Unrecognised backend transition ("
                    + lastBackend + " → " + currentBackend + "). No migration performed.");
            success = true; // record the new backend anyway
        }

        if (success) {
            writeLastBackend(currentBackend);
            logger.info("[BackendMigrator] Migration complete. Backend is now: "
                    + currentBackend.toUpperCase());
        } else {
            logger.warning("[BackendMigrator] Migration finished with errors — "
                    + "backend metadata NOT updated. The migration will retry on next restart.");
        }
    }

    // ── YAML → MySQL ──────────────────────────────────────────────────────────

    private boolean migrateYamlToMysql(DataStore mysql) {
        File[] repoDirs = dataDir.listFiles(f -> f.isDirectory() && !f.getName().startsWith("_"));
        if (repoDirs == null || repoDirs.length == 0) {
            logger.info("[BackendMigrator] No YAML repository directories found — nothing to migrate.");
            return true;
        }

        boolean allOk = true;
        int totalRecords = 0;

        for (File repoDir : repoDirs) {
            String repoName = repoDir.getName();
            try {
                YamlRepository yamlRepo = new YamlRepository(repoDir);
                DataStore.Repository mysqlRepo = mysql.repository(repoName);
                Collection<String> keys = yamlRepo.keys();
                for (String key : keys) {
                    yamlRepo.get(key).ifPresent(data -> mysqlRepo.save(key, data).join());
                }
                logger.info("[BackendMigrator]   " + repoName + ": " + keys.size() + " records → MySQL.");
                totalRecords += keys.size();
            } catch (Exception ex) {
                logger.log(Level.SEVERE,
                        "[BackendMigrator] Failed to migrate repo '" + repoName + "'", ex);
                allOk = false;
            }
        }

        logger.info("[BackendMigrator] YAML → MySQL: " + totalRecords
                + " total records across " + repoDirs.length + " repositories.");
        return allOk;
    }

    // ── MySQL → YAML ──────────────────────────────────────────────────────────

    private boolean migrateMysqlToYaml(DataStore yaml, ConfigurationSection mysqlCfg) {
        MysqlDataStore mysql = null;
        try {
            mysql = new MysqlDataStore(mysqlCfg, logger);
            List<String> repoNames = mysql.repositoryNames();

            if (repoNames.isEmpty()) {
                logger.info("[BackendMigrator] No MySQL repositories found — nothing to migrate.");
                return true;
            }

            boolean allOk = true;
            int totalRecords = 0;

            for (String repoName : repoNames) {
                try {
                    DataStore.Repository mysqlRepo = mysql.repository(repoName);
                    DataStore.Repository yamlRepo = yaml.repository(repoName);
                    Collection<String> keys = mysqlRepo.keys();
                    for (String key : keys) {
                        mysqlRepo.get(key).ifPresent(data -> yamlRepo.save(key, data).join());
                    }
                    logger.info("[BackendMigrator]   " + repoName + ": " + keys.size() + " records → YAML.");
                    totalRecords += keys.size();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE,
                            "[BackendMigrator] Failed to migrate repo '" + repoName + "'", ex);
                    allOk = false;
                }
            }

            logger.info("[BackendMigrator] MySQL → YAML: " + totalRecords
                    + " total records across " + repoNames.size() + " repositories.");
            return allOk;

        } catch (Throwable t) {
            logger.log(Level.SEVERE,
                    "[BackendMigrator] Could not open MySQL connection for migration", t);
            return false;
        } finally {
            if (mysql != null) {
                try { mysql.close(); } catch (Exception ignored) {}
            }
        }
    }

    // ── metadata helpers ──────────────────────────────────────────────────────

    private String readLastBackend() {
        File f = metaFile();
        if (!f.exists()) return "";
        return YamlConfiguration.loadConfiguration(f).getString("backend", "");
    }

    private void writeLastBackend(String backend) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("backend", backend);
        try {
            yaml.save(metaFile());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "[BackendMigrator] Failed to write backend.yml", ex);
        }
    }

    private File metaFile() {
        return new File(pluginDataFolder, BACKEND_FILE);
    }
}
