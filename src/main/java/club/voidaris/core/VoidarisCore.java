package club.voidaris.core;

import club.voidaris.core.command.VoidarisCommand;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.integration.Integrations;
import club.voidaris.core.joinquit.JoinQuitModule;
import club.voidaris.core.message.Messages;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.ModuleManager;
import club.voidaris.core.storage.Database;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class VoidarisCore extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private Integrations integrations;
    private Database database;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.reload();
        messages = new Messages(configManager);
        integrations = new Integrations(this, configManager);
        integrations.reload();
        connectDatabase();
        moduleManager = new ModuleManager(this, configManager);

        registerModules();
        moduleManager.reload();
        registerCommands();

        getLogger().info("VoidarisCore " + getPluginMeta().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("VoidarisCore deshabilitado.");
    }

    public void reload() {
        configManager.reload();
        integrations.reload();
        moduleManager.reload();
    }

    private void connectDatabase() {
        database = new Database(this);
        var settings = configManager.database().get();
        if (!settings.getBoolean("enabled")) {
            return;
        }
        try {
            database.connect(settings);
            getLogger().info("Connected to the database.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not connect to the database; features that need it will be unavailable.", e);
        }
    }

    private void registerModules() {
        var formatter = new PlayerFormatter(messages, integrations);
        moduleManager.register(new JoinQuitModule(configManager, formatter, integrations));
    }

    private void registerCommands() {
        var voidarisCommand = new VoidarisCommand(this::reload, messages);

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

    public Integrations integrations() {
        return integrations;
    }

    public Database database() {
        return database;
    }

    public ModuleManager moduleManager() {
        return moduleManager;
    }
}
