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
import java.util.Set;

public final class CosmeticsModule extends Module {

    static final String JOIN_MESSAGE_SLOT = "join-message";
    static final String TELEPORT_PARTICLE_SLOT = "teleport-particle";

    private final JavaPlugin plugin;
    private final Selections selections;
    private final JoinMessages joinMessages;
    private final TeleportParticles teleportParticles;

    public CosmeticsModule(JavaPlugin plugin, ConfigManager configs, Database database) {
        super("cosmetics");
        this.plugin = plugin;
        this.selections = new Selections(database, plugin.getLogger());
        this.joinMessages = new JoinMessages(configs, plugin.getLogger());
        this.teleportParticles = new TeleportParticles(configs, plugin.getLogger());
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new SelectionListener(selections, plugin.getLogger()));
    }

    @Override
    protected void onEnable() {
        registerPermissions();
        // Players already online when the module is toggled on never went through AsyncPlayerPreLoginEvent.
        Bukkit.getOnlinePlayers().forEach(player -> selections.load(player.getUniqueId()));
    }

    @Override
    protected void onReload() {
        registerPermissions();
    }

    @Override
    protected void onDisable() {
        selections.clear();
    }

    /**
     * The join message format the player has equipped. Empty when the module is disabled or the player owns none.
     */
    public Optional<String> joinMessage(Player player) {
        return equippedJoinMessage(player)
                .flatMap(joinMessages::find)
                .map(JoinMessage::format);
    }

    /**
     * The id of the player's pick, or of the default selection when they never picked one or no longer own it.
     */
    Optional<String> equippedJoinMessage(Player player) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return selections.get(player.getUniqueId(), JOIN_MESSAGE_SLOT)
                .flatMap(id -> joinMessages.usable(player, id))
                .or(() -> joinMessages.usable(player, joinMessages.defaultSelection()))
                .map(JoinMessage::id);
    }

    /**
     * The teleport particle the player has equipped. Empty when the module is disabled, the player owns none or
     * the equipped one shows nothing.
     */
    public Optional<TeleportParticle> teleportParticle(Player player) {
        return equippedTeleportParticle(player)
                .flatMap(teleportParticles::find)
                .filter(particle -> !particle.layers().isEmpty());
    }

    /**
     * Same fallback as {@link #equippedJoinMessage(Player)}.
     */
    Optional<String> equippedTeleportParticle(Player player) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return selections.get(player.getUniqueId(), TELEPORT_PARTICLE_SLOT)
                .flatMap(id -> teleportParticles.usable(player, id))
                .or(() -> teleportParticles.usable(player, teleportParticles.defaultSelection()))
                .map(TeleportParticle::id);
    }

    TeleportParticles teleportParticles() {
        return teleportParticles;
    }

    Selections selections() {
        return selections;
    }

    JoinMessages joinMessages() {
        return joinMessages;
    }

    // Nodes unknown to the server default to true for ops, which would unlock every cosmetic added to cosmetics.yml for ops.
    private void registerPermissions() {
        registerPermissions(Keys.Permission.COSMETICS_JOIN_MESSAGE, joinMessages.ids());
        registerPermissions(Keys.Permission.COSMETICS_TELEPORT_PARTICLE, teleportParticles.ids());
        // Parsed eagerly only to log config mistakes on enable/reload instead of on the first teleport.
        long invalid = teleportParticles.ids().stream().filter(id -> teleportParticles.find(id).isEmpty()).count();
        plugin.getLogger().info("Loaded " + (teleportParticles.ids().size() - invalid) + " teleport particle cosmetics"
                + (invalid > 0 ? " (" + invalid + " invalid)." : "."));
    }

    private static void registerPermissions(String prefix, Set<String> ids) {
        var pluginManager = Bukkit.getPluginManager();
        for (String id : ids) {
            String node = prefix + id;
            if (pluginManager.getPermission(node) == null) {
                pluginManager.addPermission(new Permission(node, PermissionDefault.FALSE));
            }
        }
    }
}
