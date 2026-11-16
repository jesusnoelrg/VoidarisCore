package club.voidaris.core.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Per-player cooldowns persisted in {@code plugins/VoidarisCore/data/<name>} as end timestamps, so they keep
 * counting while the server is down. Must be used from the main thread.
 */
public final class CooldownFile {

    private record Key(UUID player, String type) {
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<Key, Long> ends = new HashMap<>();
    private final AtomicLong version = new AtomicLong();
    private final Object writeLock = new Object();
    private long writtenVersion;
    // Volatile because a failed async write flags it again from the writer thread.
    private volatile boolean dirty;

    public CooldownFile(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.file = new File(new File(plugin.getDataFolder(), "data"), name);
    }

    public void load() {
        ends.clear();
        if (!file.exists()) {
            return;
        }
        long now = System.currentTimeMillis();
        var yaml = YamlConfiguration.loadConfiguration(file);
        for (String player : yaml.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(player);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring invalid UUID in " + file.getName() + ": " + player);
                continue;
            }
            var section = yaml.getConfigurationSection(player);
            if (section == null) {
                continue;
            }
            for (String type : section.getKeys(false)) {
                long end = section.getLong(type);
                if (end > now) {
                    ends.put(new Key(uuid, type), end);
                }
            }
        }
    }

    /**
     * @return milliseconds left, or 0 when the cooldown is over
     */
    public long remaining(UUID player, String type) {
        var key = new Key(player, type);
        Long end = ends.get(key);
        if (end == null) {
            return 0;
        }
        long left = end - System.currentTimeMillis();
        if (left <= 0) {
            ends.remove(key);
            dirty = true;
            return 0;
        }
        return left;
    }

    public void start(UUID player, String type, long durationMillis) {
        ends.put(new Key(player, type), System.currentTimeMillis() + durationMillis);
        dirty = true;
    }

    /**
     * Snapshots on the calling (main) thread and writes off it; skipped when nothing changed.
     */
    public void saveAsync() {
        if (!dirty) {
            return;
        }
        var snapshot = snapshot();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(snapshot.version(), snapshot.yaml()));
    }

    public void saveNow() {
        if (!dirty) {
            return;
        }
        var snapshot = snapshot();
        write(snapshot.version(), snapshot.yaml());
    }

    private record Snapshot(long version, String yaml) {
    }

    private Snapshot snapshot() {
        long now = System.currentTimeMillis();
        ends.values().removeIf(end -> end <= now);
        var yaml = new YamlConfiguration();
        ends.forEach((key, end) -> yaml.set(key.player() + "." + key.type(), end));
        dirty = false;
        return new Snapshot(version.incrementAndGet(), yaml.saveToString());
    }

    // The version check stops an async write that started earlier from overwriting the final save on shutdown.
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
                dirty = true;
                plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getPath(), e);
            }
        }
    }
}
