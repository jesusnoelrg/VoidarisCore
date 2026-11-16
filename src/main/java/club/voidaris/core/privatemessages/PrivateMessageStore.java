package club.voidaris.core.privatemessages;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Ignore lists, spy toggles and /msgtoggle states persisted in {@code plugins/VoidarisCore/data/<name>}. Written from the main thread;
 * the maps are concurrent because the chat listener reads them from the async chat thread.
 */
final class PrivateMessageStore {

    record IgnoreEntry(String name, IgnoreLevel level) {
    }

    private static final String IGNORES = "ignores";
    private static final String SPIES = "spies";
    private static final String DISABLED = "disabled";

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<UUID, IgnoreEntry>> ignores = new ConcurrentHashMap<>();
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();
    private final Set<UUID> disabled = ConcurrentHashMap.newKeySet();
    private final AtomicLong version = new AtomicLong();
    private final Object writeLock = new Object();
    private long writtenVersion;
    // Volatile because a failed async write flags it again from the writer thread.
    private volatile boolean dirty;

    PrivateMessageStore(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.file = new File(new File(plugin.getDataFolder(), "data"), name);
    }

    void load() {
        ignores.clear();
        spies.clear();
        disabled.clear();
        dirty = false;
        if (!file.exists()) {
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection(IGNORES);
        if (section != null) {
            for (String owner : section.getKeys(false)) {
                UUID ownerId = parse(owner);
                var targets = section.getConfigurationSection(owner);
                if (ownerId == null || targets == null) {
                    continue;
                }
                for (String target : targets.getKeys(false)) {
                    UUID targetId = parse(target);
                    IgnoreLevel level = parseLevel(targets.getString(target + ".level"));
                    if (targetId != null && level != null) {
                        list(ownerId).put(targetId, new IgnoreEntry(targets.getString(target + ".name", target), level));
                    }
                }
            }
        }
        readIds(yaml.getStringList(SPIES), spies);
        readIds(yaml.getStringList(DISABLED), disabled);
    }

    /**
     * @return how {@code owner} ignores {@code target}, or {@code null} when it does not
     */
    IgnoreLevel level(UUID owner, UUID target) {
        var list = ignores.get(owner);
        if (list == null) {
            return null;
        }
        var entry = list.get(target);
        return entry != null ? entry.level() : null;
    }

    Map<UUID, IgnoreEntry> ignored(UUID owner) {
        var list = ignores.get(owner);
        return list != null ? Collections.unmodifiableMap(list) : Map.of();
    }

    void ignore(UUID owner, UUID target, String name, IgnoreLevel level) {
        list(owner).put(target, new IgnoreEntry(name, level));
        changed();
    }

    void unignore(UUID owner, UUID target) {
        var list = ignores.get(owner);
        if (list != null && list.remove(target) != null) {
            if (list.isEmpty()) {
                ignores.remove(owner);
            }
            changed();
        }
    }

    boolean spying(UUID player) {
        return spies.contains(player);
    }

    void spying(UUID player, boolean spying) {
        if (spying ? spies.add(player) : spies.remove(player)) {
            changed();
        }
    }

    /**
     * @return whether the player turned private messages off with /msgtoggle
     */
    boolean disabled(UUID player) {
        return disabled.contains(player);
    }

    void disabled(UUID player, boolean off) {
        if (off ? disabled.add(player) : disabled.remove(player)) {
            changed();
        }
    }

    void saveNow() {
        if (!dirty) {
            return;
        }
        var snapshot = snapshot();
        write(snapshot.version(), snapshot.yaml());
    }

    private Map<UUID, IgnoreEntry> list(UUID owner) {
        return ignores.computeIfAbsent(owner, id -> new ConcurrentHashMap<>());
    }

    // Changes are rare (a command each), so every one is written right away off the main thread.
    private void changed() {
        dirty = true;
        var snapshot = snapshot();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(snapshot.version(), snapshot.yaml()));
    }

    private record Snapshot(long version, String yaml) {
    }

    private Snapshot snapshot() {
        var yaml = new YamlConfiguration();
        ignores.forEach((owner, list) -> list.forEach((target, entry) -> {
            String path = IGNORES + "." + owner + "." + target;
            yaml.set(path + ".name", entry.name());
            yaml.set(path + ".level", entry.level().name());
        }));
        yaml.set(SPIES, spies.stream().map(UUID::toString).sorted().toList());
        yaml.set(DISABLED, disabled.stream().map(UUID::toString).sorted().toList());
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

    private void readIds(List<String> values, Set<UUID> into) {
        for (String value : values) {
            UUID id = parse(value);
            if (id != null) {
                into.add(id);
            }
        }
    }

    private UUID parse(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Ignoring invalid UUID in " + file.getName() + ": " + value);
            return null;
        }
    }

    private IgnoreLevel parseLevel(String value) {
        try {
            return value != null ? IgnoreLevel.valueOf(value) : null;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Ignoring invalid ignore level in " + file.getName() + ": " + value);
            return null;
        }
    }
}
