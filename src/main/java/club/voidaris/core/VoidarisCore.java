package club.voidaris.core;

import club.voidaris.core.command.VoidarisCommand;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.cosmetics.CosmeticsCommand;
import club.voidaris.core.cosmetics.CosmeticsModule;
import club.voidaris.core.gui.MenuListener;
import club.voidaris.core.gui.MenuLoader;
import club.voidaris.core.integration.Integrations;
import club.voidaris.core.joinquit.JoinQuitModule;
import club.voidaris.core.message.Messages;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.ModuleManager;
import club.voidaris.core.spawn.SpawnCommands;
import club.voidaris.core.spawn.SpawnModule;
import club.voidaris.core.storage.Database;
import club.voidaris.core.teleport.TeleportEffectsModule;
import club.voidaris.core.teleport.Teleporter;
import club.voidaris.core.warps.WarpCommands;
import club.voidaris.core.warps.WarpsModule;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class VoidarisCore extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private Integrations integrations;
    private Database database;
    private ModuleManager moduleManager;
    private PlayerFormatter formatter;
    private CosmeticsModule cosmeticsModule;
    private Teleporter teleporter;
    private SpawnModule spawnModule;
    private WarpsModule warpsModule;

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
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        registerCommands();

        getLogger().info("VoidarisCore " + getPluginMeta().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        MenuListener.closeAll();
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
        formatter = new PlayerFormatter(messages, integrations);
        cosmeticsModule = new CosmeticsModule(this, configManager, database);
        moduleManager.register(cosmeticsModule);
        moduleManager.register(new JoinQuitModule(configManager, formatter, integrations, cosmeticsModule));
        teleporter = new Teleporter(this, messages);
        getServer().getPluginManager().registerEvents(teleporter, this);
        spawnModule = new SpawnModule(this, configManager, messages);
        moduleManager.register(spawnModule);
        warpsModule = new WarpsModule(this, configManager);
        moduleManager.register(warpsModule);
        moduleManager.register(new TeleportEffectsModule(this, configManager, cosmeticsModule));
    }

    private void registerCommands() {
        var voidarisCommand = new VoidarisCommand(this::reload, messages);
        var menuLoader = new MenuLoader(formatter, getLogger());
        var cosmeticsCommand = new CosmeticsCommand(this, configManager, cosmeticsModule, messages, formatter, menuLoader);
        voidarisCommand.help().register(cosmeticsCommand.helpEntries());
        var spawnCommands = new SpawnCommands(spawnModule, messages, teleporter);
        voidarisCommand.help().register(spawnCommands.helpEntries());
        var warpCommands = new WarpCommands(warpsModule, messages, teleporter);
        voidarisCommand.help().register(warpCommands.helpEntries());

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var registrar = event.registrar();
            registrar.register(voidarisCommand.build(), messages.raw(Keys.Message.COMMAND_VOIDARIS_DESCRIPTION));
            registrar.register(cosmeticsCommand.build(), messages.raw(Keys.Message.COMMAND_COSMETICS_DESCRIPTION),
                    List.of("cosmeticos"));
            spawnCommands.register(registrar);
            warpCommands.register(registrar);
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
