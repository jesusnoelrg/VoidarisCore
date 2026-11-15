package club.voidaris.core.storage;

import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * Applies {@code migrations/V1.sql}, {@code V2.sql}, ... from the jar in order, recording each in {@code schema_version}.
 * Applied files must never be edited; schema changes always go in a new, higher version.
 */
final class Migrations {

    private static final String PATH = "migrations/V%d.sql";

    private final JavaPlugin plugin;
    private final DataSource dataSource;

    Migrations(JavaPlugin plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }

    void apply() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS schema_version (
                            version INT PRIMARY KEY,
                            applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )""");
            }

            int version = currentVersion(connection);
            while (true) {
                String script = read(PATH.formatted(version + 1));
                if (script == null) {
                    break;
                }
                version++;
                run(connection, version, script);
                plugin.getLogger().info("Applied database migration V" + version + ".");
            }
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
            result.next();
            return result.getInt(1);
        }
    }

    private void run(Connection connection, int version, String script) throws SQLException {
        // MariaDB commits DDL implicitly, so a failed migration can leave partial changes behind;
        // keep each file small and idempotent where possible (IF NOT EXISTS).
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
            // Comments are dropped first so a ';' inside them cannot split a statement.
            // The split is still naive: no ';' inside string literals or routines.
            String code = script.lines()
                    .filter(line -> !line.stripLeading().startsWith("--"))
                    .collect(Collectors.joining("\n"));
            for (String sql : code.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
            try (var insert = connection.prepareStatement("INSERT INTO schema_version (version) VALUES (?)")) {
                insert.setInt(1, version);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Migration V" + version + " failed", e);
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private String read(String path) {
        try (InputStream in = plugin.getResource(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
