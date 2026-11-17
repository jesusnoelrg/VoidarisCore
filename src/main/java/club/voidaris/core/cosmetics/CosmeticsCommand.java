package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
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
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class CosmeticsCommand {

    private static final String NAME = "cosmetics";
    private static final String JOIN_MESSAGE = "joinmessage";
    private static final String KILL_MESSAGE = "killmessage";
    private static final String TELEPORT_PARTICLE = "teleportparticle";
    private static final String AURA = "aura";
    private static final String ARGUMENT = "cosmetic";

    private final CosmeticsModule module;
    private final Messages messages;
    private final MessageCosmeticsMenu joinMessagesMenu;
    private final MessageCosmeticsMenu killMessagesMenu;
    private final TeleportParticlesMenu particlesMenu;
    private final AurasMenu aurasMenu;

    public CosmeticsCommand(JavaPlugin plugin, ConfigManager configs, CosmeticsModule module, Messages messages,
                            PlayerFormatter formatter, MenuLoader loader) {
        this.module = module;
        this.messages = messages;
        this.joinMessagesMenu = new MessageCosmeticsMenu(plugin, configs, module, messages, loader,
                MessageCategory.JOIN, formatter::format);
        // The preview shows the player as the killer of a sample victim, with the item they are holding.
        this.killMessagesMenu = new MessageCosmeticsMenu(plugin, configs, module, messages, loader,
                MessageCategory.KILL, (player, format) -> formatter.format(player, format,
                        KillMessagePlaceholders.of(messages, player,
                                messages.get(Keys.Message.COSMETICS_KILL_MESSAGE_PREVIEW_VICTIM))));
        this.particlesMenu = new TeleportParticlesMenu(plugin, configs, module, messages, loader);
        this.aurasMenu = new AurasMenu(plugin, configs, module, messages, loader);
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(NAME)
                .executes(context -> open(context, joinMessagesMenu))
                .then(messageLiteral(JOIN_MESSAGE, joinMessagesMenu, MessageCategory.JOIN))
                .then(messageLiteral(KILL_MESSAGE, killMessagesMenu, MessageCategory.KILL))
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
                .build();
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
                        Keys.Message.HELP_COSMETICS_AURA_DESCRIPTION, null, module));
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
