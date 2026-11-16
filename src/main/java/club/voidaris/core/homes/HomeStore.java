package club.voidaris.core.homes;

import club.voidaris.core.storage.StoredLocation;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Homes of each player, persisted in {@code plugins/VoidarisCore/data/homes/<uuid>.yml}. Names are case-insensitive.
 * Changes must come from the main thread; reads are also safe from command suggestions, which may run off it.
 */
final class HomeStore {

    private static final String HOMES = "homes";
    private static final String ICON = "icon";

    private final JavaPlugin plugin;
    private final File folder;
    // Players are never evicted while the module is enabled: the cache stays the source of truth, so a read can never
    // reach a file whose queued write has not landed yet. A handful of homes per player that joined is negligible.
    private final Map<UUID, NavigableMap<String, Home>> cache = new ConcurrentHashMap<>();
    // A single thread keeps the writes of the same file in the order they were made.
    private ExecutorService writer;

    HomeStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(new File(plugin.getDataFolder(), "data"), HOMES);
    }

    void open() {
        writer = Executors.newSingleThreadExecutor();
    }

    void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Home writes did not finish within 10 seconds.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cache.clear();
    }

    /**
     * Loads the file the first time, which is a small read on the calling thread.
     */
    NavigableMap<String, Home> homes(UUID player) {
        return Collections.unmodifiableNavigableMap(loaded(player));
    }

    Optional<Home> find(UUID player, String name) {
        return Optional.ofNullable(loaded(player).get(normalize(name)));
    }

    void put(UUID player, Home home) {
        var homes = loaded(player);
        homes.put(normalize(home.name()), home);
        save(player, homes);
    }

    boolean remove(UUID player, String name) {
        var homes = loaded(player);
        if (homes.remove(normalize(name)) == null) {
            return false;
        }
        save(player, homes);
        return true;
    }

    static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private NavigableMap<String, Home> loaded(UUID player) {
        return cache.computeIfAbsent(player, this::load);
    }

    private NavigableMap<String, Home> load(UUID player) {
        NavigableMap<String, Home> homes = new ConcurrentSkipListMap<>();
        var file = file(player);
        if (!file.exists()) {
            return homes;
        }
        var section = YamlConfiguration.loadConfiguration(file).getConfigurationSection(HOMES);
        if (section == null) {
            return homes;
        }
        for (String name : section.getKeys(false)) {
            var home = section.getConfigurationSection(name);
            if (home != null) {
                StoredLocation.read(home).ifPresent(location ->
                        homes.put(normalize(name), new Home(normalize(name), location, home.getString(ICON))));
            }
        }
        return homes;
    }

    private void save(UUID player, Map<String, Home> homes) {
        var yaml = new YamlConfiguration();
        homes.forEach((name, home) -> {
            var section = yaml.createSection(HOMES + "." + name);
            home.location().write(section);
            if (home.icon() != null) {
                section.set(ICON, home.icon());
            }
        });
        String content = homes.isEmpty() ? null : yaml.saveToString();
        var file = file(player);
        writer.execute(() -> write(file, content));
    }

    private void write(File file, String content) {
        try {
            if (content == null) {
                Files.deleteIfExists(file.toPath());
                return;
            }
            folder.mkdirs();
            var temp = new File(folder, file.getName() + ".tmp").toPath();
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getPath(), e);
        }
    }

    private File file(UUID player) {
        return new File(folder, player + ".yml");
    }
}
