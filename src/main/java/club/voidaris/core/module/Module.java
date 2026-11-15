package club.voidaris.core.module;

import org.bukkit.event.Listener;

import java.util.List;

public abstract class Module {

    private final String id;
    private boolean enabled;

    /**
     * @param id the key of this module under {@code modules:} in config.yml
     */
    protected Module(String id) {
        this.id = id;
    }

    public final String id() {
        return id;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Called on every enable, so listeners are created fresh and can read the current settings.
     */
    protected List<Listener> listeners() {
        return List.of();
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /**
     * Called on {@code /voidaris reload} when the module stays enabled, so runtime state survives the reload.
     */
    protected void onReload() {
    }
}
