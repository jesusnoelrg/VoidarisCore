package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import club.voidaris.core.message.PlayerFormatter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

public final class CosmeticsCommand {

    private static final String NAME = "cosmetics";
    private static final String JOIN_MESSAGE = "joinmessage";
    private static final String ARGUMENT = "cosmetic";

    private final CosmeticsModule module;
    private final Messages messages;
    private final PlayerFormatter formatter;

    public CosmeticsCommand(CosmeticsModule module, Messages messages, PlayerFormatter formatter) {
        this.module = module;
        this.messages = messages;
        this.formatter = formatter;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(NAME)
                .then(Commands.literal(JOIN_MESSAGE)
                        .executes(this::listJoinMessages)
                        .then(Commands.argument(ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    if (module.isEnabled() && context.getSource().getSender() instanceof Player player) {
                                        module.joinMessages().usable(player).forEach(message -> builder.suggest(message.id()));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(this::selectJoinMessage)))
                .build();
    }

    private int listJoinMessages(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        var owned = module.joinMessages().usable(player);
        if (owned.isEmpty()) {
            messages.send(player, Keys.Message.COSMETICS_JOIN_MESSAGE_LIST_EMPTY);
            return Command.SINGLE_SUCCESS;
        }
        String selected = module.selections().get(player.getUniqueId(), CosmeticsModule.JOIN_MESSAGE_SLOT)
                .orElse(module.joinMessages().defaultSelection());
        var entries = owned.stream().map(message -> entry(player, message, message.id().equals(selected))).toList();
        messages.send(player, Keys.Message.COSMETICS_JOIN_MESSAGE_LIST,
                Placeholder.component("cosmetics", Component.join(JoinConfiguration.spaces(), entries)));
        return Command.SINGLE_SUCCESS;
    }

    private Component entry(Player player, JoinMessage message, boolean selected) {
        String key = selected ? Keys.Message.COSMETICS_JOIN_MESSAGE_LIST_ENTRY_SELECTED : Keys.Message.COSMETICS_JOIN_MESSAGE_LIST_ENTRY;
        Component preview = message.format().isBlank()
                ? messages.get(Keys.Message.COSMETICS_JOIN_MESSAGE_PREVIEW_HIDDEN)
                : formatter.format(player, message.format());
        return messages.get(key, Placeholder.parsed("cosmetic", message.name()))
                .hoverEvent(HoverEvent.showText(preview))
                .clickEvent(ClickEvent.runCommand("/" + NAME + " " + JOIN_MESSAGE + " " + message.id()));
    }

    private int selectJoinMessage(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(context, ARGUMENT);
        var message = module.joinMessages().find(id);
        if (message.isEmpty()) {
            messages.send(player, Keys.Message.COSMETICS_JOIN_MESSAGE_NOT_FOUND, Placeholder.unparsed("id", id));
            return 0;
        }
        var name = Placeholder.parsed("cosmetic", message.get().name());
        if (!player.hasPermission(Keys.Permission.COSMETICS_JOIN_MESSAGE + id)) {
            messages.send(player, Keys.Message.COSMETICS_JOIN_MESSAGE_LOCKED, name);
            return 0;
        }
        module.selections().select(player.getUniqueId(), CosmeticsModule.JOIN_MESSAGE_SLOT, id)
                .thenRun(() -> messages.send(player, Keys.Message.COSMETICS_JOIN_MESSAGE_SELECTED, name))
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
