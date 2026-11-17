package club.voidaris.core.announcements;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

public final class AnnouncementsCommand {

    private static final String ANNOUNCE_TOGGLE = "announcetoggle";

    private final AnnouncementsModule module;

    public AnnouncementsCommand(AnnouncementsModule module) {
        this.module = module;
    }

    public void register(Commands registrar) {
        registrar.register(Commands.literal(ANNOUNCE_TOGGLE).executes(this::toggleAnnounces).build(),
                module.messages().raw(Keys.Message.COMMAND_ANNOUNCETOGGLE_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(new HelpEntry(HelpEntry.Category.PLAYER, "/" + ANNOUNCE_TOGGLE, null,
                Keys.Message.HELP_ANNOUNCETOGGLE_DESCRIPTION, Keys.Permission.ANNOUNCEMENTS_TOGGLE, module));
    }

    private int toggleAnnounces(CommandContext<CommandSourceStack> context) {
        Player player = module.requirePlayer(context.getSource().getSender(), Keys.Permission.ANNOUNCEMENTS_TOGGLE);
        if (player == null) {
            return 0;
        }
        boolean off = !module.store().disabled(player.getUniqueId());
        module.store().disabled(player.getUniqueId(), off);
        module.messages().send(player, off
                ? Keys.Message.ANNOUNCEMENTS_DISABLED_SELF
                : Keys.Message.ANNOUNCEMENTS_ENABLED_SELF);
        return Command.SINGLE_SUCCESS;
    }
}
