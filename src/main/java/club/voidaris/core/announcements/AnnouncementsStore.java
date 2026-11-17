package club.voidaris.core.announcements;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Players who turned announcements off with /announcetoggle, persisted in {@code plugins/VoidarisCore/data/announcements.yml}.
 */
final class AnnouncementsStore {

    private static final String DISABLED = "disabled";

    private final JavaPlugin plugin;
    private final File file;
    private final Set<UUID> disabled = ConcurrentHashMap.newKeySet();
    private final AtomicLong version = new AtomicLong();
    private final Object writeLock = new Object();
    private long writtenVersion;

    AnnouncementsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(new File(plugin.getDataFolder(), "data"), "announcements.yml");
    }

    void load() {
        disabled.clear();
        if (!file.exists()) {
            return;
        }
        for (String value : YamlConfiguration.loadConfiguration(file).getStringList(DISABLED)) {
            try {
                disabled.add(UUID.fromString(value));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring invalid UUID '" + value + "' in " + file.getPath());
            }
        }
    }

    boolean disabled(UUID player) {
        return disabled.contains(player);
    }

    void disabled(UUID player, boolean off) {
        if (off ? disabled.add(player) : disabled.remove(player)) {
            long snapshotVersion = version.incrementAndGet();
            String yaml = snapshot();
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(snapshotVersion, yaml));
        }
    }

    void saveNow() {
        write(version.incrementAndGet(), snapshot());
    }

    private String snapshot() {
        var yaml = new YamlConfiguration();
        yaml.set(DISABLED, disabled.stream().map(UUID::toString).sorted().toList());
        return yaml.saveToString();
    }

    // The version check stops an async write that started earlier from overwriting a newer one.
    private void write(long snapshotVersion, String yaml) {
        synchronized (writeLock) {
            if (snapshotVersion <= writtenVersion) {
                return;
            }
            try {
                file.getParentFile().mkdirs();
                var temp = new File(file.getParentFile(), file.getName() + ".tmp").toPath();
                Files.writeString(temp, yaml, StandardCharsets.UTF_8);
                Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                writtenVersion = snapshotVersion;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getPath(), e);
            }
        }
    }
}
