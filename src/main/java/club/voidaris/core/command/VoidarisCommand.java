package club.voidaris.core.command;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class VoidarisCommand {

    private final Runnable reloader;
    private final Messages messages;

    public VoidarisCommand(Runnable reloader, Messages messages) {
        this.reloader = reloader;
        this.messages = messages;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("voidaris")
                .then(Commands.literal("reload").executes(this::reload))
                .build();
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
