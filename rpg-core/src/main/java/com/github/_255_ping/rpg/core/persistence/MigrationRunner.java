package com.github._255_ping.rpg.core.persistence;

import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs versioned SQL migration scripts bundled in the jar at {@code /migrations/V<n>__<name>.sql}.
 *
 * <p>Migration state is tracked in a {@code <prefix>schema_version} table; each script runs once,
 * in ascending {@code <n>} order. Scripts must be idempotent across a single run — we re-execute
 * the next pending one if a prior run was interrupted, but we don't try to rewind.
 *
 * <p>This runner doesn't parse SQL itself — it splits on semicolons at the start of lines, which
 * matches the simple DDL we ship. For richer scripts, switch to a real parser later.
 */
public final class MigrationRunner {

    private static final String MIGRATIONS_PREFIX = "migrations/";

    private final HikariDataSource pool;
    private final String tablePrefix;
    private final Logger logger;

    public MigrationRunner(HikariDataSource pool, String tablePrefix, Logger logger) {
        this.pool = pool;
        this.tablePrefix = tablePrefix;
        this.logger = logger;
    }

    public void run() {
        ensureSchemaTable();
        int applied = currentVersion();
        List<Script> scripts = loadScripts();
        Collections.sort(scripts);
        for (Script s : scripts) {
            if (s.version <= applied) continue;
            try {
                apply(s);
                logger.info("Applied migration V" + s.version + "__" + s.name + ".");
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Migration V" + s.version + "__" + s.name + " failed", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    private void ensureSchemaTable() {
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "schema_version` ("
                             + "`version` INT NOT NULL PRIMARY KEY, "
                             + "`name` VARCHAR(190) NOT NULL, "
                             + "`applied_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to create schema_version table", ex);
            throw new RuntimeException(ex);
        }
    }

    private int currentVersion() {
        try (Connection c = pool.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COALESCE(MAX(version), 0) FROM `" + tablePrefix + "schema_version`");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to read schema_version", ex);
            return 0;
        }
    }

    private void apply(Script s) throws Exception {
        try (Connection c = pool.getConnection()) {
            c.setAutoCommit(false);
            try (java.sql.Statement stmt = c.createStatement()) {
                for (String statement : splitStatements(s.body)) {
                    if (statement.isBlank()) continue;
                    stmt.execute(statement.replace("${prefix}", tablePrefix));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO `" + tablePrefix + "schema_version` (version, name) VALUES (?,?)")) {
                ps.setInt(1, s.version);
                ps.setString(2, s.name);
                ps.executeUpdate();
            }
            c.commit();
        }
    }

    private List<Script> loadScripts() {
        List<Script> out = new ArrayList<>();
        try {
            Enumeration<URL> urls = MigrationRunner.class.getClassLoader().getResources(MIGRATIONS_PREFIX);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url.getProtocol().equals("jar")) {
                    String path = url.getPath();
                    int sep = path.indexOf('!');
                    String jarPath = path.substring("file:".length(), sep);
                    try (JarFile jar = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry e = entries.nextElement();
                            if (!e.getName().startsWith(MIGRATIONS_PREFIX)) continue;
                            if (!e.getName().endsWith(".sql")) continue;
                            try (InputStream in = jar.getInputStream(e)) {
                                out.add(parseScript(e.getName(), in));
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to enumerate migrations", ex);
        }
        return out;
    }

    private Script parseScript(String fullName, InputStream in) throws Exception {
        String baseName = fullName.substring(MIGRATIONS_PREFIX.length());
        // Expect "V<n>__<name>.sql"
        int versionEnd = baseName.indexOf('_');
        if (!baseName.startsWith("V") || versionEnd < 0) {
            throw new IllegalArgumentException("Bad migration name: " + fullName);
        }
        int version = Integer.parseInt(baseName.substring(1, versionEnd));
        int nameStart = baseName.indexOf("__");
        String name = baseName.substring(nameStart + 2, baseName.length() - 4); // strip .sql
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append('\n');
            }
        }
        return new Script(version, name, body.toString());
    }

    private static List<String> splitStatements(String body) {
        // Naive split on ";\n" to keep things readable; sufficient for our DDL.
        List<String> out = new ArrayList<>();
        for (String chunk : body.split(";\\s*\\n")) {
            out.add(chunk.trim());
        }
        return out;
    }

    private static final class Script implements Comparable<Script> {
        final int version;
        final String name;
        final String body;

        Script(int version, String name, String body) {
            this.version = version;
            this.name = name;
            this.body = body;
        }

        @Override
        public int compareTo(Script o) { return Integer.compare(version, o.version); }
    }
}
