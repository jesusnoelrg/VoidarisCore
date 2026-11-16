package club.voidaris.core.config;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Rewrites a file on disk from one {@code config-version} to the next one.
 */
@FunctionalInterface
public interface ConfigMigration {

    void migrate(YamlConfiguration disk);
}
