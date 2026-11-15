package club.voidaris.core;

import club.voidaris.core.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class VoidarisCore extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.reload();
        getLogger().info("VoidarisCore " + getPluginMeta().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VoidarisCore deshabilitado.");
    }

    public ConfigManager configManager() {
        return configManager;
    }
}
