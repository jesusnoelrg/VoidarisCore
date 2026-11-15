package club.voidaris.core;

import org.bukkit.plugin.java.JavaPlugin;

public final class VoidarisCore extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("VoidarisCore " + getPluginMeta().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VoidarisCore deshabilitado.");
    }
}
