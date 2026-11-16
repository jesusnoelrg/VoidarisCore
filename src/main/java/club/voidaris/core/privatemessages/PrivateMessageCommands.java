package club.voidaris.core.privatemessages;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class PrivateMessageCommands {

    private static final String MESSAGE = "message";
    private static final String REPLY = "reply";
    private static final String SPY = "spy";
    private static final String TARGET = "player";
    private static final String TEXT = "text";

    private final PrivateMessagesModule module;

    public PrivateMessageCommands(PrivateMessagesModule module) {
        this.module = module;
    }

    public void register(Commands registrar) {
        var messages = module.messages();
        registrar.register(Commands.literal(MESSAGE)
                        .executes(this::messageUsage)
                        .then(onlinePlayerArgument(TARGET)
                                .executes(this::messageUsage)
                                .then(Commands.argument(TEXT, StringArgumentType.greedyString()).executes(this::message)))
                        .build(),
                messages.raw(Keys.Message.COMMAND_MESSAGE_DESCRIPTION), List.of("msg", "tell"));
        registrar.register(Commands.literal(REPLY)
                        .executes(context -> module.usage(context.getSource().getSender(),
                                Keys.Permission.PRIVATE_MESSAGES_MESSAGE, "/" + REPLY, Keys.Message.HELP_REPLY_ARGUMENTS))
                        .then(Commands.argument(TEXT, StringArgumentType.greedyString()).executes(this::reply))
                        .build(),
                messages.raw(Keys.Message.COMMAND_REPLY_DESCRIPTION), List.of("r"));
        registrar.register(Commands.literal(SPY).executes(this::spy).build(),
                messages.raw(Keys.Message.COMMAND_SPY_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + MESSAGE, Keys.Message.HELP_MESSAGE_ARGUMENTS,
                        Keys.Message.HELP_MESSAGE_DESCRIPTION, Keys.Permission.PRIVATE_MESSAGES_MESSAGE, module),
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + REPLY, Keys.Message.HELP_REPLY_ARGUMENTS,
                        Keys.Message.HELP_REPLY_DESCRIPTION, Keys.Permission.PRIVATE_MESSAGES_MESSAGE, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + SPY, null,
                        Keys.Message.HELP_SPY_DESCRIPTION, Keys.Permission.PRIVATE_MESSAGES_SPY, module));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> onlinePlayerArgument(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests((context, builder) -> {
            suggestOnlinePlayers(context.getSource().getSender(), builder);
            return builder.buildFuture();
        });
    }

    static void suggestOnlinePlayers(CommandSender sender, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != sender && (!(sender instanceof Player viewer) || viewer.canSee(online))
                    && online.getName().toLowerCase().startsWith(remaining)) {
                builder.suggest(online.getName());
            }
        }
    }

    private int messageUsage(CommandContext<CommandSourceStack> context) {
        return module.usage(context.getSource().getSender(), Keys.Permission.PRIVATE_MESSAGES_MESSAGE,
                "/" + MESSAGE, Keys.Message.HELP_MESSAGE_ARGUMENTS);
    }

    private int message(CommandContext<CommandSourceStack> context) {
        Player sender = module.requirePlayer(context.getSource().getSender(), Keys.Permission.PRIVATE_MESSAGES_MESSAGE);
        if (sender == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, TARGET);
        Player target = Bukkit.getPlayerExact(name);
        if (target == null || !sender.canSee(target)) {
            module.messages().send(sender, Keys.Message.GENERAL_PLAYER_NOT_FOUND, Placeholder.unparsed("player", name));
            return 0;
        }
        return send(sender, target, StringArgumentType.getString(context, TEXT));
    }

    private int reply(CommandContext<CommandSourceStack> context) {
        Player sender = module.requirePlayer(context.getSource().getSender(), Keys.Permission.PRIVATE_MESSAGES_MESSAGE);
        if (sender == null) {
            return 0;
        }
        var targetId = module.replyTarget(sender);
        if (targetId == null) {
            module.messages().send(sender, Keys.Message.PRIVATE_MESSAGES_NO_REPLY);
            return 0;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !sender.canSee(target)) {
            String name = Bukkit.getOfflinePlayer(targetId).getName();
            module.messages().send(sender, Keys.Message.GENERAL_PLAYER_NOT_FOUND,
                    Placeholder.unparsed("player", name != null ? name : targetId.toString()));
            return 0;
        }
        return send(sender, target, StringArgumentType.getString(context, TEXT));
    }

    private int send(Player sender, Player target, String text) {
        var messages = module.messages();
        if (sender == target) {
            messages.send(sender, Keys.Message.PRIVATE_MESSAGES_SELF);
            return 0;
        }
        // The ignorer is never told: the attempt is silently dropped on their side.
        if (module.store().level(target.getUniqueId(), sender.getUniqueId()) != null) {
            messages.send(sender, Keys.Message.PRIVATE_MESSAGES_BLOCKED, Placeholder.unparsed("player", target.getName()));
            return 0;
        }
        var message = Placeholder.unparsed("message", text);
        messages.send(sender, Keys.Message.PRIVATE_MESSAGES_SENT, message, Placeholder.unparsed("player", target.getName()));
        messages.send(target, Keys.Message.PRIVATE_MESSAGES_RECEIVED, message, Placeholder.unparsed("player", sender.getName()));
        module.recordConversation(sender, target);
        spy(sender, target, text);
        return Command.SINGLE_SUCCESS;
    }

    private void spy(Player sender, Player target, String text) {
        boolean exempt = sender.hasPermission(Keys.Permission.PRIVATE_MESSAGES_SPY_EXEMPT)
                || target.hasPermission(Keys.Permission.PRIVATE_MESSAGES_SPY_EXEMPT);
        var line = module.messages().get(Keys.Message.PRIVATE_MESSAGES_SPY_FORMAT,
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("target", target.getName()),
                Placeholder.unparsed("message", text));
        for (Player spy : Bukkit.getOnlinePlayers()) {
            if (spy == sender || spy == target || !module.store().spying(spy.getUniqueId())
                    || !spy.hasPermission(Keys.Permission.PRIVATE_MESSAGES_SPY)) {
                continue;
            }
            if (!exempt || spy.hasPermission(Keys.Permission.PRIVATE_MESSAGES_SPY_BYPASS)) {
                spy.sendMessage(line);
            }
        }
    }

    private int spy(CommandContext<CommandSourceStack> context) {
        Player player = module.requirePlayer(context.getSource().getSender(), Keys.Permission.PRIVATE_MESSAGES_SPY);
        if (player == null) {
            return 0;
        }
        boolean spying = !module.store().spying(player.getUniqueId());
        module.store().spying(player.getUniqueId(), spying);
        module.messages().send(player, spying
                ? Keys.Message.PRIVATE_MESSAGES_SPY_ENABLED
                : Keys.Message.PRIVATE_MESSAGES_SPY_DISABLED);
        return Command.SINGLE_SUCCESS;
    }
}
