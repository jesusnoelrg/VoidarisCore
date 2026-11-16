package club.voidaris.core.command;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;

public final class VoidarisCommand {

    private static final String NAME = "voidaris";
    private static final String HELP = "help";
    private static final String RELOAD = "reload";
    private static final String PAGE = "page";

    private final Runnable reloader;
    private final Messages messages;
    private final HelpMenu help;

    public VoidarisCommand(Runnable reloader, Messages messages) {
        this.reloader = reloader;
        this.messages = messages;
        this.help = new HelpMenu(messages, "/" + NAME + " " + HELP);
        help.register(helpEntries());
    }

    public HelpMenu help() {
        return help;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(NAME)
                .executes(context -> help(context, 1))
                .then(Commands.literal(HELP)
                        .executes(context -> help(context, 1))
                        .then(Commands.argument(PAGE, StringArgumentType.word())
                                .executes(this::helpPage)))
                .then(Commands.literal(RELOAD).executes(this::reload))
                .build();
    }

    private List<HelpEntry> helpEntries() {
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + NAME + " " + HELP,
                        Keys.Message.HELP_VOIDARIS_HELP_ARGUMENTS, Keys.Message.HELP_VOIDARIS_HELP_DESCRIPTION,
                        null, null),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + NAME + " " + RELOAD,
                        null, Keys.Message.HELP_VOIDARIS_RELOAD_DESCRIPTION,
                        Keys.Permission.CORE_RELOAD, null));
    }

    private int helpPage(CommandContext<CommandSourceStack> context) {
        Integer page = CommandUsage.parseInt(StringArgumentType.getString(context, PAGE), 1, Integer.MAX_VALUE);
        if (page == null) {
            return CommandUsage.send(messages, context.getSource().getSender(), "/" + NAME + " " + HELP,
                    Keys.Message.HELP_VOIDARIS_HELP_ARGUMENTS);
        }
        return help(context, page);
    }

    private int help(CommandContext<CommandSourceStack> context, int page) {
        return help.send(context.getSource().getSender(), page) ? Command.SINGLE_SUCCESS : 0;
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        // Checked here instead of requires() so the player gets the Spanish message rather than "unknown command".
        if (!sender.hasPermission(Keys.Permission.CORE_RELOAD)) {
            messages.send(sender, Keys.Message.GENERAL_NO_PERMISSION);
            return 0;
        }
        reloader.run();
        messages.send(sender, Keys.Message.GENERAL_RELOADED);
        return Command.SINGLE_SUCCESS;
    }
}
