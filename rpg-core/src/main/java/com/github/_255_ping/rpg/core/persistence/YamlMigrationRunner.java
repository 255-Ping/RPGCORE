package com.github._255_ping.rpg.core.persistence;

import com.github._255_ping.rpg.api.persistence.DataStore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies versioned, code-defined transformations to every record in a YAML repository.
 * Analogous to {@link MigrationRunner} for SQL schemas.
 *
 * <p>The last applied version is stored in {@code <metaDir>/<repoName>.yml} so migrations
 * are never applied twice. On restart, only migrations whose version exceeds the stored
 * value are run, in ascending order.
 *
 * <p>Migrations must be idempotent — if the server crashes mid-migration the same
 * transformation will be re-applied to all records on the next restart.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * File metaDir = new File(plugin.getDataFolder(), "yaml-migrations");
 * DataStore.Repository players = RpgServices.dataStore().repository("players");
 * new YamlMigrationRunner(players, metaDir, "players", getLogger()).run(List.of(
 *     new YamlMigrationRunner.Migration(2, "add_guild_id",
 *         data -> { data.putIfAbsent("guild-id", null); return data; })
 * ));
 * }</pre>
 */
public final class YamlMigrationRunner {

    /** A single versioned transformation applied to every record in the repository. */
    public record Migration(int version, String name,
                            UnaryOperator<Map<String, Object>> transform) {}

    private final DataStore.Repository repo;
    private final File metaDir;
    private final String repoName;
    private final Logger logger;

    /**
     * @param repo     the repository whose records will be transformed
     * @param metaDir  directory where per-repo schema-version files are written
     *                 (e.g. {@code plugin.getDataFolder()/yaml-migrations})
     * @param repoName repository name — used as the meta-file name ({@code <repoName>.yml})
     * @param logger   plugin logger
     */
    public YamlMigrationRunner(DataStore.Repository repo, File metaDir,
                                String repoName, Logger logger) {
        this.repo = repo;
        this.metaDir = metaDir;
        this.repoName = repoName;
        this.logger = logger;
    }

    /**
     * Runs all migrations whose version exceeds the last applied version, in ascending order.
     * A migration with version ≤ the stored version is silently skipped.
     */
    public void run(List<Migration> migrations) {
        int applied = currentVersion();
        List<Migration> pending = migrations.stream()
                .filter(m -> m.version() > applied)
                .sorted(Comparator.comparingInt(Migration::version))
                .toList();
        for (Migration m : pending) {
            apply(m);
        }
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private void apply(Migration m) {
        int count = 0;
        for (String key : repo.keys()) {
            final String k = key;
            repo.get(k).ifPresent(data -> {
                Map<String, Object> result = m.transform().apply(new LinkedHashMap<>(data));
                repo.save(k, result).join();
            });
            count++;
        }
        saveVersion(m.version());
        logger.info("[YAML-Migrations] Applied V" + m.version() + "__" + m.name()
                + " to repository '" + repoName + "' (" + count + " records).");
    }

    private int currentVersion() {
        File f = metaFile();
        if (!f.exists()) return 0;
        return YamlConfiguration.loadConfiguration(f).getInt("schema-version", 0);
    }

    private void saveVersion(int version) {
        if (!metaDir.exists()) metaDir.mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", version);
        yaml.set("repository", repoName);
        try {
            yaml.save(metaFile());
        } catch (IOException ex) {
            logger.log(Level.WARNING,
                    "[YAML-Migrations] Failed to persist version for repo '" + repoName + "'", ex);
        }
    }

    private File metaFile() {
        return new File(metaDir, repoName + ".yml");
    }
}
