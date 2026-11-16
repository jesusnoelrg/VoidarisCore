package club.voidaris.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public final class ConfigFile {

    // Wide enough that long MiniMessage strings are not folded over several lines when the file is rewritten.
    private static final int LINE_WIDTH = 10_000;

    private final JavaPlugin plugin;
    private final String name;
    private final File file;
    private final ConfigUpdater updater;
    private FileConfiguration config;

    public ConfigFile(JavaPlugin plugin, String name) {
        this(plugin, name, Set.of(), List.of());
    }

    /**
     * @see ConfigUpdater#ConfigUpdater(String, Set, List, java.util.logging.Logger)
     */
    public ConfigFile(JavaPlugin plugin, String name, Set<String> userSections, List<ConfigMigration> migrations) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name);
        this.updater = new ConfigUpdater(name, userSections, migrations, plugin.getLogger());
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        var disk = YamlConfiguration.loadConfiguration(file);
        var bundled = bundled();
        if (bundled != null) {
            if (updater.update(disk, bundled)) {
                save(disk);
            }
            // Keys added in newer versions, or with an invalid value, fall back to the bundled file.
            disk.setDefaults(bundled);
        }
        config = disk;
    }

    private YamlConfiguration bundled() {
        try (var reader = new InputStreamReader(plugin.getResource(name), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException | NullPointerException e) {
            plugin.getLogger().warning("Could not load bundled defaults for " + name + ": " + e.getMessage());
            return null;
        }
    }

    private void save(YamlConfiguration disk) {
        try {
            Files.copy(file.toPath(), new File(file.getParentFile(), file.getName() + ".bak").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            disk.options().width(LINE_WIDTH);
            disk.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not update " + file.getPath(), e);
        }
    }

    public FileConfiguration get() {
        return config;
    }

    public String name() {
        return name;
    }
}
