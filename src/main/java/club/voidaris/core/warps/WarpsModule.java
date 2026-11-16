package club.voidaris.core.warps;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.module.Module;
import club.voidaris.core.storage.LocationFile;
import club.voidaris.core.storage.StoredLocation;
import club.voidaris.core.teleport.Teleporter;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class WarpsModule extends Module {

    private final ConfigManager configs;
    private final LocationFile data;
    private Pattern namePattern;

    public WarpsModule(JavaPlugin plugin, ConfigManager configs) {
        super("warps");
        this.configs = configs;
        this.data = new LocationFile(plugin, "warps.yml");
    }

    @Override
    protected void onEnable() {
        data.load();
        compileNamePattern();
    }

    @Override
    protected void onReload() {
        compileNamePattern();
    }

    private void compileNamePattern() {
        namePattern = Pattern.compile(configs.warps().get().getString("name-pattern"));
    }

    public Optional<StoredLocation> find(String name) {
        return data.get(name);
    }

    public Collection<String> names() {
        return data.all().keySet();
    }

    public boolean isValidName(String name) {
        return namePattern.matcher(name).matches();
    }

    public void set(String name, StoredLocation location) {
        data.put(name, location);
    }

    public boolean delete(String name) {
        return data.remove(name);
    }

    public boolean canUse(Permissible permissible, String name) {
        return configs.warps().get().getBoolean("per-warp-permission")
                ? permissible.hasPermission(Keys.Permission.WARPS_USE_WARP + name.toLowerCase(Locale.ROOT))
                : permissible.hasPermission(Keys.Permission.WARPS_USE);
    }

    public Teleporter.Settings teleportSettings() {
        return Teleporter.Settings.of(configs.warps().get(),
                configs.teleports().get().getConfigurationSection("defaults"));
    }
}
