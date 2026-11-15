package club.voidaris.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ConfigFile {

    private final JavaPlugin plugin;
    private final String name;
    private final File file;
    private FileConfiguration config;

    public ConfigFile(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name);
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        // Keys added in newer versions fall back to the bundled file when an older copy exists on disk.
        try (var reader = new InputStreamReader(plugin.getResource(name), StandardCharsets.UTF_8)) {
            config.setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load bundled defaults for " + name + ": " + e.getMessage());
        }
    }

    public FileConfiguration get() {
        return config;
    }

    public String name() {
        return name;
    }
}
