package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.module.Module;
import club.voidaris.core.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

public final class CosmeticsModule extends Module {

    static final String JOIN_MESSAGE_SLOT = "join-message";

    private final JavaPlugin plugin;
    private final Selections selections;
    private final JoinMessages joinMessages;

    public CosmeticsModule(JavaPlugin plugin, ConfigManager configs, Database database) {
        super("cosmetics");
        this.plugin = plugin;
        this.selections = new Selections(database, plugin.getLogger());
        this.joinMessages = new JoinMessages(configs, plugin.getLogger());
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new SelectionListener(selections, plugin.getLogger()));
    }

    @Override
    protected void onEnable() {
        registerJoinMessagePermissions();
        // Players already online when the module is toggled on never went through AsyncPlayerPreLoginEvent.
        Bukkit.getOnlinePlayers().forEach(player -> selections.load(player.getUniqueId()));
    }

    @Override
    protected void onReload() {
        registerJoinMessagePermissions();
    }

    @Override
    protected void onDisable() {
        selections.clear();
    }

    /**
     * The join message format the player has equipped, falling back to the default selection when they never picked one
     * or no longer own it. Empty when the module is disabled or the player owns neither.
     */
    public Optional<String> joinMessage(Player player) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return selections.get(player.getUniqueId(), JOIN_MESSAGE_SLOT)
                .flatMap(id -> joinMessages.usable(player, id))
                .or(() -> joinMessages.usable(player, joinMessages.defaultSelection()))
                .map(JoinMessage::format);
    }

    Selections selections() {
        return selections;
    }

    JoinMessages joinMessages() {
        return joinMessages;
    }

    // Nodes unknown to the server default to true for ops, which would unlock every cosmetic added to cosmetics.yml for ops.
    private void registerJoinMessagePermissions() {
        var pluginManager = Bukkit.getPluginManager();
        for (String id : joinMessages.ids()) {
            String node = Keys.Permission.COSMETICS_JOIN_MESSAGE + id;
            if (pluginManager.getPermission(node) == null) {
                pluginManager.addPermission(new Permission(node, PermissionDefault.FALSE));
            }
        }
    }
}
