package club.voidaris.core.kits;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Kits in {@code plugins/VoidarisCore/data/kits/<kit>.yml} and the last claim of each kit per player in
 * {@code data/kit-claims/<uuid>.yml}. Items are written with the server's item serialization, so every data component
 * (enchantments, names, lore, trims, potions...) is kept and upgraded when the server version changes.
 * Changes must come from the main thread; kit reads are also safe from command suggestions, which may run off it.
 */
final class KitStorage {

    private static final String COOLDOWN = "cooldown";
    private static final String PERMISSION = "permission";
    private static final String ICON = "icon";
    private static final String CREATED = "created";
    private static final String ITEMS = "items";

    private final JavaPlugin plugin;
    private final File kitsFolder;
    private final File claimsFolder;
    private final NavigableMap<String, Kit> kits = new ConcurrentSkipListMap<>();
    // Never evicted while the module is enabled, so a read can never reach a file whose queued write has not landed.
    private final Map<UUID, Map<String, Long>> claims = new HashMap<>();
    // A single thread keeps the writes of the same file in the order they were made.
    private ExecutorService writer;

    KitStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        var data = new File(plugin.getDataFolder(), "data");
        this.kitsFolder = new File(data, "kits");
        this.claimsFolder = new File(data, "kit-claims");
    }

    void open() {
        writer = Executors.newSingleThreadExecutor();
        loadKits();
    }

    void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Kit writes did not finish within 10 seconds.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        kits.clear();
        claims.clear();
    }

    static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    Collection<Kit> kits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    Optional<Kit> find(String name) {
        return Optional.ofNullable(kits.get(normalize(name)));
    }

    void put(Kit kit) {
        kits.put(kit.name(), kit);
        var yaml = new YamlConfiguration();
        yaml.set(COOLDOWN, kit.cooldownSeconds());
        yaml.set(PERMISSION, kit.permission());
        if (kit.icon() != null) {
            yaml.set(ICON, kit.icon());
        }
        yaml.set(CREATED, kit.created());
        kit.items().forEach((slot, item) -> yaml.set(ITEMS + "." + slot, item));
        // Serialized here because item stacks must not be read off the main thread.
        String content = yaml.saveToString();
        var file = new File(kitsFolder, kit.name() + ".yml");
        writer.execute(() -> write(file, content));
    }

    boolean remove(String name) {
        String key = normalize(name);
        if (kits.remove(key) == null) {
            return false;
        }
        var file = new File(kitsFolder, key + ".yml");
        writer.execute(() -> write(file, null));
        return true;
    }

    /**
     * Loads the player's file the first time, which is a small read on the calling thread.
     *
     * @return the time of the last claim of this kit, ignoring claims of a deleted kit that had the same name
     */
    OptionalLong lastClaim(UUID player, Kit kit) {
        Long time = loadedClaims(player).get(kit.name());
        return time == null || time < kit.created() ? OptionalLong.empty() : OptionalLong.of(time);
    }

    void claim(UUID player, Kit kit, long time) {
        var playerClaims = loadedClaims(player);
        playerClaims.put(kit.name(), time);
        saveClaims(player, playerClaims);
    }

    boolean resetClaim(UUID player, Kit kit) {
        if (lastClaim(player, kit).isEmpty()) {
            return false;
        }
        var playerClaims = loadedClaims(player);
        playerClaims.remove(kit.name());
        saveClaims(player, playerClaims);
        return true;
    }

    private void loadKits() {
        kits.clear();
        var files = kitsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = normalize(file.getName().substring(0, file.getName().length() - ".yml".length()));
            var yaml = YamlConfiguration.loadConfiguration(file);
            Map<Integer, ItemStack> items = new HashMap<>();
            var section = yaml.getConfigurationSection(ITEMS);
            if (section != null) {
                for (String slot : section.getKeys(false)) {
                    var item = section.getItemStack(slot);
                    Integer index = parseSlot(slot);
                    if (item == null || item.isEmpty() || index == null) {
                        plugin.getLogger().warning("Ignoring invalid item " + slot + " of kit " + name + " in "
                                + file.getPath());
                        continue;
                    }
                    items.put(index, item);
                }
            }
            kits.put(name, new Kit(name, yaml.getLong(COOLDOWN), yaml.getBoolean(PERMISSION), yaml.getString(ICON),
                    items, yaml.getLong(CREATED)));
        }
    }

    private static Integer parseSlot(String slot) {
        try {
            return Integer.parseInt(slot);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Long> loadedClaims(UUID player) {
        return claims.computeIfAbsent(player, this::loadClaims);
    }

    private Map<String, Long> loadClaims(UUID player) {
        Map<String, Long> playerClaims = new HashMap<>();
        var file = claimsFile(player);
        if (!file.exists()) {
            return playerClaims;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        for (String kit : yaml.getKeys(false)) {
            playerClaims.put(normalize(kit), yaml.getLong(kit));
        }
        return playerClaims;
    }

    private void saveClaims(UUID player, Map<String, Long> playerClaims) {
        var yaml = new YamlConfiguration();
        playerClaims.forEach(yaml::set);
        String content = playerClaims.isEmpty() ? null : yaml.saveToString();
        var file = claimsFile(player);
        writer.execute(() -> write(file, content));
    }

    private void write(File file, String content) {
        try {
            if (content == null) {
                Files.deleteIfExists(file.toPath());
                return;
            }
            file.getParentFile().mkdirs();
            var temp = new File(file.getParentFile(), file.getName() + ".tmp").toPath();
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getPath(), e);
        }
    }

    private File claimsFile(UUID player) {
        return new File(claimsFolder, player + ".yml");
    }
}
