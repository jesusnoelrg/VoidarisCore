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
import club.voidaris.core.motd.MotdCommand;
import club.voidaris.core.motd.MotdModule;
import club.voidaris.core.spawn.SpawnCommands;
import club.voidaris.core.spawn.SpawnModule;
import club.voidaris.core.storage.Database;
import club.voidaris.core.teleport.TeleportEffectsModule;
import club.voidaris.core.teleport.Teleporter;
import club.voidaris.core.utilities.GameModeCommands;
import club.voidaris.core.utilities.PlayerCommands;
import club.voidaris.core.utilities.UtilitiesModule;
import club.voidaris.core.utilities.VirtualInventoryCommands;
import club.voidaris.core.utilities.WorldCommands;
import club.voidaris.core.warps.WarpCommands;
import club.voidaris.core.warps.WarpsModule;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

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
    private UtilitiesModule utilitiesModule;
    private MotdModule motdModule;
    private BukkitTask autosaveTask;

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
        scheduleAutosave();

        getLogger().info("VoidarisCore " + getPluginMeta().getVersion() + " habilitado.");
    }

    @Override
    public void onDisable() {
        MenuListener.closeAll();
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (teleporter != null) {
            teleporter.cooldowns().saveNow();
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
        scheduleAutosave();
    }

    private void scheduleAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        long ticks = Math.max(1, configManager.main().get().getLong("storage.autosave-interval-seconds")) * 20L;
        autosaveTask = getServer().getScheduler().runTaskTimer(this, teleporter.cooldowns()::saveAsync, ticks, ticks);
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
        teleporter.cooldowns().load();
        getServer().getPluginManager().registerEvents(teleporter, this);
        spawnModule = new SpawnModule(this, configManager, messages);
        moduleManager.register(spawnModule);
        warpsModule = new WarpsModule(this, configManager);
        moduleManager.register(warpsModule);
        moduleManager.register(new TeleportEffectsModule(this, configManager, cosmeticsModule));
        utilitiesModule = new UtilitiesModule();
        moduleManager.register(utilitiesModule);
        motdModule = new MotdModule(configManager, formatter);
        moduleManager.register(motdModule);
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
        var virtualInventoryCommands = new VirtualInventoryCommands(utilitiesModule, messages);
        voidarisCommand.help().register(virtualInventoryCommands.helpEntries());
        var gameModeCommands = new GameModeCommands(utilitiesModule, messages);
        voidarisCommand.help().register(gameModeCommands.helpEntries());
        var playerCommands = new PlayerCommands(utilitiesModule, messages);
        voidarisCommand.help().register(playerCommands.helpEntries());
        var worldCommands = new WorldCommands(utilitiesModule, messages);
        voidarisCommand.help().register(worldCommands.helpEntries());
        var motdCommand = new MotdCommand(motdModule, messages);
        voidarisCommand.help().register(motdCommand.helpEntries());

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var registrar = event.registrar();
            registrar.register(voidarisCommand.build(), messages.raw(Keys.Message.COMMAND_VOIDARIS_DESCRIPTION));
            registrar.register(cosmeticsCommand.build(), messages.raw(Keys.Message.COMMAND_COSMETICS_DESCRIPTION),
                    List.of("cosmeticos"));
            spawnCommands.register(registrar);
            warpCommands.register(registrar);
            virtualInventoryCommands.register(registrar);
            gameModeCommands.register(registrar);
            playerCommands.register(registrar);
            worldCommands.register(registrar);
            motdCommand.register(registrar);
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
