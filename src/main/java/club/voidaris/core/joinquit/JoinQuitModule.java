package club.voidaris.core.joinquit;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public final class JoinQuitModule extends Module {

    private final ConfigManager configs;
    private final PlayerFormatter formatter;

    public JoinQuitModule(ConfigManager configs, PlayerFormatter formatter) {
        super("join-quit");
        this.configs = configs;
        this.formatter = formatter;
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new JoinQuitListener(configs, formatter));
    }

    @Override
    protected void onEnable() {
        registerFormatPermissions();
    }

    @Override
    protected void onReload() {
        registerFormatPermissions();
    }

    // Nodes unknown to the server default to true for ops, which would give every op the highest-priority format.
    private void registerFormatPermissions() {
        var formats = configs.chatFormat().get().getConfigurationSection(JoinQuitListener.FORMATS);
        if (formats == null) {
            return;
        }
        var pluginManager = Bukkit.getPluginManager();
        for (String name : formats.getKeys(false)) {
            String node = Keys.Permission.JOIN_QUIT_FORMAT + name;
            if (pluginManager.getPermission(node) == null) {
                pluginManager.addPermission(new Permission(node, PermissionDefault.FALSE));
            }
        }
    }
}
