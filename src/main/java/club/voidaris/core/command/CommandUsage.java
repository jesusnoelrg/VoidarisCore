package club.voidaris.core.command;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/**
 * Brigadier answers a missing or unparsable argument with the client's English "Unknown or incomplete command",
 * so every node that can be left incomplete executes this instead and typed arguments are parsed by hand.
 */
public final class CommandUsage {

    private CommandUsage() {
    }

    /**
     * @param command      the literal typed in chat, such as {@code /message}
     * @param argumentsKey message key of the expected arguments (the same one shown in {@code /voidaris help})
     * @return 0, so it can be returned as the command result
     */
    public static int send(Messages messages, CommandSender sender, String command, String argumentsKey) {
        sender.sendMessage(messages.get(Keys.Message.GENERAL_USAGE,
                        Placeholder.unparsed("command", command),
                        Placeholder.component("arguments", messages.get(argumentsKey)))
                .clickEvent(ClickEvent.suggestCommand(command + " "))
                .hoverEvent(HoverEvent.showText(messages.get(Keys.Message.HELP_HOVER_SUGGEST))));
        return 0;
    }

    /**
     * @return the number, or {@code null} when the text is not an integer within the bounds
     */
    public static Integer parseInt(String text, int min, int max) {
        try {
            int value = Integer.parseInt(text);
            return value >= min && value <= max ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
