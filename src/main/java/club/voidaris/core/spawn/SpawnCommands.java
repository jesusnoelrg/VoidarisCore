package club.voidaris.core.spawn;

import club.voidaris.core.Keys;
import club.voidaris.core.command.HelpEntry;
import club.voidaris.core.message.Messages;
import club.voidaris.core.storage.StoredLocation;
import club.voidaris.core.teleport.Teleporter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

public final class SpawnCommands {

    private static final String SPAWN = "spawn";
    private static final String SET_SPAWN = "setspawn";
    private static final String DEL_SPAWN = "delspawn";

    private final SpawnModule module;
    private final Messages messages;
    private final Teleporter teleporter;

    public SpawnCommands(SpawnModule module, Messages messages, Teleporter teleporter) {
        this.module = module;
        this.messages = messages;
        this.teleporter = teleporter;
    }

    public void register(Commands registrar) {
        registrar.register(Commands.literal(SPAWN).executes(this::spawn).build(),
                messages.raw(Keys.Message.COMMAND_SPAWN_DESCRIPTION));
        registrar.register(Commands.literal(SET_SPAWN).executes(this::setSpawn).build(),
                messages.raw(Keys.Message.COMMAND_SETSPAWN_DESCRIPTION));
        registrar.register(Commands.literal(DEL_SPAWN).executes(this::deleteSpawn).build(),
                messages.raw(Keys.Message.COMMAND_DELSPAWN_DESCRIPTION));
    }

    public List<HelpEntry> helpEntries() {
        return List.of(
                new HelpEntry(HelpEntry.Category.PLAYER, "/" + SPAWN,
                        null, Keys.Message.HELP_SPAWN_DESCRIPTION, Keys.Permission.SPAWN_USE, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + SET_SPAWN,
                        null, Keys.Message.HELP_SETSPAWN_DESCRIPTION, Keys.Permission.SPAWN_SET, module),
                new HelpEntry(HelpEntry.Category.ADMIN, "/" + DEL_SPAWN,
                        null, Keys.Message.HELP_DELSPAWN_DESCRIPTION, Keys.Permission.SPAWN_DELETE, module));
    }

    private int spawn(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context, Keys.Permission.SPAWN_USE);
        if (player == null) {
            return 0;
        }
        var stored = module.spawn();
        if (stored.isEmpty()) {
            messages.send(player, Keys.Message.SPAWN_NOT_SET);
            return 0;
        }
        var target = stored.get().toLocation();
        if (target.isEmpty()) {
            messages.send(player, Keys.Message.TELEPORT_WORLD_UNAVAILABLE);
            return 0;
        }
        return teleporter.teleport(player, SPAWN, target.get(), module.teleportSettings(),
                Keys.Message.SPAWN_TELEPORTED) ? Command.SINGLE_SUCCESS : 0;
    }

    private int setSpawn(CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context, Keys.Permission.SPAWN_SET);
        if (player == null) {
            return 0;
        }
        module.setSpawn(StoredLocation.of(player.getLocation()));
        messages.send(player, Keys.Message.SPAWN_SET);
        return Command.SINGLE_SUCCESS;
    }

    private int deleteSpawn(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        if (!checkAccess(context, Keys.Permission.SPAWN_DELETE)) {
            return 0;
        }
        if (!module.deleteSpawn()) {
            messages.send(sender, Keys.Message.SPAWN_NOT_SET);
            return 0;
        }
        messages.send(sender, Keys.Message.SPAWN_DELETED);
        return Command.SINGLE_SUCCESS;
    }

    private Player requirePlayer(CommandContext<CommandSourceStack> context, String permission) {
        if (!checkAccess(context, permission)) {
            return null;
        }
        if (!(context.getSource().getSender() instanceof Player player)) {
            messages.send(context.getSource().getSender(), Keys.Message.GENERAL_PLAYER_ONLY);
            return null;
        }
        return player;
    }

    // Checked here instead of requires() so the sender gets the Spanish message rather than "unknown command".
    private boolean checkAccess(CommandContext<CommandSourceStack> context, String permission) {
        var sender = context.getSource().getSender();
        if (!module.isEnabled()) {
            messages.send(sender, Keys.Message.GENERAL_MODULE_DISABLED);
            return false;
        }
        if (!sender.hasPermission(permission)) {
            messages.send(sender, Keys.Message.GENERAL_NO_PERMISSION);
            return false;
        }
        return true;
    }
}
