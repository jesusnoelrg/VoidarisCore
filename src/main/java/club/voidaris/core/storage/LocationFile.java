package club.voidaris.core.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;

/**
 * Named locations persisted in {@code plugins/VoidarisCore/data/<name>}. Names are case-insensitive.
 * Reads are safe from async events such as the spawn location lookup on login.
 */
public final class LocationFile {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, StoredLocation> locations = new ConcurrentSkipListMap<>();

    public LocationFile(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.file = new File(new File(plugin.getDataFolder(), "data"), name);
    }

    public void load() {
        locations.clear();
        if (!file.exists()) {
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            var section = yaml.getConfigurationSection(key);
            if (section != null) {
                StoredLocation.read(section).ifPresent(location -> locations.put(normalize(key), location));
            }
        }
    }

    public Optional<StoredLocation> get(String name) {
        return Optional.ofNullable(locations.get(normalize(name)));
    }

    public Map<String, StoredLocation> all() {
        return Collections.unmodifiableMap(locations);
    }

    public void put(String name, StoredLocation location) {
        locations.put(normalize(name), location);
        save();
    }

    public boolean remove(String name) {
        if (locations.remove(normalize(name)) == null) {
            return false;
        }
        save();
        return true;
    }

    // Written synchronously: these files hold a handful of entries and only change on admin commands,
    // so ordering concerns of async writes are not worth it.
    private void save() {
        var yaml = new YamlConfiguration();
        locations.forEach((name, location) -> location.write(yaml.createSection(name)));
        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getPath(), e);
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
