package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.module.Module;
import club.voidaris.core.storage.Database;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CosmeticsModule extends Module {

    static final String TELEPORT_PARTICLE_SLOT = "teleport-particle";
    static final String AURA_SLOT = "aura";
    static final String JOIN_EFFECT_SLOT = "join-effect";

    private final JavaPlugin plugin;
    private final Selections selections;
    private final Map<MessageCategory, MessageCosmetics> messageCosmetics = new EnumMap<>(MessageCategory.class);
    private final TeleportParticles teleportParticles;
    private final Auras auras;
    private final JoinEffects joinEffects;
    private final NamespacedKey joinEffectMarker;

    public CosmeticsModule(JavaPlugin plugin, ConfigManager configs, Database database) {
        super("cosmetics");
        this.plugin = plugin;
        this.selections = new Selections(database, plugin.getLogger());
        for (MessageCategory category : MessageCategory.values()) {
            messageCosmetics.put(category, new MessageCosmetics(configs, plugin.getLogger(), category));
        }
        this.teleportParticles = new TeleportParticles(configs, plugin.getLogger());
        this.auras = new Auras(configs, plugin.getLogger());
        this.joinEffects = new JoinEffects(configs, plugin.getLogger());
        this.joinEffectMarker = new NamespacedKey(plugin, "join_effect");
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new SelectionListener(selections, plugin.getLogger()), new AuraListener(this),
                new JoinEffectListener(plugin, this));
    }

    @Override
    protected void onEnable() {
        auras.load();
        joinEffects.load();
        registerPermissions();
        // Players already online when the module is toggled on never went through AsyncPlayerPreLoginEvent.
        Bukkit.getOnlinePlayers().forEach(player -> selections.load(player.getUniqueId()));
    }

    @Override
    protected void onReload() {
        auras.load();
        joinEffects.load();
        registerPermissions();
    }

    @Override
    protected void onDisable() {
        selections.clear();
        auras.clear();
        joinEffects.clear();
    }

    /**
     * The join message format the player has equipped. Empty when the module is disabled or the player owns none.
     */
    public Optional<String> joinMessage(Player player) {
        return equippedFormat(player, MessageCategory.JOIN);
    }

    /**
     * The kill message format the player has equipped. Empty when the module is disabled, the player owns none or
     * the equipped one keeps the translated death message.
     */
    public Optional<String> killMessage(Player player) {
        return equippedFormat(player, MessageCategory.KILL).filter(format -> !format.isBlank());
    }

    private Optional<String> equippedFormat(Player player, MessageCategory category) {
        var cosmetics = messageCosmetics.get(category);
        return equipped(player, category).flatMap(cosmetics::find).map(MessageCosmetic::format);
    }

    /**
     * The id of the player's pick, or of the default selection when they never picked one or no longer own it.
     */
    Optional<String> equipped(Player player, MessageCategory category) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        var cosmetics = messageCosmetics.get(category);
        return selections.get(player.getUniqueId(), category.slot)
                .flatMap(id -> cosmetics.usable(player, id))
                .or(() -> cosmetics.usable(player, cosmetics.defaultSelection()))
                .map(MessageCosmetic::id);
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
     * Same fallback as {@link #equipped(Player, MessageCategory)}.
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

    /**
     * The aura the player has equipped. Empty when the module is disabled, the player owns none or the equipped one
     * shows nothing.
     */
    public Optional<Aura> aura(Player player) {
        return equippedAura(player)
                .flatMap(auras::find)
                .filter(aura -> !aura.layers().isEmpty());
    }

    /**
     * Same fallback as {@link #equipped(Player, MessageCategory)}.
     */
    Optional<String> equippedAura(Player player) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return selections.get(player.getUniqueId(), AURA_SLOT)
                .flatMap(id -> auras.usable(player, id))
                .or(() -> auras.usable(player, auras.defaultSelection()))
                .map(Aura::id);
    }

    /**
     * The join effect the player has equipped. Empty when the module is disabled, the player owns none or the
     * equipped one shows nothing.
     */
    public Optional<JoinEffect> joinEffect(Player player) {
        return equippedJoinEffect(player)
                .flatMap(joinEffects::find)
                .filter(effect -> !effect.steps().isEmpty());
    }

    /**
     * Same fallback as {@link #equipped(Player, MessageCategory)}.
     */
    Optional<String> equippedJoinEffect(Player player) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return selections.get(player.getUniqueId(), JOIN_EFFECT_SLOT)
                .flatMap(id -> joinEffects.usable(player, id))
                .or(() -> joinEffects.usable(player, joinEffects.defaultSelection()))
                .map(JoinEffect::id);
    }

    JoinEffects joinEffects() {
        return joinEffects;
    }

    JoinEffectStep.Stage joinEffectStage() {
        return new JoinEffectStep.Stage(plugin, joinEffectMarker, joinEffects.range());
    }

    Auras auras() {
        return auras;
    }

    TeleportParticles teleportParticles() {
        return teleportParticles;
    }

    Selections selections() {
        return selections;
    }

    MessageCosmetics messageCosmetics(MessageCategory category) {
        return messageCosmetics.get(category);
    }

    // Nodes unknown to the server default to true for ops, which would unlock every cosmetic added to cosmetics.yml for ops.
    private void registerPermissions() {
        messageCosmetics.values().forEach(cosmetics -> registerPermissions(cosmetics.category().permission, cosmetics.ids()));
        registerPermissions(Keys.Permission.COSMETICS_TELEPORT_PARTICLE, teleportParticles.ids());
        registerPermissions(Keys.Permission.COSMETICS_AURA, auras.ids());
        registerPermissions(Keys.Permission.COSMETICS_JOIN_EFFECT, joinEffects.ids());
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
