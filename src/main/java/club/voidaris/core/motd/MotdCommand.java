package club.voidaris.core.motd;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

public final class MotdCommand {

    private static final String MOTD = "motd";

    private final MotdModule module;
    private final Messages messages;

    public MotdCommand(MotdModule module, Messages messages) {
        this.module = module;
        this.messages = messages;
    }

    public void register(Commands registrar) {
        registrar.register(Commands.literal(MOTD).executes(this::motd).build(),
                messages.raw(Keys.Message.COMMAND_MOTD_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(new HelpEntry(HelpEntry.Category.PLAYER, "/" + MOTD,
                null, Keys.Message.HELP_MOTD_DESCRIPTION, Keys.Permission.MOTD_USE, module));
    }

    private int motd(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        // Checked here instead of requires() so the sender gets the Spanish message rather than "unknown command".
        if (!module.isEnabled()) {
            messages.send(sender, Keys.Message.GENERAL_MODULE_DISABLED);
            return 0;
        }
        if (!sender.hasPermission(Keys.Permission.MOTD_USE)) {
            messages.send(sender, Keys.Message.GENERAL_NO_PERMISSION);
            return 0;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, Keys.Message.GENERAL_PLAYER_ONLY);
            return 0;
        }
        if (!module.send(player)) {
            messages.send(player, Keys.Message.MOTD_EMPTY);
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }
}
