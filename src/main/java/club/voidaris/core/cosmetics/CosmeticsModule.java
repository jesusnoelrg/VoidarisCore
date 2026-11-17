package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.LegacyColors;
import club.voidaris.core.module.Module;
import club.voidaris.core.storage.Database;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
    /** Stored in the badge slot when the player wears the badge they wrote, which lives in {@link #CUSTOM_BADGE_SLOT}. */
    static final String CUSTOM_BADGE_ID = "custom";
    static final String CUSTOM_BADGE_SLOT = "custom-badge";
    /** Size of cosmetic_selections.cosmetic_id. */
    static final int MAX_STORED_LENGTH = 64;

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final Selections selections;
    private final Map<MessageCategory, MessageCosmetics> messageCosmetics = new EnumMap<>(MessageCategory.class);
    private final TeleportParticles teleportParticles;
    private final Auras auras;
    private final JoinEffects joinEffects;
    private final Map<ColorCategory, ColorCosmetics> colors = new EnumMap<>(ColorCategory.class);
    private final NamespacedKey joinEffectMarker;

    public CosmeticsModule(JavaPlugin plugin, ConfigManager configs, Database database) {
        super("cosmetics");
        this.plugin = plugin;
        this.configs = configs;
        this.selections = new Selections(database, plugin.getLogger());
        for (MessageCategory category : MessageCategory.values()) {
            messageCosmetics.put(category, new MessageCosmetics(configs, plugin.getLogger(), category));
        }
        this.teleportParticles = new TeleportParticles(configs, plugin.getLogger());
        this.auras = new Auras(configs, plugin.getLogger());
        this.joinEffects = new JoinEffects(configs, plugin.getLogger());
        for (ColorCategory category : ColorCategory.values()) {
            colors.put(category, new ColorCosmetics(configs, plugin.getLogger(), category));
        }
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
        colors.values().forEach(ColorCosmetics::load);
        registerPermissions();
        // Players already online when the module is toggled on never went through AsyncPlayerPreLoginEvent.
        Bukkit.getOnlinePlayers().forEach(player -> selections.load(player.getUniqueId()));
    }

    @Override
    protected void onReload() {
        auras.load();
        joinEffects.load();
        colors.values().forEach(ColorCosmetics::load);
        registerPermissions();
    }

    @Override
    protected void onDisable() {
        selections.clear();
        auras.clear();
        joinEffects.clear();
        colors.values().forEach(ColorCosmetics::clear);
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
        if (category == MessageCategory.BADGE && customBadgeEquipped(player)) {
            return Optional.of(CUSTOM_BADGE_ID);
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

    /**
     * MiniMessage tag(s) to put before the player's chat messages. Empty when the module is disabled or the
     * equipped color keeps the chat format's own color.
     */
    public Optional<String> chatColor(Player player) {
        return color(player, ColorCategory.CHAT);
    }

    /**
     * The player's name in their equipped nick color, or uncolored so the surrounding format colors it.
     */
    public Component nickname(Player player) {
        return color(player, ColorCategory.NICK)
                .map(color -> MINI_MESSAGE.deserialize(color + MINI_MESSAGE.escapeTags(player.getName())))
                .orElseGet(() -> Component.text(player.getName()));
    }

    /**
     * The player's equipped badge followed by badges.separator, or empty when they have none.
     */
    public Component badge(Player player) {
        if (customBadgeEquipped(player)) {
            return customBadge(player).map(text -> withSeparator(customBadgeContent(text))).orElse(Component.empty());
        }
        return equippedFormat(player, MessageCategory.BADGE).map(this::renderBadge).orElse(Component.empty());
    }

    /**
     * A badge format as it is shown, or empty for an empty format (such as none).
     */
    Component renderBadge(String format) {
        return format.isBlank() ? Component.empty() : withSeparator(MINI_MESSAGE.deserialize(format));
    }

    Component withSeparator(Component badge) {
        // Siblings rather than badge.append, so an unclosed style of the badge (such as obfuscated) does not reach it.
        return Component.textOfChildren(badge,
                MINI_MESSAGE.deserialize(configs.cosmetics().get().getString("badges.separator", " ")));
    }

    /**
     * The badge the player wrote, kept even while they wear another one. Empty without the permission.
     */
    Optional<String> customBadge(Player player) {
        return player.hasPermission(Keys.Permission.COSMETICS_CUSTOM_BADGE)
                ? selections.get(player.getUniqueId(), CUSTOM_BADGE_SLOT)
                : Optional.empty();
    }

    boolean customBadgeEquipped(Player player) {
        return isEnabled() && selections.get(player.getUniqueId(), MessageCategory.BADGE.slot)
                .filter(CUSTOM_BADGE_ID::equals).isPresent() && customBadge(player).isPresent();
    }

    int customBadgeMaxLength() {
        return Math.max(1, configs.cosmetics().get().getInt("badges.custom.max-length", 5));
    }

    // Players type & codes as in chat; any MiniMessage tag they type is shown as text.
    static Component customBadgeContent(String text) {
        return MINI_MESSAGE.deserialize(LegacyColors.toMiniMessage(MINI_MESSAGE.escapeTags(text)));
    }

    /**
     * Characters shown in chat, so color and format codes do not count towards the limit.
     */
    static int visibleLength(String text) {
        String plain = PlainTextComponentSerializer.plainText().serialize(customBadgeContent(text));
        return plain.codePointCount(0, plain.length());
    }

    /**
     * {@code <nickname>} and {@code <badge>}, available in every format rendered for the player.
     */
    public TagResolver placeholders(Player player) {
        return TagResolver.resolver(
                Placeholder.component("nickname", nickname(player)),
                Placeholder.component("badge", badge(player)));
    }

    private Optional<String> color(Player player, ColorCategory category) {
        return equippedColor(player, category)
                .map(id -> ColorCosmetics.isCustom(id) ? "<" + id + ">"
                        : colors.get(category).find(id).map(ColorCosmetic::color).orElse(""))
                .filter(color -> !color.isEmpty());
    }

    /**
     * Same fallback as {@link #equipped(Player, MessageCategory)}; a custom pick is its {@code #rrggbb} color.
     */
    Optional<String> equippedColor(Player player, ColorCategory category) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        var cosmetics = colors.get(category);
        return selections.get(player.getUniqueId(), category.slot)
                .flatMap(id -> ColorCosmetics.isCustom(id)
                        ? Optional.of(id).filter(custom -> player.hasPermission(category.customPermission))
                        : cosmetics.usable(player, id).map(ColorCosmetic::id))
                .or(() -> cosmetics.usable(player, cosmetics.defaultSelection()).map(ColorCosmetic::id));
    }

    ColorCosmetics colors(ColorCategory category) {
        return colors.get(category);
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
        colors.values().forEach(cosmetics -> registerPermissions(cosmetics.category().permission, cosmetics.ids()));
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
