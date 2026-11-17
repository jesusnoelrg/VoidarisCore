package club.voidaris.core.integration;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.LegacyColors;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class Integrations {

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private LuckPermsHook luckPerms;
    private PlaceholderApiHook placeholderApi;

    public Integrations(JavaPlugin plugin, ConfigManager configs) {
        this.plugin = plugin;
        this.configs = configs;
    }

    public void reload() {
        luckPerms = isActive("luckperms", "LuckPerms") ? new LuckPermsHook() : null;
        placeholderApi = isActive("placeholderapi", "PlaceholderAPI") ? new PlaceholderApiHook() : null;
    }

    public Optional<String> primaryGroup(Player player) {
        return luckPerms == null ? Optional.empty() : Optional.ofNullable(luckPerms.primaryGroup(player));
    }

    public String prefix(Player player) {
        return luckPerms == null ? "" : LegacyColors.toMiniMessage(luckPerms.prefix(player));
    }

    public String suffix(Player player) {
        return luckPerms == null ? "" : LegacyColors.toMiniMessage(luckPerms.suffix(player));
    }

    public boolean hasPlaceholderApi() {
        return placeholderApi != null;
    }

    public String setPlaceholders(Player player, String text) {
        return placeholderApi == null ? text : placeholderApi.apply(player, text);
    }

    private boolean isActive(String key, String pluginName) {
        boolean active = configs.main().get().getBoolean("integrations." + key)
                && plugin.getServer().getPluginManager().isPluginEnabled(pluginName);
        if (active) {
            plugin.getLogger().info("Hooked into " + pluginName + ".");
        }
        return active;
    }
}
