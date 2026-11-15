package club.voidaris.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Database {

    private static final String DRIVER = "org.mariadb.jdbc.Driver";

    private final JavaPlugin plugin;
    // JDBC blocks; virtual threads keep that off the main thread while the pool size bounds real concurrency.
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private HikariDataSource dataSource;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the pool and applies pending migrations. Blocks, so it is only meant for plugin startup.
     */
    public void connect(ConfigurationSection settings) throws SQLException {
        var config = new HikariConfig();
        config.setPoolName("VoidarisCore");
        // Library-loaded drivers are invisible to DriverManager, so Hikari must instantiate the class directly.
        config.setDriverClassName(DRIVER);
        config.setJdbcUrl("jdbc:mariadb://%s:%d/%s".formatted(
                settings.getString("host"), settings.getInt("port"), settings.getString("database")));
        config.setUsername(settings.getString("username"));
        config.setPassword(settings.getString("password"));
        config.setMaximumPoolSize(settings.getInt("pool.maximum-size"));
        config.setMinimumIdle(settings.getInt("pool.minimum-idle"));
        config.setConnectionTimeout(settings.getLong("pool.connection-timeout-ms"));
        config.setMaxLifetime(settings.getLong("pool.max-lifetime-ms"));

        dataSource = new HikariDataSource(config);
        try {
            new Migrations(plugin, dataSource).apply();
        } catch (SQLException | RuntimeException e) {
            close();
            throw e;
        }
    }

    public boolean isConnected() {
        return dataSource != null && dataSource.isRunning();
    }

    public <T> CompletableFuture<T> query(SqlFunction<T> function) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Database is not connected"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = dataSource.getConnection()) {
                return function.apply(connection);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public void close() {
        executor.shutdown();
        try {
            // Gives in-flight writes (e.g. saves triggered by quitting players) a chance to finish.
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Database tasks did not finish within 10 seconds; closing the pool anyway.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
