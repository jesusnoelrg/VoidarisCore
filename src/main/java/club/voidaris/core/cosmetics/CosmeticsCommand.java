package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.command.CommandUsage;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.gui.MenuLoader;
import club.voidaris.core.message.Messages;
import club.voidaris.core.message.PlayerFormatter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class CosmeticsCommand {

    private static final String NAME = "cosmetics";
    private static final String JOIN_MESSAGE = "joinmessage";
    private static final String KILL_MESSAGE = "killmessage";
    private static final String TELEPORT_PARTICLE = "teleportparticle";
    private static final String AURA = "aura";
    private static final String JOIN_EFFECT = "joineffect";
    private static final String BADGE = "badge";
    private static final String BADGES = "badges";
    private static final String CUSTOM = "custom";
    private static final String TEXT_ARGUMENT = "text";
    private static final String ARGUMENT = "cosmetic";
    private static final String COLOR_ARGUMENT = "color";

    private final CosmeticsModule module;
    private final Messages messages;
    private final ConfigManager configs;
    private final MenuLoader loader;
    private final MessageCosmeticsMenu joinMessagesMenu;
    private final MessageCosmeticsMenu killMessagesMenu;
    private final BadgesMenu badgesMenu;
    private final TeleportParticlesMenu particlesMenu;
    private final AurasMenu aurasMenu;
    private final JoinEffectsMenu joinEffectsMenu;
    private final Map<ColorCategory, ColorCosmeticsMenu> colorMenus = new EnumMap<>(ColorCategory.class);

    public CosmeticsCommand(JavaPlugin plugin, ConfigManager configs, CosmeticsModule module, Messages messages,
                            PlayerFormatter formatter, MenuLoader loader) {
        this.module = module;
        this.messages = messages;
        this.configs = configs;
        this.loader = loader;
        this.joinMessagesMenu = new MessageCosmeticsMenu(plugin, configs, module, messages, loader,
                MessageCategory.JOIN, formatter::format);
        // The preview shows the player as the killer of a sample victim, with the item they are holding.
        this.killMessagesMenu = new MessageCosmeticsMenu(plugin, configs, module, messages, loader,
                MessageCategory.KILL, (player, format) -> formatter.format(player, format,
                        KillMessagePlaceholders.of(messages, player,
                                messages.get(Keys.Message.COSMETICS_KILL_MESSAGE_PREVIEW_VICTIM))));
        // <preview_badge> rather than <badge>, which the formatter already resolves to the badge the player has equipped.
        var badgeListMenu = new MessageCosmeticsMenu(plugin, configs, module, messages, loader,
                MessageCategory.BADGE, (player, format) -> formatter.format(player,
                        messages.raw(Keys.Message.COSMETICS_BADGE_PREVIEW),
                        Placeholder.component("preview_badge", module.renderBadge(format))));
        this.badgesMenu = new BadgesMenu(plugin, configs, module, messages, loader, badgeListMenu);
        this.particlesMenu = new TeleportParticlesMenu(plugin, configs, module, messages, loader);
        this.aurasMenu = new AurasMenu(plugin, configs, module, messages, loader);
        this.joinEffectsMenu = new JoinEffectsMenu(plugin, configs, module, messages, loader);
        for (ColorCategory category : ColorCategory.values()) {
            colorMenus.put(category, new ColorCosmeticsMenu(plugin, configs, module, messages, loader, category));
        }
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(NAME)
                .executes(this::openHub)
                .then(messageLiteral(JOIN_MESSAGE, joinMessagesMenu, MessageCategory.JOIN))
                .then(messageLiteral(KILL_MESSAGE, killMessagesMenu, MessageCategory.KILL))
                .then(badgeLiteral(BADGE, "/" + NAME + " " + BADGE))
                .then(Commands.literal(TELEPORT_PARTICLE)
                        .executes(this::openTeleportParticles)
                        .then(Commands.argument(ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    if (module.isEnabled() && context.getSource().getSender() instanceof Player player) {
                                        module.teleportParticles().usable(player).forEach(particle -> builder.suggest(particle.id()));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(this::selectTeleportParticle)))
                .then(Commands.literal(AURA)
                        .executes(this::openAuras)
                        .then(Commands.argument(ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    if (module.isEnabled() && context.getSource().getSender() instanceof Player player) {
                                        module.auras().usable(player).forEach(aura -> builder.suggest(aura.id()));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(this::selectAura)))
                .then(Commands.literal(JOIN_EFFECT)
                        .executes(this::openJoinEffects)
                        .then(Commands.argument(ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    if (module.isEnabled() && context.getSource().getSender() instanceof Player player) {
                                        module.joinEffects().usable(player).forEach(effect -> builder.suggest(effect.id()));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(this::selectJoinEffect)))
                .then(colorLiteral(ColorCategory.CHAT, "/" + NAME + " " + ColorCategory.CHAT.command))
                .then(colorLiteral(ColorCategory.NICK, "/" + NAME + " " + ColorCategory.NICK.command))
                .build();
    }

    /**
     * Registers /badges, /chatcolor and /nickcolor, shortcuts to their /cosmetics subcommands.
     */
    public void registerShortcuts(Commands registrar) {
        registrar.register(badgeLiteral(BADGES, "/" + BADGES).build(),
                messages.raw(Keys.Message.COMMAND_BADGES_DESCRIPTION), List.of("insignias"));
        for (ColorCategory category : ColorCategory.values()) {
            registrar.register(colorLiteral(category, "/" + category.command).build(),
                    messages.raw(category.commandDescription), List.of(category.alias));
        }
    }

    public List<HelpEntry> helpEntries() {
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + NAME,
                        null, Keys.Message.HELP_COSMETICS_DESCRIPTION, null, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + NAME + " " + JOIN_MESSAGE,
                        Keys.Message.HELP_COSMETICS_JOIN_MESSAGE_ARGUMENTS,
                        Keys.Message.HELP_COSMETICS_JOIN_MESSAGE_DESCRIPTION, null, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + NAME + " " + KILL_MESSAGE,
                        Keys.Message.HELP_COSMETICS_KILL_MESSAGE_ARGUMENTS,
                        Keys.Message.HELP_COSMETICS_KILL_MESSAGE_DESCRIPTION, null, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + NAME + " " + TELEPORT_PARTICLE,
                        Keys.Message.HELP_COSMETICS_TELEPORT_PARTICLE_ARGUMENTS,
                        Keys.Message.HELP_COSMETICS_TELEPORT_PARTICLE_DESCRIPTION, null, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + NAME + " " + AURA,
                        Keys.Message.HELP_COSMETICS_AURA_ARGUMENTS,
                        Keys.Message.HELP_COSMETICS_AURA_DESCRIPTION, null, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + NAME + " " + JOIN_EFFECT,
                        Keys.Message.HELP_COSMETICS_JOIN_EFFECT_ARGUMENTS,
                        Keys.Message.HELP_COSMETICS_JOIN_EFFECT_DESCRIPTION, null, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + BADGES,
                        Keys.Message.HELP_BADGES_ARGUMENTS,
                        Keys.Message.HELP_BADGES_DESCRIPTION, null, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + BADGES + " " + CUSTOM,
                        Keys.Message.HELP_BADGES_CUSTOM_ARGUMENTS,
                        Keys.Message.HELP_BADGES_CUSTOM_DESCRIPTION, null, module),
                colorHelpEntry(ColorCategory.CHAT),
                colorHelpEntry(ColorCategory.NICK));
    }

    // The custom literal wins over the badge id argument, so "custom" cannot be a badge id (see CosmeticsModule).
    private LiteralArgumentBuilder<CommandSourceStack> badgeLiteral(String literal, String command) {
        return Commands.literal(literal)
                .executes(this::openBadges)
                .then(Commands.literal(CUSTOM)
                        .executes(context -> equipCustomBadge(context, command + " " + CUSTOM))
                        .then(Commands.argument(TEXT_ARGUMENT, StringArgumentType.greedyString())
                                .executes(this::createCustomBadge)))
                .then(Commands.argument(ARGUMENT, StringArgumentType.word())
                        .suggests((context, builder) -> {
                            if (module.isEnabled() && context.getSource().getSender() instanceof Player player) {
                                module.messageCosmetics(MessageCategory.BADGE).usable(player)
                                        .forEach(badge -> builder.suggest(badge.id()));
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> selectMessage(context, MessageCategory.BADGE)));
    }

    private int openHub(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        player.openInventory(loader.load(configs.cosmeticsGui().get(), player).getInventory());
        return Command.SINGLE_SUCCESS;
    }

    private int openBadges(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        badgesMenu.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private int equipCustomBadge(CommandContext<CommandSourceStack> context, String command) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        if (!player.hasPermission(Keys.Permission.COSMETICS_CUSTOM_BADGE)) {
            messages.send(player, Keys.Message.COSMETICS_BADGE_CUSTOM_LOCKED);
            return 0;
        }
        var text = module.customBadge(player);
        if (text.isEmpty()) {
            return CommandUsage.send(messages, player, command, Keys.Message.HELP_BADGES_CUSTOM_ARGUMENTS);
        }
        return saveCustomBadge(player, module.selections()
                .select(player.getUniqueId(), MessageCategory.BADGE.slot, CosmeticsModule.CUSTOM_BADGE_ID), text.get());
    }

    private int createCustomBadge(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        if (!player.hasPermission(Keys.Permission.COSMETICS_CUSTOM_BADGE)) {
            messages.send(player, Keys.Message.COSMETICS_BADGE_CUSTOM_LOCKED);
            return 0;
        }
        String text = StringArgumentType.getString(context, TEXT_ARGUMENT).strip();
        int max = module.customBadgeMaxLength();
        int length = CosmeticsModule.visibleLength(text);
        if (length == 0 || length > max || text.length() > CosmeticsModule.MAX_STORED_LENGTH) {
            messages.send(player, Keys.Message.COSMETICS_BADGE_CUSTOM_INVALID, Placeholder.unparsed("max", String.valueOf(max)));
            return 0;
        }
        var selections = module.selections();
        return saveCustomBadge(player, selections.select(player.getUniqueId(), CosmeticsModule.CUSTOM_BADGE_SLOT, text)
                .thenCompose(ignored -> selections.select(player.getUniqueId(), MessageCategory.BADGE.slot,
                        CosmeticsModule.CUSTOM_BADGE_ID)), text);
    }

    private int saveCustomBadge(Player player, CompletableFuture<Void> save, String text) {
        var badge = Placeholder.component("custom_badge", module.withSeparator(CosmeticsModule.customBadgeContent(text)));
        save.thenRun(() -> messages.send(player, Keys.Message.COSMETICS_BADGE_CUSTOM_SELECTED, badge))
                .exceptionally(e -> {
                    messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                    return null;
                });
        return Command.SINGLE_SUCCESS;
    }

    private HelpEntry colorHelpEntry(ColorCategory category) {
        return new HelpEntry(HelpEntry.Category.PLAYER, "/" + category.command,
                category.helpArguments, category.helpDescription, null, module);
    }

    // Greedy because word() rejects the # of hex colors; only the first word is used.
    private LiteralArgumentBuilder<CommandSourceStack> colorLiteral(ColorCategory category, String command) {
        return Commands.literal(category.command)
                .executes(context -> openColors(context, category))
                .then(Commands.argument(COLOR_ARGUMENT, StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            if (module.isEnabled() && context.getSource().getSender() instanceof Player player) {
                                module.colors(category).usable(player).forEach(color -> builder.suggest(color.id()));
                                if (player.hasPermission(category.customPermission)) {
                                    builder.suggest("#RRGGBB");
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> selectColor(context, category, command)));
    }

    private int openColors(CommandContext<CommandSourceStack> context, ColorCategory category) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        colorMenus.get(category).open(player);
        return Command.SINGLE_SUCCESS;
    }

    private int selectColor(CommandContext<CommandSourceStack> context, ColorCategory category, String command) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        String input = StringArgumentType.getString(context, COLOR_ARGUMENT).strip().split("\\s+", 2)[0];
        if (input.isEmpty()) {
            return CommandUsage.send(messages, player, command, category.helpArguments);
        }
        var color = module.colors(category).find(input);
        if (color.isPresent()) {
            var name = Placeholder.parsed("cosmetic", color.get().name());
            if (!player.hasPermission(category.permission + input)) {
                messages.send(player, category.lockedMessage, name);
                return 0;
            }
            return selectColor(player, category, input, category.selectedMessage, name);
        }
        var custom = ColorCosmetics.parseCustom(input);
        if (custom.isEmpty()) {
            messages.send(player, category.notFoundMessage, Placeholder.unparsed("id", input));
            return 0;
        }
        if (!player.hasPermission(category.customPermission)) {
            messages.send(player, category.customLockedMessage);
            return 0;
        }
        String hex = custom.get();
        return selectColor(player, category, hex, category.customSelectedMessage,
                Placeholder.component("color", MiniMessage.miniMessage().deserialize("<" + hex + ">" + hex)));
    }

    private int selectColor(Player player, ColorCategory category, String id, String selectedMessage, TagResolver resolver) {
        module.selections().select(player.getUniqueId(), category.slot, id)
                .thenRun(() -> messages.send(player, selectedMessage, resolver))
                .exceptionally(e -> {
                    messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                    return null;
                });
        return Command.SINGLE_SUCCESS;
    }

    private LiteralArgumentBuilder<CommandSourceStack> messageLiteral(String literal, MessageCosmeticsMenu menu,
                                                                      MessageCategory category) {
        return Commands.literal(literal)
                .executes(context -> open(context, menu))
                .then(Commands.argument(ARGUMENT, StringArgumentType.word())
                        .suggests((context, builder) -> {
                            if (module.isEnabled() && context.getSource().getSender() instanceof Player player) {
                                module.messageCosmetics(category).usable(player)
                                        .forEach(message -> builder.suggest(message.id()));
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> selectMessage(context, category)));
    }

    private int open(CommandContext<CommandSourceStack> context, MessageCosmeticsMenu menu) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        menu.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private int selectMessage(CommandContext<CommandSourceStack> context, MessageCategory category) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(context, ARGUMENT);
        var message = module.messageCosmetics(category).find(id);
        if (message.isEmpty()) {
            messages.send(player, category.notFoundMessage, Placeholder.unparsed("id", id));
            return 0;
        }
        var name = Placeholder.parsed("cosmetic", message.get().name());
        if (!player.hasPermission(category.permission + id)) {
            messages.send(player, category.lockedMessage, name);
            return 0;
        }
        module.selections().select(player.getUniqueId(), category.slot, id)
                .thenRun(() -> messages.send(player, category.selectedMessage, name))
                .exceptionally(e -> {
                    messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                    return null;
                });
        return Command.SINGLE_SUCCESS;
    }

    private int openTeleportParticles(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        particlesMenu.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private int selectTeleportParticle(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(context, ARGUMENT);
        var particle = module.teleportParticles().find(id);
        if (particle.isEmpty()) {
            messages.send(player, Keys.Message.COSMETICS_TELEPORT_PARTICLE_NOT_FOUND, Placeholder.unparsed("id", id));
            return 0;
        }
        var name = Placeholder.parsed("cosmetic", particle.get().name());
        if (!player.hasPermission(Keys.Permission.COSMETICS_TELEPORT_PARTICLE + id)) {
            messages.send(player, Keys.Message.COSMETICS_TELEPORT_PARTICLE_LOCKED, name);
            return 0;
        }
        module.selections().select(player.getUniqueId(), CosmeticsModule.TELEPORT_PARTICLE_SLOT, id)
                .thenRun(() -> messages.send(player, Keys.Message.COSMETICS_TELEPORT_PARTICLE_SELECTED, name))
                .exceptionally(e -> {
                    messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                    return null;
                });
        return Command.SINGLE_SUCCESS;
    }

    private int openAuras(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        aurasMenu.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private int selectAura(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(context, ARGUMENT);
        var aura = module.auras().find(id);
        if (aura.isEmpty()) {
            messages.send(player, Keys.Message.COSMETICS_AURA_NOT_FOUND, Placeholder.unparsed("id", id));
            return 0;
        }
        var name = Placeholder.parsed("cosmetic", aura.get().name());
        if (!player.hasPermission(Keys.Permission.COSMETICS_AURA + id)) {
            messages.send(player, Keys.Message.COSMETICS_AURA_LOCKED, name);
            return 0;
        }
        module.selections().select(player.getUniqueId(), CosmeticsModule.AURA_SLOT, id)
                .thenRun(() -> messages.send(player, Keys.Message.COSMETICS_AURA_SELECTED, name))
                .exceptionally(e -> {
                    messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                    return null;
                });
        return Command.SINGLE_SUCCESS;
    }

    private int openJoinEffects(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        joinEffectsMenu.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private int selectJoinEffect(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(context, ARGUMENT);
        var effect = module.joinEffects().find(id);
        if (effect.isEmpty()) {
            messages.send(player, Keys.Message.COSMETICS_JOIN_EFFECT_NOT_FOUND, Placeholder.unparsed("id", id));
            return 0;
        }
        var name = Placeholder.parsed("cosmetic", effect.get().name());
        if (!player.hasPermission(Keys.Permission.COSMETICS_JOIN_EFFECT + id)) {
            messages.send(player, Keys.Message.COSMETICS_JOIN_EFFECT_LOCKED, name);
            return 0;
        }
        module.selections().select(player.getUniqueId(), CosmeticsModule.JOIN_EFFECT_SLOT, id)
                .thenRun(() -> messages.send(player, Keys.Message.COSMETICS_JOIN_EFFECT_SELECTED, name))
                .exceptionally(e -> {
                    messages.send(player, Keys.Message.COSMETICS_UNAVAILABLE);
                    return null;
                });
        return Command.SINGLE_SUCCESS;
    }

    // Checked here instead of requires() so the sender gets the Spanish message rather than "unknown command".
    private Player requirePlayer(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!module.isEnabled()) {
            messages.send(sender, Keys.Message.GENERAL_MODULE_DISABLED);
            return null;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, Keys.Message.GENERAL_PLAYER_ONLY);
            return null;
        }
        return player;
    }
}
