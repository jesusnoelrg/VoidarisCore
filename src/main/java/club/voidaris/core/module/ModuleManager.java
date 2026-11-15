package club.voidaris.core.module;

import club.voidaris.core.config.ConfigManager;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class ModuleManager {

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Map<Module, List<Listener>> activeListeners = new HashMap<>();

    public ModuleManager(JavaPlugin plugin, ConfigManager configs) {
        this.plugin = plugin;
        this.configs = configs;
    }

    public void register(Module module) {
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalStateException("Module already registered: " + module.id());
        }
    }

    /**
     * Applies the toggles in config.yml: enables, disables or reloads each module as needed.
     */
    public void reload() {
        for (Module module : modules.values()) {
            boolean wanted = configs.main().get().getBoolean("modules." + module.id());
            if (wanted && module.isEnabled()) {
                run(module, "reload", module::onReload);
            } else if (wanted) {
                enable(module);
            } else if (module.isEnabled()) {
                disable(module);
            }
        }
    }

    public void disableAll() {
        // Reverse order so modules enabled later (which may depend on earlier ones) shut down first.
        var enabled = new ArrayList<>(modules.values());
        for (int i = enabled.size() - 1; i >= 0; i--) {
            if (enabled.get(i).isEnabled()) {
                disable(enabled.get(i));
            }
        }
    }

    public boolean isEnabled(String id) {
        Module module = modules.get(id);
        return module != null && module.isEnabled();
    }

    private void enable(Module module) {
        // A broken module must not take the rest of the plugin down with it, so it is left disabled and logged.
        if (!run(module, "enable", module::onEnable)) {
            return;
        }
        List<Listener> listeners = List.copyOf(module.listeners());
        listeners.forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));
        activeListeners.put(module, listeners);
        module.setEnabled(true);
    }

    private void disable(Module module) {
        List<Listener> listeners = activeListeners.remove(module);
        if (listeners != null) {
            listeners.forEach(HandlerList::unregisterAll);
        }
        module.setEnabled(false);
        run(module, "disable", module::onDisable);
    }

    private boolean run(Module module, String phase, Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to " + phase + " module " + module.id(), e);
            return false;
        }
    }
}
