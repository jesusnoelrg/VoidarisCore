package club.voidaris.core.config;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Rewrites a file on disk from one {@code config-version} to the next one.
 */
@FunctionalInterface
public interface ConfigMigration {

    void migrate(YamlConfiguration disk, YamlConfiguration bundled);

    /**
     * Replaces each path on disk with its bundled value (with comments), or removes it when the bundled file no
     * longer has it. Meant for admin-owned sections, such as menu items, that are never updated otherwise.
     */
    static ConfigMigration restore(String... paths) {
        return (disk, bundled) -> {
            for (String path : paths) {
                disk.set(path, null);
                if (bundled.contains(path, true)) {
                    ConfigUpdater.copy(disk, bundled, path);
                }
            }
        };
    }
}
