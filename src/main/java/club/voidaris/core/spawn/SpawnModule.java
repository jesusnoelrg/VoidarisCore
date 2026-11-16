package club.voidaris.core.spawn;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.module.Module;
import club.voidaris.core.storage.LocationFile;
import club.voidaris.core.storage.StoredLocation;
import club.voidaris.core.teleport.Teleporter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class SpawnModule extends Module {

    private static final String SPAWN = "spawn";

    private final ConfigManager configs;
    private final LocationFile data;

    public SpawnModule(JavaPlugin plugin, ConfigManager configs) {
        super("spawn");
        this.configs = configs;
        this.data = new LocationFile(plugin, "spawn.yml");
    }

    @Override
    protected void onEnable() {
        data.load();
    }

    public Optional<StoredLocation> spawn() {
        return data.get(SPAWN);
    }

    public void setSpawn(StoredLocation location) {
        data.put(SPAWN, location);
    }

    public boolean deleteSpawn() {
        return data.remove(SPAWN);
    }

    public Teleporter.Settings teleportSettings() {
        var teleports = configs.teleports().get();
        return Teleporter.Settings.of(teleports.getConfigurationSection("spawn"),
                teleports.getConfigurationSection("defaults"));
    }
}
