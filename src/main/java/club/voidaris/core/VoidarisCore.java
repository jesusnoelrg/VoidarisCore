package club.voidaris.core;

import club.voidaris.core.command.VoidarisCommand;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.Messages;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class VoidarisCore extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.reload();
        messages = new Messages(configManager);

        registerCommands();

        getLogger().info("VoidarisCore " + getPluginMeta().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VoidarisCore deshabilitado.");
    }

    private void registerCommands() {
        var voidarisCommand = new VoidarisCommand(configManager, messages);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var registrar = event.registrar();
            registrar.register(voidarisCommand.build(), messages.raw(Keys.Message.COMMAND_VOIDARIS_DESCRIPTION));
        });
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public Messages messages() {
        return messages;
    }
}
