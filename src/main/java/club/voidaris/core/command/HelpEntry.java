package club.voidaris.core.command;

import club.voidaris.core.module.Module;
import org.bukkit.command.CommandSender;

/**
 * A line of {@code /voidaris help}.
 *
 * @param command        the literal typed in chat, such as {@code /cosmetics joinmessage}
 * @param argumentsKey   message key of the arguments shown after the command; when present, clicking the line
 *                       fills the chat instead of running the command
 * @param descriptionKey message key of the description
 * @param permission     node required to see the line, or {@code null} when everyone can
 * @param module         module that must be enabled to see the line, or {@code null} for core commands
 */
public record HelpEntry(
        Category category,
        String command,
        String argumentsKey,
        String descriptionKey,
        String permission,
        Module module
) {

    public enum Category {
        PLAYER,
        ADMIN
    }

    public boolean suggests() {
        return argumentsKey != null;
    }

    boolean visibleTo(CommandSender sender) {
        return (module == null || module.isEnabled()) && (permission == null || sender.hasPermission(permission));
    }
}
